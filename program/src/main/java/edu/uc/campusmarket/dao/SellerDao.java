package edu.uc.campusmarket.dao;

import edu.uc.campusmarket.model.Seller;
import edu.uc.campusmarket.persistence.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL operations for this table or closely related aggregate. */
public class SellerDao {
    private final Database database;

    /** Creates this component from the supplied values or dependencies. */
    public SellerDao(Database database) {
        this.database = database;
    }

    /** Provides the ensure seller operation for this component. */
    public long ensureSeller(long memberId) throws SQLException {
        try (Connection connection = database.open()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM Sellers WHERE memberId = ? ORDER BY id LIMIT 1")) {
                statement.setLong(1, memberId);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        return result.getLong(1);
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO Sellers(memberId, displayName, sellerKind)
                    SELECT id, name, ? FROM Members WHERE id = ?
                    """)) {
                statement.setString(1, "STUDENT");
                statement.setLong(2, memberId);
                if (statement.executeUpdate() != 1) {
                    throw new IllegalArgumentException("Choose an existing member.");
                }
            }
            return Database.lastId(connection);
        }
    }

    /** Read seller identity separately from account identity. */
    public List<Seller> getSellers() throws SQLException {
        List<Seller> rows = new ArrayList<>();
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, memberId, displayName, sellerKind FROM Sellers ORDER BY id");
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                rows.add(new Seller(result.getLong(1), result.getLong(2), result.getString(3),
                        result.getString(4)));
            }
        }
        return List.copyOf(rows);
    }

}
