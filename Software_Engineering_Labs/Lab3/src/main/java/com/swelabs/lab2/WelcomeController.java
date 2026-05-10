package com.swelabs.lab2;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class WelcomeController {
    @FXML
    private Label welcomeLabel;

    @FXML
    private void initialize() {
    }

    public void setUser(User authenticatedUser) {
        if (authenticatedUser != null) {
            welcomeLabel.setText(
                    "Welcome, " + authenticatedUser.getUsername() + "!\n"
                    + "You are now logged in to the GCM system."
            );
        } else {
            welcomeLabel.setText("Welcome!\nYou are now logged in to the GCM system.");
        }
    }
}
