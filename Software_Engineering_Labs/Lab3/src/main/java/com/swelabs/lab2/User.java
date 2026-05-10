package com.swelabs.lab2;

public class User implements Comparable<User> {
    private String username;
    private String password;

    public User(String username, String password) throws Exception {
        setUsername(username);
        setPassword(password);
    }

    private void setUsername(String username) throws Exception {
        if (username.length() > 50) {
            throw new Exception("Username is too long, try something shorter");
        }

        String regex = "^[a-zA-Z0-9\\-\\+\\%_]+@[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,}$";
        if (!username.matches(regex)) {
            throw new Exception("Please enter a valid Email as username");
        }

        this.username = username;
    }

    private void setPassword(String password) throws Exception {
        if (password.length() < 8) {
            throw new Exception("Your password is too short, add more characters");
        }
        if (password.length() > 12) {
            throw new Exception("Your password is too long, try a shorter one");
        }

        String regex = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%\\^&*()_+])[a-zA-Z\\d!@#$%\\^&*()_+]+$";
        if (!password.matches(regex)) {
            throw new Exception("Please enter a valid password");
        }

        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }

    @Override
    public int compareTo(User other) {
        return this.username.compareTo(other.username);
    }

    @Override
    public String toString() {
        return username + " " + password;
    }
}
