package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;

/**
 * "Move to Folder" picker (FolderSelectionActivity, fragment_folder_selection.xml):
 * toolbar title "Move to Folder", rv_folders; row item_folder_selection →
 * tv_folder_name, iv_folder_icon. The list excludes the source folder, Sent and
 * Drafts. Tapping a row returns the selected folder and performs the move.
 */
public class FolderSelectionPage extends BasePage {

    public FolderSelectionPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("rv_folders", timeout) || textShown("Move to Folder", timeout);
    }

    public int folderCount() {
        try {
            return driver.findElements(byId("tv_folder_name")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    public boolean selectFirst() {
        var rows = driver.findElements(byId("tv_folder_name"));
        if (rows.isEmpty()) return false;
        try {
            rows.get(0).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean selectFolder(String name) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*tv_folder_name\").text(\"" + name + "\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }
}
