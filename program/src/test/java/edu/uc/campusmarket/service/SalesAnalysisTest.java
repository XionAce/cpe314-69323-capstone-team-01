package edu.uc.campusmarket.service;

import edu.uc.campusmarket.model.WeeklySales;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesAnalysisTest {
    @Test
    void groupsInJavaAndSortsWeeksRatherThanInputOrder() {
        var rows = SalesAnalysis.weeklyCounts(List.of(OffsetDateTime.parse("2026-11-10T08:00:00Z"),
                OffsetDateTime.parse("2026-11-03T08:00:00Z"),
                OffsetDateTime.parse("2026-11-04T08:00:00Z")));
        assertEquals(List.of(new WeeklySales(LocalDate.parse("2026-11-02"), 2),
                new WeeklySales(LocalDate.parse("2026-11-09"), 1)), rows);
    }

    @Test
    void offsetsUseTheSameInstantInUtcAtAMondayBoundary() {
        var rows = SalesAnalysis
                .weeklyCounts(List.of(OffsetDateTime.parse("2026-11-09T00:30:00+08:00"),
                        OffsetDateTime.parse("2026-11-08T16:30:00Z"),
                        OffsetDateTime.parse("2026-11-09T00:00:00Z")));
        assertEquals(2, rows.getFirst().count());
        assertEquals(LocalDate.parse("2026-11-02"), rows.getFirst().weekStarting());
        assertEquals(LocalDate.parse("2026-11-09"), rows.getLast().weekStarting());
    }

    @Test
    void weekMayStartInThePreviousYear() {
        var rows = SalesAnalysis
                .weeklyCounts(List.of(OffsetDateTime.parse("2027-01-01T12:00:00Z")));
        assertEquals(LocalDate.parse("2026-12-28"), rows.getFirst().weekStarting());
    }

    @Test
    void emptyHistoryHasAnHonestEmptyState() {
        assertTrue(SalesAnalysis.weeklyCounts(List.of()).isEmpty());
        assertTrue(SalesAnalysis.summary(List.of()).contains("No historical sales"));
    }

    @Test
    void tiesReportEveryBusiestWeekAndNoSaleWeeksAreNotInvented() {
        var rows = List.of(new WeeklySales(LocalDate.parse("2026-11-02"), 2),
                new WeeklySales(LocalDate.parse("2026-11-16"), 2));
        String text = SalesAnalysis.summary(rows);
        assertTrue(text.contains("2026-11-02"));
        assertTrue(text.contains("2026-11-16"));
        assertTrue(text.contains("4 sales"));
        assertTrue(text.contains("not plotted"));
    }

    @Test
    void resultsCannotBeChangedByTheView() {
        var rows = SalesAnalysis
                .weeklyCounts(List.of(OffsetDateTime.parse("2026-11-02T00:00:00Z")));
        assertThrows(UnsupportedOperationException.class, () -> rows.clear());
    }
}
