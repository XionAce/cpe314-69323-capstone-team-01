package edu.uc.campusmarket.service;

import edu.uc.campusmarket.TestDatabase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesHistoryImporterTest {
    @TempDir
    Path folder;
    private TestDatabase db;
    private SalesHistoryImporter importer;

    private String sale(String id, String price) {
        return "{\"sourceRecordId\":\"" + id + "\",\"categoryPath\":[\"All finds\",\"Textbooks\"],"
                + "\"price\":" + price
                + ",\"currency\":\"PHP\",\"soldAt\":\"2026-10-20T08:00:00Z\"}";
    }

    @BeforeEach
    void start() throws Exception {
        db = new TestDatabase(folder.resolve("test.db"));
        db.initialize();
        importer = db.importer();
    }

    private Path file(String content) throws Exception {
        return Files.writeString(folder.resolve("sales.json"), content);
    }

    private long count() throws Exception {
        try (var c = db.open();
                var q = c.prepareStatement("SELECT COUNT(*) FROM HistoricalSales");
                var r = q.executeQuery()) {
            r.next();
            return r.getLong(1);
        }
    }

    @Test
    void progressCountsEveryValidInvalidAndDuplicateRecord() throws Exception {
        var progress = new java.util.ArrayList<Long>();
        var result = importer.importFile(file("[" + sale("first", "1") + "," + sale("invalid", "0")
                + "," + sale("first", "2") + "]"), () -> false, progress::add);
        assertEquals(java.util.List.of(1L, 2L, 3L), progress);
        assertEquals(1, result.saved());
        assertEquals(1, result.invalid());
        assertEquals(1, result.duplicates());
    }

    @Test
    void validationMessagesAreLimitedWhileEveryInvalidRowIsCounted() throws Exception {
        String rows = java.util.stream.IntStream.range(0, 8).mapToObj(i -> sale("bad-" + i, "-1"))
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        var result = importer.importFile(file(rows), () -> false, n -> {
        });
        assertEquals(8, result.invalid());
        assertEquals(5, result.errors().size());
        assertEquals(0, count());
    }

    @Test
    void cancellationBeforeTheFirstRowPreservesAlreadySavedSales() throws Exception {
        importer.importFile(file("[" + sale("earlier", "10") + "]"), () -> false, n -> {
        });
        Path next = file("[" + sale("later", "20") + "]");
        assertThrows(CancellationException.class, () -> importer.importFile(next, () -> true, n -> {
        }));
        assertEquals(1, count());
        assertTrue(db.historyPreview().contains("earlier"));
        assertFalse(db.historyPreview().contains("later"));
    }

    @Test
    void missingInputFileLeavesExistingImportsIntact() throws Exception {
        importer.importFile(file("[" + sale("earlier", "10") + "]"), () -> false, n -> {
        });
        assertThrows(java.io.IOException.class,
                () -> importer.importFile(folder.resolve("missing.json"), () -> false, n -> {
                }));
        assertEquals(1, count());
    }

    @Test
    void anAmbiguousCategoryPathIsSkippedWithoutChoosingArbitrarily() throws Exception {
        try (var connection = db.open();
                var statement = connection.prepareStatement(
                        "INSERT INTO Categories(id, parentId, name) VALUES (99, 1, 'Textbooks')")) {
            statement.executeUpdate();
        }
        var result = importer.importFile(file("[" + sale("ambiguous", "10") + "]"), () -> false,
                n -> {
                });
        assertEquals(1, result.invalid());
        assertEquals(0, count());
    }

    @Test
    void stringPricesLowercaseCurrencyAndTimezoneFreeDatesAreInvalid() throws Exception {
        String valid = sale("one", "10");
        String rows = "[" + valid.replace("\"price\":10", "\"price\":\"10\"") + ","
                + valid.replace("PHP", "php") + ","
                + valid.replace("2026-10-20T08:00:00Z", "2026-10-20T08:00:00") + "]";
        var result = importer.importFile(file(rows), () -> false, n -> {
        });
        assertEquals(3, result.invalid());
        assertEquals(0, count());
    }

    @Test
    void savesExactMinorUnitsAndTheProvidedTimestamp() throws Exception {
        importer.importFile(file("[" + sale("decimal", "19.99") + "]"), () -> false, n -> {
        });
        try (var connection = db.open();
                var statement = connection.prepareStatement(
                        "SELECT priceMinor, soldAt FROM HistoricalSales WHERE sourceRecordId = 'decimal'");
                var result = statement.executeQuery()) {
            assertTrue(result.next());
            assertEquals(1999, result.getLong("priceMinor"));
            assertEquals("2026-10-20T08:00:00Z", result.getString("soldAt"));
        }
    }

    @Test
    void savesValidRowsSkipsInvalidAndRejectsReimportDuplicates() throws Exception {
        Path path = file("[" + sale("one", "250.00") + "," + sale("bad", "-1") + ","
                + sale("one", "250") + "]");
        var result = importer.importFile(path, () -> false, n -> {
        });
        assertEquals(1, result.saved());
        assertEquals(1, result.invalid());
        assertEquals(1, result.duplicates());
        assertEquals(0, importer.importFile(path, () -> false, n -> {
        }).saved());
        assertEquals(1, count());
        assertTrue(db.historyPreview().contains("PHP 250.00"));
    }

    @Test
    void malformedDocumentRollsBackEvenPreviouslyReadRows() throws Exception {
        assertThrows(Exception.class,
                () -> importer.importFile(file("[" + sale("one", "250") + ","), () -> false, n -> {
                }));
        assertEquals(0, count());
        assertThrows(Exception.class, () -> importer.importFile(file("[] {}"), () -> false, n -> {
        }));
    }

    @Test
    void cancellationAfterReadingARowRollsBackTheImport() throws Exception {
        AtomicBoolean cancel = new AtomicBoolean();
        Path path = file("[" + sale("one", "250") + "]");
        assertThrows(CancellationException.class,
                () -> importer.importFile(path, cancel::get, n -> cancel.set(true)));
        assertEquals(0, count());
    }

    @Test
    void rejectsUnknownCategoryBadTimestampAndFractionalCent() throws Exception {
        String valid = sale("one", "1");
        var result = importer.importFile(file("[" + valid.replace("Textbooks", "Unknown") + ","
                + valid.replace("2026-10-20T08:00:00Z", "yesterday") + "," + sale("tiny", "0.001")
                + "]"), () -> false, n -> {
                });
        assertEquals(3, result.invalid());
        assertEquals(0, result.saved());
        assertEquals(0, count());
        assertEquals(0, importer.importFile(file("[]"), () -> false, n -> {
        }).saved());
    }

    @Test
    void databaseFailureRollsBackNewRowsButPreservesEarlierImports() throws Exception {
        importer.importFile(file("[" + sale("existing", "100") + "]"), () -> false, n -> {
        });
        try (var connection = db.open(); var trigger = connection.prepareStatement("""
                CREATE TRIGGER reject_demo_sale BEFORE INSERT ON HistoricalSales
                WHEN NEW.sourceRecordId = 'fail' BEGIN SELECT RAISE(ABORT, 'test failure'); END
                """)) {
            trigger.execute();
        }
        assertThrows(java.io.IOException.class,
                () -> importer.importFile(
                        file("[" + sale("new", "200") + "," + sale("fail", "300") + "]"),
                        () -> false, n -> {
                        }));
        assertEquals(1, count());
        assertTrue(db.historyPreview().contains("existing"));
        assertFalse(db.historyPreview().contains("new |"));
    }
}
