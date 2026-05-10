import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class UsersApp {
    /*
     * Driver method verifying passwords and usernames from a file
     * args - args[0] is the path to the input file containing username-password pairs
     * Returns nothing
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java UsersApp <filename>");
            return;
        }
        ArrayList<User> users = new ArrayList<>();
        File file = new File(args[0]);

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                
                if (parts.length >= 2) {
                    String username = parts[0];
                    String password = parts[1];

                    try {
                        User user = new User(username, password);
                        users.add(user);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                } else if (parts.length == 1) {
                    try {
                        new User(parts[0], "");
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error: file '" + args[0] + "' not found. Ensure the path is correct.");
        }

        Collections.sort(users);
        for (User u : users) {
             System.out.println(u);
        }
    }
}
