package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * Email Contacts — list (ContactListActivity, fragment_contact_list.xml) and the
 * Add/Edit form (EditContactActivity, fragment_edit_contact.xml).
 * List: rv_contacts, fab_add, empty_view; row item_contact → tv_name, tv_email,
 * btn_edit, btn_delete. Form: et_first_name/last_name/company/email/phone/
 * zip_code/state/city/address/notes; Save = toolbar selectTv. Validation:
 * email must contain '@'; phone needs a country code.
 */
public class EmailContactsPage extends BasePage {

    public EmailContactsPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- list ----------------

    public boolean isLoaded(Duration timeout) {
        return isVisible("rv_contacts", timeout)
                || isVisible("fab_add", Duration.ofSeconds(3))
                || isVisible("empty_view", Duration.ofSeconds(3));
    }

    public boolean isEmpty() {
        return isVisible("empty_view", Duration.ofSeconds(3));
    }

    public int contactCount() {
        try {
            return driver.findElements(byId("tv_name")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    public boolean tapAdd() {
        try {
            waitVisible("fab_add").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isContactShown(String text, Duration timeout) {
        return textShown(text, timeout);
    }

    public boolean tapEditFirst() {
        var edits = driver.findElements(byId("btn_edit"));
        if (edits.isEmpty()) return false;
        try {
            edits.get(0).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean tapDeleteFirst() {
        var dels = driver.findElements(byId("btn_delete"));
        if (dels.isEmpty()) return false;
        try {
            dels.get(0).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Confirms the "Delete Contact" dialog. */
    public boolean confirmDelete(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().textMatches(\"(?i)delete\")")).click();
                return true;
            } catch (WebDriverException ignored) {
            }
            sleep(400);
        }
        return false;
    }

    // ---------------- add / edit form ----------------

    public boolean isFormLoaded(Duration timeout) {
        return isVisible("et_email", timeout) || isVisible("et_first_name", Duration.ofSeconds(3));
    }

    public void setFirstName(String v) { setField("et_first_name", v); }
    public void setLastName(String v)  { setField("et_last_name", v); }
    public void setEmail(String v)     { setField("et_email", v); }
    public void setPhone(String v)     { setField("et_phone", v); }
    public void setNotes(String v)     { setField("et_notes", v); }

    private void setField(String id, String value) {
        try {
            scrollToId(id);
            WebElement f = driver.findElement(byId(id));
            f.click();
            f.clear();
            f.sendKeys(value);
            hideKeyboard();
        } catch (WebDriverException ignored) {
        }
    }

    /** Taps the toolbar Save (selectTv), falling back to a "Save" text button. */
    public boolean tapSave() {
        try {
            driver.findElement(byId("selectTv")).click();
            return true;
        } catch (WebDriverException ignored) {
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)save\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** After a valid save the form closes (returns to the contact list). */
    public boolean isFormStillOpen() {
        return isVisible("et_email", Duration.ofSeconds(2));
    }

    public boolean waitFormClosed(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!isVisible("et_email", Duration.ofMillis(600))) return true;
        }
        return false;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
