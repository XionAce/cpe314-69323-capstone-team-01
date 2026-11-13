package edu.uc.campusmarket.service;

import edu.uc.campusmarket.TestDatabase;
import edu.uc.campusmarket.dao.MemberDao;
import edu.uc.campusmarket.dao.ReviewDao;
import edu.uc.campusmarket.model.InitialAccount;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountAndReviewTest {
    @TempDir
    Path directory;
    private TestDatabase database;
    private MemberService members;
    private ReviewService reviews;
    private List<InitialAccount> initial;

    @BeforeEach
    void start() {
        database = new TestDatabase(directory.resolve("test.db"));
        members = new MemberService(database, new MemberDao(database));
        reviews = new ReviewService(new ReviewDao(database));
        initial = members.initialize();
    }

    @Test
    void firstStartProvidesRandomCredentialsOnceAndPreservesThemOnReopen() throws Exception {
        assertEquals(3, initial.size());
        assertTrue(database.takeInitialAccounts().isEmpty());
        assertTrue(members.initialize().isEmpty());
        InitialAccount first = initial.getFirst();
        assertEquals(1, members.login(first.username(), first.password().toCharArray()).getId());
        assertTrue(new MemberService(new TestDatabase(database.getPath()),
                new MemberDao(new TestDatabase(database.getPath()))).initialize().isEmpty());
        assertEquals(1, members.login(first.username(), first.password().toCharArray()).getId());
        assertNotEquals(first.password(), first.recoveryCode());
    }

    @Test
    void registrationNormalizesUsernameAndAccountCanSignIn() throws Exception {
        var account = members.register(" New Member ", " New_User ",
                "NewUserPassword!".toCharArray());
        assertEquals("new_user", account.username());
        assertEquals("New Member", account.member().getName());
        assertEquals(account.member().getId(),
                members.login("NEW_USER", "NewUserPassword!".toCharArray()).getId());
        assertEquals(4, database.getMembers().size());
    }

    @Test
    void duplicateUsernameDoesNotInsertAnotherAccount() throws Exception {
        members.register("First", "same_user", "MyPassword123!".toCharArray());
        assertThrows(IllegalArgumentException.class,
                () -> members.register("Second", " SAME_USER ", "OtherPassword!".toCharArray()));
        assertEquals(4, database.getMembers().size());
    }

    @Test
    void registrationRejectsInvalidInputsBeforeSaving() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> members.register("", "valid_name", "ValidPassword".toCharArray()));
        assertThrows(IllegalArgumentException.class,
                () -> members.register("Name", "ab", "ValidPassword".toCharArray()));
        assertThrows(IllegalArgumentException.class,
                () -> members.register("Name", "bad space", "ValidPassword".toCharArray()));
        assertThrows(IllegalArgumentException.class,
                () -> members.register("Name", "valid_name", "short".toCharArray()));
        assertEquals(3, database.getMembers().size());
    }

    @Test
    void registeredAccountReceivesOneUseRecoveryAndServiceRejectsWrongPassword() {
        var account = members.register("New", "new_user", "OriginalPassword!".toCharArray());
        assertThrows(IllegalArgumentException.class,
                () -> members.login(account.username(), "wrong".toCharArray()));
        String next = members.resetPassword(account.username(),
                account.recoveryCode().toCharArray(), "ReplacementPassword!".toCharArray());
        assertEquals(account.member().getId(),
                members.login(account.username(), "ReplacementPassword!".toCharArray()).getId());
        assertThrows(IllegalArgumentException.class, () -> members.resetPassword(account.username(),
                account.recoveryCode().toCharArray(), "AnotherPassword!".toCharArray()));
        assertEquals(24, next.length());
    }

    @Test
    void reviewNeedsInterestAndStillRejectsOwnerAndDuplicate() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> reviews.submit(1, 2, 5, "No interest"));
        assertEquals("No reviews yet.", database.reviews(1));
        database.addInterest(1, 2);
        assertEquals(1, reviews.submit(1, 2, 5, "Interested buyer").size());
        assertThrows(IllegalArgumentException.class, () -> reviews.submit(1, 2, 4, "Duplicate"));
        assertThrows(IllegalArgumentException.class, () -> reviews.submit(1, 1, 5, "Own listing"));
        assertEquals(1, reviews.load(1).size());
    }

    @Test
    void registeredMemberCanJoinAndReviewAnotherMembersListing() throws Exception {
        var user = members.register("New", "new_user", "NewMemberPassword".toCharArray());
        database.addInterest(1, user.member().getId());
        var saved = reviews.submit(1, user.member().getId(), 4, "Saved");
        assertEquals(user.member().getId(), saved.getFirst().reviewerId());
    }

    @Test
    void invalidReviewCannotLeavePartialRowsAfterJoining() throws Exception {
        database.addInterest(1, 2);
        assertThrows(IllegalArgumentException.class, () -> reviews.submit(1, 2, 6, "Invalid"));
        assertTrue(reviews.load(1).isEmpty());
        assertEquals(1, reviews.submit(1, 2, 4, "Valid retry").size());
    }
}
