package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.controller.ChatController;
import edu.uc.campusmarket.controller.TaskRunner;
import edu.uc.campusmarket.model.ChatMessage;
import edu.uc.campusmarket.model.ChatThread;
import edu.uc.campusmarket.service.ChatService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Inbox and plain-text conversation bubbles; all data work belongs to the controller. */
public class ChatView extends BorderPane {
    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("MMM d, h:mm a", Locale.ENGLISH).withZone(ZoneId.systemDefault());
    private final ChatController chats;
    private final ListView<ChatThread> inbox;
    private final VBox history = new VBox(12);
    private final ScrollPane scroll = new ScrollPane(history);
    private final Label title = UiTools.label("Choose a conversation", "section-title");
    private final Label partner = UiTools.label("", "muted");
    private boolean selecting;
    private boolean jumpToEnd;

    /** Binds the inbox, selected recipient, saved messages and unsent text. */
    public ChatView(ChatController chats, TaskRunner tasks) {
        this.chats = chats;
        setId("messages-page");
        getStyleClass().add("page-content");
        inbox = inbox();
        VBox left = new VBox(12, UiTools.label("Your conversations", "section-title"),
                UiTools.button("Refresh inbox", "chat-refresh-button", tasks,
                        () -> chats.refresh(false)), inbox);
        left.getStyleClass().add("chat-inbox");
        left.setMinWidth(220);
        left.setPrefWidth(300);
        VBox.setVgrow(inbox, Priority.ALWAYS);
        SplitPane split = new SplitPane(left, conversation(tasks));
        split.setDividerPositions(.3);
        split.getStyleClass().add("chat-split");
        setCenter(split);
        chats.selectedProperty().addListener((o, before, after) -> {
            syncSelection();
            updateHeading();
            if (before == null || after == null || before.listingId() != after.listingId()
                    || before.buyerId() != after.buyerId()) {
                jumpToEnd = after != null;
                renderMessages(true);
            }
        });
        chats.threads().addListener((ListChangeListener<ChatThread>) change -> syncSelection());
        chats.messages().addListener((ListChangeListener<ChatMessage>) change -> renderMessages(false));
        renderMessages(true);
    }

