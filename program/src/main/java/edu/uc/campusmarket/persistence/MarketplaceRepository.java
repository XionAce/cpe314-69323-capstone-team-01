package edu.uc.campusmarket.persistence;

import edu.uc.campusmarket.model.BuyerInterest;
import edu.uc.campusmarket.model.Category;
import edu.uc.campusmarket.model.Electronics;
import edu.uc.campusmarket.model.Furniture;
import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.model.Member;
import edu.uc.campusmarket.model.Seller;
import edu.uc.campusmarket.model.Textbook;
import edu.uc.campusmarket.model.WearLevel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** SQL and transactions only: this class has no JavaFX dependency. */
public final class MarketplaceRepository {
    // The schema version describes database structure, separately from the design document's V0.2.
    private static final int SCHEMA_VERSION = 1;
    // A compatible saved database must contain these tables; we never silently replace it.
    private static final List<String> TABLES = List.of("Members", "Sellers", "Categories", "Listings",
            "Reviews", "BuyerInterests", "HistoricalSales", "TextbookDetails", "ElectronicsDetails",
            "FurnitureDetails");

    // Reuse this SELECT whenever we need complete Listing objects, including subclass evidence.
    // l, t, e, and f are short SQL aliases. LEFT JOIN keeps a listing even when a detail row is absent.
    // The aliases textbookId/electronicsId/furnitureId let readListing detect missing or extra details.
    // The triple quotes delimit one Java text-block string containing SQL, not Java instructions.
    private static final String LISTING_SELECT = """
            SELECT l.id, l.sellerId, l.categoryId, l.type, l.title, l.priceMinor, l.currency, l.status,
                   t.listingId AS textbookId, t.missingPages, t.markingLevel, t.coverDamaged,
                   e.listingId AS electronicsId, e.powersOn, e.hasFunctionalDefect, e.hasCosmeticDamage,
                   f.listingId AS furnitureId, f.structurallyStable, f.hasStructuralDamage, f.surfaceWear
            FROM Listings l
            LEFT JOIN TextbookDetails t ON t.listingId = l.id
            LEFT JOIN ElectronicsDetails e ON e.listingId = l.id
            LEFT JOIN FurnitureDetails f ON f.listingId = l.id
            """;

    // This helper knows the database path and how to open a fresh connection for an operation.
    private final Database database;

    public MarketplaceRepository(Database database) {
        // Constructor injection supplies the dependency; a null database would make every query fail.
        this.database = Objects.requireNonNull(database, "Database is required.");
    }

    public Path databasePath() {
        // Expose the path for display without exposing a live connection to the UI.
        return database.path();
    }

    /** A new database and its fictional sample data are created atomically, once. */
    public void initialize() throws SQLException {
        // try-with-resources closes the connection automatically when this block finishes.
        try (Connection connection = database.open()) {
            // Disable auto-commit so schema creation, demo inserts, and version marking save together.
            connection.setAutoCommit(false);
            try {
                int version;
                // user_version is an integer SQLite keeps for the application's own schema tracking.
                try (PreparedStatement statement = connection.prepareStatement("PRAGMA user_version");
                        ResultSet result = statement.executeQuery()) {
                    // ResultSet starts before row 1. next() advances it before a column can be read.
                    version = result.next() ? result.getInt(1) : 0;
                }
                if (version == 0) {
                    // Version 0 is only treated as new when there are no existing application tables.
                    if (hasExistingTables(connection)) {
                        throw new SQLException("This database contains unversioned tables. It was not changed. "
                                + "Choose a new data directory or restore a known compatible database.");
                    }
                    // These helpers share this transaction; neither commits independently.
                    createSchema(connection);
                    seedDemoData(connection);
                    // Mark success inside the same transaction, so a partial setup cannot look complete.
                    try (PreparedStatement statement = connection.prepareStatement("PRAGMA user_version = 1")) {
                        statement.execute();
                    }
                } else if (version == SCHEMA_VERSION) {
                    // Reopening a known database checks its tables and preserves the user's records.
                    verifyTables(connection);
                } else {
                    // A different version may need a future migration; guessing could damage data.
                    throw new SQLException("Unsupported database schema version " + version
                            + ". This application supports version " + SCHEMA_VERSION
                            + "; the existing database was not changed.");
                }
                // Commit makes all changes from this successful transaction permanent.
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                // A SQL failure or invalid Java value cancels the entire transaction before rethrowing.
                rollback(connection, error);
                throw error;
            }
        }
    }

