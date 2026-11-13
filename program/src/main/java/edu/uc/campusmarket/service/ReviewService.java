package edu.uc.campusmarket.service;

import edu.uc.campusmarket.dao.ReviewDao;
import edu.uc.campusmarket.model.Review;
import java.util.List;

/** Review rules are rechecked transactionally by the DAO before insertion. */
public class ReviewService {
    private final ReviewDao reviews;

    /** Creates this component from the supplied values or dependencies. */
    public ReviewService(ReviewDao reviews) {
        this.reviews = reviews;
    }

    /** Loads saved model data for the requested view or analysis. */
    public List<Review> load(long listingId) {
        return ServiceCall.get(() -> reviews.listFor(listingId));
    }

    /** Submits the signed-in buyer's review and reloads saved reviews. */
    public List<Review> submit(long listingId, long memberId, int rating, String comment) {
        return ServiceCall.get(() -> {
            reviews.addReview(listingId, memberId, rating, comment);
            return reviews.listFor(listingId);
        });
    }
}
