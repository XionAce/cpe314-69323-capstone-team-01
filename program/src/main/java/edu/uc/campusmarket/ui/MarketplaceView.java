// This package contains JavaFX screens; the model/service packages contain marketplace rules.
package edu.uc.campusmarket.ui;

// Imports let this file refer to model objects, JavaFX controls, collections and callbacks by short names.
import edu.uc.campusmarket.model.*;
import edu.uc.campusmarket.service.MarketplaceService;
import edu.uc.campusmarket.service.MarketplaceSnapshot;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

/** Buyer-first presentation; services retain business rules and persistence. */
// final prevents subclassing this view. It builds nodes and delegates data operations to the service.
public final class MarketplaceView {
    // An enum limits page names to these five choices instead of relying on arbitrary strings.
    private enum Screen { MARKETPLACE, MY_LISTINGS, HISTORY, SELL, DETAIL }
    // final keeps these references fixed after construction; the objects they refer to can still do work.
    private final MarketplaceService service;
    // The application supplies this background executor for database/service work.
    private final ExecutorService worker;
    // BorderPane divides the window into top/bottom/left/right/center areas.
    private final BorderPane root = new BorderPane();
    // The workspace holds the page header and the currently visible page.
    private final BorderPane workspace = new BorderPane();
    // VBox places its children vertically; 9 is the gap between children.
    private final VBox sidebar = new VBox(9);
    // StackPane holds overlapping pages; selectScreen() shows only one of them.
    private final StackPane pages = new StackPane();
    // EnumMap connects each Screen value to its page and its navigation button.
    private final Map<Screen, ScrollPane> screens = new EnumMap<>(Screen.class);
    private final Map<Screen, Button> navigation = new EnumMap<>(Screen.class);
    // Labels display text; the helper also adds a CSS style class such as page-title.
    private final Label pageTitle = label("Marketplace", "page-title");
    private final Label pageSubtitle = label("A good find, closer to home.", "muted");
    // Feedback and the spinning indicator tell the user whether an operation is running or finished.
    private final Label feedback = new Label("Getting your marketplace ready…");
    private final ProgressIndicator progress = new ProgressIndicator();
    // ComboBox<Member> stores actual Member objects, not just the names shown in the dropdown.
    private final ComboBox<Member> activeMember = new ComboBox<>();
    private final HBox memberPicker = new HBox(9);
    // TreeView shows the category hierarchy; the map finds a displayed node by its database ID.
    private final TreeView<CategoryChoice> categoryTree = new TreeView<>();
    private final Map<Long, TreeItem<CategoryChoice>> categoryItems = new HashMap<>();
    // The search box and three FlowPanes support marketplace, history and owned-item cards.
    private final TextField keyword = new TextField();
    private final FlowPane listingCards = new FlowPane(18, 18);
    private final FlowPane historyCards = new FlowPane(18, 18);
    private final FlowPane ownedCards = new FlowPane(18, 18);
    // These controls describe the selected member's listings, including withdrawn records.
    private final Label ownedHeading = label("Your listings", "section-heading");
    private final Label ownedDescription = label("", "muted");
    private final VBox emptyOwned = emptyState("No listings from this member yet",
            "This page shows only items owned by the member selected above.");
    // Category chips are quick filter buttons; the labels explain the current search.
    private final FlowPane categoryChips = new FlowPane(8, 8);
    private final Label resultCount = label("Finding campus goods…", "section-heading");
    private final Label filterDescription = label("All categories", "muted");
    // Empty-state panels replace blank space when a search or history has no entries.
    private final VBox emptyResults = emptyState("No finds just yet", "Try a different search or browse all categories.");
    private final VBox emptyHistory = emptyState("Your next good find starts here", "Open a listing and it will appear here, newest first.");
    private final Label historyCount = label("0", "nav-count");
    // Detail labels are created once and filled with whichever listing the user opens.
    private final Label detailTitle = label("", "detail-title");
    private final Label detailPrice = label("", "detail-price");
    private final Label detailCategory = label("", "eyebrow");
    private final Label detailGrade = label("", "condition-badge");
    private final Label detailSeller = label("", "seller-name");
    private final Label detailEvidence = label("", "body-copy");
    private final Label detailStatus = label("", "neutral-badge");
    // Owner-only controls include an extra confirmation step before withdrawal.
    private final VBox ownerActions = new VBox(12);
    private final VBox removeConfirmation = new VBox(12);
    // this::askToRemove is a method reference: call this method later when the button is clicked.
    private final Button removeButton = button("Remove listing", "removeListingButton", this::askToRemove);
    // The ListView displays a numbered, FIFO-ordered buyer-interest list.
    private final Label queueHeading = label("Buyer interest", "section-heading");
    private final Label queueHint = label("", "muted");
    private final ListView<String> waitlist = new ListView<>();
    private final Button joinButton = button("I'm interested", "joinWaitlistButton", this::joinSelected);
    // This pane holds a category illustration, not a seller-uploaded photo.
    private final StackPane detailArtwork = new StackPane();
    // Form controls collect the seller profile, category, listing type, title and PHP price.
    private final ComboBox<Seller> seller = new ComboBox<>();
    private final ComboBox<Category> postingCategory = new ComboBox<>();
    private final ComboBox<ListingType> listingType = new ComboBox<>();
    private final TextField title = new TextField();
    private final TextField price = new TextField();
    // The condition section swaps controls according to the selected listing type.
    private final VBox evidence = new VBox(10);
    // Textbook condition evidence: page count, markings and cover damage.
    private final TextField missingPages = new TextField("0");
    private final ComboBox<WearLevel> markingLevel = new ComboBox<>();
    private final CheckBox coverDamaged = new CheckBox("Cover has damage");
    // Electronics condition evidence: power, functional defects and cosmetic damage.
    private final CheckBox powersOn = new CheckBox("Powers on normally");
    private final CheckBox functionalDefect = new CheckBox("Has a functional defect");
    private final CheckBox cosmeticDamage = new CheckBox("Has cosmetic wear or damage");
    // Furniture condition evidence: stability, structural damage and surface wear.
    private final CheckBox structurallyStable = new CheckBox("Structurally stable");
    private final CheckBox structuralDamage = new CheckBox("Has structural damage");
    private final ComboBox<WearLevel> surfaceWear = new ComboBox<>();
    // These ID-to-object maps are read-only snapshots used to display names and check ownership.
    private Map<Long, Category> categories = Map.of();
    private Map<Long, String> memberNames = Map.of();
    private Map<Long, Seller> sellers = Map.of();
    // Track the visible page, selected listing and whether callbacks should currently be ignored.
    private Screen currentScreen = Screen.MARKETPLACE;
    private Listing currentListing;
    private boolean applyingSnapshot, ready, busy;

