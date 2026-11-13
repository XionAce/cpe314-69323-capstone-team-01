package edu.uc.campusmarket.controller;

/**
 * Three-second polling policy; tick() is independently testable without a
 * window.
 */
public class RefreshController {
    public static final int REFRESH_SECONDS = 3;
    private final TaskRunner tasks;
    private final UserSession session;
    private final ListingController listings;
    private final AnalyticsController analytics;
    private final RefreshSchedule schedule;
    private int page;
    private boolean dialogOpen;
    private boolean closed;

    /** Creates this component from the supplied values or dependencies. */
    public RefreshController(TaskRunner tasks, UserSession session, ListingController listings,
            AnalyticsController analytics, RefreshSchedule schedule) {
        this.tasks = tasks;
        this.session = session;
        this.listings = listings;
        this.analytics = analytics;
        this.schedule = schedule;
        session.memberProperty().addListener((o, before, after) -> {
            if (after == null || closed) {
                schedule.stop();
            } else {
                schedule.start(this::tick);
            }
        });
    }

    /** Refreshes the active page only when signed in, idle and outside a dialog. */
    public void tick() {
        if (closed || session.member() == null || tasks.busyProperty().get() || dialogOpen) {
            return;
        }
        if (page == 0) {
            listings.refresh(true);
        }
        if (page == 3) {
            analytics.refresh(true);
        }
    }

    /** Provides the set page operation for this component. */
    public void setPage(int value) {
        page = value;
    }

    /** Provides the set dialog open operation for this component. */
    public void setDialogOpen(boolean value) {
        dialogOpen = value;
    }

    /** Stops this component's recurring work before application shutdown. */
    public void close() {
        closed = true;
        schedule.stop();
    }
}
