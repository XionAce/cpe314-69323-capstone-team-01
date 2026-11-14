package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.controller.AccountController;
import edu.uc.campusmarket.controller.TaskRunner;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Sign-in controls bind to account state and call one controller action. */
public class LoginView extends VBox {
    /** Creates this component from the supplied values or dependencies. */
    public LoginView(AccountController accounts, TaskRunner tasks, Runnable registration,
            Runnable recovery) {
        super(12);
        TextField username = new TextField();
        username.setId("login-username");
        username.setPromptText("Your username");
        PasswordField password = new PasswordField();
        password.setId("login-password");
        password.setPromptText("Your password");
        Button login = UiTools.button("Sign in", "login-button", tasks, () -> {
            char[] secret = password.getText().toCharArray();
            password.clear();
            accounts.login(username.getText(), secret);
        });
        login.getStyleClass().add("primary");
        login.setDefaultButton(true);
        login.disableProperty().unbind();
        login.disableProperty().bind(tasks.busyProperty().or(accounts.readyProperty().not()));
        password.setOnAction(event -> login.fire());
        login.setMaxWidth(Double.MAX_VALUE);
        Button create = UiTools.button("Create an account", "register-link", tasks, registration);
        Button forgot = UiTools.button("Forgot password?", "forgot-password-button", tasks,
                recovery);
        Label error = UiTools.label("", "error-text");
        error.textProperty().bind(tasks.errorProperty());
        getChildren().addAll(UiTools.label("Welcome back.", "login-title"),
                UiTools.label("Sign in to find your next campus essential.", "muted"),
                UiTools.label("Username", "field-label"), username,
                UiTools.label("Password", "field-label"), password, error, login, create, forgot);
        getStyleClass().add("login-form");
        setMaxWidth(400);
        setAlignment(Pos.CENTER_LEFT);
    }
}