    // The constructor creates the screen structure; it does not open database connections.
    public MarketplaceView(MarketplaceService service, ExecutorService worker) {
        // Reject missing collaborators immediately rather than failing later during a click.
        this.service = Objects.requireNonNull(service);
        this.worker = Objects.requireNonNull(worker);
        root.getStyleClass().add("marketplace-root");
        // A listener runs when window height changes; the lambda supplies the response.
        root.heightProperty().addListener((property, old, height) -> {
            // Toggle an ancestor CSS class so smaller windows use more compact spacing and cards.
            boolean compact = height.doubleValue() < 760;
            if (compact && !root.getStyleClass().contains("compact-window")) root.getStyleClass().add("compact-window");
            else if (!compact) root.getStyleClass().remove("compact-window");
        });
        // Construct each area and wrap long pages in ScrollPanes.
        buildSidebar();
        buildHeader();
        screens.put(Screen.MARKETPLACE, scroll(buildMarketplace()));
        screens.put(Screen.HISTORY, scroll(buildHistory()));
        screens.put(Screen.MY_LISTINGS, scroll(buildMyListings()));
        screens.put(Screen.SELL, scroll(buildSell()));
        screens.put(Screen.DETAIL, scroll(buildDetails()));
        // Attach nodes to their parent containers so JavaFX can lay them out and display them.
        pages.getChildren().addAll(screens.values());
        workspace.setCenter(pages);
        root.setLeft(sidebar);
        root.setCenter(workspace);
        buildFooter();
        selectScreen(Screen.MARKETPLACE);
        setBusy(false);
    }

    // App passes this top-level Parent node to the JavaFX Scene.
    public Parent root() { return root; }

    // Consumer callbacks receive a successful snapshot or failure after asynchronous startup.
    public void initialize(Consumer<MarketplaceSnapshot> onReady, Consumer<Throwable> onFailure) {
        // The first lambda runs on the worker; the snapshot lambda runs back on the JavaFX thread.
        run("Opening your marketplace…", () -> { service.initialize(); return service.snapshot(); }, snapshot -> {
            applySnapshot(snapshot);
            ready = true;
            setBusy(false);
            success("Welcome. Browse a find or post an item for your campus.");
            onReady.accept(snapshot);
        }, onFailure);
    }

    // Create the navigation column and expandable category tree.
    private void buildSidebar() {
        Label brandName = label("UC-B\nMarketplace", "brand-name");
        brandName.setMinSize(110, 45);
        HBox brand = new HBox(11, label("uc", "brand-mark"), brandName);
        brand.setMinHeight(52);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("brand-row");
        // Each button stores an action to run later; creating it does not run that action now.
        Button market = nav("Marketplace", "marketplaceNav", Screen.MARKETPLACE, this::search);
        Button recent = nav("Recently viewed", "recentViewsNav", Screen.HISTORY, this::showHistory);
        recent.setGraphic(historyCount);
        recent.setContentDisplay(ContentDisplay.RIGHT);
        Button sell = nav("Sell an item", "sellNav", Screen.SELL, this::showSell);
        Button mine = nav("My listings", "myListingsNav", Screen.MY_LISTINGS, this::showMyListings);
        categoryTree.setId("categoryTree");
        categoryTree.setShowRoot(true);
        categoryTree.setPrefHeight(220);
        categoryTree.setMinHeight(90);
        VBox.setVgrow(categoryTree, Priority.ALWAYS);
        // A cell factory explains how JavaFX should draw each reusable tree row.
        categoryTree.setCellFactory(tree -> new TreeCell<>() {
            // updateItem is called again as rows are reused; clear text/tooltips for empty rows.
            @Override protected void updateItem(CategoryChoice item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
                setTooltip(empty || item == null ? null : new Tooltip(item.name()));
            }
        });
        // A category selection starts a search only after startup and outside updates/other work.
        categoryTree.getSelectionModel().selectedItemProperty().addListener((property, old, chosen) -> {
            if (ready && !applyingSnapshot && !busy && chosen != null) search();
        });
        Region spacer = new Region();
        spacer.setPrefHeight(4);
        sidebar.getChildren().addAll(brand, label("YOUR CAMPUS, CIRCULATING", "sidebar-eyebrow"), market, mine, recent, sell,
                new Separator(), label("BROWSE CATEGORIES", "sidebar-eyebrow"), categoryTree, spacer,
                label("Made for campus life.\nFind it. Reuse it. Pass it on.", "sidebar-note"),
                label("Local classroom prototype", "prototype-note"));
        sidebar.getStyleClass().add("sidebar");
        // Keep the sidebar width stable while the main workspace resizes.
        sidebar.setPrefWidth(216); sidebar.setMinWidth(216); sidebar.setMaxWidth(216);
    }

    // Build the page heading and demo-member selector shared by all pages.
    private void buildHeader() {
        Region spacer = new Region();
        // A growing empty Region pushes the member selector toward the right edge.
        HBox.setHgrow(spacer, Priority.ALWAYS);
        activeMember.setId("activeMemberSelector");
        activeMember.setPrefWidth(210);
        // Member::name extracts the display name without replacing the Member object.
        activeMember.setConverter(converter(Member::name));
        // Changing the demo member reloads ownership, details or history for that member.
        activeMember.setOnAction(event -> {
            if (!ready || applyingSnapshot || busy || activeMember.getValue() == null) return;
            if (currentScreen == Screen.SELL) showSell();
            else if (currentScreen == Screen.MY_LISTINGS) showMyListings();
            else if (currentScreen == Screen.DETAIL && currentListing != null) {
                // A withdrawn item should not remain open after switching to a different owner.
                if (isWithdrawn(currentListing) && !ownsCurrentListing()) {
                    // Clear this detail selection before returning to the public marketplace.
                    currentListing = null;
                    search();
                } else openListing(currentListing);
            }
            else showHistoryForMember(currentScreen == Screen.HISTORY);
        });
        memberPicker.setAlignment(Pos.CENTER_RIGHT);
        memberPicker.getChildren().addAll(label("Browsing as", "muted"), activeMember);
        HBox header = new HBox(16, new VBox(3, pageTitle, pageSubtitle), spacer, memberPicker);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("topbar");
        workspace.setTop(header);
    }

