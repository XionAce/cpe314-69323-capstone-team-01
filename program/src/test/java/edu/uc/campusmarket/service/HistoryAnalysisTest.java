package edu.uc.campusmarket.service;

import edu.uc.campusmarket.TestDatabase;
import edu.uc.campusmarket.dao.AnalyticsDao;
import edu.uc.campusmarket.dao.HistoryDao;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryAnalysisTest {
    @TempDir
    Path directory;

    @Test
    void importingThenChangingStoredRowsChangesJavaAnalysisWithoutChangingListingMeans()
            throws Exception {
        TestDatabase database = new TestDatabase(directory.resolve("test.db"));
        database.initialize();
        AnalyticsService service = new AnalyticsService(new AnalyticsDao(database),
                new HistoryDao(database), database.importer());
        Path json = directory.resolve("sales.json");
        Files.writeString(json, """
                [
                  {"sourceRecordId":"a","categoryPath":["All finds","Textbooks"],
                   "price":20,"currency":"PHP","soldAt":"2026-11-02T00:00:00Z"},
                  {"sourceRecordId":"b","categoryPath":["All finds","Textbooks"],
                   "price":30,"currency":"PHP","soldAt":"2026-11-03T00:00:00Z"},
                  {"sourceRecordId":"c","categoryPath":["All finds","Furniture"],
                   "price":40,"currency":"PHP","soldAt":"2026-11-09T00:00:00Z"}
                ]
                """);
        var prices = service.load("PHP").prices();
        assertTrue(service.importHistory(json, () -> false, count -> {
        }).contains("Saved: 3"));
        var before = service.load("PHP");
        assertEquals(2, before.weeks().getFirst().count());
        assertEquals(1, before.weeks().getLast().count());
        assertTrue(before.activitySummary().contains("2026-11-02 (2 sales each)"));
        try (var connection = database.open();
                var statement = connection
                        .prepareStatement("DELETE FROM HistoricalSales WHERE sourceRecordId = ?")) {
            statement.setString(1, "a");
            assertEquals(1, statement.executeUpdate());
        }
        var after = service.load("PHP");
        assertEquals(1, after.weeks().getFirst().count());
        assertNotEquals(before, after);
        assertEquals(prices, after.prices());
        assertTrue(service.importHistory(json, () -> false, count -> {
        }).contains("duplicates: 2"));
        assertEquals(2, service.load("PHP").weeks().getFirst().count());
    }
}
