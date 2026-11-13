package edu.uc.campusmarket.dao;

import edu.uc.campusmarket.model.Review;
import edu.uc.campusmarket.persistence.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL operations for this table or closely related aggregate. */
public class ReviewDao {
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MAX_COMMENT_LENGTH = 1000;
    private final Database database;

    /** Creates this component from the supplied values or dependencies. */
    public ReviewDao(Database database) {
        this.database = database;
    }

    /**
     * Saves a review after validating identity, ownership, interest and
     * duplication.
     */
    public void addReview(long listingId, long memberId, int rating, String comment)
            throws SQLException {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException("Rating must be 1 to 5.");
        }
        if (comment == null || comment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Comment must be at most 1000 characters.");
        }
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                requireAvailableOtherOwner(connection, listingId, memberId);
                requireMember(connection, memberId);
                rejectDuplicate(connection, listingId, memberId);
                requireInterest(connection, listingId, memberId);
                insertReview(connection, listingId, memberId, rating, comment);
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                Database.rollback(connection, error);
                throw error;
            }
        }
    }

    private void requireAvailableOtherOwner(Connection connection, long listingId, long memberId)
            throws SQLException {
        try (var query = connection.prepareStatement(
                "SELECT s.memberId, l.status FROM Listings l JOIN Sellers s ON s.id = l.sellerId WHERE l.id = ?")) {
            query.setLong(1, listingId);
            try (var row = query.executeQuery()) {
                if (!row.next()) {
                    throw new IllegalArgumentException("Listing not found.");
                }
                if (row.getLong(1) == memberId) {
                    throw new IllegalArgumentException("You cannot review your own listing.");
                }
                if (!row.getString(2).equals("ACTIVE")) {
                    throw new IllegalArgumentException("Listing is sold or removed.");
                }
            }
        }
    }

    private void requireMember(Connection connection, long memberId) throws SQLException {
        try (var query = connection.prepareStatement("SELECT id FROM Members WHERE id = ?")) {
            query.setLong(1, memberId);
            try (var row = query.executeQuery()) {
                if (!row.next()) {
                    throw new SQLException("Reviewer does not exist.");
                }
            }
        }
    }

    private void rejectDuplicate(Connection connection, long listingId, long memberId)
            throws SQLException {
        try (var query = connection.prepareStatement(
                "SELECT id FROM Reviews WHERE listingId = ? AND reviewerId = ?")) {
            query.setLong(1, listingId);
            query.setLong(2, memberId);
            try (var row = query.executeQuery()) {
                if (row.next()) {
                    throw new IllegalArgumentException("You already reviewed this listing.");
                }
            }
        }
    }

    private void requireInterest(Connection connection, long listingId, long memberId)
            throws SQLException {
        try (var query = connection.prepareStatement(
                "SELECT id FROM BuyerInterests WHERE listingId = ? AND buyerId = ?")) {
            query.setLong(1, listingId);
            query.setLong(2, memberId);
            try (var row = query.executeQuery()) {
                if (!row.next()) {
                    throw new IllegalArgumentException(
                            "Join this listing's waitlist before reviewing it.");
                }
            }
        }
    }

    private void insertReview(Connection connection, long listingId, long memberId, int rating,
            String comment) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO Reviews(listingId, reviewerId, rating, comment)
                SELECT ?, ?, ?, ? WHERE NOT EXISTS
                (SELECT 1 FROM Reviews WHERE listingId = ? AND reviewerId = ?)
                """)) {
            statement.setLong(1, listingId);
            statement.setLong(2, memberId);
            statement.setInt(3, rating);
            statement.setString(4, comment.trim());
            statement.setLong(5, listingId);
            statement.setLong(6, memberId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("You already reviewed this listing.");
            }
        }
    }

    /** Loads saved reviews in their recorded order. */
    public String reviews(long listingId) throws SQLException {
        StringBuilder text = new StringBuilder();
        try (var connection = database.open();
                var query = connection.prepareStatement(
                        """
                                SELECT m.name, r.rating, r.comment FROM Reviews r JOIN Members m ON m.id = r.reviewerId
                                WHERE r.listingId = ? ORDER BY r.id
                                """)) {
            query.setLong(1, listingId);
            try (var result = query.executeQuery()) {
                while (result.next()) {
                    text.append(result.getString(1)).append(": ").append(result.getInt(2))
                            .append("/5 - ").append(result.getString(3)).append('\n');
                }
            }
        }
        return text.isEmpty() ? "No reviews yet." : text.toString();
    }

    /** Read review objects for presentation without exposing JDBC to a view. */
    public List<Review> listFor(long listingId) throws SQLException {
        List<Review> rows = new ArrayList<>();
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT r.id, r.listingId, r.reviewerId, m.name, r.rating, r.comment "
                                + "FROM Reviews r JOIN Members m ON m.id = r.reviewerId "
                                + "WHERE r.listingId = ? ORDER BY r.id")) {
            statement.setLong(1, listingId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new Review(result.getLong(1), result.getLong(2), result.getLong(3),
                            result.getString(4), result.getInt(5), result.getString(6)));
                }
            }
        }
        return List.copyOf(rows);
    }

}