    // Build the browsing page: banner, search field, category shortcuts and listing cards.
    private Parent buildMarketplace() {
        VBox heroText = new VBox(11, label("THE CAMPUS CIRCULAR", "hero-eyebrow"),
                label("Good finds.\nRight on campus.", "hero-title"),
                label("Books for your next class. Tech for your next idea.\nGive pre-loved campus essentials a new chapter.", "hero-copy"));
        HBox.setHgrow(heroText, Priority.ALWAYS);
        VBox heroAside = new VBox(9, label("Less waste.\nMore possibility.", "hero-aside-title"),
                label("A small marketplace\nfor everyday campus life.", "hero-aside-copy"));
        heroAside.getStyleClass().add("hero-aside");
        heroAside.setMinWidth(180);
        HBox hero = new HBox(20, heroText, heroAside);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.getStyleClass().add("hero");
        keyword.setId("searchField");
        keyword.setPromptText("What are you looking for?");
        // Pressing Enter in the search field uses the same action as the search button.
        keyword.setOnAction(event -> search());
        keyword.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(keyword, Priority.ALWAYS);
        HBox searchRow = new HBox(10, keyword, primary("Search campus", "searchButton", this::search));
        searchRow.getStyleClass().add("search-row");
        Button clear = button("Clear filters", "clearFiltersButton", this::clearFilters);
        clear.getStyleClass().add("text-button");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox heading = new HBox(12, new VBox(4, resultCount, filterDescription), spacer, clear);
        heading.setAlignment(Pos.CENTER_LEFT);
        emptyResults.getChildren().add(primary("Browse all finds", "resetEmptySearch", this::clearFilters));
        listingCards.setId("listingCards");
        // Cards wrap onto new rows while remaining aligned at their top-left corners.
        listingCards.setRowValignment(VPos.TOP);
        listingCards.setColumnHalignment(HPos.LEFT);
        return page(hero, searchRow, categoryChips, heading, listingCards, emptyResults);
    }

    // Build the recent-history page; the service supplies newest-first listing order.
    private Parent buildHistory() {
        historyCards.setId("recentViews");
        historyCards.setRowValignment(VPos.TOP);
        historyCards.setColumnHalignment(HPos.LEFT);
        emptyHistory.getChildren().add(primary("Explore the marketplace", "exploreFromHistory", this::search));
        return page(label("A second look is always welcome.", "section-heading"),
                label("Your recently opened listings, with the newest at the front.", "muted"), historyCards, emptyHistory);
    }

    // Build the member-owned page; actual records are loaded in showMyListings().
    private Parent buildMyListings() {
        ownedHeading.setId("myListingsHeading");
        ownedCards.setId("myListingCards");
        ownedCards.setRowValignment(VPos.TOP);
        ownedCards.setColumnHalignment(HPos.LEFT);
        Button post = primary("Sell an item", "postFromMyListings", this::showSell);
        return page(ownedHeading, ownedDescription, post, ownedCards, emptyOwned);
    }

    // Capture the selected member on the JavaFX thread, then request only that member's items.
    private void showMyListings() {
        Member member = selectedMember();
        if (member == null) return;
        long memberId = member.id();
        run("Loading your listings…", () -> service.listingsForMember(memberId), listings -> {
            renderOwned(member, listings);
            selectScreen(Screen.MY_LISTINGS);
            success("Showing listings owned by " + member.name() + ".");
        });
    }

    // Display active and withdrawn owned listings; withdrawn items remain as an archive.
    private void renderOwned(Member member, List<Listing> listings) {
        renderCards(ownedCards, listings);
        ownedHeading.setText(member.name() + " · " + listings.size()
                + (listings.size() == 1 ? " listing" : " listings"));
        ownedDescription.setText("Open your item to see buyer interest or remove it from Marketplace. "
                + "Withdrawn items stay here as saved records.");
        visible(emptyOwned, listings.isEmpty());
    }

    // Build one reusable item-detail page with condition evidence, queue and owner actions.
    private Parent buildDetails() {
        Button back = button("← Back to marketplace", "backToMarketplace", () -> selectScreen(Screen.MARKETPLACE));
        back.getStyleClass().add("text-button");
        detailTitle.setId("detailTitle"); detailPrice.setId("detailPrice");
        detailStatus.setId("detailStatus");
        ownerActions.setId("ownerActions");
        ownerActions.getStyleClass().add("owner-actions");
        removeButton.getStyleClass().add("danger-button");
        // Cancellation only hides confirmation; no service/database mutation is requested.
        Button cancelRemove = button("Keep listing", "cancelRemoveListingButton", () -> {
            visible(removeConfirmation, false);
            visible(removeButton, true);
        });
        cancelRemove.getStyleClass().add("secondary-button");
        // Only this second button sends the withdrawal request after the user has confirmed.
        Button confirmRemove = button("Confirm removal", "confirmRemoveListingButton", this::withdrawSelected);
        confirmRemove.getStyleClass().add("danger-button");
        removeConfirmation.setId("removeListingConfirmation");
        removeConfirmation.getChildren().addAll(label("Remove this listing from Marketplace?", "section-heading"),
                label("It will be marked Withdrawn. Reviews and buyer-interest history will be kept.", "muted"),
                new FlowPane(10, 10, confirmRemove, cancelRemove));
        visible(removeConfirmation, false);
        ownerActions.getChildren().addAll(label("YOUR LISTING", "eyebrow"), removeButton, removeConfirmation);
        // VBox stacks item details; HBox places the product area beside the interest panel.
        detailArtwork.setPrefHeight(260);
        VBox product = new VBox(15, detailCategory, detailArtwork, label("Category illustration", "small-muted"),
                new HBox(10, detailGrade, detailStatus), detailTitle, detailPrice,
                label("ABOUT THIS ITEM", "eyebrow"), detailEvidence, new Separator(), label("LISTED BY", "eyebrow"), detailSeller,
                ownerActions);
        product.setMinWidth(300);
        HBox.setHgrow(product, Priority.ALWAYS);
        waitlist.setId("waitlist");
        waitlist.setPrefHeight(180); waitlist.setMinHeight(130);
        // The placeholder is shown only when the ListView has no interest entries.
        waitlist.setPlaceholder(label("Be the first to show interest.", "muted"));
        joinButton.getStyleClass().add("primary-button");
        joinButton.setMaxWidth(Double.MAX_VALUE);
        Button refresh = button("Refresh interest list", "refreshWaitlistButton", this::refreshWaitlist);
        refresh.getStyleClass().add("text-button");
        VBox interest = new VBox(14, label("FOUND SOMETHING YOU LIKE?", "eyebrow"),
                label("Get in line for\nyour next good find.", "interest-title"),
                label("Interest is saved in the order buyers join.\nNo payment is collected in this prototype.", "muted"),
                joinButton, queueHint, new Separator(), queueHeading, waitlist, refresh);
        interest.getStyleClass().add("interest-panel");
        interest.setPrefWidth(265); interest.setMinWidth(245); interest.setMaxWidth(285);
        HBox body = new HBox(24, product, interest);
        body.setAlignment(Pos.TOP_LEFT);
        return page(back, body);
    }

