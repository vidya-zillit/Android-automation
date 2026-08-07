package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The FULL comments screen ({@code CommentsFragment} / fragment_comments.xml), reached
 * from "View all N comments" on the file detail. Shows EVERY comment, where the detail
 * only previews the latest two.
 *
 * <p><b>Telling it apart from the file detail matters.</b> Both screens have
 * {@code etComment} and {@code rvComments}; only this one has {@code btnSend} (detail
 * uses {@code btnSendComment}). Checking the shared ids would report "the comments
 * screen opened" while still sitting on the detail.
 *
 * <p>As on the detail, rows are built in code and carry no ids — but here the edit and
 * delete icons have no contentDescription either, so a per-row action has to be found
 * structurally. Prefer doing edit/delete on {@link DriveFileDetailPage}, where the
 * buttons are described.
 */
public class DriveCommentsPage extends BasePage {

    public DriveCommentsPage(AndroidDriver driver) {
        super(driver);
    }

    /** btnSend is the marker: it exists ONLY here, never on the file detail. */
    public boolean isOpen(Duration timeout) {
        return isVisible("btnSend", timeout);
    }

    /** "Comments" or "Comments (N)". */
    public String title() { return text("tvTitle"); }

    public boolean isEmptyShown() { return isVisible("tvEmpty", Duration.ofSeconds(3)); }

    /** Author / body / date of every rendered row, flattened. */
    public List<String> commentTexts() {
        List<String> out = new ArrayList<>();
        try {
            WebElement list = driver.findElement(byId("rvComments"));
            for (WebElement tv : list.findElements(By.className("android.widget.TextView"))) {
                String t = tv.getText();
                if (t != null && !t.isBlank()) out.add(t);
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    public boolean hasComment(String body) {
        return commentTexts().stream().anyMatch(t -> t.equals(body));
    }

    /**
     * The list is a RecyclerView — an older comment sits below the fold and is simply
     * absent from the tree until scrolled to. Collect while scrolling, then return to
     * the top.
     */
    public List<String> allCommentTextsScrolled() {
        java.util.Set<String> seen = new java.util.LinkedHashSet<>(commentTexts());
        int stable = 0;
        for (int i = 0; i < 20 && stable < 2; i++) {
            int before = seen.size();
            swipe(0.75, 0.35);
            seen.addAll(commentTexts());
            stable = (seen.size() == before) ? stable + 1 : 0;
        }
        for (int i = 0; i < 8; i++) swipe(0.35, 0.75);
        return new ArrayList<>(seen);
    }

    public boolean hasCommentScrolled(String body) {
        return allCommentTextsScrolled().stream().anyMatch(t -> t.equals(body));
    }

    public boolean typeComment(String text) {
        try {
            WebElement field = driver.findElement(byId("etComment"));
            field.click();
            field.clear();
            if (!text.isEmpty()) field.sendKeys(text);
            hideKeyboard();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean tapSend() { return click("btnSend"); }

    public boolean back() {
        try { driver.navigate().back(); Thread.sleep(1200); return true; }
        catch (Exception e) { return false; }
    }

    private void swipe(double fromY, double toY) {
        try {
            var size = driver.manage().window().getSize();
            driver.executeScript("mobile: swipeGesture", java.util.Map.of(
                    "left", size.width / 2 - 100,
                    "top", (int) (size.height * Math.min(fromY, toY)),
                    "width", 200,
                    "height", (int) (size.height * Math.abs(fromY - toY)),
                    "direction", fromY > toY ? "up" : "down",
                    "percent", 0.8));
            Thread.sleep(600);
        } catch (Exception ignored) { }
    }

    private String text(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); return true; }
        catch (WebDriverException e) { return false; }
    }
}
