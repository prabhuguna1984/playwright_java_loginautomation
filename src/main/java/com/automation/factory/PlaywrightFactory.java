package com.automation.factory;

import com.automation.utils.ConfigReader;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public class PlaywrightFactory implements AutoCloseable {
    private Playwright playwright;
    private Browser browser;
    private BrowserContext browserContext;
    private Page page;

    /**
     * Starts Playwright and opens a page using the configured browser settings.
     *
     * @return the newly opened Playwright page
     */
    public Page start() {
        playwright = Playwright.create();

        BrowserType browserType = switch (ConfigReader.getBrowserType().toLowerCase()) {
            case "chromium" -> playwright.chromium();
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> throw new IllegalArgumentException(
                    "browser.type must be chromium, firefox, or webkit");
        };

        browser = browserType.launch(
                new BrowserType.LaunchOptions().setHeadless(ConfigReader.isHeadless()));
        browserContext = browser.newContext();
        page = browserContext.newPage();
        return page;
    }

    /**
     * Closes the browser context, browser, and Playwright instance if they were started.
     */
    @Override
    public void close() {
        try {
            if (browserContext != null) {
                browserContext.close();
            }
        } finally {
            try {
                if (browser != null) {
                    browser.close();
                }
            } finally {
                if (playwright != null) {
                    playwright.close();
                }
            }
        }
    }
}
