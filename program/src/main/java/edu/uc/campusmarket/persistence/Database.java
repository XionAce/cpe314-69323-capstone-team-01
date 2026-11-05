package edu.uc.campusmarket.persistence;

import edu.uc.campusmarket.model.*;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** SQL stays here. Each public operation opens and closes its own connection. */
public class Database {
    private final Path file;

    public Database(Path file) {
        this.file = file.toAbsolutePath().normalize();
    }

    public Path getPath() { return file; }

    public static Path defaultPath() {
        String directory = System.getProperty("campusmarket.dataDir");
        if (directory == null || directory.isBlank()) {
            return Path.of(System.getProperty("user.home"), ".ucb-campus-marketplace-simple", "marketplace.db");
        }
        return Path.of(directory, "marketplace.db");
    }

    public Connection open() throws SQLException {
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException error) {
            throw new SQLException("Cannot create the database folder: " + file.getParent(), error);
        }
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
        try {
            // SQLite needs this setting on EVERY new connection.
            try (PreparedStatement statement = connection.prepareStatement("PRAGMA foreign_keys = ON")) {
                statement.execute();
            }
            try (PreparedStatement statement = connection.prepareStatement("PRAGMA busy_timeout = 5000")) {
                statement.execute();
            }
            return connection;
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
    }

