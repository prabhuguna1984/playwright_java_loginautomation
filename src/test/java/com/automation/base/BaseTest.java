package com.automation.base;

import com.automation.factory.PlaywrightFactory;
import com.microsoft.playwright.Page;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public class BaseTest {
    private PlaywrightFactory playwrightFactory;
    protected Page page;

    /** Starts a new browser and page before each TestNG test method. */
    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        playwrightFactory = new PlaywrightFactory();
        page = playwrightFactory.start();
    }

    /** Closes browser resources after each test method. */
    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (playwrightFactory != null) {
            playwrightFactory.close();
            playwrightFactory = null;
            page = null;
        }
    }

    /**
     * Provides the current page to test listeners while the test is active.
     *
     * @return the current Playwright page, or {@code null} if it is unavailable
     */
    public Page getPage() {
        return page;
    }
}
