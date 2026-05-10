package com.swelabs.lab2;

/**
 * Immutable result returned by AuthManager.authenticate().
 * Three possible states:
 *   success         — credentials matched, not blocked.
 *   failure         — wrong credentials or internal error.
 *   blocked         — account locked; blockedForSeconds > 0.
 */
public class AuthResult {

    private final boolean success;
    private final User    user;
    private final String  message;
    /** Remaining lockout duration in seconds; 0 when not a block result. */
    private final long    blockedForSeconds;

    private AuthResult(boolean success, User user, String message, long blockedForSeconds) {
        this.success          = success;
        this.user             = user;
        this.message          = message;
        this.blockedForSeconds = blockedForSeconds;
    }

    /** Successful login. */
    public static AuthResult success(User user) {
        return new AuthResult(true, user, "Login successful", 0);
    }

    /** Generic failure — wrong credentials, unknown user, etc. */
    public static AuthResult failure(String message) {
        return new AuthResult(false, null, message, 0);
    }

    /**
     * Account is currently locked.
     * blockedForSeconds - remaining lock duration so the UI can drive a countdown.
     */
    public static AuthResult blocked(String message, long blockedForSeconds) {
        return new AuthResult(false, null, message, Math.max(1, blockedForSeconds));
    }

    public boolean isSuccess()           { return success; }
    public User    getUser()             { return user; }
    public String  getMessage()          { return message; }
    public long    getBlockedForSeconds(){ return blockedForSeconds; }
    /** True when the account is currently locked out. */
    public boolean isBlocked()           { return blockedForSeconds > 0; }
}
