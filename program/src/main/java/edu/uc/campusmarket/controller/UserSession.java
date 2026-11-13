package edu.uc.campusmarket.controller;

import edu.uc.campusmarket.model.Member;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/** The identity used by every account-bound controller action. */
public class UserSession {
    private final ObjectProperty<Member> member = new SimpleObjectProperty<>();
    private long generation;

    /** Returns observable member state for view binding. */
    public ReadOnlyObjectProperty<Member> memberProperty() {
        return member;
    }

    /** Provides the member operation for this component. */
    public Member member() {
        return member.get();
    }

    /** Provides the generation operation for this component. */
    public long generation() {
        return generation;
    }

    /** Provides the member id operation for this component. */
    public long memberId() {
        if (member.get() == null) {
            throw new IllegalArgumentException("Sign in first.");
        }
        return member.get().getId();
    }

    /** Sets the active member and advances the session generation. */
    public void signIn(Member value) {
        generation++;
        member.set(value);
    }

    /** Clears the active member and invalidates earlier asynchronous results. */
    public void signOut() {
        generation++;
        member.set(null);
    }
}