    public void initialize() throws SQLException {
        try (Connection connection = open()) {
            int tables;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = ? AND name NOT LIKE ?")) {
                statement.setString(1, "table");
                statement.setString(2, "sqlite_%");
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    tables = result.getInt(1);
                }
            }
            if (tables > 0) {
                // Never recreate or erase a database that already contains tables.
                try (PreparedStatement statement = connection.prepareStatement("PRAGMA user_version");
                     ResultSet result = statement.executeQuery()) {
                    result.next();
                    if (result.getInt(1) != 1 || tables != 10) {
                        throw new SQLException("Unrecognized database. Choose a new data folder.");
                    }
                }
                return;
            }
            connection.setAutoCommit(false);
            try {
                String schema = readSchema();
                for (String sql : schema.split(";")) {
                    if (!sql.isBlank()) {
                        try (PreparedStatement statement = connection.prepareStatement(sql)) {
                            statement.execute();
                        }
                    }
                }
                seed(connection);
                try (PreparedStatement statement = connection.prepareStatement("PRAGMA user_version = 1")) {
                    statement.execute();
                }
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                rollback(connection, error);
                throw error;
            }
        }
    }

    private String readSchema() throws SQLException {
        try (InputStream input = Database.class.getResourceAsStream("/db/schema.sql")) {
            if (input == null) throw new IOException("Missing /db/schema.sql resource.");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new SQLException("Cannot read the table definitions.", error);
        }
    }

    private void seed(Connection connection) throws SQLException {
        String[] names = {"Alex (demo)", "Bea (demo)", "Carlo (demo)"};
        for (int i = 0; i < names.length; i++) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO Members(id, name) VALUES (?, ?)")) {
                statement.setLong(1, i + 1);
                statement.setString(2, names[i]);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO Sellers(id, memberId, displayName, sellerKind) VALUES (?, ?, ?, ?)")) {
                statement.setLong(1, i + 1);
                statement.setLong(2, i + 1);
                statement.setString(3, names[i]);
                statement.setString(4, "STUDENT");
                statement.executeUpdate();
            }
        }
        String[] categories = {"All finds", "Textbooks", "Electronics", "Furniture", "Computers", "Laptops", "Gaming Laptops"};
        Long[] parents = {null, 1L, 1L, 1L, 3L, 5L, 6L};
        for (int i = 0; i < categories.length; i++) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO Categories(id, parentId, name) VALUES (?, ?, ?)")) {
                statement.setLong(1, i + 1);
                if (parents[i] == null) statement.setNull(2, Types.INTEGER);
                else statement.setLong(2, parents[i]);
                statement.setString(3, categories[i]);
                statement.executeUpdate();
            }
        }
        insertListing(connection, new Textbook(0, 1, 2, "Java textbook (demo)", new BigDecimal("350.00"),
                "PHP", "AVAILABLE", 0, WearLevel.LIGHT, false));
        insertListing(connection, new Electronics(0, 2, 7, "Gaming laptop (demo)", new BigDecimal("18000.00"),
                "PHP", "AVAILABLE", true, false, true));
        insertListing(connection, new Furniture(0, 3, 4, "Study chair (demo)", new BigDecimal("650.00"),
                "PHP", "AVAILABLE", true, false, WearLevel.LIGHT));
    }

    public List<Member> getMembers() throws SQLException {
        List<Member> members = new ArrayList<>();
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement("SELECT id, name FROM Members ORDER BY id");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) members.add(new Member(result.getLong("id"), result.getString("name")));
        }
        return members;
    }

    public List<Category> getCategories() throws SQLException {
        List<Category> categories = new ArrayList<>();
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement("SELECT id, parentId, name FROM Categories ORDER BY id");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                long parent = result.getLong("parentId");
                Long parentId = result.wasNull() ? null : parent;
                categories.add(new Category(result.getLong("id"), parentId, result.getString("name")));
            }
        }
        return categories;
    }

    public List<Listing> getListings() throws SQLException {
        List<Listing> listings = new ArrayList<>();
        // One shared listing row is joined to its type-specific evidence.
        String sql = """
                SELECT l.*, t.listingId AS textbookId, t.missingPages, t.markingLevel, t.coverDamaged,
                       e.listingId AS electronicsId, e.powersOn, e.hasFunctionalDefect, e.hasCosmeticDamage,
                       f.listingId AS furnitureId, f.structurallyStable, f.hasStructuralDamage, f.surfaceWear
                FROM Listings l
                LEFT JOIN TextbookDetails t ON t.listingId = l.id
                LEFT JOIN ElectronicsDetails e ON e.listingId = l.id
                LEFT JOIN FurnitureDetails f ON f.listingId = l.id
                ORDER BY l.id
                """;
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) listings.add(readListing(result));
        }
        return listings;
    }

    private Listing readListing(ResultSet result) throws SQLException {
        long id = result.getLong("id");
        long seller = result.getLong("sellerId");
        long category = result.getLong("categoryId");
        String title = result.getString("title");
        BigDecimal price = BigDecimal.valueOf(result.getLong("priceMinor"), 2);
        String currency = result.getString("currency");
        String status = result.getString("status");
        String type = result.getString("type");
        int detailCount = 0;
        if (result.getObject("textbookId") != null) detailCount++;
        if (result.getObject("electronicsId") != null) detailCount++;
        if (result.getObject("furnitureId") != null) detailCount++;
        if (detailCount != 1) throw new SQLException("Listing " + id + " needs exactly one matching detail row.");
        if (type.equals("TEXTBOOK") && result.getObject("textbookId") != null) {
            return new Textbook(id, seller, category, title, price, currency, status, result.getInt("missingPages"),
                    WearLevel.valueOf(result.getString("markingLevel")), result.getBoolean("coverDamaged"));
        }
        if (type.equals("ELECTRONICS") && result.getObject("electronicsId") != null) {
            return new Electronics(id, seller, category, title, price, currency, status, result.getBoolean("powersOn"),
                    result.getBoolean("hasFunctionalDefect"), result.getBoolean("hasCosmeticDamage"));
        }
        if (type.equals("FURNITURE") && result.getObject("furnitureId") != null) {
            return new Furniture(id, seller, category, title, price, currency, status, result.getBoolean("structurallyStable"),
                    result.getBoolean("hasStructuralDamage"), WearLevel.valueOf(result.getString("surfaceWear")));
        }
        throw new SQLException("Listing " + id + " has inconsistent type/details.");
    }

    public long ensureSeller(long memberId) throws SQLException {
        try (Connection connection = open()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM Sellers WHERE memberId = ? ORDER BY id LIMIT 1")) {
                statement.setLong(1, memberId);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) return result.getLong(1);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO Sellers(memberId, displayName, sellerKind) SELECT id, name, ? FROM Members WHERE id = ?")) {
                statement.setString(1, "STUDENT");
                statement.setLong(2, memberId);
                if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Choose an existing member.");
            }
            return lastId(connection);
        }
    }

    public long insertListing(Listing listing) throws SQLException {
        if (listing.getId() != 0) throw new IllegalArgumentException("A new listing must have ID 0.");
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                long id = insertListing(connection, listing);
                connection.commit();
                return id;
            } catch (SQLException | RuntimeException error) {
                rollback(connection, error);
                throw error;
            }
        }
    }

    private long insertListing(Connection connection, Listing listing) throws SQLException {
        if (!(listing instanceof Textbook) && !(listing instanceof Electronics) && !(listing instanceof Furniture)) {
            throw new IllegalArgumentException("Unsupported listing type.");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Listings(sellerId, categoryId, type, title, priceMinor, currency, status) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setLong(1, listing.getSellerId());
            statement.setLong(2, listing.getCategoryId());
            statement.setString(3, listing.getType());
            statement.setString(4, listing.getTitle());
            statement.setLong(5, listing.getPriceMinor());
            statement.setString(6, listing.getCurrency());
            statement.setString(7, listing.getStatus());
            statement.executeUpdate();
        }
        long id = lastId(connection);
        if (listing instanceof Textbook book) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO TextbookDetails VALUES (?, ?, ?, ?)")) {
                statement.setLong(1, id); statement.setInt(2, book.getMissingPages());
                statement.setString(3, book.getMarkingLevel().name()); statement.setBoolean(4, book.isCoverDamaged());
                statement.executeUpdate();
            }
        } else if (listing instanceof Electronics electronic) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO ElectronicsDetails VALUES (?, ?, ?, ?)")) {
                statement.setLong(1, id); statement.setBoolean(2, electronic.isPowersOn());
                statement.setBoolean(3, electronic.hasFunctionalDefect()); statement.setBoolean(4, electronic.hasCosmeticDamage());
                statement.executeUpdate();
            }
        } else if (listing instanceof Furniture furniture) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO FurnitureDetails VALUES (?, ?, ?, ?)")) {
                statement.setLong(1, id); statement.setBoolean(2, furniture.isStable());
                statement.setBoolean(3, furniture.hasStructuralDamage()); statement.setString(4, furniture.getSurfaceWear().name());
                statement.executeUpdate();
            }
        }
        return id;
    }

    private long lastId(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT last_insert_rowid()");
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getLong(1);
        }
    }

    public List<Long> getWaitlist(long listingId) throws SQLException {
        List<Long> buyers = new ArrayList<>();
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(
                "SELECT buyerId FROM BuyerInterests WHERE listingId = ? ORDER BY joiningOrder")) {
            statement.setLong(1, listingId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) buyers.add(result.getLong(1));
            }
        }
        return buyers;
    }

    public void addInterest(long listingId, long memberId) throws SQLException {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT l.status, s.memberId FROM Listings l JOIN Sellers s ON s.id = l.sellerId WHERE l.id = ?")) {
                    statement.setLong(1, listingId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Listing not found.");
                        if (!result.getString("status").equals("AVAILABLE")) throw new IllegalArgumentException("Listing has been withdrawn.");
                        if (result.getLong("memberId") == memberId) throw new IllegalArgumentException("You cannot join your own listing's waitlist.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM Members WHERE id = ?")) {
                    statement.setLong(1, memberId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Choose an existing member.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM BuyerInterests WHERE listingId = ? AND buyerId = ?")) {
                    statement.setLong(1, listingId); statement.setLong(2, memberId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) throw new IllegalArgumentException("You already joined this waitlist.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO BuyerInterests(listingId, buyerId, joiningOrder) SELECT ?, ?, COALESCE(MAX(joiningOrder), 0) + 1 FROM BuyerInterests WHERE listingId = ?")) {
                    statement.setLong(1, listingId); statement.setLong(2, memberId); statement.setLong(3, listingId);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                rollback(connection, error);
                throw error;
            }
        }
    }

    public void withdrawListing(long listingId, long memberId) throws SQLException {
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE Listings SET status = ? WHERE id = ? AND status = ? AND sellerId IN (SELECT id FROM Sellers WHERE memberId = ?)")) {
            statement.setString(1, "WITHDRAWN"); statement.setLong(2, listingId);
            statement.setString(3, "AVAILABLE"); statement.setLong(4, memberId);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Only the owner can withdraw an available listing.");
        }
    }

    /** These two summaries are queried separately so reviews never multiply listing prices. */
    public record CategorySummary(String category, String currency, double average, int count) { }

    public List<CategorySummary> priceSummary(String currency) throws SQLException {
        List<CategorySummary> rows = new ArrayList<>();
        try (var connection = open(); var query = connection.prepareStatement("""
                SELECT c.name, AVG(l.priceMinor) / 100.0 AS mean, COUNT(*) AS n
                FROM Listings l JOIN Categories c ON c.id = l.categoryId
                WHERE l.status = 'AVAILABLE' AND l.currency = ?
                GROUP BY c.id ORDER BY c.name, c.id
                """)) {
            query.setString(1, currency);
            try (var result = query.executeQuery()) {
                while (result.next()) rows.add(new CategorySummary(result.getString(1), currency,
                        result.getDouble(2), result.getInt(3)));
            }
        }
        return rows;
    }

    public List<CategorySummary> ratingSummary() throws SQLException {
        List<CategorySummary> rows = new ArrayList<>();
        try (var connection = open(); var query = connection.prepareStatement("""
                SELECT c.name, AVG(r.rating) AS mean, COUNT(*) AS n
                FROM Reviews r JOIN Listings l ON l.id = r.listingId
                JOIN Categories c ON c.id = l.categoryId
                WHERE l.status = 'AVAILABLE' GROUP BY c.id ORDER BY mean DESC, c.id
                """); var result = query.executeQuery()) {
            while (result.next()) rows.add(new CategorySummary(result.getString(1), "",
                    result.getDouble(2), result.getInt(3)));
        }
        return rows;
    }

    public void addReview(long listingId, long memberId, int rating, String comment) throws SQLException {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be 1 to 5.");
        if (comment == null || comment.length() > 1000) throw new IllegalArgumentException("Comment must be at most 1000 characters.");
        try (var connection = open()) {
            connection.setAutoCommit(false);
            try {
                try (var query = connection.prepareStatement("""
                        SELECT s.memberId, l.status FROM Listings l JOIN Sellers s ON s.id = l.sellerId
                        WHERE l.id = ?
                        """)) {
                    query.setLong(1, listingId);
                    try (var result = query.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Listing not found.");
                        if (result.getLong(1) == memberId) throw new IllegalArgumentException("You cannot review your own listing.");
                        if (!result.getString(2).equals("AVAILABLE")) throw new IllegalArgumentException("Listing is withdrawn.");
                    }
                }
                try (var insert = connection.prepareStatement("""
                        INSERT INTO Reviews(listingId, reviewerId, rating, comment)
                        SELECT ?, ?, ?, ? WHERE NOT EXISTS
                        (SELECT 1 FROM Reviews WHERE listingId = ? AND reviewerId = ?)
                        """)) {
                    insert.setLong(1, listingId); insert.setLong(2, memberId); insert.setInt(3, rating);
                    insert.setString(4, comment.trim()); insert.setLong(5, listingId); insert.setLong(6, memberId);
                    if (insert.executeUpdate() != 1) throw new IllegalArgumentException("You already reviewed this listing.");
                }
                connection.commit();
            } catch (SQLException | RuntimeException error) { rollback(connection, error); throw error; }
        }
    }

    public String reviews(long listingId) throws SQLException {
        StringBuilder text = new StringBuilder();
        try (var connection = open(); var query = connection.prepareStatement("""
                SELECT m.name, r.rating, r.comment FROM Reviews r JOIN Members m ON m.id = r.reviewerId
                WHERE r.listingId = ? ORDER BY r.id
                """)) {
            query.setLong(1, listingId);
            try (var result = query.executeQuery()) {
                while (result.next()) text.append(result.getString(1)).append(": ").append(result.getInt(2))
                        .append("/5 - ").append(result.getString(3)).append('\n');
            }
        }
        return text.isEmpty() ? "No reviews yet." : text.toString();
    }

    public String historyPreview() throws SQLException {
        StringBuilder text = new StringBuilder("Latest imported sales (up to 20):\n");
        try (var connection = open(); var query = connection.prepareStatement("""
                SELECT h.sourceRecordId, c.name, h.priceMinor, h.currency, h.soldAt
                FROM HistoricalSales h JOIN Categories c ON c.id = h.categoryId ORDER BY h.id DESC LIMIT 20
                """); var result = query.executeQuery()) {
            while (result.next()) text.append(result.getString(1)).append(" | ").append(result.getString(2))
                    .append(" | ").append(result.getString(4)).append(' ')
                    .append(BigDecimal.valueOf(result.getLong(3), 2)).append(" | ").append(result.getString(5)).append('\n');
        }
        return text.toString();
    }

    private void rollback(Connection connection, Exception error) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            error.addSuppressed(rollbackError);
        }
    }
}
