package edu.uc.campusmarket.dao;

import edu.uc.campusmarket.model.HomeItem;
import edu.uc.campusmarket.model.HomeSummary;
import edu.uc.campusmarket.persistence.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads only the requested member's dashboard in one consistent transaction.
 */
public class HomeDao {
    private final Database database;

    /** Uses the shared database connection factory; performs no query yet. */
    public HomeDao(Database database) {
        this.database = database;
    }

    /**
     * Loads owned active items, personal waitlists and reviews received by this
     * member.
     */
    public HomeSummary loadFor(long member) throws SQLException {
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                requireMember(connection, member);
                List<HomeItem> owned = owned(connection, member);
                List<HomeItem> waiting = waiting(connection, member);
                HomeSummary result = ratings(connection, member, owned, waiting);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException error) {
                Database.rollback(connection, error);
                throw error;
            }
        }
    }

    private void requireMember(Connection connection, long member) throws SQLException {
        try (PreparedStatement statement = connection
                .prepareStatement("SELECT id FROM Members WHERE id = ?")) {
            statement.setLong(1, member);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("Sign in with an existing member.");
                }
            }
        }
    }

    private List<HomeItem> owned(Connection connection, long member) throws SQLException {
        String sql = """
                SELECT l.id, l.title, l.status, COUNT(b.id) AS count,
                       COALESCE(GROUP_CONCAT(m.name, ', '), '') AS buyers
                FROM Listings l JOIN Sellers s ON s.id = l.sellerId
                LEFT JOIN BuyerInterests b ON b.listingId = l.id
                LEFT JOIN Members m ON m.id = b.buyerId
                WHERE s.memberId = ? AND l.status = 'ACTIVE'
                GROUP BY l.id ORDER BY l.id DESC
                """;
        return rows(connection, member, sql);
    }

    private List<HomeItem> waiting(Connection connection, long member) throws SQLException {
        String sql = """
                SELECT l.id, l.title, l.status,
                  (SELECT COUNT(*) FROM BuyerInterests earlier WHERE earlier.listingId = l.id
                   AND earlier.joiningOrder <= b.joiningOrder) AS count, '' AS buyers
                FROM BuyerInterests b JOIN Listings l ON l.id = b.listingId
                WHERE b.buyerId = ? ORDER BY l.id DESC
                """;
        return rows(connection, member, sql);
    }

    private List<HomeItem> rows(Connection connection, long member, String sql)
            throws SQLException {
        List<HomeItem> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, member);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new HomeItem(result.getLong("id"), result.getString("title"),
                            result.getString("status"), result.getInt("count"),
                            result.getString("buyers")));
                }
            }
        }
        return rows;
    }

    private HomeSummary ratings(Connection connection, long member, List<HomeItem> owned,
            List<HomeItem> waiting) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(r.id) AS count, COALESCE(AVG(r.rating), 0) AS average
                FROM Reviews r JOIN Listings l ON l.id = r.listingId
                JOIN Sellers s ON s.id = l.sellerId WHERE s.memberId = ?
                """)) {
            statement.setLong(1, member);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return new HomeSummary(owned, waiting, result.getInt("count"),
                        result.getDouble("average"));
            }
        }
    }
}
