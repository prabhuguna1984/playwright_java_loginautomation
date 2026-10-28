package com.automation.models;

public class UserLoginPOJO {
    private String username;
    private String password;
    private String expectedResult;

    /** Creates an empty login data object for JSON deserialization. */
    public UserLoginPOJO() {
    }

    /**
     * Creates a login data object with all scenario values.
     *
     * @param username the username to submit
     * @param password the password to submit
     * @param expectedResult the message expected from the application
     */
    public UserLoginPOJO(String username, String password, String expectedResult) {
        this.username = username;
        this.password = password;
        this.expectedResult = expectedResult;
    }

    /** @return the username for this login scenario */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username for this login scenario.
     *
     * @param username the username to submit
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /** @return the password for this login scenario */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password for this login scenario.
     *
     * @param password the password to submit
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /** @return the message expected from the application */
    public String getExpectedResult() {
        return expectedResult;
    }

    /**
     * Sets the message expected from the application.
     *
     * @param expectedResult the expected application message
     */
    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }
}
