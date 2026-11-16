package edu.uc.campusmarket.service;

import edu.uc.campusmarket.TestDatabase;
import edu.uc.campusmarket.dao.CategoryDao;
import edu.uc.campusmarket.dao.InterestDao;
import edu.uc.campusmarket.dao.ListingDao;
import edu.uc.campusmarket.dao.MemberDao;
import edu.uc.campusmarket.dao.ReviewDao;
import edu.uc.campusmarket.dao.SellerDao;
import edu.uc.campusmarket.model.Category;
import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.model.Member;
import java.sql.SQLException;
import java.util.List;

/**
 * Test-only adapter: legacy workflow assertions execute the focused production
 * services.
 */
public final class MarketplaceFixture {
    private final TestDatabase database;
    private final MemberService members;
    private final ReviewService reviews;
    private final ListingService listings;
    private final WaitlistService waitlists;

    public MarketplaceFixture(TestDatabase database) {
        this.database = database;
        members = new MemberService(database, new MemberDao(database));
        reviews = new ReviewService(new ReviewDao(database));
        waitlists = new WaitlistService(new InterestDao(database), members);
        listings = new ListingService(new ListingDao(database), new CategoryDao(database),
                new SellerDao(database), members, reviews, waitlists);
    }

    public void initialize() throws SQLException {
        database.initialize();
        listings.categories();
    }

    public ListingService listings() {
        return listings;
    }

    public WaitlistService waitlists() {
        return waitlists;
    }

    public List<Category> getCategories() {
        return listings.categories();
    }

    public List<Member> getMembers() {
        return members.identities();
    }

    public Member login(String user, char[] password) {
        return members.login(user, password);
    }

    public long sellerFor(long member) {
        return listings.sellerFor(member);
    }

    public long post(long member, Listing item) {
        return listings.post(member, item);
    }

    public List<Listing> search(long category, String query) {
        return listings.search(0, category, query, false);
    }

    public List<Listing> myListings(long member) {
        return listings.myListings(member);
    }

    public void view(long member, long listing) {
        listings.view(member, listing);
    }

    public List<Listing> recent(long member) {
        return listings.recent(member);
    }

    public void withdraw(long member, long listing) {
        listings.withdraw(member, listing);
    }

    public void markSold(long member, long listing) {
        listings.markSold(member, listing);
    }

    public List<Long> joinWaitlist(long listing, long member) {
        return waitlists.join(listing, member);
    }

    public List<Long> getWaitlist(long listing) {
        return waitlists.order(listing);
    }

    public void review(long listing, long member, int rating, String comment) {
        members.require(member);
        reviews.submit(listing, member, rating, comment);
    }
}
