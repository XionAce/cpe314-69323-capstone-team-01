package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.controller.AccountController;
import edu.uc.campusmarket.controller.TaskRunner;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** New-account form; the controller returns a recovery code to save once. */
public class RegistrationView extends VBox {
    /** Creates this component from the supplied values or dependencies. */
    public RegistrationView(AccountController accounts, TaskRunner tasks, Runnable back) {
        super(10);
        TextField name = new TextField();
        name.setId("register-name");
        TextField username = new TextField();
        username.setId("register-username");
        PasswordField password = new PasswordField();
        password.setId("register-password");
        PasswordField confirmation = new PasswordField();
        confirmation.setId("register-confirm");
        Label mismatch = UiTools.label("", "error-text");
        Label error = UiTools.label("", "error-text");
        error.textProperty().bind(tasks.errorProperty());
        Button save = UiTools.button("Create account", "register-button", tasks, () -> {
            if (!password.getText().equals(confirmation.getText())) {
                mismatch.setText("The two passwords must match.");
                return;
            }
            mismatch.setText("");
            char[] secret = password.getText().toCharArray();
            password.clear();
            confirmation.clear();
            accounts.register(name.getText(), username.getText(), secret);
        });
        save.getStyleClass().add("primary");
        save.disableProperty().unbind();
        save.disableProperty()
                .bind(tasks.busyProperty().or(accounts.recoveryCodeProperty().isNotEmpty()));
        TextField code = new TextField();
        code.setId("registration-recovery-code");
        code.setEditable(false);
        code.getStyleClass().add("recovery-code");
        code.textProperty().bind(accounts.recoveryCodeProperty());
        code.visibleProperty().bind(accounts.recoveryCodeProperty().isNotEmpty());
        code.managedProperty().bind(code.visibleProperty());
        Label codeLabel = UiTools.label("Recovery code - save this privately", "field-label");
        codeLabel.visibleProperty().bind(code.visibleProperty());
        codeLabel.managedProperty().bind(code.visibleProperty());
        Label notice = UiTools.label("", "muted");
        notice.textProperty().bind(accounts.noticeProperty());
        getChildren().addAll(UiTools.label("Join your campus.", "login-title"),
                UiTools.label("Use a unique username and an 8-128 character password.", "muted"),
                new Label("Display name"), name, new Label("Username"), username,
                new Label("Password"), password, new Label("Confirm password"), confirmation,
                mismatch, error, save, notice, codeLabel, code,
                UiTools.button("Back to sign in", "register-back", tasks, back));
        getStyleClass().add("login-form");
        setMaxWidth(400);
    }
}
