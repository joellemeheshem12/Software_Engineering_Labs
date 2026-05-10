package com.swelabs.lab2;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private AuthManager authManager;
    private Stage stage;

    @FXML
    private void initialize() {
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> hideError());
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> hideError());
    }

    public void setAuthManager(AuthManager authManager) {
        this.authManager = authManager;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void handleLogin() {
        String enteredUsername = usernameField.getText().trim();
        String enteredPassword = passwordField.getText().trim();

        if (enteredUsername.isEmpty() || enteredPassword.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        if (authManager == null) {
            showError("System error: authentication service is not available.");
            return;
        }

        AuthResult result = authManager.authenticate(enteredUsername, enteredPassword);

        if (result.isSuccess()) {
            openWelcomeScreen(result.getUser());
        } else {
            showError(result.getMessage());
        }
    }

    private void openWelcomeScreen(User matchedUser) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("/com/swelabs/lab2/welcome.fxml")
            );
            Parent welcomeRoot = fxmlLoader.load();

            WelcomeController welcomeController = fxmlLoader.getController();
            welcomeController.setUser(matchedUser);

            stage.setTitle("GCM System — Welcome");
            stage.setScene(new Scene(welcomeRoot, 420, 320));
            stage.setOnCloseRequest(closeEvent -> Platform.exit());

        } catch (Exception loadException) {
            showError("Failed to load the welcome screen. Please restart the application.");
            loadException.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
    }
}
