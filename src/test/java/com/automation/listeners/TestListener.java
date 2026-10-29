package com.automation.listeners;

import com.automation.base.BaseTest;
import com.automation.models.UserLoginPOJO;
import com.automation.utils.ConfigReader;
import com.automation.utils.JiraClient;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.microsoft.playwright.Page;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class TestListener implements ITestListener {
    private static ExtentReports extentReports;
    private final ThreadLocal<ExtentTest> currentTest = new ThreadLocal<>();

    /**
     * Initializes the shared HTML report before tests in this context run.
     *
     * @param context TestNG context for the running test group
     */
    @Override
    public void onStart(ITestContext context) {
        initializeReport();
    }

    /**
     * Creates an Extent report test entry when a test starts.
     *
     * @param result TestNG details for the starting test
     */
    @Override
    public void onTestStart(ITestResult result) {
        currentTest.set(createExtentTest(result));
    }

    /**
     * Marks a successful test in the Extent report.
     *
     * @param result TestNG details for the successful test
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        getCurrentTest(result).pass("Test passed.");
        currentTest.remove();
    }

    /**
     * Marks a skipped test in the Extent report and includes its cause when available.
     *
     * @param result TestNG details for the skipped test
     */
    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = getCurrentTest(result);
        if (result.getThrowable() == null) {
            test.skip("Test skipped.");
        } else {
            test.skip(result.getThrowable());
        }
        currentTest.remove();
    }

    /**
     * Captures failure details and a screenshot, then reports to Jira when configured.
     *
     * @param result TestNG details for the failed test
     */
    @Override
    public void onTestFailure(ITestResult result) {
        Instant failureTime = Instant.now();
        Path screenshot = captureScreenshot(result, failureTime);
        String testName = result.getMethod().getQualifiedName();
        String failureMessage = result.getThrowable() == null
                ? "No failure message was provided"
                : result.getThrowable().toString();
        ExtentTest extentTest = getCurrentTest(result);
        if (result.getThrowable() == null) {
            extentTest.fail(failureMessage);
        } else {
            extentTest.fail(result.getThrowable());
        }
        attachScreenshotToReport(extentTest, screenshot);

        System.err.println("Test failed: " + testName);
        System.err.println("Failure: " + failureMessage);

        if (!JiraClient.isConfigured()) {
            if (JiraClient.hasPartialConfiguration()) {
                System.err.println("Jira issue was not created: configure all four Jira environment variables.");
                extentTest.warning("Jira issue was not created: Jira configuration is incomplete.");
            }
            currentTest.remove();
            return;
        }

        try {
            String description = "Test: " + testName
                    + "\nFailure: " + failureMessage
                    + "\nTestNG suite: " + result.getTestContext().getSuite().getName()
                    + "\nTestNG test: " + result.getTestContext().getName()
                    + "\nApplication URL: " + ConfigReader.getApplicationUrl()
                    + "\nBrowser: " + ConfigReader.getBrowserType()
                    + "\nJava: " + System.getProperty("java.version")
                    + "\nOperating system: " + System.getProperty("os.name");
            JiraClient jiraClient = new JiraClient();
            String issueKey = jiraClient.createOrFindIssue(
                    testName, failureMessage, description, failureTime);
            System.err.println("Jira issue: " + issueKey);
            extentTest.info("Jira issue: " + issueKey);

            if (screenshot != null) {
                jiraClient.attachScreenshot(issueKey, screenshot);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Jira integration was interrupted: " + exception.getMessage());
            extentTest.warning("Jira integration was interrupted: " + exception.getMessage());
        } catch (IOException | RuntimeException exception) {
            System.err.println("Jira integration failed: " + exception.getMessage());
            extentTest.warning("Jira integration failed: " + exception.getMessage());
        } finally {
            currentTest.remove();
        }
    }

    /**
     * Flushes the generated report after the TestNG context finishes.
     *
     * @param context TestNG context that has completed
     */
    @Override
    public void onFinish(ITestContext context) {
        if (extentReports != null) {
            extentReports.flush();
        }
    }

    /**
     * Creates the report once and stores it under target/extent-reports.
     */
    private static synchronized void initializeReport() {
        if (extentReports != null) {
            return;
        }

        try {
            Path reportDirectory = Path.of("target", "extent-reports");
            Files.createDirectories(reportDirectory);
            Path reportPath = reportDirectory.resolve("ExtentReport.html");
            ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath.toString());
            sparkReporter.config().setOfflineMode(true);
            sparkReporter.config().setDocumentTitle("User Login Automation Report");
            sparkReporter.config().setReportName("Playwright Test Results");
            extentReports = new ExtentReports();
            extentReports.attachReporter(sparkReporter);
            extentReports.setSystemInfo("Java", System.getProperty("java.version"));
            extentReports.setSystemInfo("Operating System", System.getProperty("os.name"));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the Extent report directory", exception);
        }
    }

    /**
     * Returns the current Extent test entry, creating one if setup failed before test start.
     *
     * @param result TestNG details for the test
     * @return the Extent test entry for this test thread
     */
    private ExtentTest getCurrentTest(ITestResult result) {
        ExtentTest test = currentTest.get();
        if (test == null) {
            test = createExtentTest(result);
            currentTest.set(test);
        }
        return test;
    }

    private ExtentTest createExtentTest(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        Object[] parameters = result.getParameters();
        if (parameters.length > 0 && parameters[0] instanceof UserLoginPOJO loginScenario) {
            if (loginScenario.getScenarioName() != null && !loginScenario.getScenarioName().isBlank()) {
                testName = loginScenario.getScenarioName();
            }
            if (loginScenario.getExpectedResult() != null && !loginScenario.getExpectedResult().isBlank()) {
                description = (description == null || description.isBlank() ? "" : description + " ")
                        + "Expected application message: " + loginScenario.getExpectedResult();
            }
        }
        return extentReports.createTest(testName, description);
    }

    /**
     * Adds the captured failure screenshot to the Extent report when one is available.
     *
     * @param test the Extent test entry to update
     * @param screenshot the captured screenshot path, or {@code null} if unavailable
     */
    private void attachScreenshotToReport(ExtentTest test, Path screenshot) {
        if (screenshot == null) {
            test.info("Failure screenshot was not available.");
            return;
        }
        Path reportScreenshotDirectory = Path.of("target", "extent-reports", "screenshots");
        Path reportScreenshot = reportScreenshotDirectory.resolve(screenshot.getFileName());
        try {
            Files.createDirectories(reportScreenshotDirectory);
            Files.copy(screenshot, reportScreenshot, StandardCopyOption.REPLACE_EXISTING);
            String relativeScreenshotPath = "screenshots/" + screenshot.getFileName();
            test.fail("Failure screenshot",
                    MediaEntityBuilder.createScreenCaptureFromPath(relativeScreenshotPath).build());
        } catch (IOException | RuntimeException exception) {
            System.err.println("Could not include the failure screenshot in the Extent report: "
                    + exception.getMessage());
            test.warning("Could not include the failure screenshot in the Extent report: "
                    + exception.getMessage());
        }
    }

    /**
     * Saves a screenshot for a failed test before its browser page is closed.
     *
     * @param result TestNG details for the failed test
     * @param timestamp the time the failure was observed
     * @return the saved screenshot path, or {@code null} if capture was unavailable
     */
    private Path captureScreenshot(ITestResult result, Instant timestamp) {
        if (!(result.getInstance() instanceof BaseTest baseTest)) {
            System.err.println("Screenshot was not captured: failed test does not extend BaseTest.");
            return null;
        }

        Page page = baseTest.getPage();
        if (page == null || page.isClosed()) {
            System.err.println("Screenshot was not captured: the browser page is not available.");
            return null;
        }

        String safeTestName = result.getMethod().getMethodName()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        String time = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
                .withZone(ZoneOffset.UTC)
                .format(timestamp);
        Path screenshot = Path.of("target", "screenshots", safeTestName + "-" + time + ".png");

        try {
            Files.createDirectories(screenshot.getParent());
            page.screenshot(new Page.ScreenshotOptions().setPath(screenshot).setFullPage(true));
            System.err.println("Failure screenshot: " + screenshot.toAbsolutePath());
            return screenshot;
        } catch (IOException | RuntimeException exception) {
            System.err.println("Screenshot capture failed: " + exception.getMessage());
            return null;
        }
    }
}
