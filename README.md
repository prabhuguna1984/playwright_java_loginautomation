# User Login Automation Framework

UI automation framework built with Java 21, Playwright for Java, Maven, TestNG, Page Object Model (POM), JSON test data, ExtentReports, and optional Jira Cloud reporting.

## Framework structure

```text
UserLoginAutomation/
├── pom.xml
├── README.md
├── Jenkinsfile
├── ci/
│   └── jira.properties.example
└── src/
    ├── main/
    │   ├── java/com/automation/
    │   │   ├── factory/PlaywrightFactory.java
    │   │   ├── models/UserLoginPOJO.java
    │   │   ├── pages/LoginPage.java
    │   │   └── utils/
    │   │       ├── ConfigReader.java
    │   │       ├── JiraClient.java
    │   │       └── JSONDataReader.java
    │   └── resources/config.properties
    └── test/
        ├── java/com/automation/
        │   ├── base/BaseTest.java
        │   ├── listeners/TestListener.java
        │   └── tests/UserLoginTest.java
        └── resources/testdata/login-data.json
```

## What each file does

### Build and configuration

- **`pom.xml`** — Declares Java 21, Playwright, TestNG, Jackson, and ExtentReports dependencies. Maven Surefire discovers and runs the TestNG tests.
- **`Jenkinsfile`** — Declarative Jenkins pipeline that checks out the project, installs Playwright Chromium, runs tests in headless mode, supplies Jira secrets from Jenkins Credentials, and archives test results and reports.
- **`src/main/resources/config.properties`** — Holds the application URL, browser type, and headless setting. Choose `chromium`, `firefox`, or `webkit`. Page locators are kept in the page object, not this file.
- **`src/main/java/com/automation/utils/ConfigReader.java`** — Loads `config.properties` once and provides the application URL and browser settings to other classes.

### Browser and page objects

- **`src/main/java/com/automation/factory/PlaywrightFactory.java`** — Starts Playwright, selects the configured browser, launches it in headed or headless mode, and creates a browser context and page. Its `close()` method releases those resources.
- **`src/test/java/com/automation/base/BaseTest.java`** — TestNG `@BeforeMethod` starts a browser before each test method; `@AfterMethod` closes it afterward. The page getter lets the listener capture a screenshot before cleanup.
- **`src/main/java/com/automation/pages/LoginPage.java`** — Implements the login Page Object. It owns the username, password, submit, error, and success locators, and provides methods to open the login page, enter credentials, submit, and read the outcome. Update its locators and success-page URL check when adapting the framework to another application.

### Test data and tests

- **`src/main/java/com/automation/models/UserLoginPOJO.java`** — A simple Java object with `username`, `password`, and `expectedResult` fields, plus constructors, getters, and setters.
- **`src/test/resources/testdata/login-data.json`** — Contains one JSON object per login scenario. Use only credentials authorized for your test environment; do not store real or production credentials here.
- **`src/main/java/com/automation/utils/JSONDataReader.java`** — Uses Jackson to read the JSON array into a list of POJOs and rejects a missing, invalid, or empty data file.
- **`src/test/java/com/automation/tests/UserLoginTest.java`** — Its TestNG DataProvider converts the POJO list into test rows. TestNG runs the test once per JSON record.

### Reporting and Jira

- **`src/test/java/com/automation/listeners/TestListener.java`** — A TestNG listener that records pass, fail, and skip outcomes in ExtentReports. On failure it records the exception, captures a screenshot while the browser is still open, attaches the screenshot to the Extent report, and asks `JiraClient` to report the failure when Jira is configured.
- **`src/main/java/com/automation/utils/JiraClient.java`** — Calls Jira Cloud REST API. It searches for an existing issue with the same failure fingerprint to reduce duplicate tickets; if none is found, it creates a Bug with the test name as the summary and execution details in the description. It can attach the failure screenshot.
- **`ci/jira.properties.example`** — A sample template for non-secret Jenkins settings (`JIRA_BASE_URL` and `JIRA_PROJECT_KEY`). Jira email and API token must be supplied through Jenkins credentials, not this file.

## How a test run works

1. Maven Surefire starts the TestNG tests.
2. `BaseTest` starts a fresh browser and page before each test method.
3. TestNG runs each of the three named methods in `UserLoginTest` as a separate test.
4. The `loginData` DataProvider supplies one `UserLoginPOJO` from the JSON file per test run.
5. The test uses `LoginPage` to submit the POJO credentials and asserts the result against its expected message.
6. `TestListener` records the result in ExtentReports. If it failed, the listener captures a screenshot before teardown closes the browser.
7. When the four Jira environment variables are present, `JiraClient` searches for a duplicate failure, creates an issue if needed, and attaches the screenshot.
8. TestNG teardown closes browser resources. The listener writes the HTML report under `target/extent-reports/`.

