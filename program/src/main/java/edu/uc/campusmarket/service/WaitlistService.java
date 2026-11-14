package edu.uc.campusmarket.service;

import java.util.List;

/** Uses the retained FIFO queue and account checks. */
public class WaitlistService {
    private final MarketplaceService market;

    /** Creates this component from the supplied values or dependencies. */
    public WaitlistService(MarketplaceService market) {
        this.market = market;
    }

    /** Adds the active member to another owner's FIFO waitlist. */
    public List<Long> join(long listing, long member) {
        return ServiceCall.get(() -> market.joinWaitlist(listing, member));
    }

    /** Leaves only the requesting member's queue row. */
    public void leave(long listing, long member) {
        ServiceCall.get(() -> {
            market.leaveWaitlist(listing, member);
            return true;
        });
    }

    /** Reads the requesting member's live position, or zero when not joined. */
    public int position(long listing, long member) {
        return ServiceCall.get(() -> market.waitlistPosition(listing, member));
    }
}
