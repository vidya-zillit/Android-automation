package com.zillit.automation.pages.settings;

import com.zillit.automation.pages.common.BasePage;
import com.zillit.automation.pages.common.BottomNavPage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin Settings screens the web E2E report covers and no Android page object drove yet
 * (web gap file {@code gaps/settings.md}, cases S-M1 to S-M6).
 *
 * <ul>
 *   <li><b>User Viewing &amp; Posting Rights Grid</b> ({@code PermissionGridMainActivity},
 *       "Select Section"): rows {@code layHome} / {@code layTools} / {@code layDistribute}.
 *       Home or Tools opens {@code PermissionGridUserList} ("Select User", list
 *       {@code rvUsers} of {@code user_item.xml}: {@code nameTv}, {@code tvDesignation}).
 *       A user opens {@code PermissionUserDetails}: {@code tvName}, {@code chkStatus},
 *       {@code chkAdmin}, the section tabs {@code tvHome} / {@code tvTool}, the column
 *       headers {@code tvViewTitle} / {@code tvPostTitle} / {@code tvDownloadTitle},
 *       {@code et_search} and the unit list {@code rvUnits} whose rows
 *       ({@code item_user_permissions.xml}, root {@code admin_widget_parent}) carry
 *       {@code tvUnitName}, {@code chkView}, {@code chkPost}, {@code chkDownload}.</li>
 *   <li><b>User Management</b> rows ({@code permission_user_item.xml}): {@code nameTv},
 *       {@code changeProfile}, {@code icCheck} (Status), {@code chkAdmin} (Admin Access).</li>
 *   <li><b>Approve User Profile</b>: {@code ApproveNewUsersListActivity} titled "Select User".</li>
 * </ul>
 *
 * Nothing here changes a right by itself; the tests that toggle a switch restore it.
 */
public class AdminRightsPage extends BasePage {

    public static final String GRID_ROW = "User Viewing & Posting Rights Grid";
    public static final String APPROVE_PROFILE_ROW = "Approve User Profile";

    public AdminRightsPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- Admin Settings rows ----------------

    public boolean openSettingsTab() {
        return new BottomNavPage(driver).selectTab("Settings");
    }

