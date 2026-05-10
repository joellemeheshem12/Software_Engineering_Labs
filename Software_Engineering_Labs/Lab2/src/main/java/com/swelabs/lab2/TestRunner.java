package com.swelabs.lab2;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * A headless tester for Lab 2.
 * Reads test_inputs.txt, simulates finding the user, and writes to test_results.out.
 */
public class TestRunner {

    public static void main(String[] args) throws Exception {
        // Initialize mock users matching the ones in user.txt
        ArrayList<User> validUsers = new ArrayList<>();
        validUsers.add(new User("alice@example.com", "Pass1234!"));
        validUsers.add(new User("bob@test.org", "Hello@9world"));
        validUsers.add(new User("carol@uni.ac.il", "Secure#7x"));

        File inputFile = new File("test_inputs.txt");
        File outputFile = new File("test_results.out");

        if (!inputFile.exists()) {
            System.out.println("Could not find test_inputs.txt");
            return;
        }

        try (Scanner scanner = new Scanner(inputFile);
             PrintWriter out = new PrintWriter(new FileWriter(outputFile))) {

            out.println("=== Lab 2 Test Results ===");
            out.println("Configuration: Basic Auth (No lockouts)\n");

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();

                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    if (line.startsWith("#")) {
                        out.println(line);
                    }
                    continue;
                }

                // Parse username and password
                String[] parts = line.split("\\s+");
                String username = parts[0];
                String password = parts.length > 1 ? parts[1] : "";

                out.println("-> [INPUT]  Username: '" + username + "' | Password: '" + password + "'");

                // Simulate LoginController.findUser() logic
                User matchedUser = null;
                for (User u : validUsers) {
                    if (u.getUsername().equals(username) && u.getPassword().equals(password)) {
                        matchedUser = u;
                        break;
                    }
                }

                if (matchedUser != null) {
                    out.println("<- [RESULT] SUCCESS: Welcome screen opened for " + matchedUser.getUsername());
                } else {
                    out.println("<- [RESULT] FAILURE: Invalid username or password. Please try again.");
                }
                out.println();
            }

            System.out.println("Testing complete! Check " + outputFile.getAbsolutePath());
        }
    }
}
