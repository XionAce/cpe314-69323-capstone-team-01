// The service layer coordinates marketplace actions; the UI does not need to write SQL itself.
package edu.uc.campusmarket.service;

// Import models, the persistence repository, checked SQL errors, and standard Java helpers.
import edu.uc.campusmarket.model.BuyerInterest;
import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.model.Seller;
import edu.uc.campusmarket.persistence.MarketplaceRepository;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Coordinates use cases. All calls are serialized by the application's worker. */
// The worker performs one action at a time; these mutable collections are not general thread-safe maps.
public final class MarketplaceService {
    // private hides this dependency; final prevents replacing the repository after construction.
    private final MarketplaceRepository repository;
    // Map key = member ID. Each member gets a separate recently viewed stack for this app session.
    private final Map<Long, RecentlyViewedHistory> histories = new HashMap<>();

    // The application supplies a repository: the service uses it rather than opening SQL here.
    public MarketplaceService(MarketplaceRepository repository) {
        // Reject null immediately instead of failing later when a database action is requested.
        this.repository = Objects.requireNonNull(repository);
    }

    // throws SQLException passes database failures to the caller so the UI can report them.
    public void initialize() throws SQLException {
        // Create/check the schema and fictional initial data using the repository's transaction rules.
        repository.initialize();
        // Construction validates category parents and cycles even though this instance is not stored.
        new CategoryService(repository.categories());
    }

    // Bundle data for the interface; this runs on the worker before the UI receives the result.
    public MarketplaceSnapshot snapshot() throws SQLException {
        // Only available listings enter the public marketplace snapshot.
        // this::isAvailable is a method reference: apply this object's rule to each streamed listing.
        return new MarketplaceSnapshot(repository.members(), repository.sellers(),
                repository.categories(), repository.listings().stream().filter(this::isAvailable).toList(),
                repository.databasePath());
    }

    // Save one new listing. The member-aware UI entry point below adds ownership validation.
    public Listing postListing(Listing listing) throws SQLException {
        // Require an actual object before reading its fields.
        Objects.requireNonNull(listing, "A listing is required.");
        // The database assigns saved IDs; a new listing must start with the unsaved marker 0.
        if (listing.id() != 0) {
            throw new IllegalArgumentException("Post a new listing with an unassigned ID.");
        }
        // The repository inserts shared/subtype data together and returns the saved object with its ID.
        return repository.insertListing(listing);
    }

    // Get this member's seller profile, creating one in persistence if the member needs it to sell.
    public Seller sellerForMember(long memberId) throws SQLException {
        return repository.sellerForMember(memberId);
    }

    /** Never silently post on behalf of a different member. */
    public Listing postListingForMember(long memberId, Listing listing) throws SQLException {
        // First ensure the selected demo member exists.
        requireMember(memberId);
        // Reject a missing listing before comparing its seller ID.
        Objects.requireNonNull(listing, "A listing is required.");
        // noneMatch is true if no seller profile satisfies BOTH conditions (&&).
        // The seller-profile ID must match the listing AND that profile must belong to this member.
        if (repository.sellers().stream().noneMatch(profile -> profile.id() == listing.sellerId()
                && profile.memberId() == memberId)) {
            throw new IllegalArgumentException("This seller profile does not belong to the selected member.");
        }
        // Reuse the normal new-listing validation and transactional saving after ownership is checked.
        return postListing(listing);
    }

