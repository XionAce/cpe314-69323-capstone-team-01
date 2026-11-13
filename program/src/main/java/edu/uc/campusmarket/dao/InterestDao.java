package edu.uc.campusmarket.dao;

import edu.uc.campusmarket.persistence.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL operations for this table or closely related aggregate. */
public class InterestDao {
    private final Database database;

    /** Creates this component from the supplied values or dependencies. */
    public InterestDao(Database database) {
        this.database = database;
    }

    /** Returns buyer identities in first-in-first-out joining order. */
    public List<Long> getWaitlist(long listingId) throws SQLException {
        List<Long> buyers = new ArrayList<>();
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT buyerId FROM BuyerInterests WHERE listingId = ? ORDER BY joiningOrder")) {
            statement.setLong(1, listingId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    buyers.add(result.getLong(1));
                }
            }
        }
        return buyers;
    }

    /** Allocates FIFO order and inserts a buyer in one atomic SQLite statement. */
    public void addInterest(long listingId, long memberId) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO BuyerInterests(listingId, buyerId, joiningOrder)
                        SELECT l.id, m.id,
                            COALESCE((SELECT MAX(joiningOrder) FROM BuyerInterests
                                      WHERE listingId = l.id), 0) + 1
                        FROM Listings l JOIN Sellers s ON s.id = l.sellerId
                        JOIN Members m ON m.id = ?
                        WHERE l.id = ? AND l.status = 'ACTIVE' AND s.memberId <> m.id
                        AND NOT EXISTS (SELECT 1 FROM BuyerInterests
                                        WHERE listingId = l.id AND buyerId = m.id)
                        """)) {
            statement.setLong(1, memberId);
            statement.setLong(2, listingId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException(
                        "Join an active item from another owner, using an existing account. "
                                + "You may already be on its waitlist.");
            }
        }
    }
}
