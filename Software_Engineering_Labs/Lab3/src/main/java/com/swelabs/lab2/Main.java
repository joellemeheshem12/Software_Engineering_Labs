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
import java.util.List;
import java.util.Scanner;

public class Main extends Application {
    private static final String USER_FILE_NAME = "user.txt";

    @Override
    public void start(Stage primaryStage) throws Exception {
        List<String> parameters = getParameters().getRaw();
        if (parameters.size() < 2) {
            System.out.println("Usage: mvn javafx:run -Djavafx.args=\"<n> <t>\"");
            Platform.exit();
            return;
        }

        int maxFailedAttempts;
        int blockSeconds;
        try {
            maxFailedAttempts = Integer.parseInt(parameters.get(0));
            blockSeconds = Integer.parseInt(parameters.get(1));
            if (maxFailedAttempts <= 0 || blockSeconds <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: n and t must be positive integers.");
            Platform.exit();
            return;
        }

        ArrayList<User> validUsers = loadUsers(resolveUserFilePath());
        AuthManager authManager = new AuthManager(validUsers, maxFailedAttempts, blockSeconds);

        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/com/swelabs/lab2/login.fxml")
        );
        Parent loginRoot = fxmlLoader.load();

        LoginController loginController = fxmlLoader.getController();
        loginController.setAuthManager(authManager);
        loginController.setStage(primaryStage);

        primaryStage.setTitle("GCM System — Login");
        primaryStage.setScene(new Scene(loginRoot, 420, 320));
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(closeEvent -> Platform.exit());
        primaryStage.show();
    }

    private File resolveUserFilePath() {
        File cwdFile = new File(USER_FILE_NAME);
        if (cwdFile.exists()) {
            return cwdFile;
        }

        String jarDir = Main.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath();
        File jarFile = new File(jarDir, USER_FILE_NAME);
        if (jarFile.exists()) {
            return jarFile;
        }

        return cwdFile;
    }

    private ArrayList<User> loadUsers(File userFile) {
        ArrayList<User> userList = new ArrayList<>();

        try (Scanner fileScanner = new Scanner(userFile)) {
            while (fileScanner.hasNextLine()) {
                String rawLine = fileScanner.nextLine().trim();

                if (rawLine.isEmpty() || rawLine.startsWith("#")) {
                    continue;
                }

                String[] lineParts = rawLine.split("\\s+");

                if (lineParts.length >= 2) {
                    String username = lineParts[0];
                    String password = lineParts[1];
                    try {
                        userList.add(new User(username, password));
                    } catch (Exception validationException) {
                        System.out.println("Skipping invalid entry ["
                                + username + "]: " + validationException.getMessage());
                    }
                } else if (lineParts.length == 1) {
                    System.out.println("Skipping line with missing password field: " + lineParts[0]);
                }
            }
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("Warning: '" + USER_FILE_NAME
                    + "' was not found. The application will start with an empty user list.");
        }

        return userList;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
