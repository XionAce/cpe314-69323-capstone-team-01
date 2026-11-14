package edu.uc.campusmarket.dao;

import edu.uc.campusmarket.persistence.Database;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** SQL operations for this table or closely related aggregate. */
public class HistoryDao {
    private final Database database;

    /** Creates this component from the supplied values or dependencies. */
    public HistoryDao(Database database) {
        this.database = database;
    }

    /** Provides the history preview operation for this component. */
    public String historyPreview() throws SQLException {
        StringBuilder text = new StringBuilder("Latest imported sales (up to 20):\n");
        try (var connection = database.open(); var query = connection.prepareStatement("""
                SELECT h.sourceRecordId, c.name, h.priceMinor, h.currency, h.soldAt
                FROM HistoricalSales h JOIN Categories c ON c.id = h.categoryId
                ORDER BY h.id DESC LIMIT 20
                """); var result = query.executeQuery()) {
            while (result.next()) {
                text.append(result.getString(1)).append(" | ").append(result.getString(2))
                        .append(" | ").append(result.getString(4)).append(' ')
                        .append(BigDecimal.valueOf(result.getLong(3), 2)).append(" | ")
                        .append(result.getString(5)).append('\n');
            }
        }
        return text.toString();
    }

    /** Read sold timestamps; Java, not SQL, groups them into weeks. */
    public List<OffsetDateTime> soldDates() throws SQLException {
        List<OffsetDateTime> dates = new ArrayList<>();
        try (Connection connection = database.open();
                PreparedStatement statement = connection
                        .prepareStatement("SELECT soldAt FROM HistoricalSales");
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                dates.add(OffsetDateTime.parse(result.getString(1)));
            }
        }
        return List.copyOf(dates);
    }

    /** Prepare the import insert once, then reuse it for every parsed row. */
    public PreparedStatement prepareImport(Connection connection) throws SQLException {
        return connection.prepareStatement(
                "INSERT INTO HistoricalSales(categoryId, sourceRecordId, priceMinor, currency, soldAt)"
                        + " VALUES (?, ?, ?, ?, ?) ON CONFLICT(sourceRecordId) DO NOTHING");
    }

}
