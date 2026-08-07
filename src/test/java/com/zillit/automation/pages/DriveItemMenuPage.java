package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;

/**
 * The per-row overflow (⋮) bottom sheet on a Drive folder/file row, and the Drive
 * navigation drawer — the two small surfaces every other Drive suite has to drive
 * through.
 *
 * <p>Row menu: Add to favorites (toggles to "Remove from favorites") · Share ·
 * Move to… · Edit Info · Details · Delete.
 */
public class DriveItemMenuPage extends BasePage {

    public DriveItemMenuPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- row overflow ----------------

    public boolean isOpen() {
        return isVisible("menuDelete", Duration.ofSeconds(5))
                || isVisible("menuShare", Duration.ofSeconds(2));
    }

    public boolean hasFavorite() { return isVisible("menuFavorite", Duration.ofSeconds(3)); }
    public boolean hasShare()    { return isVisible("menuShare", Duration.ofSeconds(2)); }
    public boolean hasMove()     { return isVisible("menuMove", Duration.ofSeconds(2)); }
    public boolean hasEditInfo() { return isVisible("menuRename", Duration.ofSeconds(2)); }
    public boolean hasDetails()  { return isVisible("menuDetails", Duration.ofSeconds(2)); }
    public boolean hasDelete()   { return isVisible("menuDelete", Duration.ofSeconds(2)); }

    /** "Add to favorites" or "Remove from favorites" — the label flips with state. */
    public String favoriteLabel() { return text("tvFavorite"); }

    public boolean tapFavorite() { return click("menuFavorite"); }
    public boolean tapShare()    { return click("menuShare"); }
    public boolean tapMove()     { return click("menuMove"); }
    public boolean tapEditInfo() { return click("menuRename"); }
    public boolean tapDetails()  { return click("menuDetails"); }
    public boolean tapDelete()   { return click("menuDelete"); }

    public boolean dismiss() {
        try { driver.navigate().back(); Thread.sleep(900); return true; }
        catch (Exception e) { return false; }
    }

    // ---------------- folder details sheet ----------------

    public boolean detailsOpen() {
        return isVisible("tvFolderName", Duration.ofSeconds(6));
    }

    public boolean detailsHasCreatedOn()  { return isVisible("tvCreatedOn", Duration.ofSeconds(3)); }
    public boolean detailsHasModifiedOn() { return isVisible("tvModifiedOn", Duration.ofSeconds(2)); }
    public boolean detailsHasOwner()      { return isVisible("tvOwner", Duration.ofSeconds(2)); }
    public boolean detailsHasAccess() {
        return isVisible("rvAccess", Duration.ofSeconds(2)) || isVisible("tvNoAccess", Duration.ofSeconds(2));
    }
    public boolean detailsHasActivity() {
        return isVisible("rvActivity", Duration.ofSeconds(2)) || isVisible("tvNoActivity", Duration.ofSeconds(2));
    }
    public String detailsFolderName() { return text("tvFolderName"); }

    // ---------------- drawer ----------------

    public boolean drawerOpen() { return isVisible("navTrash", Duration.ofSeconds(5)); }

    public boolean drawerHasSearch()    { return isVisible("navSearch", Duration.ofSeconds(3)); }
    public boolean drawerHasUploads()   { return isVisible("navUploads", Duration.ofSeconds(2)); }
    public boolean drawerHasFavorites() { return isVisible("navFavorites", Duration.ofSeconds(2)); }
    public boolean drawerHasTrash()     { return isVisible("navTrash", Duration.ofSeconds(2)); }
    public boolean drawerHasActivity()  { return isVisible("navActivity", Duration.ofSeconds(2)); }
    public boolean drawerHasProfile()   { return isVisible("profileRow", Duration.ofSeconds(2)); }

    public boolean openSearch()    { return click("navSearch"); }
    public boolean openUploads()   { return click("navUploads"); }
    public boolean openFavorites() { return click("navFavorites"); }
    public boolean openTrash()     { return click("navTrash"); }
    public boolean openActivity()  { return click("navActivity"); }

    private String text(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); return true; }
        catch (WebDriverException e) { return false; }
    }
}
