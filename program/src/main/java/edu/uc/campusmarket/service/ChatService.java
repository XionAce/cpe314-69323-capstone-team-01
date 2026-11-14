package edu.uc.campusmarket.service;

import edu.uc.campusmarket.dao.ChatDao;
import edu.uc.campusmarket.model.ChatSnapshot;
import edu.uc.campusmarket.model.ChatThread;
import java.util.ArrayList;
import java.util.List;

/** Applies message limits and loads private buyer-seller conversation snapshots. */
public class ChatService {
    public static final int MAX_MESSAGE_LENGTH = 2000;
    private final ChatDao chats;

    /** Receives the SQL gateway without exposing it to a view. */
    public ChatService(ChatDao chats) {
        this.chats = chats;
    }

    /** Loads an inbox; a zero buyer ID requests a chat from a selected listing. */
    public ChatSnapshot load(long memberId, long listingId, long buyerId) {
        return ServiceCall.get(() -> {
            List<ChatThread> threads = new ArrayList<>(chats.threads(memberId));
            ChatThread selected = choose(threads, listingId, buyerId);
            if (listingId > 0 && selected == null) {
                selected = chats.thread(listingId, buyerId > 0 ? buyerId : memberId, memberId);
                if (selected != null) {
                    threads.addFirst(selected);
                }
            }
            var messages = selected == null ? List.<edu.uc.campusmarket.model.ChatMessage>of()
                    : chats.messages(selected.listingId(), selected.buyerId(), memberId);
            return new ChatSnapshot(threads, selected, messages);
        });
    }

    private ChatThread choose(List<ChatThread> threads, long listingId, long buyerId) {
        if (listingId == 0) {
            return threads.isEmpty() ? null : threads.getFirst();
        }
        return threads.stream()
                .filter(thread -> thread.listingId() == listingId
                        && (buyerId == 0 || thread.buyerId() == buyerId))
                .findFirst().orElse(null);
    }

    /** Stores plain text as a prepared value; sending never joins a waitlist or buys an item. */
    public void send(long memberId, long listingId, long buyerId, String message) {
        String text = message == null ? "" : message.strip();
        if (text.isEmpty() || text.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Enter a message of 1-2000 characters.");
        }
        ServiceCall.get(() -> {
            chats.send(listingId, buyerId, memberId, text);
            return true;
        });
    }
}
