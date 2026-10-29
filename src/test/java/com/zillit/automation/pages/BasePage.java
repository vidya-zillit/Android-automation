package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public abstract class BasePage {

    /**
     * Runtime resource-id prefix. Flavors rewrite it to the full application id
     * (e.g. com.zillit.zillitapp.dev), so it comes from configuration.
     *
     * <p><b>Deliberately NOT the foreground package.</b> This field is static, so
     * it is resolved once and reused by every page in the run — which means one
     * bad reading poisons everything after it. It used to fall back to
     * {@code getCurrentPackage()}, and whenever the app happened to be backgrounded
     * at that moment (launcher up, camera or QR scanner still in front) every
     * locator was built against that other package. Nothing matched, and because
     * the ids were merely WRONG rather than absent, each lookup ran its full
     * timeout — the suite appeared to hang instead of failing.
     */
    private static String idPrefix;

    protected final AndroidDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        if (idPrefix == null) {
            String pkg = com.zillit.automation.core.Config.configuredAppPackage();
            if (pkg.isEmpty()) {
                // Nothing configured — the foreground app is the only clue left.
                pkg = driver.getCurrentPackage();
            }
            idPrefix = pkg + ":id/";
        }
    }

    protected By byId(String id) {
        return AppiumBy.id(idPrefix + id);
    }

    /** The app package resource-ids resolve against (config override or foreground app). */
    protected String appPackage() {
        return idPrefix.substring(0, idPrefix.indexOf(":id/"));
    }

    protected WebElement waitVisible(String id) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(byId(id)));
    }

    protected boolean isVisible(String id, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.visibilityOfElementLocated(byId(id)));
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Scrolls the first scrollable container until the given id is on screen. */
    protected void scrollToId(String id) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().resourceId(\"" + idPrefix + id + "\"))"));
        } catch (WebDriverException ignored) {
            // Not scrollable or already on screen — the follow-up lookup will decide.
        }
    }

    /** Types into the EditText nested inside a widget container. */
    protected void typeInto(String containerId, String text) {
        scrollToId(containerId);
        WebElement field = waitVisible(containerId)
                .findElement(By.className("android.widget.EditText"));
        field.clear();
        if (!text.isEmpty()) {
            field.sendKeys(text);
        }
        hideKeyboard();
    }

    protected void hideKeyboard() {
        try {
            driver.hideKeyboard();
        } catch (WebDriverException ignored) {
            // Keyboard was not open.
        }
    }

    /**
     * Dismisses the soft keyboard ONLY when one is actually up.
     *
     * <p>{@link #hideKeyboard()} is not safe to call blindly: UiAutomator2 implements it
     * by pressing BACK, and {@code sendKeys} frequently types without ever raising the
     * soft keyboard — so the BACK lands on the ACTIVITY instead and navigates away. That
     * is how a search case ended up asserting against the Tools screen: it typed a query,
     * "hid" a keyboard that was never shown, and the BACK press closed Document
     * Distribution entirely. The failure then read as "the empty state did not appear",
     * which is true but says nothing about search.
     *
     * <p>Prefer this in any page where a stray BACK would leave the screen under test.
     */
    protected void hideKeyboardIfShown() {
        try {
            if (driver.isKeyboardShown()) driver.hideKeyboard();
        } catch (WebDriverException ignored) {
            // No keyboard, or the driver cannot tell — either way, do NOT press BACK.
        }
    }

    /**
     * Settles the IME WITHOUT any possibility of a BACK press.
     *
     * <p>Use this inside dialogs and bottom sheets. {@link #hideKeyboardIfShown()} only
     * narrows the window rather than closing it: {@code isKeyboardShown()} still reports
     * true for a moment after the IME has begun animating away, so the guarded
     * {@code hideKeyboard()} can still fire a BACK — and in a cancelable
     * {@code BottomSheetDialog} that BACK dismisses the SHEET. The symptom is brutal to
     * read: the next field lookup reports "field missing" and the Save button reports
     * "disabled", for a sheet that no longer exists.
     *
     * <p>{@code performEditorAction done} is consumed by the focused editor and never
     * reaches the dialog. Leaving the keyboard up afterwards is harmless — a
     * {@code BottomSheetDialog} uses {@code SOFT_INPUT_ADJUST_RESIZE}, so its buttons sit
     * above the IME.
     */
    /**
     * Closes the soft keyboard with BACK — but ONLY when it is stably up.
     *
     * <p>Needed because a covered view is not merely off-screen, it is absent from the
     * accessibility tree: with the IME up, {@code dd_recipients_recycler} disappears from
     * the dump entirely, so scrolling for it cannot help and a recipient that was added
     * perfectly well reads back as zero rows.
     *
     * <p>BACK is safe HERE and nowhere else: with the IME showing, Android gives the BACK
     * to the input method, which closes it and never reaches the dialog. The danger is
     * firing BACK when the keyboard is already going away — then the dialog takes it and
     * dismisses. {@code isKeyboardShown()} briefly reports true during that animation, so
     * this samples it TWICE with a gap and only acts when both agree.
     */
    protected boolean closeKeyboardIfStablyShown() {
        try {
            if (!driver.isKeyboardShown()) return false;
            Thread.sleep(400);
            if (!driver.isKeyboardShown()) return false;   // it was already closing
            driver.navigate().back();
            Thread.sleep(500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected void settleImeSafely() {
        // DELIBERATELY A NO-OP. Both ways of dismissing the IME after typing turned out to
        // corrupt the test:
        //   • driver.hideKeyboard() is a BACK press on UiAutomator2 — inside a cancelable
        //     dialog it dismisses the SHEET, after which every field reads as "missing"
        //     and every button as "disabled".
        //   • mobile: performEditorAction {done} appends literal text to the focused
        //     field. Observed on device: an address typed as "not-an-email" read back as
        //     "not-an-email/done/" immediately before the Add tap.
        // Leaving the keyboard up is harmless — dialogs use SOFT_INPUT_ADJUST_RESIZE, so
        // their buttons stay reachable — and anything the IME covers is brought back by
        // the pages' explicit reveal helpers (revealRecipients / revealBodyEditor), which
        // is the honest fix: scroll to what you need instead of relying on a side effect.
        // Kept as a named method so the reasoning stays attached to the call sites.
    }

    /** True when any element containing the text (snackbar, toast, label) appears. */
    public boolean textShown(String text, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.presenceOfElementLocated(
                            AppiumBy.androidUIAutomator(
                                    "new UiSelector().textContains(\"" + text + "\")")));
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean textShown(String text) {
        return textShown(text, Duration.ofSeconds(10));
    }

    /**
     * True only when the ADMIN SETTINGS screen itself is up.
     *
     * <p>Do NOT implement this as {@code textShown("ADMIN SETTINGS")}.
     * {@code UiSelector.textContains()} is case-INSENSITIVE, so that check also
     * matches the Settings tab's row labelled "Admin Settings" — verified on
     * device, where it returned the {@code nameTv} whose text is "Admin
     * Settings". Every page object carried that version, so a caller standing on
     * the Settings tab was told it had already arrived, skipped tapping the row,
     * and then hunted for Admin Settings entries that were never on screen.
     *
     * <p>The two screens are separated on structure instead:
     * {@code adminSettingsRecyclerView} exists only here, and the bottom nav is
     * absent here but always present on the Settings tab.
     */
    protected boolean onAdminSettingsScreen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                if (!driver.findElements(byId("adminSettingsRecyclerView")).isEmpty()
                        && driver.findElements(byId("bottomNavigation")).isEmpty()
                        && "ADMIN SETTINGS".equalsIgnoreCase(headerTextOrEmpty())) {
                    return true;
                }
            } catch (WebDriverException ignored) {
                // Tree in flux mid-transition — retry until the budget runs out.
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            pause(200);
        }
    }

    /**
     * True when the SETTINGS TAB is up — the bottom-nav screen carrying the
     * "Admin Settings" row. Mutually exclusive with
     * {@link #onAdminSettingsScreen} by construction: this one requires the
     * bottom nav, that one requires its absence.
     */
    protected boolean onSettingsTabScreen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                boolean bottomNav = !driver.findElements(byId("bottomNavigation")).isEmpty();
                boolean adminRow = !driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceIdMatches(\".*:id/nameTv\")"
                                + ".textMatches(\"(?i)Admin Settings\")")).isEmpty();
                if (bottomNav && adminRow) {
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            pause(200);
        }
    }

    /**
     * The screen's header, or "" when it has none.
     *
     * <p>Needed because {@code adminSettingsRecyclerView} is NOT unique to Admin
     * Settings — the USER MANAGEMENT screen reuses the same id and also has no
     * bottom nav, so without the header those two screens are indistinguishable
     * and a caller standing in User Management is told it is on Admin Settings.
     */
    private String headerTextOrEmpty() {
        try {
            var found = driver.findElements(byId("headerText"));
            if (found.isEmpty()) {
                return "";
            }
            String t = found.get(0).getText();
            return t == null ? "" : t.trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /**
     * Private on purpose: JoinUserDetailPage and StartProjectPage already declare
     * their own private pause(long), and a protected one here would clash with
     * both as a weaker-access override.
     */
    private void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
