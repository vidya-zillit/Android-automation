package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;

/** Entry screen offering Start a project / Join a project / QR join (StartAndJoinActivity). */
public class StartAndJoinPage extends BasePage {

    public StartAndJoinPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        waitVisible("start_project_btn");
        return driver.findElement(byId("join_project_btn")).isDisplayed();
    }

    public JoinProjectPage tapJoinProject() {
        waitVisible("join_project_btn").click();
        return new JoinProjectPage(driver);
    }

    public StartProjectPage tapStartProject() {
        waitVisible("start_project_btn").click();
        return new StartProjectPage(driver);
    }
}
