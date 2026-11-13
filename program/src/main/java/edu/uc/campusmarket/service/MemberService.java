package edu.uc.campusmarket.service;

import edu.uc.campusmarket.dao.MemberDao;
import edu.uc.campusmarket.model.InitialAccount;
import edu.uc.campusmarket.model.Member;
import edu.uc.campusmarket.model.Registration;
import edu.uc.campusmarket.persistence.Database;
import java.util.List;

/** Account initialization, registration, authentication and recovery. */
public class MemberService {
    private final Database database;
    private final MemberDao members;

    /** Creates this component from the supplied values or dependencies. */
    public MemberService(Database database, MemberDao members) {
        this.database = database;
        this.members = members;
    }

    /**
     * Initializes the schema or account state while preserving existing records.
     */
    public List<InitialAccount> initialize() {
        return ServiceCall.get(() -> {
            database.initialize();
            return database.takeInitialAccounts();
        });
    }

    /**
     * Verifies the supplied credentials before establishing the current account.
     */
    public Member login(String username, char[] password) {
        return ServiceCall.get(() -> members.authenticate(username, password));
    }

    /** Creates a local account and returns its one-time recovery code. */
    public Registration register(String name, String username, char[] password) {
        return ServiceCall.get(() -> members.register(name, username, password));
    }

    /**
     * Consumes a recovery code and returns a replacement after updating the
     * password.
     */
    public String resetPassword(String username, char[] code, char[] password) {
        return ServiceCall.get(() -> members.resetPassword(username, code, password));
    }
}
