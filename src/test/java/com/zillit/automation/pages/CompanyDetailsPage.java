package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Settings → Admin Settings → "Company Details" (CompanyDetailsActivity).
 * Fields: etCompanyName, etCompanyAddress, etCountryCode (opens a country
 * picker), etCompanyPhone, etCompanyEmail, etCompanyNumber,
 * etRegisteredAddress, with a "Submit" button (buttonBtn) below the fold —
 * the screen is scrollable.
 */
public class CompanyDetailsPage extends BasePage {

    public CompanyDetailsPage(AndroidDriver driver) {
        super(driver);
    }

    /** Same menuText-in-bottomNavigation idiom as ToolsPage.openToolsTab(), for the Settings tab. */
    public boolean openSettingsTab() {
        try {
            WebElement navBar = driver.findElement(byId("bottomNavigation"));
            for (WebElement it : navBar.findElements(byId("menuText"))) {
                String t = it.getText();
                if (t != null && t.equalsIgnoreCase("Settings")) {
                    it.click();
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)Settings\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean openAdminSettings() {
        return tapListRow("Admin Settings");
    }

    public boolean openCompanyDetails() {
        return tapListRow("Company Details");
    }

    /**
     * Same ancestor-row xpath idiom already used by ProjectListPage.tapProject()
     * and DepartmentListingOrderPage.tapListRow(), with a bounded wait — this is
     * called right after an Activity transition, and an instant lookup can fire
     * before that screen's RecyclerView has finished rendering.
     */
    private boolean tapListRow(String label) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and contains(@text,\"" + label
                                    + "\")]/ancestor::*[contains(@resource-id,'commonListItemHolder')]")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isLoaded() {
        return isVisible("etCompanyName", Duration.ofSeconds(15));
    }

    public void setCompanyName(String value) {
        setField("etCompanyName", value);
    }

    public void setCompanyAddress(String value) {
        setField("etCompanyAddress", value);
    }

    public void setCompanyPhone(String value) {
        setField("etCompanyPhone", value);
    }

    public void setCompanyEmail(String value) {
        setField("etCompanyEmail", value);
    }

    public void setCompanyNumber(String value) {
        setField("etCompanyNumber", value);
    }

    public void setRegisteredAddress(String value) {
        setField("etRegisteredAddress", value);
    }

    /**
     * Bounded wait, not an instant lookup — this is often called right after a
     * screen transition (e.g. returning from the country picker), and a bare
     * findElement can fire before the field is back in the tree.
     */
    private void setField(String id, String value) {
        WebElement field = new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.presenceOfElementLocated(byId(id)));
        field.click();
        field.clear();
        if (!value.isEmpty()) {
            field.sendKeys(value);
        }
        hideKeyboard();
    }

    /** Opens the country ISD code picker. */
    public boolean tapCountryCode() {
        try {
            driver.findElement(byId("etCountryCode")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isCountryPickerOpen() {
        return isVisible("search_widget_edit_textvw", Duration.ofSeconds(5));
    }

    /** Searches the country picker's list (same search widget id as ProjectListPage). */
    public void searchCountry(String query) {
        WebElement field = waitVisible("search_widget_edit_textvw");
        field.clear();
        if (!query.isEmpty()) {
            field.sendKeys(query);
        }
        hideKeyboard();
    }

    /** Selects a country by its exact name from the (already searched/filtered) list. */
    public boolean selectCountry(String countryName) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                            "new UiSelector().resourceId(\"" + appPackage() + ":id/valueTextView\")"
                                    + ".text(\"" + countryName + "\")")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Scrolls the form down until the Submit button is reachable. */
    public void scrollToSubmit() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollToEnd(10)"));
        } catch (WebDriverException ignored) {
        }
    }

    /**
     * Scrolls the form back to the top (Company Logo section) — needed
     * whenever an earlier step (e.g. Submit) left the form scrolled down.
     */
    public void scrollToTop() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollToBeginning(10)"));
        } catch (WebDriverException ignored) {
        }
    }

