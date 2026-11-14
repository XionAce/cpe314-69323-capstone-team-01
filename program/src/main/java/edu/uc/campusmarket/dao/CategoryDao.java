package edu.uc.campusmarket.dao;

import edu.uc.campusmarket.model.Category;
import edu.uc.campusmarket.persistence.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL operations for this table or closely related aggregate. */
public class CategoryDao {
    private final Database database;

    /** Creates this component from the supplied values or dependencies. */
    public CategoryDao(Database database) {
        this.database = database;
    }

    /** Returns the stored category hierarchy. */
    public List<Category> getCategories() throws SQLException {
        List<Category> categories = new ArrayList<>();
        try (Connection connection = database.open();
                PreparedStatement statement = connection
                        .prepareStatement("SELECT id, parentId, name FROM Categories ORDER BY id");
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                long parent = result.getLong("parentId");
                Long parentId = result.wasNull() ? null : parent;
                categories.add(
                        new Category(result.getLong("id"), parentId, result.getString("name")));
            }
        }
        return categories;
    }

}
