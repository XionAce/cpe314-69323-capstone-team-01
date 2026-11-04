package edu.uc.campusmarket.service;

import edu.uc.campusmarket.model.Category;
import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.model.Member;
import edu.uc.campusmarket.persistence.Database;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

/** Connects the screen's actions to the models and database. */
public class MarketplaceService {
    private final Database database;
    private List<Member> members = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    // Each member has a separate recently-viewed stack for this session.
    private final Map<Long, Stack<Long>> histories = new HashMap<>();

    public MarketplaceService(Database database) {
        this.database = database;
    }

    public void initialize() throws SQLException {
        database.initialize();
        members = database.getMembers();
        categories = database.getCategories();
        validateCategories(categories);
    }

    public Path databasePath() { return database.getPath(); }
    public List<Member> getMembers() { return new ArrayList<>(members); }
    public List<Category> getCategories() { return new ArrayList<>(categories); }

    public List<Listing> search(long categoryId, String keyword) throws SQLException {
        List<Long> categoryIds = categoryIdsUnder(categoryId, categories);
        String query = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Listing> results = new ArrayList<>();
        // This SELECT reads the saved records again, including newly posted items.
        for (Listing listing : database.getListings()) {
            if (listing.getStatus().equals("AVAILABLE")
                    && categoryIds.contains(listing.getCategoryId())
                    && listing.getTitle().toLowerCase(Locale.ROOT).contains(query)) {
                results.add(listing);
            }
        }
        return results;
    }

    public long sellerFor(long memberId) throws SQLException {
        requireMember(memberId);
        return database.ensureSeller(memberId);
    }

    public long post(long memberId, Listing listing) throws SQLException {
        long sellerId = sellerFor(memberId);
        if (listing.getSellerId() != sellerId) {
            throw new IllegalArgumentException("Post using your own seller profile.");
        }
        if (!listing.getStatus().equals("AVAILABLE")) {
            throw new IllegalArgumentException("New listings must be available.");
        }
        return database.insertListing(listing);
    }

    public List<Listing> myListings(long memberId) throws SQLException {
        long sellerId = sellerFor(memberId);
        List<Listing> results = new ArrayList<>();
        for (Listing listing : database.getListings()) {
            if (listing.getSellerId() == sellerId) results.add(listing);
        }
        return results;
    }

    public void withdraw(long memberId, long listingId) throws SQLException {
        requireMember(memberId);
        database.withdrawListing(listingId, memberId);
    }

    public List<Long> joinWaitlist(long listingId, long memberId) throws SQLException {
        requireMember(memberId);
        database.addInterest(listingId, memberId);
        return getWaitlist(listingId);
    }

    public List<Long> getWaitlist(long listingId) throws SQLException {
        // Database rows arrive in joining order. offer() adds to the queue's tail.
        Queue<Long> buyers = new ArrayDeque<>();
        for (Long buyerId : database.getWaitlist(listingId)) buyers.offer(buyerId);
        List<Long> order = new ArrayList<>();
        // poll() removes from the head: the first buyer who joined is displayed first.
        while (!buyers.isEmpty()) order.add(buyers.poll());
        return order;
    }

    public void view(long memberId, long listingId) throws SQLException {
        requireMember(memberId);
        Listing listing = findListing(listingId);
        if (!listing.getStatus().equals("AVAILABLE")) {
            throw new IllegalArgumentException("This listing was withdrawn.");
        }
        Stack<Long> history = histories.get(memberId);
        if (history == null) {
            history = new Stack<>();
            histories.put(memberId, history);
        }
        history.remove(Long.valueOf(listingId));
        history.push(listingId); // Most recently viewed item is now on top.
        if (history.size() > 20) history.remove(0);
    }

    public List<Listing> recent(long memberId) throws SQLException {
        requireMember(memberId);
        List<Listing> results = new ArrayList<>();
        Stack<Long> history = histories.get(memberId);
        if (history == null) return results;
        // Pop a copy so reading history does not destroy the member's real stack.
        Stack<Long> newestFirst = new Stack<>();
        newestFirst.addAll(history);
        List<Listing> saved = database.getListings();
        while (!newestFirst.empty()) {
            long id = newestFirst.pop();
            for (Listing listing : saved) {
                if (listing.getId() == id && listing.getStatus().equals("AVAILABLE")) {
                    results.add(listing);
                    break;
                }
            }
        }
        return results;
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
            if (category.getId() == id) return category;
        }
        throw new IllegalArgumentException("Category or parent category does not exist: " + id);
    }

    private Listing findListing(long id) throws SQLException {
        for (Listing listing : database.getListings()) {
            if (listing.getId() == id) return listing;
        }
        throw new IllegalArgumentException("Listing does not exist.");
    }

    private void requireMember(long id) {
        for (Member member : members) {
            if (member.getId() == id) return;
        }
        throw new IllegalArgumentException("Choose a valid demo member.");
    }
}
