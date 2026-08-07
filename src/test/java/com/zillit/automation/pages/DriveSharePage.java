package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * DRIVE SHARE — "Manage File Access" (ShareFragment), reached from a row's ⋮ → Share.
 *
 * <p>Two tabs: <b>People</b> (search a project user, grant per-flag rights, Save) and
 * <b>Share via link</b> (generate a link with permission / expiry / max-views, copy,
 * revoke).
 *
 * <p><b>Per-user rights are three unlabelled Switches</b> — Can View, Can Edit, Can
 * Download — and none of them carries a resource id, so they can only be reached by
 * pairing each Switch with the label sitting on the same row (see
 * {@link #setRight(String, boolean)}). Worth raising with the app team: three ids would
 * remove the most fragile locator in this whole suite.
 *
 * <p>Adding a user is likewise an unlabelled control at the right edge of their search
 * row, so {@link #addUser(String)} taps by geometry rather than by id.
 */
public class DriveSharePage extends BasePage {

    public static final String VIEW = "Can View";
    public static final String EDIT = "Can Edit";
    public static final String DOWNLOAD = "Can Download";

    public DriveSharePage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isOpen(Duration timeout) {
        return isVisible("tabShareMode", timeout) || isVisible("etSearchUsers", Duration.ofSeconds(3));
    }

    public String title() { return text("tvTitle"); }

    public boolean hasPeopleTab() { return textShown("People", Duration.ofSeconds(4)); }
    public boolean hasLinkTab()   { return textShown("Share via link", Duration.ofSeconds(3)); }

    public boolean openPeopleTab() { return tapText("People"); }
    public boolean openLinkTab()   { return tapText("Share via link"); }

    // ---------------- People tab ----------------

    /** "No one has access yet. Add people above to share." */
    public boolean noAccessYet() { return isVisible("tvNoAccess", Duration.ofSeconds(4)); }

    /**
     * Everyone currently listed under "who has access".
     *
     * <p>The rows are added in code and carry no ids, so this reads every text node in
     * {@code accessContainer} — names, emails and role labels all come back together.
     * Enough to answer the only question that matters: did the grant SURVIVE the save?
     */
    public List<String> grantedUsers() {
        try {
            WebElement container = driver.findElement(byId("accessContainer"));
            return container.findElements(org.openqa.selenium.By.xpath(".//*")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    /** True when the access list names this user (case-insensitive, substring-tolerant). */
    public boolean hasGrantFor(String user) {
        return grantedUsers().stream()
                .anyMatch(t -> t.equalsIgnoreCase(user) || t.toLowerCase().contains(user.toLowerCase()));
    }

    public boolean searchUser(String name) {
        try {
            WebElement el = driver.findElement(byId("etSearchUsers"));
            el.click(); el.clear(); el.sendKeys(name);
            hideKeyboard();
            sleep(2000);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public List<String> searchResults() {
        try {
            return driver.findElements(AppiumBy.androidUIAutomator(
                            "new UiSelector().resourceId(\"" + appPackage() + ":id/rvSearchUsers\")"
                                    + ".childSelector(new UiSelector().className(\"android.widget.TextView\"))"))
                    .stream().map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    public boolean userListed(String name) {
        return textShown(name, Duration.ofSeconds(6));
    }

    /**
     * Adds a user to the access list — the orange <b>+</b> at the right of their search
     * row. Nothing else on the row grants access: the name is a plain TextView with no
     * click listener, so "tap the user" silently does nothing and the share is never made.
     *
     * <p>{@code UserSearchAdapter} builds each row in code as
     * {@code [ avatar, [name, email], ImageButton(+) ]} — the + has no id and no
     * contentDescription, but it IS the row's only {@code ImageButton}. So anchor on the
     * name, walk up to the row, and take that button.
     *
     * <p>The previous implementation tapped blind coordinates (~89% of the screen width at
     * the row's centre) calibrated once by hand. That silently missed here: the tap
     * reported success, no access was granted, and the folder never reached the recipient
     * — which is what made "folder sharing is broken" look like a product bug.
     *
     * <p>Either way the result is CONFIRMED by {@link #rightsShown()}, never assumed.
     */
    public boolean addUser(String name) {
        // 1. The + button itself, found structurally — SCOPED TO THE SEARCH LIST.
        //    A user who already has access appears TWICE on this screen: once in the
        //    search results (with +) and once in the access list (with ×). Both are
        //    unlabelled ImageButtons, so an unscoped match hit the × and opened
        //    "Remove access for <user>?" — the exact opposite of the intent.
        String xpath = "//*[@resource-id='" + appPackage() + ":id/rvSearchUsers']"
                + "//android.widget.TextView[@text='" + name + "']"
                + "/parent::*/parent::*//android.widget.ImageButton";
        try {
            driver.findElement(AppiumBy.xpath(xpath)).click();
            sleep(2500);
            if (rightsShown()) return true;
        } catch (WebDriverException notFound) {
            // fall through to the geometry fallback
        }

        // 2. Fallback: tap where the + sits, in case the row structure changes.
        try {
            WebElement label = driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + name + "\")"));
            Rectangle lr = label.getRect();
            int rowY = lr.getY() + lr.getHeight() / 2;
            int width = driver.manage().window().getSize().getWidth();
            for (double frac : new double[]{0.892, 0.86, 0.93}) {
                driver.executeScript("mobile: clickGesture", java.util.Map.of(
                        "x", (int) (width * frac), "y", rowY));
                sleep(2500);
                if (rightsShown()) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    /**
     * True once the user has actually been ADDED — whichever access row this screen uses.
     *
     * <p>The two item types render completely different rows (see {@code ShareFragment}'s
     * own "Row anatomy" comment):
     *
     * <pre>
     *   FILE:   (A) Name  [×]        · Can View / Can Edit / Can Download switches
     *   FOLDER: (A) Name  [Editor ▾] [×]      · a role SPINNER, no switches at all
     * </pre>
     *
     * <p>Checking only for the switches — which is what this did — meant every FOLDER
     * share reported failure while the user sat correctly in the access list, waiting for
     * Save Permissions. That is what made "folder sharing never reaches the other user"
     * look like a product bug for two sessions.
     */
    public boolean rightsShown() {
        if (textShown(VIEW, Duration.ofSeconds(3)) && switches().size() >= 3) return true;   // file
        return !roleSpinners().isEmpty();                                                    // folder
    }

    /** True when this screen is sharing a FOLDER (role dropdown instead of switches). */
    public boolean isFolderMode() {
        return switches().size() < 3 && !roleSpinners().isEmpty();
    }

    private List<WebElement> roleSpinners() {
        try {
            return driver.findElements(AppiumBy.className("android.widget.Spinner"));
        } catch (WebDriverException e) { return List.of(); }
    }

    /**
     * Sets a folder recipient's role — the {@code viewer / editor / owner} dropdown on
     * their access row.
     *
     * <p>Anchored on the user's name and walked up to the row, because every row's
     * spinner is an identical unlabelled {@code Spinner}; the Link tab's own spinners
     * (permission / expiry / max views) are on screen too, so "the first Spinner" is
     * routinely the wrong one.
     */
    public boolean setRole(String user, String role) {
        WebElement spinner = roleSpinnerFor(user);
        if (spinner == null) return false;
        try {
            if (role.equalsIgnoreCase(currentRole(user))) return true;
            spinner.click();
            sleep(1200);
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)" + role + "\")")).click();
            sleep(1500);
            return role.equalsIgnoreCase(currentRole(user));
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** The role currently shown on a folder recipient's row ("viewer" / "editor" / "owner"). */
    public String currentRole(String user) {
        WebElement spinner = roleSpinnerFor(user);
        if (spinner == null) return "";
        try {
            for (WebElement t : spinner.findElements(By.className("android.widget.TextView"))) {
                String v = t.getText();
                if (v != null && !v.isBlank()) return v;
            }
            String own = spinner.getText();
            return own == null ? "" : own;
        } catch (WebDriverException e) { return ""; }
    }

    private WebElement roleSpinnerFor(String user) {
        String xpath = "//android.widget.TextView[@text='" + user + "']"
                + "/ancestor::android.widget.LinearLayout[1]//android.widget.Spinner";
        try {
            return driver.findElement(AppiumBy.xpath(xpath));
        } catch (WebDriverException notFound) {
            // The name may sit one level deeper (info column) — widen by one ancestor.
            try {
                return driver.findElement(AppiumBy.xpath(
                        "//android.widget.TextView[@text='" + user + "']"
                                + "/ancestor::android.widget.LinearLayout[2]//android.widget.Spinner"));
            } catch (WebDriverException e) { return null; }
        }
    }

    private List<WebElement> switches() {
        try {
            return driver.findElements(AppiumBy.className("android.widget.Switch"));
        } catch (WebDriverException e) { return List.of(); }
    }

    /**
     * Reads a right's state by pairing the label with the Switch on the SAME ROW —
     * the switches have no ids, so vertical alignment is the only link between them.
     */
    public Boolean rightState(String label) {
        try {
            WebElement lbl = driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + label + "\")"));
            int ly = lbl.getRect().getY() + lbl.getRect().getHeight() / 2;
            WebElement best = null;
            int bestDist = Integer.MAX_VALUE;
            for (WebElement s : switches()) {
                int sy = s.getRect().getY() + s.getRect().getHeight() / 2;
                int d = Math.abs(sy - ly);
                if (d < bestDist) { bestDist = d; best = s; }
            }
            if (best == null) return null;
            return "true".equalsIgnoreCase(best.getAttribute("checked"));
        } catch (Exception e) { return null; }
    }

    /** Sets a right on/off, no-op when it already matches. */
    public boolean setRight(String label, boolean on) {
        Boolean cur = rightState(label);
        if (cur == null) return false;
        if (cur == on) return true;
        try {
            WebElement lbl = driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + label + "\")"));
            int ly = lbl.getRect().getY() + lbl.getRect().getHeight() / 2;
            WebElement best = null;
            int bestDist = Integer.MAX_VALUE;
            for (WebElement s : switches()) {
                int sy = s.getRect().getY() + s.getRect().getHeight() / 2;
                int d = Math.abs(sy - ly);
                if (d < bestDist) { bestDist = d; best = s; }
            }
            if (best == null) return false;
            best.click();
            sleep(800);
            return Boolean.valueOf(on).equals(rightState(label));
        } catch (Exception e) { return false; }
    }

    public boolean save() {
        if (click("btnSave")) { sleep(3000); return true; }
        boolean t = tapText("Save Permissions");
        sleep(3000);
        return t;
    }

    public boolean saveBarShown() { return isVisible("saveBar", Duration.ofSeconds(4)); }

    /**
     * Grants a user exactly the given rights and saves.
     *
     * <p>Four separate things have to happen — search, <b>+</b>, the three rights, and
     * <b>Save Permissions</b> — and until the save the grant exists only on screen. Each
     * step is logged, because a bare false here is indistinguishable between "the user
     * was never found" and "everything worked but Save did not stick", and that ambiguity
     * is what made a broken share look like a broken sync.
     */
    public boolean shareWith(String user, boolean view, boolean edit, boolean download) {
        // Already in the access list (an earlier run, or an inherited grant)? Then there
        // is nothing to add — go straight to the rights and Save. Re-adding is not just
        // wasteful: the search row for such a user sits next to their access row, and
        // that is how a share turned into a "Remove access?" prompt.
        if (hasGrantFor(user)) {
            System.out.println(">>> shareWith(" + user + "): already in the access list — "
                    + "setting rights only");
            return applyRightsAndSave(user, view, edit, download);
        }

        boolean searched = searchUser(user);
        boolean listed = searched && userListed(user);
        boolean added = listed && addUser(user);
        System.out.println(">>> shareWith(" + user + "): search=" + searched
                + " listed=" + listed + " added(+)=" + added);
        if (!added) {
            System.out.println(">>> on screen now: " + visibleTexts());
            return false;
        }

        return applyRightsAndSave(user, view, edit, download);
    }

    /** The second half of a share: set the rights for this item type, then Save. */
    private boolean applyRightsAndSave(String user, boolean view, boolean edit, boolean download) {
        if (isFolderMode()) {
            // A FOLDER has no per-flag switches — one role covers it. Edit rights mean
            // "editor"; anything less is "viewer".
            String role = edit ? "editor" : "viewer";
            boolean roleSet = setRole(user, role);
            boolean saved = save();
            System.out.println(">>> shareWith(" + user + "): FOLDER role=" + role
                    + " set=" + roleSet + " saved=" + saved + " access=" + grantedUsers());
            return saved;
        }

        boolean v = setRight(VIEW, view);
        boolean e = setRight(EDIT, edit);
        boolean d = setRight(DOWNLOAD, download);
        boolean saveBar = saveBarShown();
        boolean saved = save();
        System.out.println(">>> shareWith(" + user + "): FILE view=" + v + " edit=" + e
                + " download=" + d + " saveBar=" + saveBar + " saved=" + saved
                + " access=" + grantedUsers());
        return saved;
    }

    /** Every non-blank text on screen — a cheap "what am I actually looking at". */
    public List<String> visibleTexts() {
        try {
            return driver.findElements(AppiumBy.androidUIAutomator(
                            "new UiSelector().textMatches(\".+\")"))
                    .stream().map(WebElement::getText)
                    .filter(t -> t != null && !t.isBlank()).distinct().limit(40).toList();
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    // ---------------- Link tab ----------------

    /** Either control counts — this build exposes the email-link button, not a bare one. */
    public boolean hasGenerateLink() {
        return isVisible("btnGenerateLink", Duration.ofSeconds(5))
                || isVisible("btnGenerateEmailLink", Duration.ofSeconds(2));
    }
    public boolean noActiveLinks()   { return isVisible("tvLinkEmptyHint", Duration.ofSeconds(4)); }
    public boolean generateLink() {
        return click("btnGenerateLink") || click("btnGenerateEmailLink");
    }
    public String  linkUrl()         { return text("tvShareLinkUrl"); }
    public boolean copyLink()        { return click("btnCopyLink"); }
    public boolean linkShown(Duration timeout) { return isVisible("tvShareLinkUrl", timeout); }

    public boolean hasPermissionSpinner() { return isVisible("spPermission", Duration.ofSeconds(3)); }
    public boolean hasExpirySpinner()     { return isVisible("spExpires", Duration.ofSeconds(3)); }
    public boolean hasMaxViewsSpinner()   { return isVisible("spMaxViews", Duration.ofSeconds(3)); }
    public boolean hasRecipients()        { return isVisible("etRecipients", Duration.ofSeconds(3)); }
    public boolean hasMessage()           { return isVisible("etMessage", Duration.ofSeconds(3)); }

    public List<String> spinnerOptions(String id) {
        try {
            driver.findElement(byId(id)).click();
            sleep(1200);
            List<String> out = driver.findElements(AppiumBy.id("android:id/text1")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank()).toList();
            driver.navigate().back();
            sleep(600);
            return out;
        } catch (Exception e) { return List.of(); }
    }

    public boolean setRecipients(String emails) {
        try {
            WebElement el = driver.findElement(byId("etRecipients"));
            el.click(); el.clear(); el.sendKeys(emails);
            hideKeyboard();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean generateEmailLink() { return click("btnGenerateEmailLink"); }
    public String  errorText()         { return text("tvError"); }

    // ---------------- helpers ----------------

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
                    "new UiSelector().text(\"" + t + "\")")).click();
            sleep(1500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
