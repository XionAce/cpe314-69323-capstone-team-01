package edu.uc.campusmarket.controller;

import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.model.Review;
import edu.uc.campusmarket.service.ReviewService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** Owns review actions; the active session supplies the reviewer identity. */
public class ReviewController {
    private final ReviewService reviews;
    private final TaskRunner tasks;
    private final UserSession session;
    private final ObservableList<Review> items = FXCollections.observableArrayList();

    /** Creates this component from the supplied values or dependencies. */
    public ReviewController(ReviewService reviews, TaskRunner tasks, UserSession session) {
        this.reviews = reviews;
        this.tasks = tasks;
        this.session = session;
        session.memberProperty().addListener((o, a, b) -> items.clear());
    }

    /**
     * Loads reviews on the worker and opens the presentation only for the current
     * session.
     */
    public void load(Listing listing, Runnable loaded) {
        if (listing == null) {
            return;
        }
        long generation = session.generation();
        tasks.run(() -> reviews.load(listing.getId()), result -> {
            if (generation == session.generation()) {
                items.setAll(result);
                loaded.run();
            }
        });
    }

    /** Submits the signed-in buyer's review and reloads saved reviews. */
    public void submit(Listing listing, int stars, String comment, Runnable saved) {
        if (listing == null) {
            return;
        }
        long member = session.memberId();
        tasks.run(() -> reviews.submit(listing.getId(), member, stars, comment), result -> {
            items.setAll(result);
            saved.run();
        });
    }

    /** Provides the items operation for this component. */
    public ObservableList<Review> items() {
        return items;
    }
}
