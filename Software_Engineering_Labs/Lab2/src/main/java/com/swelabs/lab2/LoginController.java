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

import java.util.ArrayList;

/*
 * Controller for the Login screen.
 * parameters: none
 * Returns nothing
 */
public class LoginController {

    /* ---- FXML-injected UI components (fx:id values must match login.fxml) ---- */

    /** Text field where the user types their email username. */
    @FXML
    private TextField usernameField;

    /** Masked password field. */
    @FXML
    private PasswordField passwordField;

    /** Red error label shown on failed login; hidden by default. */
    @FXML
    private Label errorLabel;

    /* ---- State injected by Main before the stage is shown ---- */

    /**
     * Validated users loaded from user.txt.
     * Initialized to an empty list so findUser() never throws NullPointerException
     * even if setUsers() is somehow skipped.
     */
    private ArrayList<User> users = new ArrayList<>();

    /** The primary Stage — needed to swap scenes on successful login. */
    private Stage stage;

    /* ------------------------------------------------------------------ */
    /*  JavaFX lifecycle                                                    */
    /* ------------------------------------------------------------------ */

    /*
     * Called automatically by FXMLLoader after all @FXML fields are injected.
     * parameters: none
     * Returns nothing
     */
    @FXML
    private void initialize() {
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> hideError());
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> hideError());
    }

    /* ------------------------------------------------------------------ */
    /*  Setters called by Main.java                                         */
    /* ------------------------------------------------------------------ */

    /*
     * Injects the validated user list before the screen is shown.
     * users - ArrayList of valid User objects read from user.txt
     * Returns nothing
     */
    public void setUsers(ArrayList<User> users) {
        // Defensive: if null is passed, fall back to an empty list
        this.users = (users != null) ? users : new ArrayList<>();
    }

    /*
     * Injects the primary Stage reference so this controller can swap scenes.
     * stage - the JavaFX Stage that owns this scene
     * Returns nothing
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /* ------------------------------------------------------------------ */
    /*  Event handlers (wired in login.fxml)                               */
    /* ------------------------------------------------------------------ */

    /*
     * Handles a click on the Login button.
     * parameters: none
     * Returns nothing
     */
    @FXML
    private void handleLogin() {
        String enteredUsername = usernameField.getText().trim();
        String enteredPassword = passwordField.getText().trim();

        // Guard: reject empty fields immediately
        if (enteredUsername.isEmpty() || enteredPassword.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        User matchedUser = findUser(enteredUsername, enteredPassword);

        if (matchedUser != null) {
            // Requirement 2.3: credentials match → open Welcome screen
            openWelcomeScreen(matchedUser);
        } else {
            // Requirement 2.3: no match → show inline error (do not reveal which field is wrong)
            showError("Invalid username or password. Please try again.");
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Private helpers                                                     */
    /* ------------------------------------------------------------------ */

    /*
     * Searches the user list for an exact match on both username and password.
     * enteredUsername - the trimmed string from the username field
     * enteredPassword - the trimmed string from the password field
     * Returns the matching User, or null if no match is found
     */
    private User findUser(String enteredUsername, String enteredPassword) {
        for (User user : users) {
            if (user.getUsername().equals(enteredUsername)
                    && user.getPassword().equals(enteredPassword)) {
                return user;
            }
        }
        return null;
    }

    /*
     * Loads welcome.fxml, passes the authenticated user to its controller.
     * matchedUser - the successfully authenticated User
     * Returns nothing
     */
    private void openWelcomeScreen(User matchedUser) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("/com/swelabs/lab2/welcome.fxml")
            );
            Parent welcomeRoot = fxmlLoader.load();

            // Pass the authenticated user so WelcomeController can personalize the message
            WelcomeController welcomeController = fxmlLoader.getController();
            welcomeController.setUser(matchedUser);

            // Replace the scene on the same Stage (per requirement 2.3 / assignment diagram)
            stage.setTitle("GCM System \u2014 Welcome");
            stage.setScene(new Scene(welcomeRoot, 420, 320));

            // Requirement 2.4: X on the welcome screen also terminates the program
            stage.setOnCloseRequest(closeEvent -> Platform.exit());

        } catch (Exception loadException) {
            showError("Failed to load the welcome screen. Please restart the application.");
            loadException.printStackTrace();
        }
    }

    /*
     * Displays an error message in the error label and makes it visible.
     * message - the human-readable error string to display
     * Returns nothing
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    /*
     * Hides the error label.
     * parameters: none
     * Returns nothing
     */
    private void hideError() {
        errorLabel.setVisible(false);
    }
}
