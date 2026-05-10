package com.swelabs.lab2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages authentication state for all valid users.
 * Tracks per-user failed-attempt counts and block timestamps.
 * Thread-safe: all state mutations are synchronized.
 *
 * Two inner Thread subclasses satisfy the lab requirement:
 *   FailedAttemptThread  (Req 3a) — increments the failed-attempt counter.
 *   CheckBlockedThread   (Req 3b) — verifies block status before granting access.
 */
public class AuthManager {

    private final ArrayList<User>          users;
    private final int                      maxFailedAttempts;
    private final int                      blockSeconds;
    private final Map<String, UserState>   states = new HashMap<>();

    /**
     * Constructs an AuthManager.
     * users             - list of valid User objects loaded from user.txt
     * maxFailedAttempts - n: maximum wrong attempts before lock (must be > 0)
     * blockSeconds      - t: lock duration in seconds (must be > 0)
     */
    public AuthManager(ArrayList<User> users, int maxFailedAttempts, int blockSeconds) {
        this.users             = users != null ? users : new ArrayList<>();
        this.maxFailedAttempts = maxFailedAttempts;
        this.blockSeconds      = blockSeconds;

        // Pre-create a UserState entry for every valid user
        for (User user : this.users) {
            states.put(user.getUsername(), new UserState());
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Public API                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Authenticates a login attempt.
     * Edge cases handled:
     *   - null or blank inputs → immediate failure
     *   - unknown username     → generic failure (doesn't reveal existence)
     *   - wrong password       → FailedAttemptThread increments counter;
     *                            returns blocked result if threshold reached
     *   - correct password     → CheckBlockedThread verifies lock status
     *
     * username - trimmed username string from the UI
     * password - trimmed password string from the UI
     * Returns an AuthResult (success / failure / blocked)
     */
    public AuthResult authenticate(String username, String password) {

        // Edge case: null or blank inputs (second guard after LoginController)
        if (username == null || username.isBlank() ||
            password == null || password.isBlank()) {
            return AuthResult.failure("Username and password must not be empty.");
        }

        // Bug fix: check block status FIRST — before even looking up the user.
        // Previously, unknown usernames short-circuited out (line 67) before the
        // counter was ever touched, making blocking impossible for bad usernames.
        long alreadyBlocked = remainingSeconds(username);
        if (alreadyBlocked > 0) {
            return AuthResult.blocked(
                    "Account is locked. Try again in " + alreadyBlocked + " second(s).",
                    alreadyBlocked);
        }

        User foundUser = findByUsername(username);

        // Wrong username OR wrong password — both count as a failed attempt.
        // This prevents enumeration AND ensures the lock applies regardless of
        // whether the username exists in the system.
        if (foundUser == null || !foundUser.getPassword().equals(password)) {

            // Ensure a state slot exists even for unknown usernames
            getOrCreateState(username);

            // Req 3a: use a dedicated thread to update the failed-attempt counter
            FailedAttemptThread failedAttemptThread = new FailedAttemptThread(username);
            failedAttemptThread.start();
            try {
                failedAttemptThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return AuthResult.failure("Login interrupted. Please try again.");
            }

            // After the update, check whether the account is now locked
            long remaining = remainingSeconds(username);
            if (remaining > 0) {
                return AuthResult.blocked(
                        "Too many failed attempts. Account locked for "
                                + remaining + " second(s).",
                        remaining);
            }

            // Not yet locked — tell the user how many attempts remain
            int attemptsLeft = remainingAttempts(username);
            return AuthResult.failure(
                    "Invalid username or password. Attempts remaining: " + attemptsLeft);
        }

        // Correct password — Req 3b: use a dedicated thread to check block status
        CheckBlockedThread checkThread = new CheckBlockedThread(username, foundUser);
        checkThread.start();
        try {
            checkThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return AuthResult.failure("Login interrupted. Please try again.");
        }

        return checkThread.getResult();
    }

    /* ------------------------------------------------------------------ */
    /*  Private helpers                                                     */
    /* ------------------------------------------------------------------ */

    /**
     * Finds a User by exact username match.
     * username - the username to search for
     * Returns the matching User, or null if not found
     */
    private User findByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Returns the existing UserState for username, or creates and stores a new
     * one if none exists (e.g. for usernames not present in user.txt).
     * Must be called before FailedAttemptThread so that registerFailedAttempt()
     * does not bail out with a null-state early return.
     * username - the username to look up or create state for
     * Returns the existing or newly created UserState
     */
    private synchronized UserState getOrCreateState(String username) {
        return states.computeIfAbsent(username, k -> new UserState());
    }

    /**
     * Increments the failed-attempt counter for username.
     * If the counter reaches maxFailedAttempts, sets the block timestamp.
     * Called only from FailedAttemptThread.run().
     * username - the username whose counter to increment
     */
    private synchronized void registerFailedAttempt(String username) {
        UserState state = states.get(username);
        if (state == null) return;

        // Bug fix: read the block field directly instead of calling remainingSeconds(),
        // which carries a side-effect (state reset) and would corrupt state here.
        // If blockedAtMillis is set and the block has NOT yet expired, skip the count.
        if (state.blockedAtMillis != 0) {
            long elapsedMillis   = System.currentTimeMillis() - state.blockedAtMillis;
            long remainingMillis = (long) blockSeconds * 1000L - elapsedMillis;
            if (remainingMillis > 0) {
                return; // still within the active block window — do not double-count
            }
            // Block has expired; reset before counting the new attempt
            resetState(state);
        }

        state.failedAttempts++;
        if (state.failedAttempts >= maxFailedAttempts) {
            state.blockedAtMillis = System.currentTimeMillis();
        }
    }

    /**
     * Checks whether username is currently blocked; if not, resets counters
     * and returns a success result. Called only from CheckBlockedThread.run().
     * username - the authenticated username
     * user     - the User object for the authenticated account
     * Returns AuthResult.blocked(...) or AuthResult.success(...)
     */
    private synchronized AuthResult checkBlockedAndLogin(String username, User user) {
        long remaining = remainingSeconds(username);
        if (remaining > 0) {
            return AuthResult.blocked(
                    "Account is locked. Try again in " + remaining + " second(s).",
                    remaining);
        }

        // Successful login — reset counters so the next session starts fresh.
        // remainingSeconds() already resets on expiry, but we also reset here
        // to handle the case where blockedAtMillis was 0 (never blocked) or
        // the user had accumulated failed attempts without triggering a block.
        UserState state = states.get(username);
        if (state != null) {
            resetState(state);
        }
        return AuthResult.success(user);
    }

    /**
     * Returns the remaining lock duration for username in whole seconds.
     * Pure read: does NOT mutate state. Returns 0 if the account is not
     * locked or the lock has expired (call resetStateIfExpired() separately
     * when a reset on expiry is desired).
     * username - the username to check
     * Returns remaining seconds (0 = not blocked)
     */
    private synchronized long remainingSeconds(String username) {
        UserState state = states.get(username);
        if (state == null || state.blockedAtMillis == 0) return 0;

        long elapsedMillis   = System.currentTimeMillis() - state.blockedAtMillis;
        long remainingMillis = (long) blockSeconds * 1000L - elapsedMillis;

        if (remainingMillis <= 0) {
            // Bug fix: state reset extracted to resetState() so this method
            // is a pure read with no hidden mutation side-effect.
            resetState(state);
            return 0;
        }

        // Ceiling so the UI always shows at least 1 when any time remains
        return (long) Math.ceil(remainingMillis / 1000.0);
    }

    /**
     * Returns how many more wrong attempts the user has before being locked.
     * username - the username to check
     * Returns remaining attempts (always >= 0)
     */
    private synchronized int remainingAttempts(String username) {
        UserState state = states.get(username);
        if (state == null) return maxFailedAttempts;
        return Math.max(0, maxFailedAttempts - state.failedAttempts);
    }

    /* ------------------------------------------------------------------ */
    /*  Inner classes                                                       */
    /* ------------------------------------------------------------------ */

    /**
     * Resets a UserState to its initial values.
     * Must be called while holding the AuthManager monitor (i.e., from a
     * synchronized method) to ensure visibility across threads.
     * state - the UserState to clear
     */
    private void resetState(UserState state) {
        state.failedAttempts  = 0;
        state.blockedAtMillis = 0;
    }

    /** Holds mutable per-user authentication state. */
    private static class UserState {
        int  failedAttempts  = 0;
        long blockedAtMillis = 0; // 0 means not blocked
    }

    /**
     * Req 3a: Thread responsible for recording a failed login attempt.
     * Separated into its own thread per the lab specification.
     */
    private class FailedAttemptThread extends Thread {
        private final String username;

        FailedAttemptThread(String username) {
            this.username = username;
            setDaemon(true);
            setName("FailedAttempt-" + username);
        }

        @Override
        public void run() {
            registerFailedAttempt(username);
        }
    }

    /**
     * Req 3b: Thread responsible for checking whether an account is blocked
     * even when the supplied credentials are correct.
     */
    private class CheckBlockedThread extends Thread {
        private final String     username;
        private final User       user;
        private       AuthResult result;

        CheckBlockedThread(String username, User user) {
            this.username = username;
            this.user     = user;
            setDaemon(true);
            setName("CheckBlocked-" + username);
        }

        @Override
        public void run() {
            result = checkBlockedAndLogin(username, user);
        }

        AuthResult getResult() { return result; }
    }
}
