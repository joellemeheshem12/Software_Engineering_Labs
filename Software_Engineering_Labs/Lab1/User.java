public class User implements Comparable<User> {
    private String username;
    private String password;

    /*
     * Instantiates a new User after validating credentials
     * username - the email string to be processed
     * password - candidate password string matching validation rules
     * Returns the finalized User object
     */
    public User(String username, String password) throws Exception {
        setUsername(username);
        setPassword(password);
    }

    /*
     * Validates the format and bounds of the email username
     * username - the candidate username string
     * Returns nothing
     */
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

    /*
     * Confirms the string meets length and strict characters criteria
     * password - the candidate password string
     * Returns nothing
     */
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

    /*
     * Retrieves the current validated username
     * No parameters
     * Returns the username string
     */
    public String getUsername() {
        return username;
    }

    /*
     * Retrieves the current validated password
     * No parameters
     * Returns the password string
     */
    public String getPassword() {
        return password;
    }

    /*
     * Compares two users by ASCII value of their usernames
     * other - the secondary User to evaluate against
     * Returns the ASCII comparison integer result
     */
    @Override
    public int compareTo(User other) {
        return this.username.compareTo(other.username);
    }

    /*
     * Formats the username and password with a space delimiter
     * No parameters
     * Returns the space delimited string output
     */
    @Override
    public String toString() {
        return username + " " + password;
    }
}
