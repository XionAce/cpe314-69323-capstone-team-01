package edu.uc.campusmarket.model;

import java.time.Instant;

/** One saved message, ordered by its database ID rather than the sender's clock. */
public record ChatMessage(long id, long senderId, String senderName, String body, Instant sentAt) {
}
