package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The DRIVE ACTIVITY LOG ({@code ActivityFragment} / fragment_activity.xml), reached
 * from the drawer's Activity row. One row per action: an icon, "Action · item name"
 * ({@code tvAction}), optional details and a relative timestamp.
 *
 * <p>⚠ {@code recyclerView}, {@code emptyState} and {@code swipeRefresh} are the SAME ids
 * Drive home and Trash use, so they cannot tell you which screen you are on. The only
 * markers unique to this screen are a rendered {@code tvAction} row or the
 * "No recent activity" empty text — {@link #isOpen} uses exactly those.
 */
public class DriveActivityPage extends BasePage {

    public DriveActivityPage(AndroidDriver driver) {
        super(driver);
    }

    /**
     * True only on the activity log. Deliberately does NOT accept a bare
     * {@code recyclerView}: that also matches home and Trash, and a suite that believed
     * it was on the log while sitting on Trash would assert against the wrong list.
     */
    public boolean isOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isVisible("tvAction", Duration.ofSeconds(2))) return true;
            if (isEmptyShown()) return true;
            sleep(500);
        }
        return false;
    }

    public boolean isEmptyShown() {
        return textShown("No recent activity", Duration.ofSeconds(2));
    }

    /** The "Action · item" line of every rendered row. */
    public List<String> entries() {
        try {
            return driver.findElements(byId("tvAction")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    /** Rendered rows are only one screenful — scroll-collect before judging absence. */
    public List<String> allEntriesScrolled() {
        Set<String> seen = new LinkedHashSet<>(entries());
        int stable = 0;
        for (int i = 0; i < 20 && stable < 2; i++) {
            int before = seen.size();
            swipe(0.75, 0.35);
            seen.addAll(entries());
            stable = (seen.size() == before) ? stable + 1 : 0;
        }
        for (int i = 0; i < 8; i++) swipe(0.35, 0.75);
        return new ArrayList<>(seen);
    }

    public boolean hasEntryContaining(String fragment) {
        return allEntriesScrolled().stream()
                .anyMatch(e -> e.toLowerCase().contains(fragment.toLowerCase()));
    }

    /**
     * The log is written server-side after the action completes, so it can lag the UI by
     * a few seconds. Poll, refreshing between attempts.
     */
    public boolean waitForEntryContaining(String fragment, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (hasEntryContaining(fragment)) return true;
            refresh();
            sleep(2500);
        }
        return false;
    }

    /** Pull-to-refresh. Returns false when the screen has no swipeRefresh at all. */
    public boolean refresh() {
        try {
            driver.findElement(byId("swipeRefresh"));
            swipe(0.35, 0.80);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /** True when the log is READABLE — rows rendered, or the empty state showing. */
    public boolean isReadable() {
        return !entries().isEmpty() || isEmptyShown();
    }

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
            Thread.sleep(700);
        } catch (Exception ignored) { }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