    // Build the posting form. The active member determines the seller profile automatically.
    private Parent buildSell() {
        seller.setId("sellerSelector");
        seller.setConverter(converter(Seller::displayName));
        // Prevent selecting somebody else's profile while posting as the current member.
        seller.setDisable(true);
        postingCategory.setId("postingCategorySelector");
        // A lambda turns each Category into its full parent/child path for the dropdown.
        postingCategory.setConverter(converter(category -> categoryPath(category.id())));
        listingType.setId("listingTypeSelector");
        // Populate choices from enum values rather than maintaining duplicate lists of strings.
        listingType.setItems(FXCollections.observableArrayList(ListingType.values()));
        listingType.setConverter(converter(MarketplaceView::friendly));
        // Selecting a type immediately replaces the condition fields with the matching set.
        listingType.valueProperty().addListener((property, old, type) -> renderEvidence());
        markingLevel.setItems(FXCollections.observableArrayList(WearLevel.values()));
        surfaceWear.setItems(FXCollections.observableArrayList(WearLevel.values()));
        title.setId("listingTitleField"); title.setPromptText("e.g. Engineering mathematics textbook");
        price.setId("listingPriceField"); price.setPromptText("e.g. 350.00");
        missingPages.setId("missingPagesField"); missingPages.setMaxWidth(150);
        evidence.getStyleClass().add("evidence-panel");
        resetConditionFields();
        listingType.setValue(ListingType.TEXTBOOK);
        renderEvidence();
        // GridPane uses columns and rows; column 0 holds labels and column 1 holds controls.
        GridPane fields = new GridPane();
        fields.setHgap(20); fields.setVgap(20);
        ColumnConstraints inputs = new ColumnConstraints(); inputs.setHgrow(Priority.ALWAYS);
        fields.getColumnConstraints().addAll(new ColumnConstraints(125), inputs);
        // These row numbers determine where each label/control pair appears in the form.
        addField(fields, 0, "Posting as", seller); addField(fields, 1, "Item type", listingType);
        addField(fields, 2, "Category", postingCategory); addField(fields, 3, "Item title", title);
        addField(fields, 4, "Price in PHP", price);
        fields.add(label("Item condition", "field-label"), 0, 5); fields.add(evidence, 1, 5);
        Button cancel = button("Back to browsing", "cancelPost", this::search);
        cancel.getStyleClass().add("secondary-button");
        VBox form = new VBox(23, label("Tell us about your item", "section-heading"),
                label("A clear title, a fair price, and an honest condition help buyers decide.", "muted"),
                fields, new Separator(), new HBox(12, primary("Publish listing  →", "postListingButton", this::postListing), cancel));
        form.getStyleClass().add("form-panel"); form.setMaxWidth(930);
        return page(label("SELLER WORKSPACE  /  NEW LISTING", "eyebrow"),
                label("Your item. Someone else's next find.", "sell-heading"),
                label("Post a textbook, electronics item, or piece of furniture for your campus.", "muted"), form);
    }

    // Ensure this member has a seller profile, then reload the form with that profile selected.
    private void showSell() {
        Member member = selectedMember();
        if (member == null) return;
        long memberId = member.id();
        run("Opening your seller space…", () -> {
            // The service creates a profile if needed, allowing the same member to buy and sell.
            service.sellerForMember(memberId);
            return service.snapshot();
        }, snapshot -> {
            applySnapshot(snapshot);
            selectScreen(Screen.SELL);
            success("Posting as " + member.name() + ". Your item will appear in My listings.");
        });
    }

    // Reuse existing controls, showing only the fields relevant to this listing type.
    private void renderEvidence() {
        if (listingType.getValue() == null) return;
        Label note = label("We calculate a condition grade from these details.", "small-muted");
        switch (listingType.getValue()) {
            case TEXTBOOK -> evidence.getChildren().setAll(note, label("Number of missing pages", "field-label"),
                    missingPages, label("Marking level", "field-label"), markingLevel, coverDamaged);
            case ELECTRONICS -> evidence.getChildren().setAll(note, powersOn, functionalDefect, cosmeticDamage);
            case FURNITURE -> evidence.getChildren().setAll(note, structurallyStable, structuralDamage,
                    label("Surface wear", "field-label"), surfaceWear);
        }
    }

    // Build the always-visible status message and small busy indicator at the window bottom.
    private void buildFooter() {
        progress.setMaxSize(17, 17); progress.setPrefSize(17, 17);
        progress.setVisible(false); progress.setManaged(false);
        feedback.setId("feedbackLabel"); feedback.setWrapText(true);
        feedback.getStyleClass().add("feedback");
        HBox.setHgrow(feedback, Priority.ALWAYS);
        HBox footer = new HBox(10, progress, feedback);
        footer.setAlignment(Pos.CENTER_LEFT); footer.getStyleClass().add("statusbar");
        root.setBottom(footer);
    }

