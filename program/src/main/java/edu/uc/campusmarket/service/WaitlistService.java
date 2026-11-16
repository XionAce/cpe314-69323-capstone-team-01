package edu.uc.campusmarket.service;

import edu.uc.campusmarket.dao.InterestDao;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/** Owns FIFO waitlist rules independently of listing and account operations. */
public class WaitlistService {
    private final InterestDao interests;
    private final MemberService members;

    /** Uses the waitlist DAO and the account identity validator. */
    public WaitlistService(InterestDao interests, MemberService members) {
        this.interests = interests;
        this.members = members;
    }

    /** Adds a signed-in buyer atomically to another owner's active item. */
    public List<Long> join(long listing, long member) {
        members.require(member);
        return ServiceCall.get(() -> {
            interests.addInterest(listing, member);
            return order(listing);
        });
    }

    /** Deletes only this member's row, without rewriting other joining orders. */
    public void leave(long listing, long member) {
        members.require(member);
        ServiceCall.get(() -> {
            interests.leave(listing, member);
            return true;
        });
    }

    /** Counts earlier remaining entries to obtain the buyer's current position. */
    public int position(long listing, long member) {
        members.require(member);
        return ServiceCall.get(() -> interests.position(listing, member));
    }

    /** Uses queue tail/head operations to preserve the saved FIFO display order. */
    public List<Long> order(long listing) {
        Queue<Long> buyers = new ArrayDeque<>(
                ServiceCall.get(() -> interests.getWaitlist(listing)));
        List<Long> result = new ArrayList<>();
        while (!buyers.isEmpty()) {
            result.add(buyers.poll());
        }
        return result;
    }
}
