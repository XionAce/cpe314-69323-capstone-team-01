package edu.uc.campusmarket.service;

import edu.uc.campusmarket.dao.AnalyticsDao;
import edu.uc.campusmarket.dao.CategoryDao;
import edu.uc.campusmarket.dao.HistoryDao;
import edu.uc.campusmarket.dao.InterestDao;
import edu.uc.campusmarket.dao.ListingDao;
import edu.uc.campusmarket.dao.MemberDao;
import edu.uc.campusmarket.dao.ReviewDao;
import edu.uc.campusmarket.dao.SellerDao;
import edu.uc.campusmarket.model.Category;
import edu.uc.campusmarket.model.CategorySummary;
import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.model.ListingForRepair;
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
    private static final int MAX_RECENT_VIEWS = 20;
    private final Database database;
    private final MemberDao memberDao;
    private final SellerDao sellerDao;
    private final CategoryDao categoryDao;
    private final ListingDao listingDao;
    private final InterestDao interestDao;
    private final ReviewDao reviewDao;
    private final AnalyticsDao analyticsDao;
    private final HistoryDao historyDao;

    private List<Category> categories = new ArrayList<>();
    // Each member has a separate recently-viewed stack for this session.
    private final Map<Long, Stack<Long>> histories = new HashMap<>();

    /** Creates this component from the supplied values or dependencies. */
    public MarketplaceService(Database database, MemberDao memberDao, SellerDao sellerDao,
            CategoryDao categoryDao, ListingDao listingDao, InterestDao interestDao,
            ReviewDao reviewDao, AnalyticsDao analyticsDao, HistoryDao historyDao) {
        this.database = database;
        this.memberDao = memberDao;
        this.sellerDao = sellerDao;
        this.categoryDao = categoryDao;
        this.listingDao = listingDao;
        this.interestDao = interestDao;
        this.reviewDao = reviewDao;
        this.analyticsDao = analyticsDao;
        this.historyDao = historyDao;
    }

    /**
     * Initializes the schema or account state while preserving existing records.
     */
    public void initialize() throws SQLException {
        database.initialize();

        categories = categoryDao.getCategories();
        validateCategories(categories);
    }

    /** Provides the database path operation for this component. */
    public Path databasePath() {
        return database.getPath();
    }

    /**
     * Verifies the supplied credentials before establishing the current account.
     */
    public Member login(String username, char[] password) throws SQLException {
        return memberDao.authenticate(username, password);
    }

    /**
     * Consumes a recovery code and returns a replacement after updating the
     * password.
     */
    public String resetPassword(String username, char[] recoveryCode, char[] newPassword)
            throws SQLException {
        return memberDao.resetPassword(username, recoveryCode, newPassword);
    }

    /** Provides the importer operation for this component. */
    public SalesHistoryImporter importer() {
        return new SalesHistoryImporter(database, categoryDao, historyDao);
    }

    /** Provides the prices operation for this component. */
    public List<CategorySummary> prices(String currency) throws SQLException {
        return analyticsDao.priceSummary(currency);
    }

    /** Provides the ratings operation for this component. */
    public List<CategorySummary> ratings() throws SQLException {
        return analyticsDao.ratingSummary();
    }

    /** Provides the history preview operation for this component. */
    public String historyPreview() throws SQLException {
        return historyDao.historyPreview();
    }

    /** Loads saved reviews in their recorded order. */
    public String reviews(long listingId) throws SQLException {
        return reviewDao.reviews(listingId);
    }

    /** Provides the review operation for this component. */
    public void review(long listingId, long memberId, int rating, String comment)
            throws SQLException {
        requireMember(memberId);
        reviewDao.addReview(listingId, memberId, rating, comment);
    }

    /** Loads member identities without exposing stored credential hashes. */
    public List<Member> getMembers() throws SQLException {
        return memberDao.getMembers();
    }

    /** Returns the stored category hierarchy. */
    public List<Category> getCategories() {
        return new ArrayList<>(categories);
    }

    /** Provides the listings for repair operation for this component. */
    public List<ListingForRepair> listingsForRepair(long memberId) throws SQLException {
        requireMember(memberId);
        return listingDao.listingsForRepair(memberId);
    }

    /**
     * Restores missing condition evidence transactionally after rechecking
     * ownership.
     */
    public void restoreDetails(long memberId, Listing observed) throws SQLException {
        requireMember(memberId);
        listingDao.restoreDetails(memberId, observed);
    }

    /** Finds listings matching the category, title and ownership filter. */
    public List<Listing> search(long categoryId, String keyword) throws SQLException {
        List<Long> categoryIds = categoryIdsUnder(categoryId, categories);
        String query = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Listing> results = new ArrayList<>();
        // This SELECT reads the saved records again, including newly posted items.
        for (Listing listing : listingDao.getListings()) {
            if (listing.getStatus().equals("ACTIVE")
                    && categoryIds.contains(listing.getCategoryId())
                    && listing.getTitle().toLowerCase(Locale.ROOT).contains(query)) {
                results.add(listing);
            }
        }
        return results;
    }

    /** Provides the seller for operation for this component. */
    public long sellerFor(long memberId) throws SQLException {
        requireMember(memberId);
        return sellerDao.ensureSeller(memberId);
    }

    /** Creates a listing using the signed-in member's seller identity. */
    public long post(long memberId, Listing listing) throws SQLException {
        long sellerId = sellerFor(memberId);
        if (listing.getSellerId() != sellerId) {
            throw new IllegalArgumentException("Post using your own seller profile.");
        }
        if (!listing.getStatus().equals("ACTIVE")) {
            throw new IllegalArgumentException("New listings must be active.");
        }
        return listingDao.insertListing(listing);
    }

    /** Provides the my listings operation for this component. */
    public List<Listing> myListings(long memberId) throws SQLException {
        long sellerId = sellerFor(memberId);
        List<Listing> results = new ArrayList<>();
        for (Listing listing : listingDao.getListings()) {
            if (listing.getSellerId() == sellerId) {
                results.add(listing);
            }
        }
        return results;
    }

    /** Withdraws an available listing only for its owner. */
    public void withdraw(long memberId, long listingId) throws SQLException {
        requireMember(memberId);
        listingDao.withdrawListing(listingId, memberId);
    }

    /** Marks an active listing sold only for the signed-in owner. */
    public void markSold(long memberId, long listingId) throws SQLException {
        requireMember(memberId);
        listingDao.markSold(listingId, memberId);
    }

    /** Provides the join waitlist operation for this component. */
    public List<Long> joinWaitlist(long listingId, long memberId) throws SQLException {
        requireMember(memberId);
        interestDao.addInterest(listingId, memberId);
        return getWaitlist(listingId);
    }

    /** Returns buyer identities in first-in-first-out joining order. */
    public List<Long> getWaitlist(long listingId) throws SQLException {
        // Database rows arrive in joining order. offer() adds to the queue's tail.
        Queue<Long> buyers = new ArrayDeque<>();
        for (Long buyerId : interestDao.getWaitlist(listingId)) {
            buyers.offer(buyerId);
        }
        List<Long> order = new ArrayList<>();
        // poll() removes from the head: the first buyer who joined is displayed first.
        while (!buyers.isEmpty()) {
            order.add(buyers.poll());
        }
        return order;
    }

    /** Adds a visible listing to the active member's bounded recent-view stack. */
    public void view(long memberId, long listingId) throws SQLException {
        requireMember(memberId);
        Listing listing = findListing(listingId);
        if (!listing.getStatus().equals("ACTIVE")) {
            throw new IllegalArgumentException("This listing was withdrawn.");
        }
        Stack<Long> history = histories.get(memberId);
        if (history == null) {
            history = new Stack<>();
            histories.put(memberId, history);
        }
        history.remove(Long.valueOf(listingId));
        history.push(listingId); // Most recently viewed item is now on top.
        if (history.size() > MAX_RECENT_VIEWS) {
            history.remove(0);
        }
    }

    /** Reads the member's recent items without consuming their actual stack. */
    public List<Listing> recent(long memberId) throws SQLException {
        requireMember(memberId);
        List<Listing> results = new ArrayList<>();
        Stack<Long> history = histories.get(memberId);
        if (history == null) {
            return results;
        }
        // Pop a copy so reading history does not destroy the member's real stack.
        Stack<Long> newestFirst = new Stack<>();
        newestFirst.addAll(history);
        List<Listing> saved = listingDao.getListings();
        while (!newestFirst.empty()) {
            long id = newestFirst.pop();
            for (Listing listing : saved) {
                if (listing.getId() == id && listing.getStatus().equals("ACTIVE")) {
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

    private Listing findListing(long id) throws SQLException {
        for (Listing listing : listingDao.getListings()) {
            if (listing.getId() == id) {
                return listing;
            }
        }
        throw new IllegalArgumentException("Listing does not exist.");
    }

    private void requireMember(long id) throws SQLException {
        for (Member member : memberDao.getMembers()) {
            if (member.getId() == id) {
                return;
            }
        }
        throw new IllegalArgumentException("Sign in with an existing member.");
    }
}