    public List<Member> members() throws SQLException {
        // All three resources close automatically, in reverse order: result, statement, connection.
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement("SELECT id, name FROM Members ORDER BY id");
                ResultSet result = statement.executeQuery()) {
            // Build a Java list from the rows returned by SELECT, which is ordered by member ID.
            List<Member> members = new ArrayList<>();
            while (result.next()) {
                // Read named columns from the current row and construct one Member value object.
                members.add(new Member(result.getLong("id"), result.getString("name")));
            }
            // Return an unmodifiable copy, preventing callers from changing this result list.
            return List.copyOf(members);
        }
    }

    public List<Seller> sellers() throws SQLException {
        // executeQuery() is for SQL that returns rows; executeUpdate() is used for data changes.
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, memberId, displayName, sellerKind FROM Sellers ORDER BY id");
                ResultSet result = statement.executeQuery()) {
            List<Seller> sellers = new ArrayList<>();
            while (result.next()) {
                // id identifies the seller profile; memberId identifies the person who owns it.
                sellers.add(new Seller(result.getLong("id"), result.getLong("memberId"),
                        result.getString("displayName"), result.getString("sellerKind")));
            }
            return List.copyOf(sellers);
        }
    }

    /** Creates a seller profile on first use; reuses it on later visits. */
    public Seller sellerForMember(long memberId) throws SQLException {
        // A member may browse first and obtain a seller profile when they first open the posting form.
        try (Connection connection = database.open()) {
            // Keep member validation, optional profile creation, and profile retrieval together.
            connection.setAutoCommit(false);
            try {
                String name;
                // ? is a data placeholder. JDBC parameters are numbered from 1, not 0.
                try (PreparedStatement statement = connection.prepareStatement("SELECT name FROM Members WHERE id = ?")) {
                    // Bind the member's numeric ID as data rather than joining it into SQL text.
                    statement.setLong(1, memberId);
                    try (ResultSet result = statement.executeQuery()) {
                        // No matching row means the supplied member ID is invalid.
                        if (!result.next()) throw new IllegalArgumentException("Choose a valid marketplace member.");
                        name = result.getString("name");
                    }
                }
                // INSERT ... SELECT ... WHERE NOT EXISTS creates a profile only if this member has none.
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO Sellers (memberId, displayName, sellerKind) "
                                + "SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM Sellers WHERE memberId = ?)")) {
                    // Fill the four placeholders in the order in which they appear in the SQL.
                    statement.setLong(1, memberId); statement.setString(2, name);
                    statement.setString(3, "STUDENT"); statement.setLong(4, memberId);
                    // This may insert one row or zero rows when a profile already exists.
                    statement.executeUpdate();
                }
                Seller profile;
                // Read the saved profile, regardless of whether it was created just now or earlier.
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, memberId, displayName, sellerKind FROM Sellers WHERE memberId = ? ORDER BY id LIMIT 1")) {
                    statement.setLong(1, memberId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new SQLException("Could not open this member's seller profile.");
                        // Keep both IDs: listing.sellerId must use profile.id(), not memberId directly.
                        profile = new Seller(result.getLong("id"), result.getLong("memberId"),
                                result.getString("displayName"), result.getString("sellerKind"));
                    }
                }
                // Only return the profile after its creation (if needed) has been committed.
                connection.commit();
                return profile;
            } catch (SQLException | RuntimeException error) {
                rollback(connection, error);
                throw error;
            }
        }
    }

    public List<Category> categories() throws SQLException {
        // Load flat database rows; CategoryService later organizes them into a parent/child tree.
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, parentId, name FROM Categories ORDER BY id");
                ResultSet result = statement.executeQuery()) {
            List<Category> categories = new ArrayList<>();
            while (result.next()) {
                // getLong returns 0 for SQL NULL, so immediately inspect wasNull() to distinguish it.
                long parent = result.getLong("parentId");
                // Long can hold null (a root category); primitive long cannot.
                Long parentId = result.wasNull() ? null : parent;
                categories.add(new Category(result.getLong("id"), parentId, result.getString("name")));
            }
            return List.copyOf(categories);
        }
    }

    public List<Listing> listings() throws SQLException {
        // This returns active and withdrawn records; service/UI decide which belong in each view.
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(LISTING_SELECT + " ORDER BY l.id");
                ResultSet result = statement.executeQuery()) {
            List<Listing> listings = new ArrayList<>();
            while (result.next()) {
                // Mapping reconstructs the correct concrete Java subclass for each saved row.
                listings.add(readListing(result));
            }
            return List.copyOf(listings);
        }
    }

    public Listing insertListing(Listing listing) throws SQLException {
        // Models use ID 0 to mean 'not saved yet'; SQLite assigns the persistent ID.
        Objects.requireNonNull(listing, "Listing is required.");
        if (listing.id() != 0) {
            throw new IllegalArgumentException("A new listing must have id 0; SQLite assigns its saved ID.");
        }
        try (Connection connection = database.open()) {
            // One logical listing needs a common row AND one subtype-details row, or neither.
            connection.setAutoCommit(false);
            try {
                // The private overload uses the same connection and returns the newly saved object.
                Listing saved = insertListing(connection, listing);
                connection.commit();
                return saved;
            } catch (SQLException | RuntimeException error) {
                rollback(connection, error);
                throw error;
            }
        }
    }

    /** Withdraws only an owner's available listing, retaining its related records. */
    public Listing withdrawListing(long memberId, long listingId) throws SQLException {
        // Withdrawal archives the listing by status. It does not DELETE reviews, interests, or details.
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                // Check ownership inside the UPDATE itself, instead of trusting a visible UI button.
                // EXISTS follows listing -> seller profile -> owning member before allowing the change.
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE Listings SET status = ?
                        WHERE id = ? AND status <> ?
                          AND EXISTS (SELECT 1 FROM Sellers s
                                      WHERE s.id = Listings.sellerId AND s.memberId = ?)
                        """)) {
                    // Parameter order: new status, listing ID, excluded old status, owning member ID.
                    statement.setString(1, "WITHDRAWN");
                    statement.setLong(2, listingId);
                    statement.setString(3, "WITHDRAWN");
                    statement.setLong(4, memberId);
                    // executeUpdate returns affected-row count. Exactly one row must be changed.
                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException("Only the owner can remove an available listing. "
                                + "It may already be withdrawn or no longer exist.");
                    }
                }
                Listing withdrawn;
                // Reload all saved fields so the returned object contains status WITHDRAWN.
                try (PreparedStatement statement = connection.prepareStatement(LISTING_SELECT + " WHERE l.id = ?")) {
                    statement.setLong(1, listingId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new SQLException("Could not reload the withdrawn listing.");
                        withdrawn = readListing(result);
                    }
                }
                connection.commit();
                return withdrawn;
            } catch (SQLException | RuntimeException error) {
                rollback(connection, error);
                throw error;
            }
        }
    }

    public List<BuyerInterest> interests(long listingId) throws SQLException {
        // This public entry point owns a fresh connection; the private overload only reads on it.
        try (Connection connection = database.open()) {
            return interests(connection, listingId);
        }
    }

    public List<BuyerInterest> joinInterest(long listingId, long buyerId) throws SQLException {
        // Validation, position allocation, insertion, and reloading form one transaction.
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                // Reject a missing member/listing, withdrawn item, owner, or duplicate buyer.
                requireEligibleBuyer(connection, listingId, buyerId);
                long order;
                // MAX finds the final position. COALESCE changes NULL to 0 for an empty waitlist.
                // Adding 1 allocates position 1 first, then 2, then 3, preserving FIFO order.
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COALESCE(MAX(joiningOrder), 0) + 1 FROM BuyerInterests WHERE listingId = ?")) {
                    // Bind the listing ID so positions are calculated separately for each item.
                    statement.setLong(1, listingId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Could not allocate a buyer interest position.");
                        }
                        // This SELECT returns a single numeric column, read using column index 1.
                        order = result.getLong(1);
                    }
                }
                // Persist the buyer's allocated position. Database UNIQUE rules also reject duplicates.
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO BuyerInterests (listingId, buyerId, joiningOrder) VALUES (?, ?, ?)")) {
                    // These three numeric values match (listingId, buyerId, joiningOrder).
                    statement.setLong(1, listingId);
                    statement.setLong(2, buyerId);
                    statement.setLong(3, order);
                    statement.executeUpdate();
                }
                // Reload using joiningOrder so the caller receives the complete ordered waitlist.
                List<BuyerInterest> waitlist = interests(connection, listingId);
                connection.commit();
                return waitlist;
            } catch (SQLException | RuntimeException error) {
                rollback(connection, error);
                throw error;
            }
        }
    }

    private boolean hasExistingTables(Connection connection) throws SQLException {
        // sqlite_schema is SQLite's catalog of tables, indexes, and other schema objects.
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM sqlite_schema WHERE type = ? AND name NOT LIKE ?")) {
            // Ignore SQLite's own internal tables, whose names start with sqlite_.
            statement.setString(1, "table");
            statement.setString(2, "sqlite_%");
            try (ResultSet result = statement.executeQuery()) {
                // One returned row is sufficient to know the file is not an empty application database.
                return result.next();
            }
        }
    }

    private void verifyTables(Connection connection) throws SQLException {
        // Reuse one prepared query while binding a different expected table name each iteration.
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM sqlite_schema WHERE type = ? AND name = ?")) {
            for (String table : TABLES) {
                // The table name is a comparison VALUE here, so binding it to ? is valid.
                statement.setString(1, "table");
                statement.setString(2, table);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        // Do not recreate missing tables and accidentally hide damage to a saved file.
                        throw new SQLException("Schema version 1 is missing table " + table
                                + ". The application will not silently rebuild or reseed this database.");
                    }
                }
            }
        }
    }

    private void createSchema(Connection connection) throws SQLException {
        String schema;
        // A leading / resolves this bundled file from the classpath, including inside a built JAR.
        // InputStream reads the schema file; this try-with-resources closes it after reading.
        try (InputStream input = MarketplaceRepository.class.getResourceAsStream("/db/schema-v1.sql")) {
            if (input == null) {
                throw new SQLException("The application is missing /db/schema-v1.sql.");
            }
            // Decode the resource bytes explicitly as UTF-8 rather than a computer-specific default.
            schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            // Present resource-reading failure through the same SQLException API as JDBC failures.
            throw new SQLException("The bundled database schema could not be read.", error);
        }
        // This controlled resource contains DDL only, with no semicolons in strings.
        // Remove whole-line SQL comments, then assemble the remaining CREATE statements.
        String ddl = schema.lines().filter(line -> !line.stripLeading().startsWith("--"))
                .reduce("", (text, line) -> text + line + "\n");
        // Split this controlled schema at semicolons; this is not a general-purpose SQL parser.
        for (String sql : ddl.split(";")) {
            if (!sql.isBlank()) {
                // Execute each table/index definition on the initialization transaction's connection.
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.execute();
                }
            }
        }
    }

    private void seedDemoData(Connection connection) throws SQLException {
        // These fictional records make a newly created prototype immediately usable for a demo.
        // initialize() calls this only for a new empty database, not whenever the app restarts.
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO Members (id, name) VALUES (?, ?)")) {
            String[] names = {"Demo Seller (fictional)", "Demo Buyer A (fictional)", "Demo Buyer B (fictional)"};
            for (int i = 0; i < names.length; i++) {
                // Array indexes start at 0; sample member IDs start at 1. L marks a long literal.
                statement.setLong(1, i + 1L);
                statement.setString(2, names[i]);
                statement.executeUpdate();
            }
        }
        // Initially only member 1 has a seller profile. Other members get one on first use.
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Sellers (id, memberId, displayName, sellerKind) VALUES (?, ?, ?, ?)")) {
            statement.setLong(1, 1);
            statement.setLong(2, 1);
            statement.setString(3, "Campus Demo Seller (fictional)");
            statement.setString(4, "STUDENT");
            statement.executeUpdate();
        }
        // null means root. The numbered parent links form Electronics -> Computers -> Laptops -> Gaming.
        List<Category> categories = List.of(new Category(1, null, "Textbooks"),
                new Category(2, null, "Electronics"), new Category(3, 2L, "Computers"),
                new Category(4, 3L, "Laptops"), new Category(5, 4L, "Gaming Laptops"),
                new Category(6, null, "Furniture"));
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Categories (id, parentId, name) VALUES (?, ?, ?)")) {
            for (Category category : categories) {
                statement.setLong(1, category.id());
                if (category.parentId() == null) {
                    // Store actual SQL NULL for root categories rather than a fake ID such as 0.
                    statement.setNull(2, java.sql.Types.INTEGER);
                } else {
                    statement.setLong(2, category.parentId());
                }
                statement.setString(3, category.name());
                statement.executeUpdate();
            }
        }
        // Each object supplies both common Listing fields and its own condition evidence.
        // Underscores make Java numbers easier to read: 35_000 means 35000 centavos = PHP 350.00.
        // Use the connection overload so all sample listings remain in initialization's transaction.
        insertListing(connection, new Textbook(0, 1, 1, "Demo: Introduction to Java", 35_000,
                "PHP", "ACTIVE", 0, WearLevel.LIGHT, false));
        insertListing(connection, new Electronics(0, 1, 5, "Demo: Used gaming laptop", 1_500_000,
                "PHP", "ACTIVE", true, false, true));
        insertListing(connection, new Furniture(0, 1, 6, "Demo: Study table", 180_000,
                "PHP", "ACTIVE", true, false, WearLevel.LIGHT));
    }

    private Listing insertListing(Connection connection, Listing listing) throws SQLException {
        // Ensure the enum discriminator agrees with the real Java subclass before writing either row.
        requireMatchingSubtype(listing);
        // Insert shared fields first. Every ? receives a bound value below, in the same column order.
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO Listings (sellerId, categoryId, type, title, priceMinor, currency, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            // Methods such as title() are model accessors; they return the object's stored values.
            statement.setLong(1, listing.sellerId());
            statement.setLong(2, listing.categoryId());
            // Store an enum's stable name, such as TEXTBOOK, rather than its object representation.
            statement.setString(3, listing.type().name());
            statement.setString(4, listing.title());
            // A long preserves the integer minor-unit amount without floating-point rounding.
            statement.setLong(5, listing.priceMinor());
            statement.setString(6, listing.currency());
            statement.setString(7, listing.status());
            statement.executeUpdate();
        }
        long id;
        // SQLite remembers the most recent inserted row ID on THIS connection.
        // Read it before inserting the detail row so both rows use the same new listing ID.
        try (PreparedStatement statement = connection.prepareStatement("SELECT last_insert_rowid()");
                ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("SQLite did not return the new listing ID.");
            }
            id = result.getLong(1);
        }
        // Pattern matching checks the concrete type and gives a safely typed variable in each branch.
        if (listing instanceof Textbook textbook) {
            // Textbooks store pages, markings, and cover damage rather than electronics/furniture fields.
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO TextbookDetails (listingId, missingPages, markingLevel, coverDamaged)
                    VALUES (?, ?, ?, ?)
                    """)) {
                // Reuse the parent row's ID as the detail row's primary key and foreign key.
                statement.setLong(1, id);
                statement.setInt(2, textbook.missingPages());
                statement.setString(3, textbook.markingLevel().name());
                // SQLite stores this Java boolean as 1 for true or 0 for false.
                statement.setInt(4, textbook.coverDamaged() ? 1 : 0);
                statement.executeUpdate();
            }
        } else if (listing instanceof Electronics electronics) {
            // Electronics have three boolean evidence fields, each converted to SQLite's 0/1 form.
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO ElectronicsDetails (listingId, powersOn, hasFunctionalDefect, hasCosmeticDamage)
                    VALUES (?, ?, ?, ?)
                    """)) {
                statement.setLong(1, id);
                statement.setInt(2, electronics.powersOn() ? 1 : 0);
                statement.setInt(3, electronics.hasFunctionalDefect() ? 1 : 0);
                statement.setInt(4, electronics.hasCosmeticDamage() ? 1 : 0);
                statement.executeUpdate();
            }
        } else if (listing instanceof Furniture furniture) {
            // Furniture combine boolean safety evidence with a named WearLevel value.
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO FurnitureDetails (listingId, structurallyStable, hasStructuralDamage, surfaceWear)
                    VALUES (?, ?, ?, ?)
                    """)) {
                statement.setLong(1, id);
                statement.setInt(2, furniture.structurallyStable() ? 1 : 0);
                statement.setInt(3, furniture.hasStructuralDamage() ? 1 : 0);
                statement.setString(4, furniture.surfaceWear().name());
                statement.executeUpdate();
            }
        }
        // Read back the saved parent + detail rows rather than returning the original ID-0 object.
        try (PreparedStatement statement = connection.prepareStatement(LISTING_SELECT + " WHERE l.id = ?")) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("The newly saved listing could not be read.");
                }
                return readListing(result);
            }
        }
    }

    private void requireMatchingSubtype(Listing listing) {
        // A switch expression returns true only when type() matches the object's concrete class.
        boolean matches = switch (listing.type()) {
            case TEXTBOOK -> listing instanceof Textbook;
            case ELECTRONICS -> listing instanceof Electronics;
            case FURNITURE -> listing instanceof Furniture;
        };
        if (!matches) {
            // Detect a broken model contract before saving an inconsistent database discriminator.
            throw new IllegalArgumentException("Listing type must match its concrete Java class.");
        }
    }

    private Listing readListing(ResultSet result) throws SQLException {
        // Map the ResultSet's current row; the caller already advanced it with next().
        long id = result.getLong("id");
        // LEFT JOIN gives NULL for a missing details row. getObject preserves that NULL distinction.
        boolean textbook = result.getObject("textbookId") != null;
        boolean electronics = result.getObject("electronicsId") != null;
        boolean furniture = result.getObject("furnitureId") != null;
        // Convert each presence flag to 1/0 and require exactly one matching details row overall.
        if ((textbook ? 1 : 0) + (electronics ? 1 : 0) + (furniture ? 1 : 0) != 1) {
            throw new SQLException("Listing " + id + " must have exactly one subtype-details row.");
        }
        // All subclasses need these common Listing values from the parent table.
        long sellerId = result.getLong("sellerId");
        long categoryId = result.getLong("categoryId");
        String title = result.getString("title");
        long priceMinor = result.getLong("priceMinor");
        String currency = result.getString("currency");
        String status = result.getString("status");
        try {
            // The saved type selects which concrete Java object to reconstruct.
            // A List<Listing> can hold all three types; later gradeCondition() calls dispatch by subtype.
            return switch (result.getString("type")) {
                case "TEXTBOOK" -> {
                    // Exactly one details row is not enough: it must also be the correct table.
                    if (!textbook) {
                        throw new SQLException("Listing " + id + " has the wrong subtype-details table.");
                    }
                    // yield supplies this switch branch's object. valueOf restores the WearLevel enum.
                    // Comparing a stored integer to 1 restores the original Java boolean.
                    yield new Textbook(id, sellerId, categoryId, title, priceMinor, currency, status,
                            result.getInt("missingPages"), WearLevel.valueOf(result.getString("markingLevel")),
                            result.getInt("coverDamaged") == 1);
                }
                case "ELECTRONICS" -> {
                    if (!electronics) {
                        throw new SQLException("Listing " + id + " has the wrong subtype-details table.");
                    }
                    // The Electronics constructor validates the loaded common and evidence fields.
                    yield new Electronics(id, sellerId, categoryId, title, priceMinor, currency, status,
                            result.getInt("powersOn") == 1, result.getInt("hasFunctionalDefect") == 1,
                            result.getInt("hasCosmeticDamage") == 1);
                }
                case "FURNITURE" -> {
                    if (!furniture) {
                        throw new SQLException("Listing " + id + " has the wrong subtype-details table.");
                    }
                    // This branch restores Furniture evidence instead of treating all items alike.
                    yield new Furniture(id, sellerId, categoryId, title, priceMinor, currency, status,
                            result.getInt("structurallyStable") == 1, result.getInt("hasStructuralDamage") == 1,
                            WearLevel.valueOf(result.getString("surfaceWear")));
                }
                // Reject unexpected stored data rather than silently choosing the wrong Java class.
                default -> throw new SQLException("Listing " + id + " has an unsupported type.");
            };
        } catch (IllegalArgumentException error) {
            // Enum/model validation failures indicate invalid saved values and become database errors.
            throw new SQLException("Listing " + id + " contains invalid stored values.", error);
        }
    }

    private List<BuyerInterest> interests(Connection connection, long listingId) throws SQLException {
        // ORDER BY is essential: database rows have no guaranteed order without it.
        // Ascending joiningOrder reconstructs the persisted queue from earliest buyer to latest.
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, listingId, buyerId, joiningOrder FROM BuyerInterests
                WHERE listingId = ? ORDER BY joiningOrder
                """)) {
            statement.setLong(1, listingId);
            try (ResultSet result = statement.executeQuery()) {
                // Map each ordered row into a BuyerInterest record for the Java queue/service.
                List<BuyerInterest> interests = new ArrayList<>();
                while (result.next()) {
                    interests.add(new BuyerInterest(result.getLong("id"), result.getLong("listingId"),
                            result.getLong("buyerId"), result.getLong("joiningOrder")));
                }
                return List.copyOf(interests);
            }
        }
    }

    private void requireEligibleBuyer(Connection connection, long listingId, long buyerId) throws SQLException {
        // A buyer ID is a Members.id, not a Sellers.id. First ensure the member really exists.
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM Members WHERE id = ?")) {
            statement.setLong(1, buyerId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("The selected buyer does not exist.");
                }
            }
        }
        // Join Listings to Sellers to discover the owning MEMBER and the listing's current status.
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT s.memberId, l.status FROM Listings l JOIN Sellers s ON s.id = l.sellerId WHERE l.id = ?
                """)) {
            statement.setLong(1, listingId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("The selected listing does not exist.");
                }
                if ("WITHDRAWN".equals(result.getString("status"))) {
                    throw new SQLException("This listing has been withdrawn and is no longer accepting buyer interest.");
                }
                // Compare member IDs on both sides; seller profile IDs cannot establish person identity.
                if (result.getLong("memberId") == buyerId) {
                    throw new SQLException("A seller cannot join the buyer interest waitlist for their own listing.");
                }
            }
        }
        // Give a clear duplicate message before the schema's UNIQUE constraint rejects the insert.
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM BuyerInterests WHERE listingId = ? AND buyerId = ?")) {
            statement.setLong(1, listingId);
            statement.setLong(2, buyerId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    throw new SQLException("This member is already in the buyer interest waitlist.");
                }
            }
        }
    }

    private void rollback(Connection connection, Exception original) {
        try {
            // Undo every uncommitted change made by the failed operation on this connection.
            connection.rollback();
        } catch (SQLException error) {
            // Cleanup can fail too. Preserve it without replacing the error that caused the rollback.
            original.addSuppressed(error);
        }
    }
}
