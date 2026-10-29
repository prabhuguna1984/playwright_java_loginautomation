package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.models.UserLoginPOJO;
import com.automation.pages.LoginPage;
import com.automation.utils.JSONDataReader;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;

@Listeners(com.automation.listeners.TestListener.class)
public class UserLoginTest extends BaseTest {
    /**
     * Loads login scenarios and supplies each one as a separate TestNG data row.
     */
    @DataProvider(name = "loginData")
    public Object[][] loginData() {
        List<UserLoginPOJO> records = JSONDataReader.readLoginData();
        Object[][] testData = new Object[records.size()][1];

        for (int index = 0; index < records.size(); index++) {
            testData[index][0] = records.get(index);
        }

        return testData;
    }

    /**
     * Runs one login scenario and checks the displayed result.
     *
     * @param loginData credentials and expected message from one JSON record
     */
    @Test(dataProvider = "loginData",
            description = "Verifies that each login scenario displays its expected result.")
    public void loginScenariosShowExpectedMessage(UserLoginPOJO loginData) {
        LoginPage loginPage = new LoginPage(page);
        loginPage.open();
        loginPage.enterUsername(loginData.getUsername());
        loginPage.enterPassword(loginData.getPassword());
        loginPage.clickLogin();

        Assert.assertEquals(loginPage.getMessage(), loginData.getExpectedResult(),
                "The login result should match the expected message in the JSON test data.");
    }
}
