package edu.uc.campusmarket.service;

import edu.uc.campusmarket.dao.CategoryDao;
import edu.uc.campusmarket.dao.ListingDao;
import edu.uc.campusmarket.dao.SellerDao;
import edu.uc.campusmarket.model.Category;
import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.model.ListingForRepair;
import edu.uc.campusmarket.model.Member;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

/** Listing validation, searching, ownership and condition restoration. */
public class ListingService {
    private static final int RECENT_DETAIL_LIMIT = 3;
    private static final DateTimeFormatter DETAIL_DATE = DateTimeFormatter
            .ofPattern("d MMM yyyy, HH:mm 'UTC'", java.util.Locale.ENGLISH)
            .withZone(java.time.ZoneOffset.UTC);
    private static final int MAX_RECENT_VIEWS = 20;
    private final Map<Long, Stack<Long>> histories = new HashMap<>();
    private final MemberService members;
    private final SellerDao sellers;
    private final ReviewService reviews;
    private final WaitlistService waitlists;
    private final ListingDao listings;
    private final CategoryDao categories;

    /**
     * Uses focused account, review and waitlist services instead of a shared
     * facade.
     */
    public ListingService(ListingDao listings, CategoryDao categories, SellerDao sellers,
            MemberService members, ReviewService reviews, WaitlistService waitlists) {
        this.listings = listings;
        this.categories = categories;
        this.sellers = sellers;
        this.members = members;
        this.reviews = reviews;
        this.waitlists = waitlists;
    }

    /** Provides the categories operation for this component. */
    public List<Category> categories() {
        return ServiceCall.get(() -> {
            List<Category> result = categories.getCategories();
            CategoryTree.validateCategories(result);
            return result;
        });
    }

    /** Finds listings matching the category, title and ownership filter. */
    public List<Listing> search(long member, long category, String query, boolean own) {
        List<Long> ids = CategoryTree.categoryIdsUnder(category, categories());
        String text = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        List<Listing> saved = own ? myListings(member) : ServiceCall.get(listings::getListings);
        return saved.stream().filter(item -> own || Listing.ACTIVE.equals(item.getStatus()))
                .filter(item -> ids.contains(item.getCategoryId()))
                .filter(item -> item.getTitle().toLowerCase(Locale.ROOT).contains(text)).toList();
    }

    /**
     * Filters an owner's archive without exposing sold/removed items in public
     * search.
     */
    public List<Listing> search(long member, long category, String query, boolean own,
            String status) {
        return search(member, category, query, own).stream()
                .filter(item -> !own || "ALL".equals(status) || item.getStatus().equals(status))
                .toList();
    }

    /** Creates a listing using the signed-in member's seller identity. */
    public long post(long member, String title, String price, String type, long category,
            String evidence) {
        BigDecimal amount = parsePrice(price);
        return ServiceCall.get(() -> {
            long seller = sellerFor(member);
            Listing item = ListingFactory.create(0, seller, category, title, amount, "PHP",
                    "ACTIVE", type, evidence);
            return post(member, item);
        });
    }

    /**
     * Validates an edit using the model's title/money rules; type and condition
     * stay unchanged.
     */
    public void update(long member, Listing original, String title, String price) {
        Listing validated = Listing.incomplete(original.getId(), original.getSellerId(),
                original.getCategoryId(), title, parsePrice(price), original.getCurrency(),
                original.getStatus(), original.getCreatedAt(), original.getType(),
                "Edit validation");
        ServiceCall.get(() -> {
            // Checking the member and owner at write time prevents stale UI edits.
            sellerFor(member);
            listings.update(original.getId(), member, validated.getTitle(),
                    validated.getPriceMinor());
            return true;
        });
    }

    private BigDecimal parsePrice(String text) {
        String message = "Price must be a number like 250.00.";
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        try {
            return new BigDecimal(text.strip());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(message, error);
        }
    }

    /**
     * Checks form values before asynchronous persistence, using the model's rules.
     */
    public String validateForm(String title, String price, String type, String evidence) {
        try {
            ListingFactory.create(0, 1, 1, title, parsePrice(price), "PHP", "ACTIVE", type,
                    evidence);
            return "";
        } catch (IllegalArgumentException failure) {
            return failure.getMessage();
        }
    }

    /**
     * Checks only editable values while retaining the saved listing's other fields.
     */
    public String validateEdit(Listing original, String title, String price) {
        try {
            Listing.incomplete(original.getId(), original.getSellerId(), original.getCategoryId(),
                    title, parsePrice(price), original.getCurrency(), original.getStatus(),
                    original.getCreatedAt(), original.getType(), "Edit validation");
            return "";
        } catch (IllegalArgumentException failure) {
            return failure.getMessage();
        }
    }