    private ListView<ChatThread> inbox() {
        ListView<ChatThread> list = new ListView<>(chats.threads());
        list.setId("chat-inbox");
        list.setPlaceholder(UiTools.label(
                "No conversations yet. Buyers can open an item to message its seller.", "muted"));
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(ChatThread item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : threadCard(item));
            }
        });
        list.getSelectionModel().selectedItemProperty().addListener((o, before, value) -> {
            if (!selecting && !chats.isPublishing() && value != null) {
                chats.select(value);
            }
        });
        return list;
    }

    private VBox threadCard(ChatThread thread) {
        String preview = thread.lastMessage().isBlank() ? "Start a conversation"
                : thread.lastMessage().replace('\n', ' ');
        if (preview.length() > 75) {
            preview = preview.substring(0, 72) + "...";
        }
        VBox card = new VBox(5, UiTools.label(thread.partnerName(chats.memberId()), "section-title"),
                UiTools.label(thread.title(), "chat-item-title"), UiTools.label(preview, "muted"));
        card.getStyleClass().add("chat-thread-card");
        return card;
    }

    private VBox conversation(TaskRunner tasks) {
        VBox heading = new VBox(5, title, partner);
        heading.getStyleClass().add("chat-heading");
        history.getStyleClass().add("chat-history");
        scroll.setId("chat-history-scroll");
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox panel = new VBox(12, heading, scroll, composer(tasks), UiTools.label(
                "Updates automatically. Local classroom chat; separate laptops do not share messages.",
                "muted"));
        panel.setId("chat-conversation");
        panel.getStyleClass().add("chat-conversation");
        panel.setMinWidth(350);
        return panel;
    }

    private VBox composer(TaskRunner tasks) {
        TextArea input = new TextArea();
        input.setId("chat-message-input");
        input.setWrapText(true);
        input.setPrefRowCount(3);
        input.setPromptText("Write a message about this item or your meetup...");
        input.textProperty().bindBidirectional(chats.draftProperty());
        var unavailable = Bindings.createBooleanBinding(
                () -> chats.selectedProperty().get() == null
                        || !chats.selectedProperty().get().canSend(), chats.selectedProperty());
        input.disableProperty().bind(unavailable);
        Button send = new Button("Send message");
        send.setId("chat-send-button");
        send.disableProperty().bind(tasks.busyProperty().or(unavailable)
                .or(Bindings.createBooleanBinding(() -> chats.draftProperty().get().isBlank()
                        || chats.draftProperty().get().strip().length() > ChatService.MAX_MESSAGE_LENGTH,
                        chats.draftProperty())));
        send.setOnAction(event -> chats.send());
        input.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                send.fire();
                event.consume();
            }
        });
        Label count = UiTools.label("", "muted");
        count.textProperty().bind(chats.draftProperty().length().asString("%d / 2000 characters"));
        HBox actions = new HBox(14, send, count, UiTools.label("Ctrl+Enter to send", "muted"));
        actions.setAlignment(Pos.CENTER_LEFT);
        return new VBox(10, input, actions);
    }

    private void syncSelection() {
        selecting = true;
        inbox.getSelectionModel().select(chats.selectedProperty().get());
        selecting = false;
    }

    private void updateHeading() {
        ChatThread thread = chats.selectedProperty().get();
        title.setText(thread == null ? "Choose a conversation" : thread.title());
        partner.setText(thread == null ? "Only the buyer and seller can read their conversation."
                : "Chat with " + thread.partnerName(chats.memberId()) + "  |  "
                        + UiTools.statusText(thread.listingStatus())
                        + (thread.canSend() ? "" : "  |  Message history is read-only"));
    }

    private void renderMessages(boolean newSelection) {
        boolean atEnd = newSelection || jumpToEnd || scroll.getVvalue() >= .95
                || history.getHeight() <= scroll.getViewportBounds().getHeight() + 1
                || (!chats.messages().isEmpty()
                        && chats.messages().getLast().senderId() == chats.memberId());
        double position = scroll.getVvalue();
        history.getChildren().clear();
        if (chats.messages().isEmpty()) {
            history.getChildren().add(UiTools.label(chats.selectedProperty().get() == null
                    ? "Use an item's Open chat button, or select an existing conversation."
                    : "No messages yet. Ask a question or suggest a meeting place and time.", "muted"));
        }
        for (ChatMessage message : chats.messages()) {
            history.getChildren().add(messageBubble(message));
        }
        if (!chats.messages().isEmpty()) {
            jumpToEnd = false;
        }
        Platform.runLater(() -> {
            scroll.applyCss();
            scroll.layout();
            scroll.setVvalue(atEnd ? 1 : position);
        });
    }

    private HBox messageBubble(ChatMessage message) {
        boolean own = message.senderId() == chats.memberId();
        Label body = UiTools.label(message.body(), "chat-body");
        body.setId("chat-message-body-" + message.id());
        body.setMaxWidth(Double.MAX_VALUE);
        Label time = UiTools.label(TIME.format(message.sentAt()), "chat-time");
        time.setTooltip(new Tooltip(message.sentAt().toString()));
        VBox bubble = new VBox(6,
                UiTools.label(own ? "You" : message.senderName(), "chat-sender"), body, time);
        bubble.getStyleClass().addAll("chat-bubble", own ? "chat-outgoing" : "chat-incoming");
        bubble.maxWidthProperty().bind(scroll.widthProperty().multiply(.78));
        HBox row = new HBox(bubble);
        row.setId("chat-message-" + message.id());
        row.setAlignment(own ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }
}
