package edu.uc.campusmarket.service;

import edu.uc.campusmarket.persistence.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class SalesHistoryImporterTest {
    @TempDir Path folder;
    private Database db;
    private SalesHistoryImporter importer;
    private String sale(String id, String price) {
        return "{\"sourceRecordId\":\"" + id + "\",\"categoryPath\":[\"All finds\",\"Textbooks\"],"
                + "\"price\":" + price + ",\"currency\":\"PHP\",\"soldAt\":\"2026-10-20T08:00:00Z\"}";
    }
    @BeforeEach void start() throws Exception {
        db = new Database(folder.resolve("test.db")); db.initialize(); importer = new SalesHistoryImporter(db);
    }
    private Path file(String content) throws Exception { return Files.writeString(folder.resolve("sales.json"), content); }
    private long count() throws Exception {
        try (var c = db.open(); var q = c.prepareStatement("SELECT COUNT(*) FROM HistoricalSales"); var r = q.executeQuery()) {
            r.next(); return r.getLong(1);
        }
    }

    @Test void savesValidRowsSkipsInvalidAndRejectsReimportDuplicates() throws Exception {
        Path path = file("[" + sale("one", "250.00") + "," + sale("bad", "-1") + "," + sale("one", "250") + "]");
        var result = importer.importFile(path, () -> false, n -> {});
        assertEquals(1, result.saved()); assertEquals(1, result.invalid()); assertEquals(1, result.duplicates());
        assertEquals(0, importer.importFile(path, () -> false, n -> {}).saved());
        assertEquals(1, count());
        assertTrue(db.historyPreview().contains("PHP 250.00"));
    }

    @Test void malformedDocumentRollsBackEvenPreviouslyReadRows() throws Exception {
        assertThrows(Exception.class, () -> importer.importFile(file("[" + sale("one", "250") + ","), () -> false, n -> {}));
        assertEquals(0, count());
        assertThrows(Exception.class, () -> importer.importFile(file("[] {}"), () -> false, n -> {}));
    }

    @Test void cancellationAfterReadingARowRollsBackTheImport() throws Exception {
        AtomicBoolean cancel = new AtomicBoolean();
        Path path = file("[" + sale("one", "250") + "]");
        assertThrows(CancellationException.class, () -> importer.importFile(path, cancel::get, n -> cancel.set(true)));
        assertEquals(0, count());
    }

    @Test void rejectsUnknownCategoryBadTimestampAndFractionalCent() throws Exception {
        String valid = sale("one", "1");
        var result = importer.importFile(file("[" + valid.replace("Textbooks", "Unknown") + ","
                + valid.replace("2026-10-20T08:00:00Z", "yesterday") + "," + sale("tiny", "0.001") + "]"), () -> false, n -> {});
        assertEquals(3, result.invalid()); assertEquals(0, result.saved()); assertEquals(0, count());
        assertEquals(0, importer.importFile(file("[]"), () -> false, n -> {}).saved());
    }

    @Test void databaseFailureRollsBackNewRowsButPreservesEarlierImports() throws Exception {
        importer.importFile(file("[" + sale("existing", "100") + "]"), () -> false, n -> {});
        try (var connection = db.open(); var trigger = connection.prepareStatement("""
                CREATE TRIGGER reject_demo_sale BEFORE INSERT ON HistoricalSales
                WHEN NEW.sourceRecordId = 'fail' BEGIN SELECT RAISE(ABORT, 'test failure'); END
                """)) { trigger.execute(); }
        assertThrows(java.io.IOException.class, () -> importer.importFile(
                file("[" + sale("new", "200") + "," + sale("fail", "300") + "]"), () -> false, n -> {}));
        assertEquals(1, count());
        assertTrue(db.historyPreview().contains("existing"));
        assertFalse(db.historyPreview().contains("new |"));
    }
}
