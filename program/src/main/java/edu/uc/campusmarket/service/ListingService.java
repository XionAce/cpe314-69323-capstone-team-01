package edu.uc.campusmarket.service;

import edu.uc.campusmarket.dao.CategoryDao;
import edu.uc.campusmarket.dao.ListingDao;
import edu.uc.campusmarket.model.Category;
import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.model.ListingForRepair;
import edu.uc.campusmarket.model.Member;
import java.math.BigDecimal;
import java.util.List;

/** Listing validation, searching, ownership and condition restoration. */
public class ListingService {
    private static final int RECENT_DETAIL_LIMIT = 3;
    private static final java.time.format.DateTimeFormatter DETAIL_DATE = java.time.format.DateTimeFormatter
            .ofPattern("d MMM yyyy, HH:mm 'UTC'", java.util.Locale.ENGLISH)
            .withZone(java.time.ZoneOffset.UTC);
    private final MarketplaceService market;
    private final ListingDao listings;
    private final CategoryDao categories;

    /** Creates this component from the supplied values or dependencies. */
    public ListingService(MarketplaceService market, ListingDao listings, CategoryDao categories) {
        this.market = market;
        this.listings = listings;
        this.categories = categories;
    }

    /** Provides the categories operation for this component. */
    public List<Category> categories() {
        return ServiceCall.get(() -> {
            market.initialize();
            return categories.getCategories();
        });
    }

    /** Finds listings matching the category, title and ownership filter. */
    public List<Listing> search(long member, long category, String query, boolean own) {
        return ServiceCall.get(() -> {
            if (!own) {
                return market.search(category, query);
            }
            List<Long> ids = MarketplaceService.categoryIdsUnder(category, market.getCategories());
            String text = query == null ? "" : query.strip().toLowerCase(java.util.Locale.ROOT);
            return market.myListings(member).stream()
                    .filter(item -> ids.contains(item.getCategoryId())).filter(item -> item
                            .getTitle().toLowerCase(java.util.Locale.ROOT).contains(text))
                    .toList();
        });
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
        return ServiceCall.get(() -> {
            long seller = market.sellerFor(member);
            Listing item = ListingFactory.create(0, seller, category, title, new BigDecimal(price),
                    "PHP", "ACTIVE", type, evidence);
            return market.post(member, item);
        });
    }

    /**
     * Validates an edit using the model's title/money rules; type and condition
     * stay unchanged.
     */
    public void update(long member, Listing original, String title, String price) {
        Listing validated = Listing.incomplete(original.getId(), original.getSellerId(),
                original.getCategoryId(), title, new BigDecimal(price), original.getCurrency(),
                original.getStatus(), original.getCreatedAt(), original.getType(),
                "Edit validation");
        ServiceCall.get(() -> {
            // Checking the member and owner at write time prevents stale UI edits.
            market.sellerFor(member);
            listings.update(original.getId(), member, validated.getTitle(),
                    validated.getPriceMinor());
            return true;
        });
    }

    /** Withdraws an available listing only for its owner. */
    public void withdraw(long member, long id) {
        ServiceCall.get(() -> {
            market.withdraw(member, id);
            return true;
        });
    }

    /** Records an owner-confirmed sale without deleting the listing. */
    public void markSold(long member, long id) {
        ServiceCall.get(() -> {
            market.markSold(member, id);
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
            market.restoreDetails(member, observed);
            return true;
        });
    }

    /** Loads the selected listing's reviews, waitlist and recent-view details. */
    public String details(long member, Listing item) {
        return ServiceCall.get(() -> {
            if (Listing.ACTIVE.equals(item.getStatus())) {
                market.view(member, item.getId());
            }
            StringBuilder text = new StringBuilder(item.getTitle() + " | " + item.getCurrency()
                    + " " + item.getPrice() + "\nStatus: " + item.getStatus() + "\nListed: "
                    + (item.getCreatedAt() == null ? "Date not recorded"
                            : DETAIL_DATE.format(item.getCreatedAt()))
                    + "\nCondition: " + item.getConditionLabel());
            if (!item.hasConditionDetails()) {
                text.append("\n").append(item.getConditionIssue());
            }
            text.append("\nReviews:\n").append(market.reviews(item.getId()));
            int position = market.waitlistPosition(item.getId(), member);
            text.append("\nYour waitlist position: ")
                    .append(position == 0 ? "Not joined" : position);
            text.append("\nWaitlist (first buyer first): ")
                    .append(buyerNames(market.getWaitlist(item.getId())));
            text.append("\nRecently viewed (newest first):");
            List<Listing> recent = market.recent(member);
            for (int i = 0; i < Math.min(RECENT_DETAIL_LIMIT, recent.size()); i++) {
                text.append("\n- ").append(recent.get(i).getTitle());
            }
            return text.toString();
        });
    }

    private String buyerNames(List<Long> ids) throws java.sql.SQLException {
        List<Member> members = market.getMembers();
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
}
