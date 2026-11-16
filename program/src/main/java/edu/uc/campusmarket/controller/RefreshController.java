package edu.uc.campusmarket.controller;

import edu.uc.campusmarket.persistence.DatabaseChanges;
import edu.uc.campusmarket.service.ServiceCall;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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
    private HomeController home;
    private ChatController chats;
    private int page;
    private boolean dialogOpen;
    private boolean closed;
    private DatabaseChanges changes;
    private final Map<Integer, Long> versions = new HashMap<>();

    /** Creates this component from the supplied values or dependencies. */
    public RefreshController(TaskRunner tasks, UserSession session, ListingController listings,
            AnalyticsController analytics, RefreshSchedule schedule) {
        this.tasks = tasks;
        this.session = session;
        this.listings = listings;
        this.analytics = analytics;
        this.schedule = schedule;
        session.memberProperty().addListener((o, before, after) -> {
            versions.clear();
            if (after == null || closed) {
                schedule.stop();
            } else {
                schedule.start(this::tick);
            }
        });
    }

    /** Includes the personal dashboard in the existing idle polling policy. */
    public RefreshController(TaskRunner tasks, UserSession session, ListingController listings,
            AnalyticsController analytics, RefreshSchedule schedule, HomeController home) {
        this(tasks, session, listings, analytics, schedule);
        this.home = home;
    }

    /** Refreshes the active page only when signed in, idle and outside a dialog. */
    public void tick() {
        if (closed || session.member() == null || tasks.busyProperty().get() || dialogOpen) {
            return;
        }
        if (changes != null && (page == 0 || page == 3)) {
            checkChanges();
            return;
        }
        if (page == 0) {
            listings.refresh(true);
        }
        if (page == 4 && home != null) {
            home.load(true);
        }
        if (page == 3) {
            analytics.refresh(true);
        }
        if (page == 5 && chats != null) {
            chats.refresh(true);
        }
    }

    private void checkChanges() {
        int requestedPage = page;
        long generation = session.generation();
        tasks.run(() -> ServiceCall.get(changes::version), version -> {
            if (closed || generation != session.generation() || page != requestedPage || dialogOpen
                    || version.equals(versions.get(requestedPage))) {
                return;
            }
            Runnable read = () -> versions.put(requestedPage, version);
            if (requestedPage == 0) {
                listings.refresh(true, read);
            } else {
                analytics.refresh(true, read);
            }
        }, true);
    }

    /** Watches committed data before reloading listings and analytics. */
    public void setChanges(DatabaseChanges value) {
        changes = value;
        versions.clear();
    }

    /**
     * Adds the signed-in inbox to the existing idle three-second refresh policy.
     */
    public void setChats(ChatController value) {
        chats = value;
    }

    /** Provides the set page operation for this component. */
    public void setPage(int value) {
        if (page != value) {
            versions.remove(value);
        }
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
        if (changes != null) {
            try {
                changes.close();
            } catch (SQLException failure) {
                System.getLogger(RefreshController.class.getName()).log(System.Logger.Level.WARNING,
                        "Could not close database observer.", failure);
            }
        }
    }
}