    // Copy loaded data into controls on the JavaFX thread while suppressing selection callbacks.
    private void applySnapshot(MarketplaceSnapshot snapshot) {
        // Avoid accidental searches triggered by programmatically replacing dropdown/tree values.
        applyingSnapshot = true;
        try {
            // Remember selections so a refresh does not unexpectedly switch the member/category.
            Long previousMember = activeMember.getValue() == null ? null : activeMember.getValue().id();
            Long previousCategory = selectedCategoryId();
            // ID maps provide direct lookups; Map.copyOf exposes an immutable copy to this view.
            Map<Long, Category> categoryMap = new LinkedHashMap<>();
            snapshot.categories().forEach(value -> categoryMap.put(value.id(), value));
            categories = Map.copyOf(categoryMap);
            Map<Long, String> names = new HashMap<>();
            snapshot.members().forEach(value -> names.put(value.id(), value.name()));
            memberNames = Map.copyOf(names);
            Map<Long, Seller> sellerMap = new HashMap<>();
            snapshot.sellers().forEach(value -> sellerMap.put(value.id(), value));
            sellers = Map.copyOf(sellerMap);
            activeMember.setItems(FXCollections.observableArrayList(snapshot.members()));
            // On first startup, prefer a member without a seller profile for the buyer demo.
            Set<Long> sellerMembers = new HashSet<>();
            snapshot.sellers().forEach(value -> sellerMembers.add(value.memberId()));
            Member defaultBuyer = snapshot.members().stream().filter(value -> !sellerMembers.contains(value.id()))
                    .findFirst().orElse(snapshot.members().isEmpty() ? null : snapshot.members().getFirst());
            activeMember.setValue(snapshot.members().stream().filter(value -> Objects.equals(value.id(), previousMember))
                    .findFirst().orElse(defaultBuyer));
            // Offer only profiles belonging to the active member, preserving correct ownership.
            List<Seller> ownedProfiles = snapshot.sellers().stream()
                    .filter(value -> activeMember.getValue() != null && value.memberId() == activeMember.getValue().id()).toList();
            seller.setItems(FXCollections.observableArrayList(ownedProfiles));
            seller.setValue(ownedProfiles.isEmpty() ? null : ownedProfiles.getFirst());
            Category oldCategory = postingCategory.getValue();
            postingCategory.setItems(FXCollections.observableArrayList(snapshot.categories()));
            postingCategory.setValue(snapshot.categories().stream().filter(value -> oldCategory != null && value.id() == oldCategory.id())
                    .findFirst().orElse(snapshot.categories().isEmpty() ? null : snapshot.categories().getFirst()));
            // null represents the artificial All finds root; real category IDs come from SQLite.
            TreeItem<CategoryChoice> all = new TreeItem<>(new CategoryChoice(null, "All finds"));
            // Reveal one category level at a time; search still includes hidden descendants.
            all.setExpanded(false); categoryItems.clear();
            // First create every node so parents can be found regardless of the input order.
            for (Category category : snapshot.categories()) {
                TreeItem<CategoryChoice> item = new TreeItem<>(new CategoryChoice(category.id(), category.name()));
                item.setExpanded(false); categoryItems.put(category.id(), item);
            }
            // Then attach each node to its parent, supporting any category depth.
            for (Category category : snapshot.categories()) {
                TreeItem<CategoryChoice> parent = category.parentId() == null ? all : categoryItems.get(category.parentId());
                if (parent != null) parent.getChildren().add(categoryItems.get(category.id()));
            }
            // Expansion controls visibility only; recursive descendant searching lives in CategoryService.
            categoryTree.setRoot(all);
            categoryTree.getSelectionModel().select(categoryItems.getOrDefault(previousCategory, all));
            categoryChips.getChildren().clear();
            categoryChips.getChildren().add(chip("All finds", null));
            // stream/filter keeps top-level categories for shortcut buttons; forEach adds each one.
            snapshot.categories().stream().filter(value -> value.parentId() == null)
                    .forEach(value -> categoryChips.getChildren().add(chip(value.name(), value.id())));
            feedback.setTooltip(new Tooltip("Local database: " + snapshot.databasePath()));
            renderListings(snapshot.listings());
        // finally restores listener handling even if an exception interrupts the refresh.
        } finally { applyingSnapshot = false; }
    }

    // A chip selects the matching tree node and runs exactly one explicit search.
    private Button chip(String text, Long categoryId) {
        Button chip = button(text, "categoryChip-" + (categoryId == null ? "all" : categoryId), () -> {
            applyingSnapshot = true;
            categoryTree.getSelectionModel().select(categoryId == null ? categoryTree.getRoot() : categoryItems.get(categoryId));
            applyingSnapshot = false; search();
        });
        chip.getStyleClass().add("category-chip"); chip.setUserData(categoryId);
        return chip;
    }

    // Read controls before background work, then render returned listings/history on the FX thread.
    private void search() {
        Long categoryId = selectedCategoryId();
        String query = keyword.getText().strip();
        Member member = selectedMember();
        if (member == null) return;
        long memberId = member.id();
        selectScreen(Screen.MARKETPLACE);
        run("Looking for your next find…", () -> new BrowseResult(service.search(categoryId, query), service.history(memberId)), result -> {
            renderListings(result.listings());
            renderHistory(result.history());
            int count = result.listings().size();
            success(count + " campus find" + (count == 1 ? "" : "s") + " to explore.");
        });
    }

    // Clear text and select All finds before reusing the normal search operation.
    private void clearFilters() {
        keyword.clear(); applyingSnapshot = true;
        categoryTree.getSelectionModel().select(categoryTree.getRoot());
        applyingSnapshot = false; search();
    }

    // Replace marketplace cards and update result counts, filter text and highlighted category chip.
    private void renderListings(List<Listing> listings) {
        renderCards(listingCards, listings);
        resultCount.setText(listings.size() + (listings.size() == 1 ? " campus find" : " campus finds"));
        String scope = selectedCategoryId() == null ? "All categories" : categoryPath(selectedCategoryId());
        filterDescription.setText(scope + (keyword.getText().isBlank() ? " · Ready for a new home" : " · “" + keyword.getText().strip() + "”"));
        visible(emptyResults, listings.isEmpty());
        for (Node node : categoryChips.getChildren()) {
            node.getStyleClass().remove("selected-chip");
            if (Objects.equals(node.getUserData(), selectedCategoryId())) node.getStyleClass().add("selected-chip");
        }
    }

    // Rebuild a card container from the supplied list while retaining the list's order.
    private void renderCards(FlowPane pane, List<Listing> listings) {
        pane.getChildren().clear();
        for (Listing listing : listings) {
            Button card = listingCard(listing, pane == historyCards);
            if (pane == ownedCards) card.setId("ownedCard-" + listing.id());
            pane.getChildren().add(card);
        }
    }

    // A card is a Button containing a VBox graphic, making the whole card keyboard/click accessible.
    private Button listingCard(Listing listing, boolean history) {
        ListingArtwork art = new ListingArtwork(listing.type());
        art.setPrefHeight(154); art.setMinHeight(154); art.setMaxHeight(154);
        // Polymorphism calls the grading implementation of Textbook, Electronics or Furniture.
        Label grade = label(friendly(listing.gradeCondition()), "condition-badge");
        grade.getStyleClass().add("grade-" + listing.gradeCondition().name().toLowerCase(Locale.ROOT));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox meta = new HBox(8, label(friendly(listing.type()), "card-category"), spacer, grade);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label name = label(listing.title(), "card-title"); name.setMinHeight(42); name.setMaxHeight(42);
        VBox content = new VBox(10, meta, name, label(money(listing), "card-price"),
                label((isWithdrawn(listing) ? "Withdrawn" : categoryName(listing.categoryId()))
                        + "  ·  View item →", "card-location"));
        content.getStyleClass().add("card-content");
        VBox graphic = new VBox(art, content); graphic.setPrefWidth(238); graphic.setMaxWidth(238);
        graphic.setMaxHeight(Region.USE_PREF_SIZE);
        Button card = button("", (history ? "historyCard-" : "listingCard-") + listing.id(), () -> openListing(listing));
        card.setGraphic(graphic); card.getStyleClass().add("listing-card");
        // Screen readers receive a meaningful description even though the button shows a graphic.
        card.setAccessibleText("View " + listing.title() + ", " + money(listing) + ", " + friendly(listing.gradeCondition()));
        card.setTooltip(new Tooltip(listing.title()));
        return card;
    }

