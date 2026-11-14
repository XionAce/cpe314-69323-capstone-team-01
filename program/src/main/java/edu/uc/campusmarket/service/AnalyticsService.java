package edu.uc.campusmarket.service;

import edu.uc.campusmarket.dao.AnalyticsDao;
import edu.uc.campusmarket.dao.HistoryDao;
import edu.uc.campusmarket.model.AnalyticsSnapshot;
import edu.uc.campusmarket.model.WeeklySales;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;

/** Loads independent summaries and runs the Java historical-sale analysis. */
public class AnalyticsService {
    private final AnalyticsDao analytics;
    private final HistoryDao history;
    private final SalesHistoryImporter importer;

    /** Creates this component from the supplied values or dependencies. */
    public AnalyticsService(AnalyticsDao analytics, HistoryDao history,
            SalesHistoryImporter importer) {
        this.analytics = analytics;
        this.history = history;
        this.importer = importer;
    }

    /** Loads saved model data for the requested view or analysis. */
    public AnalyticsSnapshot load(String currency) {
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Enter a three-letter uppercase currency.");
        }
        return ServiceCall.get(() -> {
            List<WeeklySales> weeks = SalesAnalysis.weeklyCounts(history.soldDates());
            return new AnalyticsSnapshot(analytics.priceSummary(currency),
                    analytics.ratingSummary(), weeks, SalesAnalysis.summary(weeks));
        });
    }

    /** Imports historical sales and returns the committed result and preview. */
    public String importHistory(Path file, BooleanSupplier cancelled, LongConsumer progress) {
        try {
            var result = importer.importFile(file, cancelled, progress);
            String summary = "Saved: " + result.saved() + ", invalid: " + result.invalid()
                    + ", duplicates: " + result.duplicates() + "\n"
                    + String.join("\n", result.errors());
            // Import is already committed: preview failure must not claim a rollback.
            try {
                return summary + "\n" + history.historyPreview();
            } catch (SQLException error) {
                return summary + "\nSaved successfully; preview unavailable: " + error.getMessage();
            }
        } catch (IOException | SQLException error) {
            throw new AppException(error.getMessage(), error);
        }
    }
}
