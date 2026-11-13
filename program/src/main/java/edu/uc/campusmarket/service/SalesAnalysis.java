package edu.uc.campusmarket.service;

import edu.uc.campusmarket.model.WeeklySales;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Java frequency analysis: UTC dates are grouped into Monday-starting weeks.
 */
public final class SalesAnalysis {
    private SalesAnalysis() {
    }

    /** Counts sales by Monday-starting UTC week in Java and sorts the results. */
    public static List<WeeklySales> weeklyCounts(List<OffsetDateTime> dates) {
        Map<LocalDate, Integer> counts = new TreeMap<>();
        for (OffsetDateTime date : dates) {
            LocalDate monday = date.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            counts.merge(monday, 1, Integer::sum);
        }
        List<WeeklySales> result = new ArrayList<>();
        for (Map.Entry<LocalDate, Integer> entry : counts.entrySet()) {
            result.add(new WeeklySales(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(result);
    }

    /** Describes the observed weekly counts, including every tied busiest week. */
    public static String summary(List<WeeklySales> weeks) {
        if (weeks.isEmpty()) {
            return "No historical sales yet. Import JSON to calculate weekly activity.";
        }
        int total = 0;
        int largest = 0;
        for (WeeklySales week : weeks) {
            total += week.count();
            largest = Math.max(largest, week.count());
        }
        List<String> busiest = new ArrayList<>();
        for (WeeklySales week : weeks) {
            if (week.count() == largest) {
                busiest.add(week.weekStarting().toString());
            }
        }
        return total + " sales across " + weeks.size() + " active weeks. Busiest week(s): "
                + String.join(", ", busiest) + " (" + largest + " sales each). "
                + "Weeks begin Monday in UTC; weeks without sales are not plotted.";
    }
}
