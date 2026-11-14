package edu.uc.campusmarket.controller;

import edu.uc.campusmarket.model.InitialAccount;
import edu.uc.campusmarket.service.MemberService;
import java.util.Arrays;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** Controls account actions without constructing controls or dialogs. */
public class AccountController {
    private final MemberService members;
    private final TaskRunner tasks;
    private final UserSession session;
    private final BooleanProperty ready = new SimpleBooleanProperty();
    private final StringProperty recoveryCode = new SimpleStringProperty("");
    private final StringProperty notice = new SimpleStringProperty("");
    private final ObservableList<InitialAccount> initialAccounts = FXCollections
            .observableArrayList();

    /** Creates this component from the supplied values or dependencies. */
    public AccountController(MemberService members, TaskRunner tasks, UserSession session) {
        this.members = members;
        this.tasks = tasks;
        this.session = session;
    }

    /**
     * Initializes the schema or account state while preserving existing records.
     */
    public void initialize() {
        tasks.run(members::initialize, accounts -> {
            initialAccounts.setAll(accounts);
            ready.set(true);
            notice.set("Ready to sign in or create an account.");
        });
    }

    /**
     * Verifies the supplied credentials before establishing the current account.
     */
    public void login(String username, char[] password) {
        if (!ready.get()) {
            Arrays.fill(password, '\0');
            return;
        }
        if (!tasks.run(() -> {
            try {
                return members.login(username, password);
            } finally {
                Arrays.fill(password, '\0');
            }
        }, session::signIn)) {
            Arrays.fill(password, '\0');
        }
    }

    /** Creates a local account and returns its one-time recovery code. */
    public void register(String name, String username, char[] password) {
        recoveryCode.set("");
        if (!tasks.run(() -> {
            try {
                return members.register(name, username, password);
            } finally {
                Arrays.fill(password, '\0');
            }
        }, result -> {
            recoveryCode.set(result.recoveryCode());
            notice.set("Account created. Save the recovery code, then sign in as "
                    + result.username() + ".");
        })) {
            Arrays.fill(password, '\0');
        }
    }

    /** Requests a password reset and exposes the replacement recovery code. */
    public void reset(String username, char[] code, char[] password) {
        recoveryCode.set("");
        if (!tasks.run(() -> {
            try {
                return members.resetPassword(username, code, password);
            } finally {
                Arrays.fill(code, '\0');
                Arrays.fill(password, '\0');
            }
        }, next -> {
            recoveryCode.set(next);
            notice.set("Password updated. Save the replacement recovery code.");
        })) {
            Arrays.fill(code, '\0');
            Arrays.fill(password, '\0');
        }
    }

    /** Provides the clear feedback operation for this component. */
    public void clearFeedback() {
        recoveryCode.set("");
        notice.set("");
    }

    /** Signs out when no foreground operation is active. */
    public void logout() {
        if (!tasks.busyProperty().get()) {
            recoveryCode.set("");
            notice.set("");
            session.signOut();
        }
    }

    /** Returns observable ready state for view binding. */
    public ReadOnlyBooleanProperty readyProperty() {
        return ready;
    }

    /** Returns observable recovery code state for view binding. */
    public ReadOnlyStringProperty recoveryCodeProperty() {
        return recoveryCode;
    }

    /** Returns observable notice state for view binding. */
    public ReadOnlyStringProperty noticeProperty() {
        return notice;
    }

    /** Provides the initial accounts operation for this component. */
    public ObservableList<InitialAccount> initialAccounts() {
        return initialAccounts;
    }
}
