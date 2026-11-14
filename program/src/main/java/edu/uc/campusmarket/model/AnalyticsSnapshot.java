package edu.uc.campusmarket.model;

import java.util.List;

/** Immutable snapshot so unchanged polling results do not redraw the charts. */
public record AnalyticsSnapshot(List<CategorySummary> prices, List<CategorySummary> ratings,
        List<WeeklySales> weeks, String activitySummary) {
}
