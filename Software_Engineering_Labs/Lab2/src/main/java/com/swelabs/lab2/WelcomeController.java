package com.swelabs.lab2;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/*
 * Controller for the Welcome screen.
 * parameters: none
 * Returns nothing
 */
public class WelcomeController {

    /** Label that shows the personalised welcome message. */
    @FXML
    private Label welcomeLabel;

    /*
     * Called automatically by FXMLLoader after @FXML fields are injected.
     * parameters: none
     * Returns nothing
     */
    @FXML
    private void initialize() {
        // No setup needed at construction time; label text is set via setUser()
    }

    /*
     * Populates the welcome label with the authenticated user's details.
     * authenticatedUser - the User object that successfully logged in
     * Returns nothing
     */
    public void setUser(User authenticatedUser) {
        if (authenticatedUser != null) {
            welcomeLabel.setText(
                    "Welcome, " + authenticatedUser.getUsername() + "!\n"
                    + "You are now logged in to the GCM system."
            );
        } else {
            // Defensive: should never happen in normal flow, but avoids a blank label
            welcomeLabel.setText("Welcome!\nYou are now logged in to the GCM system.");
        }
    }
}
