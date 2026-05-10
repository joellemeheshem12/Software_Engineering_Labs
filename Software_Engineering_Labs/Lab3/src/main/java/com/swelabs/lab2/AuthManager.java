package com.swelabs.lab2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AuthManager {
    private final ArrayList<User> users;
    private final int maxFailedAttempts;
    private final int blockSeconds;
    private final Map<String, UserState> states = new HashMap<>();

    public AuthManager(ArrayList<User> users, int maxFailedAttempts, int blockSeconds) {
        this.users = users != null ? users : new ArrayList<>();
        this.maxFailedAttempts = maxFailedAttempts;
        this.blockSeconds = blockSeconds;

        for (User user : this.users) {
            states.put(user.getUsername(), new UserState());
        }
    }

    public AuthResult authenticate(String username, String password) {
        User userByUsername = findByUsername(username);

        if (userByUsername == null) {
            return AuthResult.failure("Invalid username or password. Please try again.");
        }

        if (!userByUsername.getPassword().equals(password)) {
            FailedAttemptThread failedAttemptThread = new FailedAttemptThread(username);
            failedAttemptThread.start();
            try {
                failedAttemptThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return AuthResult.failure("Login interrupted. Please try again.");
            }
            return AuthResult.failure(getWrongPasswordMessage(username));
        }

        CheckBlockedThread checkBlockedThread = new CheckBlockedThread(username, userByUsername);
        checkBlockedThread.start();
        try {
            checkBlockedThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return AuthResult.failure("Login interrupted. Please try again.");
        }

        return checkBlockedThread.getResult();
    }

    private User findByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    private synchronized void registerFailedAttempt(String username) {
        UserState state = states.get(username);
        if (state == null) {
            return;
        }

        if (isBlocked(username)) {
            return;
        }

        state.failedAttempts++;
        if (state.failedAttempts >= maxFailedAttempts) {
            state.blockedAtMillis = System.currentTimeMillis();
        }
    }

    private synchronized AuthResult checkBlockedAndLogin(String username, User user) {
        if (isBlocked(username)) {
            return AuthResult.failure(getBlockedMessage(username));
        }

        UserState state = states.get(username);
        if (state != null) {
            state.failedAttempts = 0;
            state.blockedAtMillis = 0;
        }
        return AuthResult.success(user);
    }

    private synchronized boolean isBlocked(String username) {
        UserState state = states.get(username);
        if (state == null || state.blockedAtMillis == 0) {
            return false;
        }

        long elapsedMillis = System.currentTimeMillis() - state.blockedAtMillis;
        if (elapsedMillis >= blockSeconds * 1000L) {
            state.failedAttempts = 0;
            state.blockedAtMillis = 0;
            return false;
        }

        return true;
    }

    private synchronized long remainingSeconds(String username) {
        UserState state = states.get(username);
        if (state == null || state.blockedAtMillis == 0) {
            return 0;
        }

        long elapsedMillis = System.currentTimeMillis() - state.blockedAtMillis;
        long remainingMillis = blockSeconds * 1000L - elapsedMillis;
        return Math.max(1, (long) Math.ceil(remainingMillis / 1000.0));
    }

    private synchronized String getWrongPasswordMessage(String username) {
        UserState state = states.get(username);
        if (state == null) {
            return "Invalid username or password. Please try again.";
        }

        if (isBlocked(username)) {
            return "Too many failed attempts. User is blocked for "
                    + remainingSeconds(username) + " seconds.";
        }

        int remainingAttempts = maxFailedAttempts - state.failedAttempts;
        return "Invalid username or password. Attempts left: " + remainingAttempts;
    }

    private synchronized String getBlockedMessage(String username) {
        return "This user is blocked. Please wait "
                + remainingSeconds(username) + " seconds and try again.";
    }

    private static class UserState {
        int failedAttempts = 0;
        long blockedAtMillis = 0;
    }

    private class FailedAttemptThread extends Thread {
        private final String username;

        FailedAttemptThread(String username) {
            this.username = username;
        }

        @Override
        public void run() {
            registerFailedAttempt(username);
        }
    }

    private class CheckBlockedThread extends Thread {
        private final String username;
        private final User user;
        private AuthResult result;

        CheckBlockedThread(String username, User user) {
            this.username = username;
            this.user = user;
        }

        @Override
        public void run() {
            result = checkBlockedAndLogin(username, user);
        }

        AuthResult getResult() {
            return result;
        }
    }
}
