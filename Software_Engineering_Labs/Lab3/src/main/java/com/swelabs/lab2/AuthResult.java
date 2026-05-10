package com.swelabs.lab2;

public class AuthResult {
    private final boolean success;
    private final User user;
    private final String message;

    private AuthResult(boolean success, User user, String message) {
        this.success = success;
        this.user = user;
        this.message = message;
    }

    public static AuthResult success(User user) {
        return new AuthResult(true, user, "Login successful");
    }

    public static AuthResult failure(String message) {
        return new AuthResult(false, null, message);
    }

    public boolean isSuccess() { return success; }
    public User getUser() { return user; }
    public String getMessage() { return message; }
}
