package edu.uc.campusmarket.dao;

import edu.uc.campusmarket.model.ChatMessage;
import edu.uc.campusmarket.model.ChatThread;
import edu.uc.campusmarket.persistence.Database;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Prepared, participant-filtered queries for listing conversations. */
public class ChatDao {
    private final Database database;
    private static final String THREAD_COLUMNS = """
            SELECT l.id AS listingId, b.id AS buyerId, s.memberId AS sellerMemberId,
                l.title, b.name AS buyerName, owner.name AS sellerName, l.status,
                COALESCE((SELECT body FROM Messages
                    WHERE listingId = l.id AND buyerId = b.id ORDER BY id DESC LIMIT 1), '') AS lastMessage,
                COALESCE((SELECT MAX(id) FROM Messages
                    WHERE listingId = l.id AND buyerId = b.id), 0) AS lastMessageId
            FROM Listings l JOIN Sellers s ON s.id = l.sellerId
            JOIN Members owner ON owner.id = s.memberId
            """;

    /** Uses the existing SQLite file; each operation opens and closes its connection. */
    public ChatDao(Database database) {
        this.database = database;
    }

    /** Lists only conversations involving the requesting buyer or listing owner. */
    public List<ChatThread> threads(long memberId) throws SQLException {
        List<ChatThread> result = new ArrayList<>();
        try (Connection connection = database.open();
                var query = connection.prepareStatement(THREAD_COLUMNS + """
                        JOIN Members b ON b.id IN (SELECT buyerId FROM Messages WHERE listingId = l.id)
                        WHERE (b.id = ? OR s.memberId = ?)
                        ORDER BY lastMessageId DESC
                        """)) {
            query.setLong(1, memberId);
            query.setLong(2, memberId);
            try (var rows = query.executeQuery()) {
                while (rows.next()) {
                    result.add(readThread(rows));
                }
            }
        }
        return List.copyOf(result);
    }

    /** Loads an authorized thread, or prepares a buyer's first message on an active item. */
    public ChatThread thread(long listingId, long buyerId, long memberId) throws SQLException {
        try (Connection connection = database.open();
                var query = connection.prepareStatement(THREAD_COLUMNS + """
                        JOIN Members b ON b.id = ?
                        WHERE l.id = ? AND b.id <> s.memberId
                        AND (b.id = ? OR s.memberId = ?)
                        AND ((l.status = 'ACTIVE' AND b.id = ?)
                            OR EXISTS (SELECT 1 FROM Messages WHERE listingId = l.id AND buyerId = b.id))
                        """)) {
            query.setLong(1, buyerId);
            query.setLong(2, listingId);
            query.setLong(3, memberId);
            query.setLong(4, memberId);
            query.setLong(5, memberId);
            try (var rows = query.executeQuery()) {
                return rows.next() ? readThread(rows) : null;
            }
        }
    }

    /** Reads history in saved order, applying participant permissions inside SQL. */
    public List<ChatMessage> messages(long listingId, long buyerId, long memberId)
            throws SQLException {
        List<ChatMessage> result = new ArrayList<>();
        try (Connection connection = database.open();
                var query = connection.prepareStatement("""
                        SELECT m.id, m.senderId, author.name, m.body, m.sentAt
                        FROM Messages m JOIN Members author ON author.id = m.senderId
                        JOIN Listings l ON l.id = m.listingId JOIN Sellers s ON s.id = l.sellerId
                        WHERE m.listingId = ? AND m.buyerId = ? AND (m.buyerId = ? OR s.memberId = ?)
                        ORDER BY m.id
                        """)) {
            query.setLong(1, listingId);
            query.setLong(2, buyerId);
            query.setLong(3, memberId);
            query.setLong(4, memberId);
            try (var rows = query.executeQuery()) {
                while (rows.next()) {
                    result.add(new ChatMessage(rows.getLong(1), rows.getLong(2), rows.getString(3),
                            rows.getString(4), Instant.parse(rows.getString(5))));
                }
            }
        }
        return List.copyOf(result);
    }

    /** Saves a message only if the sender is still allowed when SQLite inserts it. */
    public void send(long listingId, long buyerId, long senderId, String body) throws SQLException {
        try (Connection connection = database.open();
                var query = connection.prepareStatement("""
                        INSERT INTO Messages(listingId, buyerId, senderId, body, sentAt)
                        SELECT l.id, b.id, author.id, ?, ?
                        FROM Listings l JOIN Sellers s ON s.id = l.sellerId
                        JOIN Members b ON b.id = ? JOIN Members author ON author.id = ?
                        WHERE l.id = ? AND b.id <> s.memberId AND author.id IN (b.id, s.memberId)
                        AND (author.id = b.id
                            OR EXISTS (SELECT 1 FROM Messages WHERE listingId = l.id AND buyerId = b.id))
                        AND (l.status = 'ACTIVE' OR (l.status = 'SOLD'
                            AND EXISTS (SELECT 1 FROM Messages WHERE listingId = l.id AND buyerId = b.id)))
                        """)) {
            query.setString(1, body);
            query.setString(2, Instant.now().toString());
            query.setLong(3, buyerId);
            query.setLong(4, senderId);
            query.setLong(5, listingId);
            if (query.executeUpdate() != 1) {
                throw new IllegalArgumentException(
                        "Open an active listing as its buyer, or reply to an existing buyer chat. "
                                + "Removed listings cannot receive messages.");
            }
        }
    }

    private ChatThread readThread(ResultSet row) throws SQLException {
        return new ChatThread(row.getLong("listingId"), row.getLong("buyerId"),
                row.getLong("sellerMemberId"), row.getString("title"), row.getString("buyerName"),
                row.getString("sellerName"), row.getString("status"), row.getString("lastMessage"),
                row.getLong("lastMessageId"));
    }
}
