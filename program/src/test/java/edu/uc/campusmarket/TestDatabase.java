package edu.uc.campusmarket;

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
import edu.uc.campusmarket.service.MarketplaceService;
import edu.uc.campusmarket.service.SalesHistoryImporter;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

/**
 * Test-only convenience fixture; assertions still execute the production DAOs.
 */
public class TestDatabase extends Database {
    public TestDatabase(Path file) {
        super(file);
    }

    public MarketplaceService market() {
        return new MarketplaceService(this, new MemberDao(this), new SellerDao(this),
                new CategoryDao(this), new ListingDao(this), new InterestDao(this),
                new ReviewDao(this), new AnalyticsDao(this), new HistoryDao(this));
    }

    public SalesHistoryImporter importer() {
        return new SalesHistoryImporter(this, new CategoryDao(this), new HistoryDao(this));
    }

    /** Verifies a salted password hash and returns the matching member. */
    public Member authenticate(String username, char[] password) throws SQLException {
        return new MemberDao(this).authenticate(username, password);
    }

    /**
     * Consumes a recovery code and returns a replacement after updating the
     * password.
     */
    public String resetPassword(String username, char[] recoveryCode, char[] newPassword)
            throws SQLException {
        return new MemberDao(this).resetPassword(username, recoveryCode, newPassword);
    }

    /** Loads member identities without exposing stored credential hashes. */
    public List<Member> getMembers() throws SQLException {
        return new MemberDao(this).getMembers();
    }

    /** Provides the ensure seller operation for this component. */
    public long ensureSeller(long memberId) throws SQLException {
        return new SellerDao(this).ensureSeller(memberId);
    }

    /** Returns the stored category hierarchy. */
    public List<Category> getCategories() throws SQLException {
        return new CategoryDao(this).getCategories();
    }

    /** Loads listings together with the matching subtype evidence. */
    public List<Listing> getListings() throws SQLException {
        return new ListingDao(this).getListings();
    }

    /** Provides the insert listing operation for this component. */
    public long insertListing(Listing listing) throws SQLException {
        return new ListingDao(this).insertListing(listing);
    }

    /** Provides the listings for repair operation for this component. */
    public List<ListingForRepair> listingsForRepair(long memberId) throws SQLException {
        return new ListingDao(this).listingsForRepair(memberId);
    }

    /**
     * Restores missing condition evidence transactionally after rechecking
     * ownership.
     */
    public void restoreDetails(long memberId, Listing observed) throws SQLException {
        new ListingDao(this).restoreDetails(memberId, observed);
    }

    /** Provides the withdraw listing operation for this component. */
    public void withdrawListing(long listingId, long memberId) throws SQLException {
        new ListingDao(this).withdrawListing(listingId, memberId);
    }

    /** Returns buyer identities in first-in-first-out joining order. */
    public List<Long> getWaitlist(long listingId) throws SQLException {
        return new InterestDao(this).getWaitlist(listingId);
    }

    /** Provides the add interest operation for this component. */
    public void addInterest(long listingId, long memberId) throws SQLException {
        new InterestDao(this).addInterest(listingId, memberId);
    }

    /**
     * Saves a review after validating identity, ownership, interest and
     * duplication.
     */
    public void addReview(long listingId, long memberId, int rating, String comment)
            throws SQLException {
        new ReviewDao(this).addReview(listingId, memberId, rating, comment);
    }

    /** Loads saved reviews in their recorded order. */
    public String reviews(long listingId) throws SQLException {
        return new ReviewDao(this).reviews(listingId);
    }

    /** Provides the history preview operation for this component. */
    public String historyPreview() throws SQLException {
        return new HistoryDao(this).historyPreview();
    }

    /**
     * Reads available-listing price averages for one currency without review
     * fan-out.
     */
    public List<CategorySummary> priceSummary(String currency) throws SQLException {
        return new AnalyticsDao(this).priceSummary(currency);
    }

    /** Reads averages weighted by individual saved reviews. */
    public List<CategorySummary> ratingSummary() throws SQLException {
        return new AnalyticsDao(this).ratingSummary();
    }

}
