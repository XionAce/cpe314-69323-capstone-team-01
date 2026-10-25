// Category navigation is a business rule, so it stays independent of JavaFX and JDBC.
package edu.uc.campusmarket.service;

// Import the data objects and standard collection types used to index/search the tree.
import edu.uc.campusmarket.model.Category;
import edu.uc.campusmarket.model.Listing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A validated snapshot of a category forest and recursive subtree searches. */
// A forest is several root trees: Textbooks, Electronics, and Furniture may each be a root.
public final class CategoryService {
    // Map key = category ID, value = the category object. LinkedHashMap remembers insertion order.
    private final Map<Long, Category> categories = new LinkedHashMap<>();
    // Map key = parent ID, value = IDs of its immediate children. A leaf needs no entry here.
    private final Map<Long, List<Long>> children = new HashMap<>();

    // Build and validate indexes once for the supplied category data.
    public CategoryService(List<Category> categories) {
        // Reject a missing list before trying to loop over it.
        if (categories == null) {
            throw new IllegalArgumentException("Categories are required.");
        }
        // Enhanced for-loop: visit every supplied category in turn.
        for (Category category : categories) {
            // Each list element must be an actual Category object.
            if (category == null) {
                throw new IllegalArgumentException("A category must not be null.");
            }
            // putIfAbsent keeps the first value and returns an old value if the ID already exists.
            // this.categories means the field, unlike categories, which is the constructor argument.
            if (this.categories.putIfAbsent(category.id(), category) != null) {
                throw new IllegalArgumentException("Duplicate category ID: " + category.id());
            }
        }
        // After every ID has been indexed, connect each non-root category to its parent.
        for (Category category : this.categories.values()) {
            // A null parent ID means root; it has no parent-child link to add.
            if (category.parentId() != null) {
                // A child must not point to a parent missing from the supplied category data.
                if (!this.categories.containsKey(category.parentId())) {
                    throw new IllegalArgumentException("Missing parent for category: " + category.id());
                }
                // computeIfAbsent creates an empty child list only the first time a parent is seen.
                // key -> new ArrayList<>() is a lambda: a small function that supplies that list.
                children.computeIfAbsent(category.parentId(), key -> new ArrayList<>())
                        .add(category.id());
            }
        }
        // complete remembers nodes whose descendants were already verified as cycle-free.
        Set<Long> complete = new HashSet<>();
        // Check every node, including a disconnected cyclic component with no valid root.
        for (long categoryId : this.categories.keySet()) {
            // A new visiting Set tracks the current recursion path, separately from completed work.
            validateAcyclic(categoryId, new HashSet<>(), complete);
        }
    }

    // Find listings in the selected category AND all its descendants, at any number of levels.
    public List<Listing> listingsUnder(long categoryId, List<Listing> listings) {
        // The requested starting node must exist in the validated category snapshot.
        if (!categories.containsKey(categoryId)) {
            throw new IllegalArgumentException("Unknown category ID: " + categoryId);
        }
        // anyMatch uses the lambda listing -> ... to reject null elements as well as a null list.
        if (listings == null || listings.stream().anyMatch(listing -> listing == null)) {
            throw new IllegalArgumentException("Listings must not be null or contain null entries.");
        }
        // A Set stores each qualifying category ID once.
        Set<Long> descendants = new HashSet<>();
        // This genuinely recursive helper fills the Set, including the chosen category itself.
        collectSubtree(categoryId, descendants);
        // A stream filters the supplied listings, keeping only those whose category ID is in the Set.
        return listings.stream().filter(listing -> descendants.contains(listing.categoryId())).toList();
    }

    // private limits this implementation helper to CategoryService; result accumulates all visited IDs.
    private void collectSubtree(long categoryId, Set<Long> result) {
        // Include the current category before descending into its children.
        result.add(categoryId);
        // getOrDefault returns an empty List for a leaf, avoiding a null check.
        List<Long> childIds = children.getOrDefault(categoryId, List.of());
        // Recursion must stop somewhere: a leaf has nothing left to visit.
        if (childIds.isEmpty()) {
            return; // Base case: a leaf category has no children to search.
        }
        // Recursive case: repeat the same operation for each immediate child.
        for (long childId : childIds) {
            // This call reaches grandchildren, then deeper descendants, without hardcoded levels.
            collectSubtree(childId, result);
        }
    }

    // Detect cycles before searching, so a path such as A -> B -> A cannot recurse indefinitely.
    private void validateAcyclic(long categoryId, Set<Long> visiting, Set<Long> complete) {
        // Base case: a fully checked node and its descendants do not need checking again.
        if (complete.contains(categoryId)) {
            return;
        }
        // Set.add returns false if this ID is already on the current path, which proves a cycle.
        if (!visiting.add(categoryId)) {
            throw new IllegalArgumentException("Category tree contains a cycle at: " + categoryId);
        }
        // Recursively validate all children; an empty list ends the descent for a leaf.
        for (long childId : children.getOrDefault(categoryId, List.of())) {
            validateAcyclic(childId, visiting, complete);
        }
        // Backtrack: we have finished this path, so this node is no longer being visited.
        visiting.remove(categoryId);
        // Memoize successful validation to avoid repeating completed subtree work.
        complete.add(categoryId);
    }
}
