package edu.uc.campusmarket.controller;

import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.service.WaitlistService;

/** Supplies the signed-in buyer identity to the FIFO service. */
public class WaitlistController {
    private final WaitlistService waitlists;
    private final TaskRunner tasks;
    private final UserSession session;

    /** Creates this component from the supplied values or dependencies. */
    public WaitlistController(WaitlistService waitlists, TaskRunner tasks, UserSession session) {
        this.waitlists = waitlists;
        this.tasks = tasks;
        this.session = session;
    }

    /** Adds the active member to another owner's FIFO waitlist. */
    public void join(Listing item, Runnable saved) {
        if (item == null) {
            return;
        }
        long member = session.memberId();
        tasks.run(() -> waitlists.join(item.getId(), member), order -> saved.run());
    }
}