    /** Withdraws an available listing only for its owner. */
    public void withdraw(long member, long id) {
        ServiceCall.get(() -> {
            members.require(member);
            listings.withdrawListing(id, member);
            return true;
        });
    }

    /** Records an owner-confirmed sale without deleting the listing. */
    public void markSold(long member, long id) {
        ServiceCall.get(() -> {
            members.require(member);
            listings.markSold(id, member);
            return true;
        });
    }

    /** Provides the repairable operation for this component. */
    public List<ListingForRepair> repairable(long member) {
        return ServiceCall.get(() -> listings.listingsForRepair(member));
    }

    /**
     * Restores condition evidence only for an owned listing with missing details.
     */
    public void restore(long member, ListingForRepair item, String evidence) {
        Listing observed = ListingFactory.create(item.id(), item.sellerId(), item.categoryId(),
                item.title(), item.price(), item.currency(), item.status(), item.type(), evidence);
        ServiceCall.get(() -> {
            members.require(member);
            listings.restoreDetails(member, observed);
            return true;
        });
    }

    /** Loads the selected listing's reviews, waitlist and recent-view details. */
    public String details(long member, Listing item) {
        return ServiceCall.get(() -> {
            if (Listing.ACTIVE.equals(item.getStatus())) {
                view(member, item.getId());
            }
            StringBuilder text = new StringBuilder(item.getTitle() + " | " + item.getCurrency()
                    + " " + item.getPrice() + "\nStatus: " + item.getStatus() + "\nListed: "
                    + (item.getCreatedAt() == null ? "Date not recorded"
                            : DETAIL_DATE.format(item.getCreatedAt()))
                    + "\nCondition: " + item.getConditionLabel());
            if (!item.hasConditionDetails()) {
                text.append("\n").append(item.getConditionIssue());
            }
            text.append("\nReviews:\n").append(reviews.summary(item.getId()));
            int position = waitlists.position(item.getId(), member);
            text.append("\nYour waitlist position: ")
                    .append(position == 0 ? "Not joined" : position);
            text.append("\nWaitlist (first buyer first): ")
                    .append(buyerNames(waitlists.order(item.getId())));
            text.append("\nRecently viewed (newest first):");
            List<Listing> recent = recent(member);
            for (int i = 0; i < Math.min(RECENT_DETAIL_LIMIT, recent.size()); i++) {
                text.append("\n- ").append(recent.get(i).getTitle());
            }
            return text.toString();
        });
    }

    private String buyerNames(List<Long> ids) throws java.sql.SQLException {
        List<Member> members = this.members.identities();
        StringBuilder names = new StringBuilder();
        for (long id : ids) {
            for (Member member : members) {
                if (member.getId() == id) {
                    if (!names.isEmpty()) {
                        names.append(" -> ");
                    }
                    names.append(member.getName());
                }
            }
        }
        return names.isEmpty() ? "No buyers yet." : names.toString();
    }

    /** Resolves a seller profile only for a known signed-in member. */
    public long sellerFor(long member) {
        members.require(member);
        return ServiceCall.get(() -> sellers.ensureSeller(member));
    }

    /**
     * Validates typed listing ownership before saving its matching condition row.
     */
    public long post(long member, Listing item) {
        if (item.getSellerId() != sellerFor(member)) {
            throw new IllegalArgumentException("Post using your own seller profile.");
        }
        if (!Listing.ACTIVE.equals(item.getStatus())) {
            throw new IllegalArgumentException("New listings must be active.");
        }
        return ServiceCall.get(() -> listings.insertListing(item));
    }

    /** Reads every owned lifecycle state without exposing it in public search. */
    public List<Listing> myListings(long member) {
        long seller = sellerFor(member);
        return new ArrayList<>(ServiceCall.get(listings::getListings).stream()
                .filter(item -> item.getSellerId() == seller).toList());
    }

    /** Adds a visible listing to the active member's bounded recent-view stack. */
    public void view(long memberId, long listingId) {
        members.require(memberId);
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
    public List<Listing> recent(long memberId) {
        members.require(memberId);
        List<Listing> results = new ArrayList<>();
        Stack<Long> history = histories.get(memberId);
        if (history == null) {
            return results;
        }
        // Pop a copy so reading history does not destroy the member's real stack.
        Stack<Long> newestFirst = new Stack<>();
        newestFirst.addAll(history);
        List<Listing> saved = ServiceCall.get(listings::getListings);
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

    private Listing findListing(long id) {
        return ServiceCall.get(listings::getListings).stream().filter(item -> item.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Listing does not exist."));
    }
}
