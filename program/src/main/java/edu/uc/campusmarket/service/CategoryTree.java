package edu.uc.campusmarket.service;

import edu.uc.campusmarket.model.Category;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates and recursively traverses categories at arbitrary nesting depth.
 */
public final class CategoryTree {
    private CategoryTree() {
    }

    /** Return a category and all its descendants, however deeply nested. */
    public static List<Long> categoryIdsUnder(long id, List<Category> categories) {
        validateCategories(categories);
        findCategory(id, categories);
        List<Long> ids = new ArrayList<>();
        collectCategoryIds(id, categories, ids);
        return ids;
    }

    private static void collectCategoryIds(long id, List<Category> categories, List<Long> ids) {
        ids.add(id);
        for (Category child : categories) {
            if (child.getParentId() != null && child.getParentId() == id) {
                collectCategoryIds(child.getId(), categories, ids); // Recursive call.
            }
        }
        // Base case: a leaf has no children, so no recursive call occurs and we return.
    }

    /** Rejects duplicate categories, missing parents and cyclic relationships. */
    public static void validateCategories(List<Category> categories) {
        Set<Long> uniqueIds = new HashSet<>();
        for (Category category : categories) {
            if (!uniqueIds.add(category.getId())) {
                throw new IllegalArgumentException("Duplicate category ID.");
            }
            Set<Long> ancestors = new HashSet<>();
            Category current = category;
            while (current != null) {
                if (!ancestors.add(current.getId())) {
                    throw new IllegalArgumentException("Category tree contains a cycle.");
                }
                Long parentId = current.getParentId();
                current = parentId == null ? null : findCategory(parentId, categories);
            }
        }
    }

    private static Category findCategory(long id, List<Category> categories) {
        for (Category category : categories) {
            if (category.getId() == id) {
                return category;
            }
        }
        throw new IllegalArgumentException("Category or parent category does not exist: " + id);
    }

}
