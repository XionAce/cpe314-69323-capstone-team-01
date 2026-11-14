package edu.uc.campusmarket.controller;

import edu.uc.campusmarket.model.ChatMessage;
import edu.uc.campusmarket.model.ChatSnapshot;
import edu.uc.campusmarket.model.ChatThread;
import edu.uc.campusmarket.service.ChatService;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** Owns private chat state, worker requests and stale-session/selection rejection. */
public class ChatController {
    private final ChatService service;
    private final TaskRunner tasks;
    private final UserSession session;
    private final ObservableList<ChatThread> threads = FXCollections.observableArrayList();
    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
    private final SimpleObjectProperty<ChatThread> selected = new SimpleObjectProperty<>();
    private final StringProperty draft = new SimpleStringProperty("");
    private long listingId, buyerId, request;
    private boolean pending;
    private boolean publishing;

    /** Clears private data on account change and defers navigation until the worker is idle. */
    public ChatController(ChatService service, TaskRunner tasks, UserSession session) {
        this.service = service;
        this.tasks = tasks;
        this.session = session;
        session.memberProperty().addListener((o, before, after) -> clear());
        tasks.busyProperty().addListener((o, before, busy) -> {
            if (!busy && pending) {
                pending = false;
                refresh(true);
            }
        });
    }

    private void clear() {
        request++;
        listingId = buyerId = 0;
        pending = false;
        threads.clear();
        messages.clear();
        selected.set(null);
        draft.set("");
    }

    /** Opens the selected listing's seller chat, or its existing buyer conversations for an owner. */
    public void open(long itemId) {
        listingId = itemId;
        buyerId = 0;
        request++;
        selected.set(null);
        messages.clear();
        draft.set("");
        refresh(false);
    }

    /** Selects a private conversation without carrying a draft across recipients. */
    public void select(ChatThread thread) {
        if (thread == null || (listingId == thread.listingId() && buyerId == thread.buyerId())) {
            return;
        }
        listingId = thread.listingId();
        buyerId = thread.buyerId();
        request++;
        selected.set(thread);
        messages.clear();
        draft.set("");
        refresh(false);
    }

    /** Refreshes from SQLite so separate application windows see committed replies. */
    public void refresh(boolean quiet) {
        if (session.member() == null) {
            return;
        }
        if (tasks.busyProperty().get()) {
            pending = true;
            return;
        }
        long member = session.memberId(), generation = session.generation(), ticket = request;
        long item = listingId, buyer = buyerId;
        tasks.run(() -> service.load(member, item, buyer),
                result -> apply(result, generation, ticket), quiet);
    }

    private void apply(ChatSnapshot result, long generation, long ticket) {
        if (generation != session.generation() || ticket != request) {
            return;
        }
        publishing = true;
        try {
            if (result.selected() != null) {
                listingId = result.selected().listingId();
                buyerId = result.selected().buyerId();
            }
            if (!threads.equals(result.threads())) {
                threads.setAll(result.threads());
            }
            selected.set(result.selected());
            if (!messages.equals(result.messages())) {
                messages.setAll(result.messages());
            }
        } finally {
            publishing = false;
        }
    }

    /** Captures the recipient and text, saving once and clearing only that unchanged draft. */
    public void send() {
        ChatThread thread = selected.get();
        if (thread == null || tasks.busyProperty().get()) {
            return;
        }
        long member = session.memberId(), generation = session.generation(), ticket = request;
        String text = draft.get();
        tasks.run(() -> {
            service.send(member, thread.listingId(), thread.buyerId(), text);
            return service.load(member, thread.listingId(), thread.buyerId());
        }, result -> {
            if (generation == session.generation() && ticket == request) {
                if (text.equals(draft.get())) {
                    draft.set("");
                }
                apply(result, generation, ticket);
            }
        });
    }

    /** Returns the signed-in member ID for presentation of incoming and outgoing bubbles. */
    public long memberId() {
        return session.member() == null ? 0 : session.memberId();
    }

    /** Distinguishes an inbox refresh from a user deliberately choosing another recipient. */
    public boolean isPublishing() {
        return publishing;
    }

    /** Exposes only this account's inbox rows. */
    public ObservableList<ChatThread> threads() {
        return threads;
    }

    /** Exposes only the currently selected conversation's ordered messages. */
    public ObservableList<ChatMessage> messages() {
        return messages;
    }

    /** Supplies selected conversation metadata for view bindings. */
    public ReadOnlyObjectProperty<ChatThread> selectedProperty() {
        return selected;
    }

    /** Holds the unsent text for this selection only. */
    public StringProperty draftProperty() {
        return draft;
    }
}