    // Record this member's view and load its saved queue plus session-only viewing history.
    private void openListing(Listing listing) {
        Member member = selectedMember();
        if (member == null) return;
        long memberId = member.id();
        run("Opening this campus find…", () -> {
            service.recordView(memberId, listing.id());
            return new ListingDetails(service.waitlist(listing.id()), service.history(memberId));
        }, result -> {
            // This callback is back on the JavaFX thread, so changing labels and nodes is safe.
            currentListing = listing;
            detailTitle.setText(listing.title()); detailPrice.setText(money(listing));
            detailStatus.setText(isWithdrawn(listing) ? "Withdrawn" : "Campus find");
            detailCategory.setText(categoryPath(listing.categoryId()).toUpperCase(Locale.ROOT));
            detailGrade.setText(friendly(listing.gradeCondition()));
            // Remove the old condition color before assigning the newly opened item's color.
            detailGrade.getStyleClass().removeIf(style -> style.startsWith("grade-"));
            detailGrade.getStyleClass().add("grade-" + listing.gradeCondition().name().toLowerCase(Locale.ROOT));
            detailEvidence.setText(conditionDescription(listing));
            Seller owner = sellers.get(listing.sellerId());
            detailSeller.setText(owner == null ? "Campus seller" : owner.displayName());
            detailArtwork.getChildren().setAll(new ListingArtwork(listing.type()));
            renderWaitlist(result.queue()); renderHistory(result.history());
            selectScreen(Screen.DETAIL); screens.get(Screen.DETAIL).setVvalue(0);
            success(isWithdrawn(listing) ? "Viewing your withdrawn listing “" + listing.title() + "”."
                    : "Viewing “" + listing.title() + "”. Added to your recent finds.");
        });
    }

    // First click only reveals confirmation, after checking that the current member owns the item.
    private void askToRemove() {
        if (!ownsCurrentListing() || isWithdrawn(currentListing)) return;
        visible(removeConfirmation, true);
        visible(removeButton, false);
        // Lay out the revealed controls, then scroll down so confirmation is visible.
        root.applyCss();
        root.layout();
        screens.get(Screen.DETAIL).setVvalue(1);
    }

    // After confirmation, ask the service to mark the owned item WITHDRAWN instead of deleting it.
    private void withdrawSelected() {
        Member member = selectedMember();
        Listing listing = currentListing;
        // Guard against missing selection, non-owners, repeated removal and bypassing confirmation.
        if (member == null || listing == null || !ownsCurrentListing() || isWithdrawn(listing)
                || !removeConfirmation.isVisible()) return;
        long memberId = member.id(), listingId = listing.id();
        Long categoryId = selectedCategoryId();
        String query = keyword.getText().strip();
        run("Removing your listing from Marketplace…", () -> {
            // The service/repository also enforce ownership; a hidden button alone is not enough.
            service.withdrawListing(memberId, listingId);
            // Reload every affected collection so marketplace/history hide it and the owner keeps it.
            return new WithdrawalResult(service.snapshot(), service.search(categoryId, query),
                    service.listingsForMember(memberId), service.history(memberId));
        }, result -> {
            applySnapshot(result.snapshot());
            renderListings(result.marketplace());
            renderOwned(member, result.owned());
            renderHistory(result.history());
            // Clear the old detail selection before returning to the member's listing/archive page.
            currentListing = null;
            visible(removeConfirmation, false);
            selectScreen(Screen.MY_LISTINGS);
            screens.get(Screen.MY_LISTINGS).setVvalue(0);
            success("Removed “" + listing.title() + "” from Marketplace. Its saved history is preserved.");
        });
    }

    // Ownership follows Listing -> Seller -> Member; a demo name does not determine permissions.
    private boolean ownsCurrentListing() {
        Member member = activeMember.getValue();
        Seller owner = currentListing == null ? null : sellers.get(currentListing.sellerId());
        return member != null && owner != null && member.id() == owner.memberId();
    }

    // equals with the constant first is safe even if the listing's status were null.
    private static boolean isWithdrawn(Listing listing) {
        return listing != null && "WITHDRAWN".equals(listing.status());
    }

    // Save buyer interest through the service; it rejects self-interest, duplicates and withdrawn items.
    private void joinSelected() {
        Member member = selectedMember(); Listing listing = currentListing;
        if (member == null || listing == null) return;
        long memberId = member.id();
        run("Saving your interest…", () -> service.joinInterest(listing.id(), memberId), queue -> {
            renderWaitlist(queue); success("You're on the interest list for “" + listing.title() + "”.");
        });
    }

    // Reload the current item's queue without changing who is browsing or adding an interest.
    private void refreshWaitlist() {
        if (currentListing == null) return;
        long id = currentListing.id();
        run("Refreshing buyer interest…", () -> service.waitlist(id), queue -> {
            renderWaitlist(queue); success("Buyer interest is up to date.");
        });
    }

