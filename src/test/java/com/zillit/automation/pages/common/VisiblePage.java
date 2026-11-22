package com.zillit.automation.pages.common;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for screens that keep more than one copy of the same ids alive — a ViewPager's
 * neighbouring page, a list of identical rows — and for the app's shared non-cancelable
 * confirm dialog ({@code common_dialog_layout_with_button}). Shared by the Continuity and
 * Schedule Distribution page objects, which both have the ViewPager trap.
 */
public abstract class VisiblePage extends BasePage {

    protected VisiblePage(AndroidDriver driver) {
        super(driver);
    }

    /** Rows of a platform PopupMenu: {@code android:id/title} — the ANDROID package's id. */
    public List<String> popupMenuTitles() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(By.id("android:id/title"))) out.add(text(e));
        } catch (WebDriverException ignored) { }
        return out;
    }

    /** The rows of the app's own option PopupWindow ({@code rv_option_view} → {@code mainTitle}). */
    public List<String> menuItems() {
        List<String> out = new ArrayList<>();
        for (WebElement t : elementsById("mainTitle")) out.add(text(t));
        return out;
    }

    public boolean tapMenuItem(String label) {
        for (WebElement t : elementsById("mainTitle")) {
            if (label.equals(text(t))) {
                try { t.click(); pause(1500); return true; }
                catch (WebDriverException e) { return false; }
            }
        }
        return false;
    }

    /** That PopupWindow ignores an outside tap; BACK closes it. */
    public void closeMenu() {
        if (!elementsById("mainTitle").isEmpty()) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            pause(800);
        }
    }

    // ───────────────────────────── dialogs ─────────────────────────────

    /** The common {@code showDialogWithButton} dialog: header, message, left/right buttons. */
    public String dialogHeader()  { return firstText("header"); }
    public String dialogMessage() { return firstText("message"); }
    public String dialogLeft()    { return firstText("leftButton"); }
    public String dialogRight()   { return firstText("rightButton"); }

    public boolean dialogShown() { return !onScreen("leftButton").isEmpty() && !onScreen("message").isEmpty(); }

    public boolean tapDialogLeft()  { return tapFirst("leftButton"); }
    public boolean tapDialogRight() { return tapFirst("rightButton"); }

    // ───────────────────────────── helpers ─────────────────────────────

    /**
     * Elements with this id whose centre is on the screen. Drops the off-screen ViewPager
     * page, which UiAutomator may still report once it has been visited.
     */
    protected List<WebElement> onScreen(String id) {
        List<WebElement> out = new ArrayList<>();
        int width;
        try { width = driver.manage().window().getSize().getWidth(); }
        catch (WebDriverException e) { width = Integer.MAX_VALUE; }
        for (WebElement e : elementsById(id)) {
            try {
                int cx = centreX(e);
                if (cx >= 0 && cx <= width && e.isDisplayed()) out.add(e);
            } catch (WebDriverException ignored) { }
        }
        return out;
    }

    protected String firstText(String id) {
        List<WebElement> e = onScreen(id);
        return e.isEmpty() ? null : text(e.get(0));
    }

    protected boolean tapFirst(String id) {
        List<WebElement> e = onScreen(id);
        if (e.isEmpty()) return false;
        try { e.get(0).click(); pause(1200); return true; }
        catch (WebDriverException ex) { return false; }
    }

    protected static String text(WebElement e) {
        try {
            String s = e.getText();
            return s == null ? "" : s.trim();
        } catch (WebDriverException ex) { return ""; }
    }

    protected static int centreX(WebElement e) {
        try { return e.getRect().getX() + e.getRect().getWidth() / 2; }
        catch (WebDriverException ex) { return -1; }
    }

    protected static List<WebElement> leftToRight(List<WebElement> in) {
        List<WebElement> out = new ArrayList<>(in);
        out.sort((a, b) -> Integer.compare(centreX(a), centreX(b)));
        return out;
    }

    protected void longPress(WebElement e) {
        try {
            int x = centreX(e);
            int y = e.getRect().getY() + e.getRect().getHeight() / 2;
            PointerInput f = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence s = new Sequence(f, 1);
            s.addAction(f.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
            s.addAction(f.createPointerDown(0));
            s.addAction(f.createPointerMove(Duration.ofMillis(1200), PointerInput.Origin.viewport(), x, y));
            s.addAction(f.createPointerUp(0));
            driver.perform(List.of(s));
        } catch (WebDriverException ignored) { }
    }

    /**
     * Whether a toast or snackbar carrying {@code message} appears. Polls the page SOURCE:
     * a Toast lives in its own window, which a UiSelector lookup never searches (see
     * {@code ManageToolGroupsPage.snackbarShown}).
     */
    public boolean messageShown(String message, Duration timeout) {
        String escaped = message.replace("&", "&amp;").replace("'", "&apos;")
                .replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                String src = driver.getPageSource();
                if (src != null && (src.contains(message) || src.contains(escaped))) return true;
            } catch (WebDriverException ignored) { }
            pause(250);
        }
        return false;
    }

    /** The text of the snackbar currently up, or null. */
    public String snackbarText(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        By snack = AppiumBy.androidUIAutomator(
                "new UiSelector().resourceIdMatches(\".*:id/snackbar_text\")");
        while (System.currentTimeMillis() < deadline) {
            try {
                List<WebElement> s = driver.findElements(snack);
                if (!s.isEmpty()) return text(s.get(0));
            } catch (WebDriverException ignored) { }
            pause(250);
        }
        return null;
    }

    protected void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
