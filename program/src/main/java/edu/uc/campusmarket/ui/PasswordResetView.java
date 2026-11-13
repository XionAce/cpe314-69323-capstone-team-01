package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.controller.AccountController;
import edu.uc.campusmarket.controller.TaskRunner;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Recovery form uses an existing one-use code; it does not send email. */
public class PasswordResetView extends VBox {
    /** Creates this component from the supplied values or dependencies. */
    public PasswordResetView(AccountController accounts, TaskRunner tasks, Runnable back) {
        super(10);
        TextField username = new TextField();
        username.setId("reset-username");
        PasswordField code = new PasswordField();
        code.setId("reset-code");
        PasswordField password = new PasswordField();
        password.setId("reset-password");
        PasswordField confirmation = new PasswordField();
        confirmation.setId("reset-confirm");
        Label mismatch = UiTools.label("", "error-text");
        Label error = UiTools.label("", "error-text");
        error.textProperty().bind(tasks.errorProperty());
        Button reset = resetButton(accounts, tasks, username, code, password, confirmation, mismatch);
        reset.getStyleClass().add("primary");
        reset.disableProperty().unbind();
        reset.disableProperty()
                .bind(tasks.busyProperty().or(accounts.recoveryCodeProperty().isNotEmpty()));
        TextField replacement = new TextField();
        replacement.setId("new-recovery-code");
        replacement.setEditable(false);
        replacement.getStyleClass().add("recovery-code");
        replacement.textProperty().bind(accounts.recoveryCodeProperty());
        replacement.visibleProperty().bind(accounts.recoveryCodeProperty().isNotEmpty());
        replacement.managedProperty().bind(replacement.visibleProperty());
        Label codeLabel = UiTools.label("New recovery code - replaces your previous code",
                "field-label");
        codeLabel.visibleProperty().bind(replacement.visibleProperty());
        codeLabel.managedProperty().bind(replacement.visibleProperty());
        Label notice = UiTools.label("", "muted");
        notice.textProperty().bind(accounts.noticeProperty());
        getChildren().addAll(UiTools.label("Reset your password.", "login-title"), UiTools.label(
                "Use your saved recovery code. A successful reset supplies a replacement code.",
                "muted"), new Label("Username"), username, new Label("Recovery code"), code,
                new Label("New password"), password, new Label("Confirm password"), confirmation,
                mismatch, error, reset, notice, codeLabel, replacement,
                UiTools.button("Back to sign in", "reset-back", tasks, back));
        getStyleClass().add("login-form");
        setMaxWidth(400);
    }

    private Button resetButton(AccountController accounts, TaskRunner tasks, TextField username, PasswordField code, PasswordField password, PasswordField confirmation, Label mismatch) {
        Button reset = UiTools.button("Update password", "reset-button", tasks, () -> {
            if (!password.getText().equals(confirmation.getText())) {
                mismatch.setText("The two passwords must match.");
                return;
            }
            mismatch.setText("");
            char[] secret = password.getText().toCharArray(),
                    recovery = code.getText().toCharArray();
            password.clear();
            confirmation.clear();
            code.clear();
            accounts.reset(username.getText(), recovery, secret);
        });
        return reset;
    }
}
