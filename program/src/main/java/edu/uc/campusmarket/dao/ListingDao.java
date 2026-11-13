package edu.uc.campusmarket.dao;

import edu.uc.campusmarket.model.Electronics;
import edu.uc.campusmarket.model.Furniture;
import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.model.ListingForRepair;
import edu.uc.campusmarket.model.Textbook;
import edu.uc.campusmarket.model.WearLevel;
import edu.uc.campusmarket.persistence.Database;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** SQL operations for this table or closely related aggregate. */
public class ListingDao {
    private final Database database;

    /** Creates this component from the supplied values or dependencies. */
    public ListingDao(Database database) {
        this.database = database;
    }

    /** Loads listings together with the matching subtype evidence. */
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
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                listings.add(readListing(result));
            }
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
        String savedDate = result.getString("created_at");
        Instant createdAt = savedDate == null ? null : Instant.parse(savedDate);
        int detailCount = 0;
        if (result.getObject("textbookId") != null) {
            detailCount++;
        }
        if (result.getObject("electronicsId") != null) {
            detailCount++;
        }
        if (result.getObject("furnitureId") != null) {
            detailCount++;
        }
        if (detailCount != 1) {
            String issue = detailCount == 0
                    ? "Condition details are missing. The owner can use Restore missing details."
                    : "Conflicting condition rows need maintainer review.";
            return Listing.incomplete(id, seller, category, title, price, currency, status,
                    createdAt, type, issue);
        }
        if (type.equals("TEXTBOOK") && result.getObject("textbookId") != null) {
            return new Textbook(id, seller, category, title, price, currency, status,
                    result.getInt("missingPages"),
                    WearLevel.valueOf(result.getString("markingLevel")),
                    result.getBoolean("coverDamaged"), createdAt);
        }
        if (type.equals("ELECTRONICS") && result.getObject("electronicsId") != null) {
            return new Electronics(id, seller, category, title, price, currency, status,
                    result.getBoolean("powersOn"), result.getBoolean("hasFunctionalDefect"),
                    result.getBoolean("hasCosmeticDamage"), createdAt);
        }
        if (type.equals("FURNITURE") && result.getObject("furnitureId") != null) {
            return new Furniture(id, seller, category, title, price, currency, status,
                    result.getBoolean("structurallyStable"),
                    result.getBoolean("hasStructuralDamage"),
                    WearLevel.valueOf(result.getString("surfaceWear")), createdAt);
        }
        return Listing.incomplete(id, seller, category, title, price, currency, status, createdAt,
                type, "Condition row does not match this listing type. Ask the maintainer.");
    }

    /** Provides the insert listing operation for this component. */
    public long insertListing(Listing listing) throws SQLException {
        if (listing.getId() != 0) {
            throw new IllegalArgumentException("A new listing must have ID 0.");
        }
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                long id = insertListing(connection, listing);
                connection.commit();
                return id;
            } catch (SQLException | RuntimeException error) {
                Database.rollback(connection, error);
                throw error;
            }
        }
    }

    /** Provides the insert listing operation for this component. */
    public static long insertListing(Connection connection, Listing listing) throws SQLException {
        if (!(listing instanceof Textbook) && !(listing instanceof Electronics)
                && !(listing instanceof Furniture)) {
            throw new IllegalArgumentException("Unsupported listing type.");
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO Listings(sellerId, categoryId, type, title, priceMinor,
                                     currency, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, listing.getSellerId());
            statement.setLong(2, listing.getCategoryId());
            statement.setString(3, listing.getType());
            statement.setString(4, listing.getTitle());
            statement.setLong(5, listing.getPriceMinor());
            statement.setString(6, listing.getCurrency());
            statement.setString(7, listing.getStatus());
            statement.setString(8, Instant.now().toString());
            statement.executeUpdate();
        }
        long id = Database.lastId(connection);
        insertDetails(connection, id, listing);
        return id;
    }

    private static void insertDetails(Connection connection, long id, Listing listing)
            throws SQLException {
        if (listing instanceof Textbook book) {
            try (PreparedStatement statement = connection
                    .prepareStatement("INSERT INTO TextbookDetails VALUES (?, ?, ?, ?)")) {
                statement.setLong(1, id);
                statement.setInt(2, book.getMissingPages());
                statement.setString(3, book.getMarkingLevel().name());
                statement.setBoolean(4, book.isCoverDamaged());
                statement.executeUpdate();
            }
        } else if (listing instanceof Electronics electronic) {
            try (PreparedStatement statement = connection
                    .prepareStatement("INSERT INTO ElectronicsDetails VALUES (?, ?, ?, ?)")) {
                statement.setLong(1, id);
                statement.setBoolean(2, electronic.isPowersOn());
                statement.setBoolean(3, electronic.hasFunctionalDefect());
                statement.setBoolean(4, electronic.hasCosmeticDamage());
                statement.executeUpdate();
            }
        } else if (listing instanceof Furniture furniture) {
            try (PreparedStatement statement = connection
                    .prepareStatement("INSERT INTO FurnitureDetails VALUES (?, ?, ?, ?)")) {
                statement.setLong(1, id);
                statement.setBoolean(2, furniture.isStable());
                statement.setBoolean(3, furniture.hasStructuralDamage());
                statement.setString(4, furniture.getSurfaceWear().name());
                statement.executeUpdate();
            }
        }
    }

    /** Provides the listings for repair operation for this component. */
    public List<ListingForRepair> listingsForRepair(long memberId) throws SQLException {
        List<ListingForRepair> rows = new ArrayList<>();
        String sql = """
                SELECT l.* FROM Listings l JOIN Sellers s ON s.id = l.sellerId
                LEFT JOIN TextbookDetails t ON t.listingId = l.id
                LEFT JOIN ElectronicsDetails e ON e.listingId = l.id
                LEFT JOIN FurnitureDetails f ON f.listingId = l.id
                WHERE s.memberId = ? AND t.listingId IS NULL
                  AND e.listingId IS NULL AND f.listingId IS NULL ORDER BY l.id
                """;
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new ListingForRepair(result.getLong("id"), result.getLong("sellerId"),
                            result.getLong("categoryId"), result.getString("title"),
                            BigDecimal.valueOf(result.getLong("priceMinor"), 2),
                            result.getString("currency"), result.getString("status"),
                            result.getString("type")));
                }
            }
        }
        return rows;
    }

    /**
     * Restores missing condition evidence transactionally after rechecking
     * ownership.
     */
    public void restoreDetails(long memberId, Listing observed) throws SQLException {
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                // Recheck ownership, type and missing rows at save time, not just in the
                // dialog.
                String sql = """
                        SELECT s.memberId, l.type,
                          (SELECT COUNT(*) FROM TextbookDetails WHERE listingId = l.id)
                        + (SELECT COUNT(*) FROM ElectronicsDetails WHERE listingId = l.id)
                        + (SELECT COUNT(*) FROM FurnitureDetails WHERE listingId = l.id) AS details
                        FROM Listings l JOIN Sellers s ON s.id = l.sellerId WHERE l.id = ?
                        """;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, observed.getId());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("Listing does not exist.");
                        }
                        if (result.getLong("memberId") != memberId) {
                            throw new IllegalArgumentException(
                                    "Only the listing owner can restore details.");
                        }
                        if (!result.getString("type").equals(observed.getType())) {
                            throw new IllegalArgumentException(
                                    "Condition details must match the listing type.");
                        }
                        if (result.getInt("details") != 0) {
                            throw new IllegalArgumentException(
                                    "Details already exist. No record was replaced.");
                        }
                    }
                }
                if (!(observed instanceof Textbook) && !(observed instanceof Electronics)
                        && !(observed instanceof Furniture)) {
                    throw new IllegalArgumentException("Unsupported listing type.");
                }
                insertDetails(connection, observed.getId(), observed);
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                Database.rollback(connection, error);
                throw error;
            }
        }
    }

    /** Removes an active listing without deleting its related records. */
    public void withdrawListing(long listingId, long memberId) throws SQLException {
        changeStatus(listingId, memberId, Listing.REMOVED);
    }

    /** Marks an owner's active item as sold, retaining reviews and the waitlist. */
    public void markSold(long listingId, long memberId) throws SQLException {
        changeStatus(listingId, memberId, Listing.SOLD);
    }

    private void changeStatus(long listingId, long memberId, String status) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement("""
                        UPDATE Listings SET status = ? WHERE id = ? AND status = ?
                        AND sellerId IN (SELECT id FROM Sellers WHERE memberId = ?)
                        """)) {
            statement.setString(1, status);
            statement.setLong(2, listingId);
            statement.setString(3, Listing.ACTIVE);
            statement.setLong(4, memberId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Only the owner can change an active listing.");
            }
        }
    }
}
