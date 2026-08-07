package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * FILE REQUESTS — the two folder-scoped sheets behind the in-folder FAB:
 *
 * <ul>
 *   <li><b>Request Files</b> ({@code RequestFilesBottomSheet} /
 *       bottom_sheet_request_files.xml) — the config form, the success card with the
 *       created link, and an always-visible list of this folder's active requests.</li>
 *   <li><b>View File Requests</b> ({@code ViewFileRequestsBottomSheet} /
 *       bottom_sheet_view_file_requests.xml) — read-only cards with a ⋯ menu offering
 *       Copy link and Revoke request.</li>
 * </ul>
 *
 * <p>Both sheets render their rows/cards in code, so only the CONTAINERS have ids
 * ({@code requestsContainer}, {@code cardsContainer}) — a request is found by its title
 * text inside them, never by index.
 *
 * <p>⚠ Validation here is a <b>Toast</b> ("Please enter a title for the request."), which
 * is invisible to both the page source and a uiautomator dump. Assert on behaviour: the
 * form is still showing and no success card appeared.
 */
public class DriveRequestFilesPage extends BasePage {

    public DriveRequestFilesPage(AndroidDriver driver) {
        super(driver);
    }

    // ==================== Request Files sheet ====================

    public boolean isOpen(Duration timeout) {
        return isVisible("btnCreateRequest", timeout) || isVisible("etTitle", Duration.ofSeconds(3));
    }

    /** "Files uploaded through this link will land in X." */
    public String destinationInfo() { return text("tvDestinationInfo"); }

    public boolean hasTitleField()        { return isVisible("etTitle", Duration.ofSeconds(4)); }
    public boolean hasInstructionsField() { return isVisible("etInstructions", Duration.ofSeconds(2)); }
    public boolean hasThankYouField()     { return present("etThankYou"); }
    public boolean hasExpirySpinner()     { return present("spExpires"); }
    public boolean hasMaxFilesSpinner()   { return present("spMaxFiles"); }
    public boolean hasSizeSpinner()       { return present("spSize"); }
    public boolean hasFileTypeSpinner()   { return present("spFileType"); }
    public boolean hasRequireEmail()      { return present("cbRequireEmail"); }
    public boolean hasRequireName()       { return present("cbRequireName"); }
    public boolean hasRecipientInput()    { return present("etRecipientInput"); }
    public boolean hasCreateButton()      { return present("btnCreateRequest"); }

    /** The form half of the sheet — hidden once a request is created. */
    public boolean isFormShown() { return isVisible("formContainer", Duration.ofSeconds(3)); }

    /** Creating a request is a server round-trip, so give it room. */
    public boolean isSuccessShown() { return isSuccessShown(Duration.ofSeconds(25)); }

    /**
     * Short-timeout variant for the NEGATIVE check ("no request was created"): waiting
     * 25s for a container that must never appear would add half a minute to the
     * validation case for nothing.
     */
    public boolean isSuccessShown(Duration timeout) { return isVisible("successContainer", timeout); }

    public String titleText() { return text("etTitle"); }

    public boolean setTitle(String v)        { return type("etTitle", v); }
    public boolean setInstructions(String v) { return type("etInstructions", v); }
    public boolean setThankYou(String v)     { return type("etThankYou", v); }

    /**
     * Enters a recipient and commits it to a chip.
     *
     * <p>The field commits on IME_ACTION_DONE <i>or</i> on a trailing comma / space /
     * semicolon — the comma is used here because it does not depend on the soft keyboard
     * being up, which it often is not after {@code sendKeys}.
     */
    public boolean addRecipient(String email) {
        return type("etRecipientInput", email + ",");
    }

    /** Text of each committed recipient chip. */
    public List<String> recipientChips() { return childTexts("chipGroupRecipients"); }

    public boolean create() {
        return clickScrolling("btnCreateRequest");
    }

    // ---- success card ----

    public String createdUrl() { return text("tvCreatedUrl"); }

    public boolean createdBannerShown() {
        return textShown("Request created", Duration.ofSeconds(8));
    }

    public boolean copyCreatedUrl() { return clickScrolling("btnCopyCreatedUrl"); }

    public boolean createAnother()  { return clickScrolling("btnCreateAnother"); }

    /** Limit chips on the success card: expiry, max size, email/name required. */
    public List<String> createdChips() { return childTexts("createdChips"); }

    // ---- the sheet's own active-requests list ----

    /** "(N)" next to the active-requests header. */
    public String requestsCount()  { return text("tvRequestsCount"); }
    public boolean noRequestsShown() { return isVisible("tvNoRequests", Duration.ofSeconds(3)); }

    public List<String> activeRequestTexts() { return childTexts("requestsContainer"); }

    public boolean listsRequest(String title) {
        return activeRequestTexts().stream().anyMatch(t -> t.equals(title));
    }

