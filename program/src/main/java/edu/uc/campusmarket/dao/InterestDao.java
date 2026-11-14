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

    /**
     * Removes only the requesting member's interest; existing reviews remain saved.
     */
    public void leave(long listingId, long memberId) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM BuyerInterests WHERE listingId = ? AND buyerId = ?")) {
            statement.setLong(1, listingId);
            statement.setLong(2, memberId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("You are not on this waitlist.");
            }
        }
    }

    /** Returns the member's current one-based position, or zero if not waiting. */
    public int position(long listingId, long memberId) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT COUNT(*) FROM BuyerInterests earlier
                        JOIN BuyerInterests mine ON mine.listingId = earlier.listingId
                        WHERE mine.listingId = ? AND mine.buyerId = ?
                        AND earlier.joiningOrder <= mine.joiningOrder
                        """)) {
            statement.setLong(1, listingId);
            statement.setLong(2, memberId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
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