    // Format already-ordered queue entries; index 0 appears as position 01.
    private void renderWaitlist(List<BuyerInterest> queue) {
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++)
            entries.add(String.format(Locale.ROOT, "%02d   %s", i + 1, memberNames.getOrDefault(queue.get(i).buyerId(), "Campus member")));
        // JavaFX observable lists notify the control when its displayed items change.
        waitlist.setItems(FXCollections.observableArrayList(entries));
        queueHeading.setText("Buyer interest  ·  " + queue.size());
        Member buyer = activeMember.getValue();
        Seller owner = currentListing == null ? null : sellers.get(currentListing.sellerId());
        boolean own = buyer != null && owner != null && buyer.id() == owner.memberId();
        boolean withdrawn = isWithdrawn(currentListing);
        // anyMatch is true as soon as an entry belongs to this buyer.
        boolean joined = buyer != null && queue.stream().anyMatch(entry -> entry.buyerId() == buyer.id());
        // Owner, already-joined and withdrawn states disable actions and explain the reason.
        joinButton.setDisable(withdrawn || own || joined || buyer == null);
        joinButton.setText(withdrawn ? "Listing withdrawn" : own ? "This is your listing" : joined ? "You're on the list  ✓" : "I'm interested  →");
        queueHint.setText(withdrawn ? "This item is no longer available. Saved buyer interest is kept for your records."
                : own ? "Switch to another member to try the interest list."
                : joined ? "Your place is saved. Buyers are listed in joining order."
                : "Showing interest adds you to this item's waitlist.");
        // Show removal controls only for an active item owned by the current member.
        visible(ownerActions, own && !withdrawn);
        visible(removeConfirmation, false);
        visible(removeButton, true);
    }

    // Navigation shortcut: load history and switch to the history page.
    private void showHistory() { showHistoryForMember(true); }

    // A member switch may refresh history without changing the page currently being displayed.
    private void showHistoryForMember(boolean navigate) {
        Member member = selectedMember();
        if (member == null) return;
        long id = member.id();
        run("Loading your recent finds…", () -> service.history(id), history -> {
            renderHistory(history);
            if (navigate) selectScreen(Screen.HISTORY);
            success("Browsing as " + member.name() + ".");
        });
    }

    // Render the service's newest-first order and update the navigation count/empty state.
    private void renderHistory(List<Listing> history) {
        renderCards(historyCards, history); historyCount.setText(String.valueOf(history.size()));
        visible(emptyHistory, history.isEmpty());
    }

    // Validate form data first, then save as the selected member using background service work.
    private void postListing() {
        Member member = selectedMember();
        if (member == null) return;
        long memberId = member.id();
        final Listing draft;
        // Validation failures stop before any database write and become a helpful status message.
        try { draft = readDraft(); }
        catch (IllegalArgumentException failure) { error(failure.getMessage()); return; }
        run("Publishing your campus find…", () -> {
            // Use the captured member ID so the service assigns ownership to the correct person.
            Listing saved = service.postListingForMember(memberId, draft);
            return new PostedListing(saved, service.search(null, ""));
        }, result -> {
            keyword.clear(); applyingSnapshot = true;
            categoryTree.getSelectionModel().select(categoryTree.getRoot());
            applyingSnapshot = false; renderListings(result.listings());
            // Clear successful input so the next posting starts with clean condition evidence.
            title.clear(); price.clear(); resetConditionFields();
            selectScreen(Screen.MARKETPLACE); screens.get(Screen.MARKETPLACE).setVvalue(0.55);
            success("Published “" + result.saved().title() + "” · " + friendly(result.saved().gradeCondition()) + ". Your listing is saved.");
        });
    }

    // Read JavaFX controls into an unsaved domain object; all these reads happen on the FX thread.
    private Listing readDraft() {
        if (seller.getValue() == null) throw new IllegalArgumentException("Choose a seller.");
        if (postingCategory.getValue() == null) throw new IllegalArgumentException("Choose a category.");
        if (listingType.getValue() == null) throw new IllegalArgumentException("Choose an item type.");
        // strip removes surrounding whitespace; blank titles are rejected.
        String itemTitle = title.getText().strip();
        if (itemTitle.isBlank()) throw new IllegalArgumentException("Give your item a title.");
        String amount = price.getText().strip();
        // The regular expression allows digits plus an optional decimal part of one or two digits.
        if (!amount.matches("[0-9]+(?:\\.[0-9]{1,2})?"))
            throw new IllegalArgumentException("Enter a positive PHP price with up to two decimal places, such as 350.00.");
        long minor;
        // BigDecimal converts PHP to integer centavos exactly: 350.50 -> 35050, without double rounding.
        try { minor = new BigDecimal(amount).movePointRight(2).longValueExact(); }
        // longValueExact rejects amounts that cannot fit into a long instead of silently overflowing.
        catch (ArithmeticException failure) { throw new IllegalArgumentException("That price is too large. Enter a smaller amount."); }
        if (minor <= 0) throw new IllegalArgumentException("The price must be greater than zero.");
        long sellerId = seller.getValue().id(), categoryId = postingCategory.getValue().id();
        // switch creates the chosen subclass; ID 0 means unsaved and ACTIVE means publicly available.
        return switch (listingType.getValue()) {
            case TEXTBOOK -> new Textbook(0, sellerId, categoryId, itemTitle, minor, "PHP", "ACTIVE",
                    readMissingPages(), markingLevel.getValue(), coverDamaged.isSelected());
            case ELECTRONICS -> new Electronics(0, sellerId, categoryId, itemTitle, minor, "PHP", "ACTIVE",
                    powersOn.isSelected(), functionalDefect.isSelected(), cosmeticDamage.isSelected());
            case FURNITURE -> new Furniture(0, sellerId, categoryId, itemTitle, minor, "PHP", "ACTIVE",
                    structurallyStable.isSelected(), structuralDamage.isSelected(), surfaceWear.getValue());
        };
    }

    // Parse whole-number textbook evidence and translate invalid input into a clear form error.
    private int readMissingPages() {
        try {
            int count = Integer.parseInt(missingPages.getText().strip());
            if (count < 0) throw new NumberFormatException();
            return count;
        } catch (NumberFormatException failure) { throw new IllegalArgumentException("Missing pages must be a whole number of zero or more."); }
    }

    // Restore neutral/default evidence for all three listing types after posting.
    private void resetConditionFields() {
        missingPages.setText("0"); markingLevel.setValue(WearLevel.NONE); coverDamaged.setSelected(false);
        powersOn.setSelected(true); functionalDefect.setSelected(false); cosmeticDamage.setSelected(false);
        structurallyStable.setSelected(true); structuralDamage.setSelected(false); surfaceWear.setValue(WearLevel.NONE);
    }

    // Show one existing page and highlight its navigation button rather than recreating the window.
    private void selectScreen(Screen screen) {
        currentScreen = screen;
        // A lambda visits every map entry; only the selected page stays visible and managed.
        screens.forEach((key, pane) -> visible(pane, key == screen));
        navigation.forEach((key, button) -> {
            button.getStyleClass().remove("active-nav");
            if (key == screen || (key == Screen.MARKETPLACE && screen == Screen.DETAIL)) button.getStyleClass().add("active-nav");
        });
        // A switch expression chooses both the title and subtitle for the selected page.
        pageTitle.setText(switch (screen) {
            case MARKETPLACE -> "Marketplace"; case HISTORY -> "Recently viewed";
            case MY_LISTINGS -> "My listings";
            case SELL -> "Sell an item"; case DETAIL -> "A closer look";
        });
        pageSubtitle.setText(switch (screen) {
            case MARKETPLACE -> "A good find, closer to home.";
            case MY_LISTINGS -> "Your items and the buyers interested in them.";
            case HISTORY -> "Your next find might be one you've already seen.";
            case SELL -> "Give something useful a new chapter.";
            case DETAIL -> "Check the details. Find your place in line.";
        });
        visible(memberPicker, true);
    }

    // Return a real category ID or null for All finds/no selection.
    private Long selectedCategoryId() {
        TreeItem<CategoryChoice> selected = categoryTree.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.getValue().id();
    }

    // Retrieve the current demo member and show feedback if no member is selected.
    private Member selectedMember() {
        Member member = activeMember.getValue();
        if (member == null) error("Choose a demo buyer in the top-right corner.");
        return member;
    }

    // Convert a stored category ID into a short name, with a fallback for missing data.
    private String categoryName(long id) {
        Category category = categories.get(id);
        return category == null ? "Campus goods" : category.name();
    }

    // Walk up parent links to build a breadcrumb; this display helper is not the recursive search.
    private String categoryPath(long id) {
        List<String> names = new ArrayList<>(); Set<Long> visited = new HashSet<>();
        Category category = categories.get(id);
        // visited stops an unexpected category cycle from causing an endless display loop.
        while (category != null && visited.add(category.id())) {
            // Add parents at the front so the final path reads from root category to leaf.
            names.addFirst(category.name());
            category = category.parentId() == null ? null : categories.get(category.parentId());
        }
        return String.join("  /  ", names);
    }

    // Pattern-matching instanceof gives access to each subclass's own evidence fields.
    private static String conditionDescription(Listing listing) {
        if (listing instanceof Textbook book) return "Missing pages: " + book.missingPages()
                + "\nMarkings: " + friendly(book.markingLevel()) + "\nCover damage: " + yesNo(book.coverDamaged());
        if (listing instanceof Electronics tech) return "Powers on: " + yesNo(tech.powersOn())
                + "\nFunctional defect: " + yesNo(tech.hasFunctionalDefect()) + "\nCosmetic damage: " + yesNo(tech.hasCosmeticDamage());
        if (listing instanceof Furniture item) return "Structurally stable: " + yesNo(item.structurallyStable())
                + "\nStructural damage: " + yesNo(item.hasStructuralDamage()) + "\nSurface wear: " + friendly(item.surfaceWear());
        return friendly(listing.gradeCondition());
    }

    // Disable input during background work to prevent overlapping actions and stale member changes.
    private void setBusy(boolean value) {
        busy = value; workspace.setDisable(value || !ready); sidebar.setDisable(value || !ready);
        progress.setVisible(value); progress.setManaged(value);
    }

    // Generic T means any result type; this overload supplies an optional no-op failure callback.
    private <T> void run(String message, Operation<T> action, Consumer<T> success) { run(message, action, success, ignored -> { }); }

    // Shared asynchronous boundary: action runs off-thread; all control changes return to JavaFX.
    private <T> void run(String message, Operation<T> action, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        if (busy) return;
        setBusy(true); feedback.getStyleClass().removeAll("error-message", "success-message"); feedback.setText(message);
        // Submit the operation to the background executor so database work does not freeze the UI.
        worker.execute(() -> {
            try {
                // perform may return a snapshot, list or record depending on this operation's T.
                T result = action.perform();
                // Queue the success callback on the JavaFX Application Thread; never edit controls here directly.
                Platform.runLater(() -> { setBusy(false); onSuccess.accept(result); });
            // Catch background failures and schedule the error display on the JavaFX thread as well.
            } catch (Exception failure) {
                Platform.runLater(() -> {
                    setBusy(false);
                    error(failure.getMessage() == null ? "This action could not be completed. Please try again." : failure.getMessage());
                    onFailure.accept(failure);
                });
            }
        });
    }

    // Replace previous status styling with error styling and explain what the user can correct.
    private void error(String message) {
        feedback.getStyleClass().removeAll("error-message", "success-message");
        feedback.getStyleClass().add("error-message"); feedback.setText(message);
    }

    // Display successful completion using a different CSS style from errors.
    private void success(String message) {
        feedback.getStyleClass().removeAll("error-message", "success-message");
        feedback.getStyleClass().add("success-message"); feedback.setText(message);
    }

    // Build/register a sidebar button. Runnable represents a later action with no return value.
    private Button nav(String text, String id, Screen screen, Runnable action) {
        Button button = button(text, id, action); button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE); button.setAlignment(Pos.CENTER_LEFT); navigation.put(screen, button);
        return button;
    }

    // Node... is varargs: callers may supply any number of page children in their display order.
    private static VBox page(Node... children) {
        VBox page = new VBox(22, children); page.setFillWidth(true); page.getStyleClass().add("page-body"); return page;
    }

    // Fit content to available width and allow vertical scrolling on smaller displays.
    private static ScrollPane scroll(Parent body) {
        ScrollPane pane = new ScrollPane(body); pane.setFitToWidth(true); pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pane.getStyleClass().add("page-scroll"); return pane;
    }

    // Build the same centered explanatory panel for each empty-data situation.
    private static VBox emptyState(String heading, String copy) {
        VBox box = new VBox(12, label(heading, "section-heading"), label(copy, "muted"));
        box.setAlignment(Pos.CENTER); box.getStyleClass().add("empty-state"); return box;
    }

    // A style class connects this JavaFX Label to a .class selector in marketplace.css.
    private static Label label(String text, String style) {
        Label label = new Label(text); label.getStyleClass().add(style); label.setWrapText(true); return label;
    }

    // setId identifies a specific control; setOnAction registers the click/keyboard handler.
    private static Button button(String text, String id, Runnable action) {
        Button button = new Button(text); button.setId(id); button.setOnAction(event -> action.run()); return button;
    }

    // Reuse button creation and add the green primary-button CSS class.
    private static Button primary(String text, String id, Runnable action) {
        Button button = button(text, id, action); button.getStyleClass().add("primary-button"); return button;
    }

    // Place an accessible label and its input on the same GridPane row.
    private static void addField(GridPane pane, int row, String text, Region control) {
        Label name = label(text, "field-label"); name.setLabelFor(control);
        pane.add(name, 0, row); control.setMaxWidth(Double.MAX_VALUE); pane.add(control, 1, row);
        GridPane.setHgrow(control, Priority.ALWAYS);
    }

    // A generic converter turns model objects into readable dropdown text using the supplied function.
    private static <T> StringConverter<T> converter(Function<T, String> formatter) {
        return new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : formatter.apply(value); }
            // These dropdowns select existing objects; creating objects by typing text is unsupported.
            @Override public T fromString(String value) { throw new UnsupportedOperationException("Choose an existing entry."); }
        };
    }

    // visible controls drawing; managed controls whether layout still reserves space for this node.
    private static void visible(Node node, boolean value) { node.setVisible(value); node.setManaged(value); }
    // Convert boolean evidence into words instead of displaying Java true/false.
    private static String yesNo(boolean value) { return value ? "Yes" : "No"; }
    // Turn enum names such as LIKE_NEW into readable text such as Like new.
    private static String friendly(Object value) {
        String text = String.valueOf(value).toLowerCase(Locale.ROOT).replace('_', ' ');
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
    // Scale 2 converts centavos back into decimal currency for display, preserving exact values.
    private static String money(Listing listing) { return listing.currency() + " " + BigDecimal.valueOf(listing.priceMinor(), 2).toPlainString(); }
    // One abstract method makes Operation usable as a lambda, including checked exceptions.
    @FunctionalInterface private interface Operation<T> { T perform() throws Exception; }
    // Records are compact data carriers with final components; accessors such as name() are generated by Java.
    private record CategoryChoice(Long id, String name) { }
    // These result records bundle related background results for one JavaFX callback.
    private record ListingDetails(List<BuyerInterest> queue, List<Listing> history) { }
    private record PostedListing(Listing saved, List<Listing> listings) { }
    private record BrowseResult(List<Listing> listings, List<Listing> history) { }
    private record WithdrawalResult(MarketplaceSnapshot snapshot, List<Listing> marketplace,
                                    List<Listing> owned, List<Listing> history) { }
}
