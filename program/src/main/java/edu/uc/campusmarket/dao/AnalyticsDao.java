package edu.uc.campusmarket.dao;

import edu.uc.campusmarket.model.CategorySummary;
import edu.uc.campusmarket.persistence.Database;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL operations for this table or closely related aggregate. */
public class AnalyticsDao {
    private final Database database;

    /** Creates this component from the supplied values or dependencies. */
    public AnalyticsDao(Database database) {
        this.database = database;
    }

    /**
     * Reads available-listing price averages for one currency without review
     * fan-out.
     */
    public List<CategorySummary> priceSummary(String currency) throws SQLException {
        List<CategorySummary> rows = new ArrayList<>();
        try (var connection = database.open(); var query = connection.prepareStatement("""
                SELECT c.name, AVG(l.priceMinor) / 100.0 AS mean, COUNT(*) AS n
                FROM Listings l JOIN Categories c ON c.id = l.categoryId
                WHERE l.status = 'ACTIVE' AND l.currency = ?
                GROUP BY c.id ORDER BY c.name, c.id
                """)) {
            query.setString(1, currency);
            try (var result = query.executeQuery()) {
                while (result.next()) {
                    rows.add(new CategorySummary(result.getString(1), currency, result.getDouble(2),
                            result.getInt(3)));
                }
            }
        }
        return rows;
    }

    /** Reads averages weighted by individual saved reviews. */
    public List<CategorySummary> ratingSummary() throws SQLException {
        List<CategorySummary> rows = new ArrayList<>();
        try (var connection = database.open(); var query = connection.prepareStatement("""
                SELECT c.name, AVG(r.rating) AS mean, COUNT(*) AS n
                FROM Reviews r JOIN Listings l ON l.id = r.listingId
                JOIN Categories c ON c.id = l.categoryId
                WHERE l.status = 'ACTIVE' GROUP BY c.id ORDER BY mean DESC, c.id
                """); var result = query.executeQuery()) {
            while (result.next()) {
                rows.add(new CategorySummary(result.getString(1), "", result.getDouble(2),
                        result.getInt(3)));
            }
        }
        return rows;
    }

}