## Configure the application

Edit `src/main/resources/config.properties`:

```properties
application.url=https://your-application.example/login
browser.type=chromium
browser.headless=true
```

Set the URL to your actual test application. In `LoginPage.java`, update the accessible locators and success-page detection to match that application's labels, button name, result element, and URL behavior. Update the credentials and expected messages in `login-data.json` to match the application and authorized test accounts. In a company CI environment, store real credentials in Jenkins Credentials or another secret manager rather than in JSON; JSON fixtures should contain only non-sensitive test data or placeholder values.

## Install browser and run tests

Run these commands from the project directory. Install the browser matching `browser.type`:

```powershell
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
mvn clean test
```

For Firefox or WebKit, install that browser instead:

```powershell
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install firefox"
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install webkit"
```

The UI tests require a reachable application, selectors that match its page, valid test data, and expected messages that match its behavior.

## Output files

- `target/surefire-reports/` — TestNG/Maven test results and failure details.
- `target/screenshots/` — Screenshots captured for failed tests.
- `target/extent-reports/` — ExtentReports HTML output with test statuses and failure screenshots copied into its `screenshots/` subfolder.

The report is written as `target/extent-reports/ExtentReport.html`. Its screenshot links use paths relative to that report directory. When Jira reporting is configured and a failure issue is created or reused, the failed test entry includes a clickable Jira ticket link. The Jenkins pipeline publishes all files under this directory using the HTML Publisher plugin; install that plugin on the Jenkins controller for the report link to appear.

The Extent Spark report uses JavaScript for its sidebar views and charts, and Font Awesome fonts for its icons. Jenkins' Content Security Policy can block these report assets. If the report's sidebar or icons do not work in Jenkins, ask a Jenkins administrator to configure a Jenkins Resource Root URL on a separate origin, as recommended by the [Jenkins security documentation](https://www.jenkins.io/doc/book/security/configuring-content-security-policy/). Do not disable CSP globally or attempt to change the controller's security policy from a pipeline running on an agent. The report can be opened locally from `target/extent-reports/ExtentReport.html` to distinguish a Jenkins policy issue from a generated-report issue.

## Optional Jira Cloud failure reporting

The listener reads these environment variables at runtime:

- `JIRA_BASE_URL` — Jira Cloud site URL, for example `https://your-site.atlassian.net`.
- `JIRA_EMAIL` — Email address for the Jira account that creates issues.
- `JIRA_API_TOKEN` — API token for that account.
- `JIRA_PROJECT_KEY` — Jira project key where failure Bugs should be created.

The Jira account needs permission to create issues in the project and attach files. Example PowerShell setup for a local run:

```powershell
$env:JIRA_BASE_URL = "https://your-site.atlassian.net"
$env:JIRA_EMAIL = "your-jira-email@example.com"
$env:JIRA_API_TOKEN = "your-api-token"
$env:JIRA_PROJECT_KEY = "PROJECT"
mvn clean test
```

Keep the API token out of source code and test data. If Jira is not configured, the test still reports its failure and saves its screenshot, but no Jira issue is created. If only some Jira variables are set, the listener reports that the configuration is incomplete.

## Jenkins CI/CD

The root `Jenkinsfile` defines a declarative pipeline that supports Windows and Linux agents. Configure Jenkins with:

1. Make Java 21 and Maven available on the Jenkins agent's `PATH`.
2. Install the Jenkins **Credentials Binding** plugin.
3. Add four Jenkins credentials with the exact IDs `jira-base-url`, `jira-email`, `jira-api-token`, and `jira-project-key`. Use **Secret text** credentials for the email and API token. The base URL and project key can also be stored as Secret text credentials for this pipeline.
4. The Jenkinsfile maps those credentials to `JIRA_BASE_URL`, `JIRA_EMAIL`, `JIRA_API_TOKEN`, and `JIRA_PROJECT_KEY`, which the test listener reads. Jira reporting is skipped with a report warning if any of the four values is absent.
5. Ensure the agent can access the application and has the operating-system libraries required by Chromium. The pipeline installs the Chromium browser binary.
6. Create a Pipeline or Multibranch Pipeline job that loads the root-level `Jenkinsfile`.

The pipeline runs `mvn clean test -Dbrowser.headless=true`; the system property overrides `browser.headless` from `config.properties` for CI without changing the local setting. TestNG XML results, screenshots, and ExtentReports are published/archived even when tests fail. Jira reporting uses the two bound credentials plus the configured URL and project key.
