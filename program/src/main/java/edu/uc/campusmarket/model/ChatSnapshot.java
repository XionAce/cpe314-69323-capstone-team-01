package edu.uc.campusmarket.model;

import java.util.List;

/** An immutable inbox and selected conversation returned by background work. */
public record ChatSnapshot(List<ChatThread> threads, ChatThread selected,
        List<ChatMessage> messages) {
    /** Copies collections so views cannot change a loaded result. */
    public ChatSnapshot {
        threads = List.copyOf(threads);
        messages = List.copyOf(messages);
    }
}
