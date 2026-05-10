package com.swelabs.lab2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

/*
 * Main entry point for the Lab 2 JavaFX GCM Login application.
 * parameters: none
 * Returns nothing
 */
public class Main extends Application {

    /** Filename of the input file containing username-password pairs. */
    private static final String USER_FILE_NAME = "user.txt";

    /* ------------------------------------------------------------------ */
    /*  JavaFX lifecycle                                                    */
    /* ------------------------------------------------------------------ */

    /*
     * JavaFX start method — called by the JavaFX runtime after launch().
     * primaryStage - the main application window provided by the JavaFX runtime
     * Returns nothing
     */
    @Override
    public void start(Stage primaryStage) throws Exception {

        // Step 1: Load and validate users (reuses Lab 1 validation logic)
        ArrayList<User> validUsers = loadUsers(resolveUserFilePath());

        // Step 2: Load the Login screen FXML
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/com/swelabs/lab2/login.fxml")
        );
        Parent loginRoot = fxmlLoader.load();

        // Inject dependencies into the controller before the window is shown
        LoginController loginController = fxmlLoader.getController();
        loginController.setUsers(validUsers);
        loginController.setStage(primaryStage);

        // Step 3: Configure and display the primary stage
        primaryStage.setTitle("GCM System \u2014 Login");
        primaryStage.setScene(new Scene(loginRoot, 420, 320));
        primaryStage.setResizable(false);

        // Requirement 2.4: pressing X on the login screen exits the application
        primaryStage.setOnCloseRequest(closeEvent -> Platform.exit());

        primaryStage.show();
    }

    /* ------------------------------------------------------------------ */
    /*  Private helpers                                                     */
    /* ------------------------------------------------------------------ */

    /*
     * Resolves the path to user.txt.
     * parameters: none
     * Returns a File pointing to user.txt
     */
    private File resolveUserFilePath() {
        // Primary: CWD — correct when launched via Maven from the Lab2/ directory
        File cwdFile = new File(USER_FILE_NAME);
        if (cwdFile.exists()) {
            return cwdFile;
        }

        // Fallback: directory of the running JAR / class files
        String jarDir = Main.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath();
        File jarFile = new File(jarDir, USER_FILE_NAME);
        if (jarFile.exists()) {
            return jarFile;
        }

        // Return the CWD path anyway — loadUsers() will print a clear warning
        return cwdFile;
    }

    /*
     * Reads the given file line-by-line and builds a list of valid User objects.
     * userFile - the File to read
     * Returns ArrayList of valid User instances
     */
    private ArrayList<User> loadUsers(File userFile) {
        ArrayList<User> userList = new ArrayList<>();

        try (Scanner fileScanner = new Scanner(userFile)) {
            while (fileScanner.hasNextLine()) {
                String rawLine = fileScanner.nextLine().trim();

                // Skip blank lines and comment lines
                if (rawLine.isEmpty() || rawLine.startsWith("#")) {
                    continue;
                }

                // Split on one or more whitespace characters (handles multiple spaces/tabs)
                String[] lineParts = rawLine.split("\\s+");

                if (lineParts.length >= 2) {
                    String username = lineParts[0];
                    String password = lineParts[1];
                    try {
                        userList.add(new User(username, password));
                    } catch (Exception validationException) {
                        // Validation failure: print to console only, not to any output file
                        System.out.println("Skipping invalid entry ["
                                + username + "]: " + validationException.getMessage());
                    }
                } else if (lineParts.length == 1) {
                    // Line has a username but no password — skip with a console message
                    System.out.println("Skipping line with missing password field: " + lineParts[0]);
                }
                // lineParts.length == 0 cannot occur after trim() + isEmpty() guard above
            }
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("Warning: '" + USER_FILE_NAME
                    + "' was not found. The application will start with an empty user list.");
        }

        return userList;
    }

    /* ------------------------------------------------------------------ */
    /*  Standard Java entry point                                           */
    /* ------------------------------------------------------------------ */

    /*
     * Launches the JavaFX application.
     * args - command-line arguments
     * Returns nothing
     */
    public static void main(String[] args) {
        launch(args);
    }
}