    public void tapSubmit() {
        scrollToSubmit();
        driver.findElement(byId("buttonBtn")).click();
    }

    /** Top toolbar back arrow. */
    public boolean tapBackButton() {
        try {
            driver.findElement(byId("backBtn")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isOnAdminSettings(Duration timeout) {
        return textShown("ADMIN SETTINGS", timeout);
    }

    // ---------------- Custom Fields ----------------
    // "Add Field" (tvAddCustomField) inserts one Label/Value row (etFieldLabel,
    // etFieldValue) with its own remove icon (ivRemoveField), below the fold —
    // same scrollable form as Submit.

    /** Retries the tap and verifies the Label field actually appeared — a plain
     *  single click occasionally lands while the scroll is still settling and
     *  produces no visible effect. */
    public boolean tapAddField() {
        scrollToSubmit();
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                new WebDriverWait(driver, Duration.ofSeconds(8))
                        .until(ExpectedConditions.elementToBeClickable(byId("tvAddCustomField")))
                        .click();
            } catch (WebDriverException e) {
                return false;
            }
            if (isVisible("etFieldLabel", Duration.ofSeconds(5))) {
                return true;
            }
        }
        return false;
    }

    public void setFieldLabel(String value) {
        setField("etFieldLabel", value);
    }

    public void setFieldValue(String value) {
        setField("etFieldValue", value);
    }

    public String fieldLabelText() {
        return driver.findElement(byId("etFieldLabel")).getText();
    }

    public String fieldValueText() {
        return driver.findElement(byId("etFieldValue")).getText();
    }

    // ---------------- Company Logo upload ----------------
    // Edit icon -> "Choose below" sheet (Photo/Gallery) -> the app's own
    // Fallery gallery (same component as MediaPreviewPage: album list ->
    // photo grid -> select -> send FAB -> caption/preview -> send) -> back on
    // this screen with the logo uploaded (ivRemoveLogo appears) -> Preview
    // Header shows it in a bottom sheet (ivPreviewLogo) -> Close.

    public boolean tapEditLogo() {
        // An earlier Submit may have left the form scrolled down — the logo
        // section is at the very top, so a click there while scrolled away
        // silently no-ops (the element is found but not actually rendered).
        scrollToTop();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(byId("ivEditLogo")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isChoosePickerOpen() {
        return isVisible("rvItems", Duration.ofSeconds(5));
    }

    public boolean tapGalleryOption() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                            "new UiSelector().resourceId(\"" + appPackage() + ":id/tvLabel\").text(\"Gallery\")")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Opens the "Pictures" album in the app's own gallery (Fallery). */
    public boolean openPicturesFolder() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(
                            AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Pictures\")")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Selects the first photo thumbnail in the currently open album. */
    public boolean selectFirstGalleryPhoto() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(byId("imageViewPhotoMedia")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** The gallery grid's own Send FAB (after selecting at least one photo). */
    public boolean tapGallerySendFab() {
        try {
            driver.findElement(byId("floatingButtonSendMedia")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isMediaPreviewOpen() {
        return isVisible("sendBtn", Duration.ofSeconds(8));
    }

    /** The preview screen's own Send (caption pre-filled with "Company Logo"). */
    public boolean tapPreviewSend() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(byId("sendBtn")))
                    .click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** True once the logo has uploaded and this screen shows the remove/delete icon. */
    public boolean isLogoUploaded(Duration timeout) {
        return isVisible("ivRemoveLogo", timeout);
    }

    public boolean tapPreviewHeaderButton() {
        try {
            driver.findElement(byId("btnPreviewHeader")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isHeaderPreviewOpen() {
        return isVisible("ivPreviewLogo", Duration.ofSeconds(8));
    }

    public boolean tapCloseHeaderPreview() {
        try {
            driver.findElement(byId("btnClosePreview")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }
}