    /** The list arrives from the server, so poll instead of reading once. */
    public boolean waitForRequest(String title, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (listsRequest(title)) return true;
            sleep(1000);
        }
        return false;
    }

    public boolean close() { return click("btnClose"); }

    // ==================== View File Requests sheet ====================

    /** btnDone/btnRefresh exist only on the read-only sheet, so they identify it. */
    public boolean isViewSheetOpen(Duration timeout) {
        return isVisible("btnDone", timeout) || isVisible("btnRefresh", Duration.ofSeconds(3));
    }

    public boolean viewSheetEmptyShown() { return isVisible("tvEmpty", Duration.ofSeconds(3)); }

    public List<String> viewSheetCardTexts() { return childTexts("cardsContainer"); }

    public boolean viewSheetHasRequest(String title) {
        return viewSheetCardTexts().stream().anyMatch(t -> t.equals(title));
    }

    public boolean viewSheetRefresh() { return click("btnRefresh"); }
    public boolean viewSheetDone()    { return click("btnDone"); }

    public boolean waitForViewSheetRequest(String title, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (viewSheetHasRequest(title)) return true;
            viewSheetRefresh();
            sleep(1500);
        }
        return false;
    }

    public boolean waitForViewSheetRequestGone(String title, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!viewSheetHasRequest(title)) return true;
            viewSheetRefresh();
            sleep(1500);
        }
        return false;
    }

    /**
     * Opens the ⋯ menu on the card with this title.
     *
     * <p>The card is {@code [ header[ title, ⋯ ], … ]}, so the overflow is a sibling of
     * the title one level up. Every card's button carries the same "More options"
     * description, so tapping the first match would open the wrong card's menu.
     */
    public boolean openCardMenu(String title) {
        String xpath = "//android.widget.TextView[@text=" + quote(title) + "]"
                + "/parent::*//*[@content-desc='More options']";
        try {
            driver.findElement(AppiumBy.xpath(xpath)).click();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean menuHasRevoke() { return textShown("Revoke request", Duration.ofSeconds(5)); }
    public boolean menuHasCopyLink() { return textShown("Copy link", Duration.ofSeconds(2)); }

    public boolean tapRevokeMenu() { return tapText("Revoke request"); }
    public boolean tapCopyLinkMenu() { return tapText("Copy link"); }

    /** The confirm dialog's own words — "The upload link stops working immediately." */
    public boolean revokeConfirmShown() {
        return textShown("stops working immediately", Duration.ofSeconds(6));
    }

    public boolean confirmRevoke(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceId(\"android:id/button1\")")).click();
                return true;
            } catch (WebDriverException ignored) { }
            sleep(400);
        }
        return false;
    }

    // ==================== helpers ====================

    /**
     * Every non-blank text inside a programmatically-populated container.
     *
     * <p>Matches on ANY descendant, not on {@code android.widget.TextView}: a Material
     * {@code Chip} extends CompoundButton and surfaces as a CheckBox/Button in the tree,
     * so a TextView-only search silently returns no recipient chips at all.
     */
    private List<String> childTexts(String containerId) {
        List<String> out = new ArrayList<>();
        try {
            WebElement container = driver.findElement(byId(containerId));
            for (WebElement child : container.findElements(By.xpath(".//*"))) {
                String t = child.getText();
                if (t != null && !t.isBlank()) out.add(t);
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    /**
     * "Is this field part of the form?" — scrolling if needed.
     *
     * <p>The sheet is a long scroll view capped at 92% of the screen, so everything from
     * the size spinner down starts OFF screen. A plain visibility check would report the
     * lower half of the form as missing and fail the "every field is present" case
     * against a perfectly good sheet.
     */
    private boolean present(String id) {
        if (isVisible(id, Duration.ofSeconds(2))) return true;
        scrollToId(id);
        if (isVisible(id, Duration.ofSeconds(2))) return true;
        for (int i = 0; i < 4; i++) {
            swipeUpInSheet();
            if (isVisible(id, Duration.ofSeconds(1))) return true;
        }
        return false;
    }

    /**
     * Clicks a control that may be below the fold of the sheet's scroll view.
     *
     * <p>{@code scrollToId} drives {@code UiScrollable(scrollable(true))}, which grabs
     * the FIRST scrollable in the tree — with a bottom sheet open that can be the folder
     * list behind it, leaving the sheet untouched. So: try the direct click, then scroll
     * by gesture INSIDE the sheet, then try again.
     */
    private boolean clickScrolling(String id) {
        if (click(id)) return true;
        scrollToId(id);
        if (click(id)) return true;
        for (int i = 0; i < 4; i++) {
            swipeUpInSheet();
            if (click(id)) return true;
        }
        return false;
    }

    private void swipeUpInSheet() {
        try {
            var size = driver.manage().window().getSize();
            driver.executeScript("mobile: swipeGesture", java.util.Map.of(
                    "left", size.width / 2 - 100,
                    "top", (int) (size.height * 0.35),
                    "width", 200,
                    "height", (int) (size.height * 0.40),
                    "direction", "up",
                    "percent", 0.8));
            Thread.sleep(600);
        } catch (Exception ignored) { }
    }

    /** Types into a field, scrolling the sheet if it starts below the fold. */
    private boolean type(String id, String value) {
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                WebElement field = driver.findElement(byId(id));
                field.click();
                field.clear();
                if (!value.isEmpty()) field.sendKeys(value);
                hideKeyboard();
                return true;
            } catch (WebDriverException notReachable) {
                if (attempt == 0) scrollToId(id); else swipeUpInSheet();
            }
        }
        return false;
    }

    private String text(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); return true; }
        catch (WebDriverException e) { return false; }
    }

    private boolean tapText(String t) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + t + "\")")).click();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    private static String quote(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
