package edu.uc.campusmarket.model;

/** A private conversation between one listing's seller and one buyer. */
public record ChatThread(long listingId, long buyerId, long sellerMemberId, String title,
        String buyerName, String sellerName, String listingStatus, String lastMessage,
        long lastMessageId) {
    /** Returns the other participant's display name for the signed-in member. */
    public String partnerName(long memberId) {
        return memberId == buyerId ? sellerName : buyerName;
    }

    /** Removed listings retain their chat history, but cannot receive new messages. */
    public boolean canSend() {
        return !Listing.REMOVED.equals(listingStatus);
    }
}
