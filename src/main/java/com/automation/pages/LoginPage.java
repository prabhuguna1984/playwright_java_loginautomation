package com.automation.pages;

import com.automation.utils.ConfigReader;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class LoginPage {
    private final Page page;
    private final Locator usernameInput;
    private final Locator passwordInput;
    private final Locator loginButton;
    private final Locator errorMessage;
    private final Locator successHeading;

    /**
     * Creates the page object and initializes its page-specific locators.
     *
     * @param page the Playwright page to interact with
     */
    public LoginPage(Page page) {
        this.page = page;
        usernameInput = page.getByLabel("Username");
        passwordInput = page.getByLabel("Password");
        loginButton = page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit"));
        errorMessage = page.locator("#error");
        successHeading = page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Logged In Successfully"));
    }

    /** Navigates to the configured application URL. */
    public void open() {
        page.navigate(ConfigReader.getApplicationUrl());
    }

    /**
     * Enters a username in the username field.
     *
     * @param username the username to enter
     */
    public void enterUsername(String username) {
        usernameInput.fill(username);
    }

    /**
     * Enters a password in the password field.
     *
     * @param password the password to enter
     */
    public void enterPassword(String password) {
        passwordInput.fill(password);
    }

    /** Clicks the configured login button. */
    public void clickLogin() {
        loginButton.click();
    }

    /**
     * Reads and trims the visible login success or error message.
     *
     * @return the displayed message text
     */
    public String getMessage() {
        if (page.url().contains("logged-in-successfully")) {
            return successHeading.innerText().trim();
        }
        return errorMessage.innerText().trim();
    }
}
