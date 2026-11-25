package com.zillit.automation.pages.cc;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * C&C → Contacts tab (ContactsFragment, fragment_members.xml). Search
 * (search_widget_edit_textvw) + contact list (recMembers) with rows
 * chat_member_item.xml: userNameTv, designationTv, isFevouritesIv (fav-star
 * indicator), and per-row action icons message_button / voice_call / video_call
 * / location_icon / favorite_icon. Location + Favourites are per-row actions
 * here (no sub-tabs).
 */
public class CcContactsPage extends BasePage {

    public CcContactsPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("recMembers", timeout)
                || isVisible("search_widget_edit_textvw", Duration.ofSeconds(3))
                || isVisible("userNameTv", Duration.ofSeconds(3));
    }

    public int rowCount() {
        try { return driver.findElements(byId("userNameTv")).size(); }
        catch (WebDriverException e) { return 0; }
    }

    public List<String> names() {
        try {
            return driver.findElements(byId("userNameTv")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    public boolean isSearchVisible() {
        return isVisible("search_widget_edit_textvw", Duration.ofSeconds(4));
    }

    public void search(String text) {
        if (!isVisible("search_widget_edit_textvw", Duration.ofSeconds(5))) return;
        try {
            WebElement f = driver.findElement(byId("search_widget_edit_textvw"));
            f.click();
            f.clear();
            f.sendKeys(text);
            sleep(1200);
        } catch (WebDriverException ignored) {
        }
    }

    public void clearSearch() {
        try {
            WebElement f = driver.findElement(byId("search_widget_edit_textvw"));
            f.clear();
            sleep(800);
        } catch (WebDriverException ignored) {
        }
    }

    /** Row action icons present (proves the per-contact action row rendered). */
    public boolean hasActionIcons() {
        return has("message_button") || has("voice_call") || has("video_call")
                || has("location_icon") || has("favorite_icon");
    }

    public boolean hasFavouriteIcon() { return has("favorite_icon"); }
    public boolean hasLocationIcon()  { return has("location_icon"); }

    /** Tap the favourite (star) on the first contact row → toggles fav state. */
    public boolean tapFirstFavourite() {
        return tapFirst("favorite_icon");
    }

    /** Tap the location (pin) on the first contact row. */
    public boolean tapFirstLocation() {
        return tapFirst("location_icon");
    }

    public boolean tapFirstMessage() { return tapFirst("message_button"); }
    public boolean tapFirstVoiceCall() { return tapFirst("voice_call"); }
    public boolean tapFirstVideoCall() { return tapFirst("video_call"); }

    /** Count of rows currently flagged as favourite (isFevouritesIv shown). */
    public int favouriteMarkCount() {
        try { return driver.findElements(byId("isFevouritesIv")).size(); }
        catch (WebDriverException e) { return 0; }
    }

    // ---------------- list geometry (CNC-02) ----------------

    /** Bounds of the contact list (recMembers), or null. */
    public org.openqa.selenium.Rectangle listRect() {
        try { return driver.findElement(byId("recMembers")).getRect(); }
        catch (WebDriverException e) { return null; }
    }

    private static final String[] ROW_PARTS = {"userNameTv", "designationTv", "message_button", "voice_call",
            "video_call", "location_icon", "favorite_icon", "profileIv"};

    /** Bottom edge of the lowest visible contact row (lowest bottom of any row part), or -1. */
    public int lastRowBottom() {
        int max = -1;
        for (String id : ROW_PARTS) {
            for (WebElement e : elementsById(id)) {
                try { var r = e.getRect(); max = Math.max(max, r.getY() + r.getHeight()); }
                catch (WebDriverException ignored) { }
            }
        }
        return max;
    }

    /** Row pitch between two consecutive contact names, else one row's own height; -1 when empty. */
    public int rowHeight() {
        List<WebElement> names = elementsById("userNameTv");
        try {
            if (names.size() >= 2) return names.get(1).getRect().getY() - names.get(0).getRect().getY();
            if (names.size() == 1) {
                int top = names.get(0).getRect().getY();
                int bottom = lastRowBottom();
                return bottom > top ? bottom - top : -1;
            }
        } catch (WebDriverException ignored) {
        }
        return -1;
    }

    private boolean has(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    private boolean tapFirst(String id) {
        try {
            var els = driver.findElements(byId(id));
            if (els.isEmpty()) return false;
            els.get(0).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