    // Long permits null for "All finds"; a nonnull ID selects a category subtree.
    public List<Listing> search(Long categoryId, String keyword) throws SQLException {
        // Read saved listings and filter withdrawn records before applying any public search criteria.
        List<Listing> candidates = repository.listings().stream().filter(this::isAvailable).toList();
        // Rebuild a validated category index from current database rows.
        CategoryService categories = new CategoryService(repository.categories());
        // All finds skips this branch; a selected category triggers the real recursive search.
        if (categoryId != null) {
            candidates = categories.listingsUnder(categoryId, candidates);
        }
        // ?: chooses an empty search for null; otherwise trim and normalize case consistently.
        // Locale.ROOT avoids computer-language-specific capitalization changing the comparison.
        String query = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        // The lambda keeps titles containing the query. Every title contains "", so blank means all.
        return candidates.stream()
                .filter(listing -> listing.title().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    // The repository enforces ownership/availability/duplicate rules and persists the buyer's position.
    public List<BuyerInterest> joinInterest(long listingId, long buyerId) throws SQLException {
        // Reconstruct a real Queue from saved rows, then return its read-only FIFO snapshot.
        return new BuyerInterestQueue(repository.joinInterest(listingId, buyerId)).entries();
    }

    // Withdrawal keeps the record and related history rather than deleting the item permanently.
    public Listing withdrawListing(long memberId, long listingId) throws SQLException {
        // Validate the acting member; persistence also verifies this member owns the listing.
        requireMember(memberId);
        // Return the listing after its status has become WITHDRAWN.
        return repository.withdrawListing(memberId, listingId);
    }

    /** Ownership follows the seller's member ID, not the seller profile's ID. */
    public List<Listing> listingsForMember(long memberId) throws SQLException {
        requireMember(memberId);
        // var lets Java infer Set<Long>. Collect all seller-profile IDs belonging to this member.
        var sellerIds = repository.sellers().stream()
                // filter keeps profiles owned by the selected member.
                .filter(seller -> seller.memberId() == memberId)
                // map extracts profile IDs; collect(toSet()) stores them in a membership-check Set.
                .map(seller -> seller.id()).collect(java.util.stream.Collectors.toSet());
        // Include all this member's listings, even withdrawn ones, for My listings and its archive.
        return repository.listings().stream()
                .filter(listing -> sellerIds.contains(listing.sellerId())).toList();
    }

    // Load the existing queue without adding anyone to it.
    public List<BuyerInterest> waitlist(long listingId) throws SQLException {
        // Queue construction sorts and validates saved entries, so display order follows joining order.
        return new BuyerInterestQueue(repository.interests(listingId)).entries();
    }

    // Remember a viewed listing for this member; this state stays in memory for the current session.
    public void recordView(long memberId, long listingId) throws SQLException {
        requireMember(memberId);
        // Find the requested saved listing. orElseThrow supplies an exception if no row matches.
        Listing selected = repository.listings().stream().filter(listing -> listing.id() == listingId)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("This listing no longer exists."));
        // Withdrawn items may be opened only by their owner through the archive.
        // ! is NOT, && is AND; the lambda checks the seller profile's owning member.
        if (!isAvailable(selected) && repository.sellers().stream().noneMatch(seller ->
                seller.id() == selected.sellerId() && seller.memberId() == memberId)) {
            throw new IllegalArgumentException("This listing has been withdrawn and is no longer available.");
        }
        // Lazily create a 20-item history for this member, then move this listing to its stack top.
        histories.computeIfAbsent(memberId, key -> new RecentlyViewedHistory(20)).record(listingId);
    }

    // Convert a member's remembered IDs back into available Listing objects for recent-view cards.
    public List<Listing> history(long memberId) throws SQLException {
        requireMember(memberId);
        // No history object exists until this member has opened a listing in this session.
        RecentlyViewedHistory history = histories.get(memberId);
        if (history == null) {
            // List.of() returns an empty, unmodifiable list for the UI's empty state.
            return List.of();
        }
        // Index available listings by ID for quick lookup in the remembered order.
        Map<Long, Listing> byId = new HashMap<>();
        for (Listing listing : repository.listings()) {
            // A withdrawn listing stays stored but no longer appears as a public recent-view card.
            if (isAvailable(listing)) byId.put(listing.id(), listing);
        }
        // map(byId::get) resolves each ID in newest-first order; filter drops missing/withdrawn items.
        return history.ids().stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    // This small shared rule defines public availability for the current milestone.
    private boolean isAvailable(Listing listing) {
        // String.equals compares text content; ! excludes the exact withdrawn status.
        return !"WITHDRAWN".equals(listing.status());
    }

    // A private helper ensures every member-specific action refers to a current demo member.
    private void requireMember(long memberId) throws SQLException {
        // noneMatch reports that no saved member has the requested ID.
        if (repository.members().stream().noneMatch(member -> member.id() == memberId)) {
            throw new IllegalArgumentException("Choose a valid demo member.");
        }
    }
}
