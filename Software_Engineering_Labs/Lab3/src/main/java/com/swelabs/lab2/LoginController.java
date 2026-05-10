package com.swelabs.lab2;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the Login screen (login.fxml).
 *
 * Key behaviors:
 *   - Runs authenticate() in a background daemon thread to avoid blocking the
 *     JavaFX Application Thread (which would freeze the UI).
 *   - On a blocked result, starts a countdown daemon thread that disables the
 *     Login button and updates the error label every second (Req 5).
 *   - Limits input length to defend against pathologically long strings.
 *   - Disables the Login button while an auth request is in flight to prevent
 *     rapid double-clicks from firing multiple concurrent requests.
 */
public class LoginController {

    /* ---- FXML-injected UI components ---- */

    /** Text field for the email username. */
    @FXML private TextField     usernameField;

    /** Masked password entry field. */
    @FXML private PasswordField passwordField;

    /** Inline error / status label; hidden until needed. */
    @FXML private Label         errorLabel;

    /** Login button; disabled during auth and during lockout countdown. */
    @FXML private Button        loginButton;

    /* ---- State injected by Main before the stage is shown ---- */

    /** Manages failed-attempt counts and block state for all valid users. */
    private AuthManager authManager;

    /** The primary Stage — needed to swap scenes on success. */
    private Stage stage;

    /**
     * Background thread driving the lockout countdown.
     * Kept so it can be interrupted if the user somehow succeeds
     * (shouldn't happen during a block, but defensive).
     */
    private Thread countdownThread;

    /* ------------------------------------------------------------------ */
    /*  JavaFX lifecycle                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * Called by FXMLLoader after all @FXML fields are injected.
     * Registers listeners that clear the error label whenever the user edits
     * either field, so stale messages don't linger.
     */
    @FXML
    private void initialize() {
        usernameField.textProperty()
                .addListener((obs, oldVal, newVal) -> hideError());
        passwordField.textProperty()
                .addListener((obs, oldVal, newVal) -> hideError());
    }

    /* ------------------------------------------------------------------ */
    /*  Setters called by Main.java                                         */
    /* ------------------------------------------------------------------ */

    /**
     * Injects the authentication manager.
     * authManager - the AuthManager built from user.txt
     */
    public void setAuthManager(AuthManager authManager) {
        this.authManager = authManager;
    }

    /**
     * Injects the primary Stage so this controller can swap scenes.
     * stage - the JavaFX Stage owning this scene
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /* ------------------------------------------------------------------ */
    /*  Event handlers                                                      */
    /* ------------------------------------------------------------------ */

    /**
     * Handles a click on the Login button (also triggered by Enter key via
     * defaultButton="true" in FXML).
     *
     * Edge cases handled before hitting AuthManager:
     *   - Empty / whitespace-only fields
     *   - Input longer than 200 characters (pathological input guard)
     *   - Null authManager (internal error)
     *
     * Authentication itself runs on a daemon background thread to keep the
     * JavaFX Application Thread free (avoids UI freeze from thread.join()).
     */
    @FXML
    private void handleLogin() {
        String enteredUsername = usernameField.getText().trim();
        String enteredPassword = passwordField.getText().trim();

        // Edge case: empty fields
        if (enteredUsername.isEmpty() || enteredPassword.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        // Edge case: pathologically long input (beyond any valid credential)
        if (enteredUsername.length() > 200 || enteredPassword.length() > 200) {
            showError("Input is too long. Please check your credentials.");
            return;
        }

        // Edge case: auth service missing (shouldn't happen in normal flow)
        if (authManager == null) {
            showError("System error: authentication service is unavailable.");
            return;
        }

        // Disable button to prevent double-clicks while request is in flight
        loginButton.setDisable(true);
        showError("Authenticating…");

        // Run authenticate() off the FX thread — join() would freeze UI otherwise
        Thread authThread = new Thread(() -> {
            AuthResult result = authManager.authenticate(enteredUsername, enteredPassword);

            // All UI updates must happen back on the FX Application Thread
            Platform.runLater(() -> {
                if (result.isSuccess()) {
                    openWelcomeScreen(result.getUser());
                } else if (result.isBlocked()) {
                    // Start a countdown that re-enables the button after t seconds
                    startCountdown(result.getBlockedForSeconds(), result.getMessage());
                } else {
                    loginButton.setDisable(false);
                    showError(result.getMessage());
                }
            });
        });
        authThread.setDaemon(true);
        authThread.setName("AuthRequest-Thread");
        authThread.start();
    }

    /* ------------------------------------------------------------------ */
    /*  Private helpers                                                     */
    /* ------------------------------------------------------------------ */

    /**
     * Starts a daemon countdown thread that:
     *   1. Keeps the Login button disabled.
     *   2. Updates the error label every second with remaining time.
     *   3. Re-enables the button once the lockout expires.
     *
     * Satisfies Req 5: "display a message, wait t seconds, then allow n more attempts."
     *
     * totalSeconds - remaining lockout duration from AuthResult
     * initialMessage - the message returned by AuthManager (shown first)
     */
    private void startCountdown(long totalSeconds, String initialMessage) {
        // Cancel any previously running countdown (defensive)
        if (countdownThread != null && countdownThread.isAlive()) {
            countdownThread.interrupt();
        }

        loginButton.setDisable(true);
        showError(initialMessage);

        countdownThread = new Thread(() -> {
            for (long remaining = totalSeconds; remaining > 0; remaining--) {
                final long r = remaining;
                Platform.runLater(() ->
                        showError("Account locked. Try again in " + r + " second(s).")
                );
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Countdown was cancelled (e.g., window closing)
                    return;
                }
            }
            // Lockout expired — re-enable the button
            Platform.runLater(() -> {
                loginButton.setDisable(false);
                showError("Lockout expired. You may try again.");
            });
        });
        countdownThread.setDaemon(true);
        countdownThread.setName("Countdown-Thread");
        countdownThread.start();
    }

    /**
     * Loads welcome.fxml, passes the authenticated user to WelcomeController,
     * and replaces the current scene on the same Stage.
     * matchedUser - the successfully authenticated User
     */
    private void openWelcomeScreen(User matchedUser) {
        // Cancel any running countdown (shouldn't be active on success, but defensive)
        if (countdownThread != null && countdownThread.isAlive()) {
            countdownThread.interrupt();
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("/com/swelabs/lab2/welcome.fxml")
            );
            Parent welcomeRoot = fxmlLoader.load();

            WelcomeController welcomeController = fxmlLoader.getController();
            welcomeController.setUser(matchedUser);

            // Swap the scene on the same Stage (Req 2.3 — not a new window)
            stage.setTitle("GCM System \u2014 Welcome");
            stage.setScene(new Scene(welcomeRoot, 420, 320));

            // Req 2.4: X on welcome screen also exits the program
            stage.setOnCloseRequest(e -> Platform.exit());

        } catch (Exception ex) {
            loginButton.setDisable(false);
            showError("Failed to load the welcome screen. Please restart.");
            ex.printStackTrace();
        }
    }

    /**
     * Displays message in the error label and makes it visible.
     * message - human-readable status/error string
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    /**
     * Hides the error label.
     * Called whenever the user edits username or password fields.
     */
    private void hideError() {
        errorLabel.setVisible(false);
    }
}