    /** Taps an Admin Settings row by its label (scrolls it into view first). */
    public boolean tapAdminRow(String label) {
        String needle = label.replace("\"", "\\\"");
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().resourceIdMatches(\".*:id/nameTv\").text(\""
                            + needle + "\"))"));
        } catch (WebDriverException ignored) {
            // short list, or already visible
        }
        try {
            for (WebElement n : driver.findElements(byId("nameTv"))) {
                if (label.equals(safeText(n))) {
                    n.click();
                    sleep(1800);
                    return true;
                }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    public boolean adminSettingsOpen(Duration timeout) {
        return onAdminSettingsScreen(timeout);
    }

    /** The current screen's toolbar header text ("" when none). */
    public String header() {
        try {
            var h = driver.findElements(byId("headerText"));
            if (!h.isEmpty()) {
                String t = h.get(0).getText();
                return t == null ? "" : t.trim();
            }
        } catch (WebDriverException ignored) { }
        return "";
    }

    public boolean headerIs(String text, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (header().equalsIgnoreCase(text)) return true;
            if (System.currentTimeMillis() >= deadline) return false;
            sleep(300);
        }
    }

    public String currentActivity() {
        try {
            String a = driver.currentActivity();
            return a == null ? "" : a;
        } catch (WebDriverException e) {
            return "";
        }
    }

    public void back() {
        try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        sleep(1200);
    }

    // ---------------- Rights grid: Select Section ----------------

    public boolean gridMainOpen(Duration timeout) {
        return isVisible("layHome", timeout) && isVisible("layTools", Duration.ofSeconds(2));
    }

    public boolean adminListGone() {
        return driver.findElements(byId("groupTitleTv")).isEmpty()
                && driver.findElements(byId("adminSettingsRecyclerView")).isEmpty();
    }

    /** "Home" or "Tools" on the Select Section screen. */
    public boolean openSection(String section) {
        String id = section.equalsIgnoreCase("Tools") ? "layTools" : "layHome";
        try {
            driver.findElement(byId(id)).click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- Rights grid: Select User ----------------

    public boolean userListOpen(Duration timeout) {
        return isVisible("rvUsers", timeout);
    }

    /** Names on the grid's user list (the visible slice). */
    public List<String> gridUserNames() {
        List<String> out = new ArrayList<>();
        try {
            var rv = driver.findElements(byId("rvUsers"));
            if (rv.isEmpty()) return out;
            for (WebElement n : rv.get(0).findElements(byId("nameTv"))) {
                String t = safeText(n);
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    public boolean openGridUser(String name) {
        try {
            var rv = driver.findElements(byId("rvUsers"));
            if (rv.isEmpty()) return false;
            for (WebElement n : rv.get(0).findElements(byId("nameTv"))) {
                if (name.equals(safeText(n) == null ? null : safeText(n).trim())) {
                    n.click();
                    sleep(2500);
                    return true;
                }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    // ---------------- Rights grid: user details ----------------

    public boolean userDetailsOpen(Duration timeout) {
        return isVisible("tvName", timeout) && isVisible("rvUnits", Duration.ofSeconds(8));
    }

    public String detailsUserName() {
        return textOf("tvName");
    }

    public Boolean detailsAdminChecked() {
        return checkedOf(first("chkAdmin"));
    }

    public Boolean detailsStatusChecked() {
        return checkedOf(first("chkStatus"));
    }

    public boolean columnHeadersShown() {
        return isVisible("tvViewTitle", Duration.ofSeconds(3))
                && isVisible("tvPostTitle", Duration.ofSeconds(1))
                && isVisible("tvDownloadTitle", Duration.ofSeconds(1));
    }

    public boolean tapToolsTab() { return tapId("tvTool"); }

    public boolean tapHomeTab() { return tapId("tvHome"); }

    /** One unit row of the rights table. */
    public static final class RightsRow {
        public final String unit;
        public final Boolean view, post, download;              // checked
        public final Boolean viewOn, postOn, downloadOn;        // enabled
        public final boolean hasAllThree;

        RightsRow(String unit, Boolean view, Boolean post, Boolean download,
                  Boolean viewOn, Boolean postOn, Boolean downloadOn, boolean hasAllThree) {
            this.unit = unit; this.view = view; this.post = post; this.download = download;
            this.viewOn = viewOn; this.postOn = postOn; this.downloadOn = downloadOn;
            this.hasAllThree = hasAllThree;
        }

        @Override public String toString() {
            return unit + "{V=" + view + (Boolean.FALSE.equals(viewOn) ? "(x)" : "")
                    + " P=" + post + (Boolean.FALSE.equals(postOn) ? "(x)" : "")
                    + " D=" + download + (Boolean.FALSE.equals(downloadOn) ? "(x)" : "") + "}";
        }
    }

    /** The rows currently rendered in {@code rvUnits}. */
    public List<RightsRow> rightsRows() {
        List<RightsRow> out = new ArrayList<>();
        try {
            for (WebElement row : driver.findElements(byId("admin_widget_parent"))) {
                String unit = textIn(row, "tvUnitName");
                if (unit.isBlank()) continue;
                WebElement v = firstIn(row, "chkView");
                WebElement p = firstIn(row, "chkPost");
                WebElement d = firstIn(row, "chkDownload");
                out.add(new RightsRow(unit, checkedOf(v), checkedOf(p), checkedOf(d),
                        enabledOf(v), enabledOf(p), enabledOf(d), v != null && p != null && d != null));
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    public RightsRow rightsRow(String unit) {
        for (RightsRow r : rightsRows()) if (r.unit.equals(unit)) return r;
        return null;
    }

    /** Taps one switch ("chkView" / "chkPost" / "chkDownload") on the named unit's row. */
    public boolean tapRight(String unit, String switchId) {
        try {
            for (WebElement row : driver.findElements(byId("admin_widget_parent"))) {
                if (!unit.equals(textIn(row, "tvUnitName"))) continue;
                WebElement sw = firstIn(row, switchId);
                if (sw == null) return false;
                sw.click();
                sleep(800);
                return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /**
     * Waits until the named row's switch reads {@code wanted}. The success handler
     * re-fetches the whole list ({@code PermissionUserDetails}), so the row is re-read each poll.
     */
    public boolean waitRight(String unit, String switchId, boolean wanted, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            RightsRow r = rightsRow(unit);
            if (r != null) {
                Boolean v = switch (switchId) {
                    case "chkView" -> r.view;
                    case "chkPost" -> r.post;
                    default -> r.download;
                };
                if (v != null && v == wanted) return true;
            }
            if (System.currentTimeMillis() >= deadline) return false;
            sleep(700);
        }
    }

    // ---------------- User Management rows ----------------

    /** A control in the named user's User Management card, tied by geometry (same as UserManagementPage). */
    public WebElement userManagementControl(String name, String controlId) {
        try {
            WebElement label = null;
            for (WebElement n : driver.findElements(byId("nameTv"))) {
                String t = safeText(n);
                if (t != null && t.toLowerCase().contains(name.toLowerCase())) { label = n; break; }
            }
            if (label == null) return null;
            Rectangle lr = label.getRect();
            WebElement best = null;
            int bestDelta = Integer.MAX_VALUE;
            for (WebElement c : driver.findElements(byId(controlId))) {
                Rectangle cr = c.getRect();
                int delta = cr.getY() - lr.getY();
                if (delta < -lr.getHeight()) continue;
                int score = Math.abs(delta);
                if (score < bestDelta && delta < 600) { bestDelta = score; best = c; }
            }
            return best;
        } catch (WebDriverException e) {
            return null;
        }
    }

    /** User names on the User Management screen (visible slice). */
    public List<String> userManagementNames() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement n : driver.findElements(byId("nameTv"))) {
                String t = safeText(n);
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    public Boolean enabledState(WebElement e) { return enabledOf(e); }

    // ---------------- Approve User Profile ----------------

    public boolean approveListOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (currentActivity().contains("ApproveNewUsersList")
                    || header().equalsIgnoreCase("Select User")) return true;
            if (System.currentTimeMillis() >= deadline) return false;
            sleep(400);
        }
    }

    /** The approve list shows rows or its empty view. */
    public boolean approveListHasRowsOrEmptyView() {
        // The empty view is an <include android:id="@+id/noDataFound"> of empty_view.xml, so its root
        // is reported under the include's id; its picture is iconIv.
        return !driver.findElements(byId("nameTv")).isEmpty()
                || !driver.findElements(byId("noDataFound")).isEmpty()
                || !driver.findElements(byId("emptyMainLi")).isEmpty()
                || !driver.findElements(byId("iconIv")).isEmpty();
    }

    // ---------------- toasts ----------------

    /** A Toast (or snackbar) carrying the text, polled for up to {@code timeout}. */
    public boolean toastOrSnackShown(String text, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            try {
                for (WebElement t : driver.findElements(AppiumBy.xpath("//android.widget.Toast"))) {
                    String s = safeText(t);
                    if (s != null && s.contains(text)) return true;
                }
                if (!driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"" + text.replace("\"", "\\\"") + "\")")).isEmpty())
                    return true;
            } catch (WebDriverException ignored) { }
            if (System.currentTimeMillis() >= deadline) return false;
            sleep(250);
        }
    }

    // ---------------- internals ----------------

    private boolean tapId(String id) {
        try {
            driver.findElement(byId(id)).click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private WebElement first(String id) {
        try {
            var l = driver.findElements(byId(id));
            return l.isEmpty() ? null : l.get(0);
        } catch (WebDriverException e) {
            return null;
        }
    }

    private WebElement firstIn(WebElement parent, String id) {
        try {
            var l = parent.findElements(byId(id));
            return l.isEmpty() ? null : l.get(0);
        } catch (WebDriverException e) {
            return null;
        }
    }

    private String textIn(WebElement parent, String id) {
        WebElement e = firstIn(parent, id);
        String t = e == null ? null : safeText(e);
        return t == null ? "" : t.trim();
    }

    private String textOf(String id) {
        WebElement e = first(id);
        String t = e == null ? null : safeText(e);
        return t == null ? "" : t.trim();
    }

    private static Boolean checkedOf(WebElement e) {
        if (e == null) return null;
        try { return "true".equals(e.getAttribute("checked")); }
        catch (WebDriverException ex) { return null; }
    }

    private static Boolean enabledOf(WebElement e) {
        if (e == null) return null;
        try { return "true".equals(e.getAttribute("enabled")); }
        catch (WebDriverException ex) { return null; }
    }

    private static String safeText(WebElement e) {
        try { return e.getText(); }
        catch (WebDriverException ex) { return null; }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
