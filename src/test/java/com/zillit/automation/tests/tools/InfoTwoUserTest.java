package com.zillit.automation.tests.tools;

import com.zillit.automation.core.TestOrder;

import com.zillit.automation.core.Config;
import com.zillit.automation.core.DriverFactory;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import com.zillit.automation.pages.tools.InfoFeedPage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Info / Confidential Info with TWO accounts on two devices — the web-gap groups that need a
 * second member: live updates (G-LIVE, G-LIVE-INFO, G-LIVE-CI / CONF-10), another member's
 * message and comment (G-OTHER, INFO-16 / CONF-24), the Info tile badge (G-BADGE) and the
 * confidential text not leaking outside the tool (CONF-07).
 *
 * <p>Roles (memory: two-device roles): A = the emulator (Vidya Testing) watches, B = the Pixel
 * (Vidya Pixel) acts. Nothing changes either user's rights.
 *
 * <pre>
 * ZILLIT_FLAVOR=dev ZILLIT_UDID=emulator-5554 ZILLIT_RECEIVER_UDID=34281FDH2005XC \
 *   ./run-and-report.sh --only InfoTwoUserTest
 * </pre>
 * Coordinator: class order constant TOOL_INFO_TWO_USER = 210 (literal until added).
 */
@Order(TestOrder.INFO_TWO_USER)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(ReasonLogger.class)
class InfoTwoUserTest {

    private static final Duration DELIVER = Duration.ofSeconds(30);

    private AndroidDriver a;   // watcher (emulator)
    private AndroidDriver b;   // actor (Pixel)
    private String pkg;
    private boolean ready;

    @BeforeAll
    void setUp() {
        String bUdid = first(Config.get("zillit.receiver.udid"), System.getenv("ZILLIT_RECEIVER_UDID"));
        assumeTrue(bUdid != null && !bUdid.isBlank(),
                "Two-device cases need ZILLIT_RECEIVER_UDID (the second device) — skipping");
        String aUdid = first(Config.get("zillit.sender.udid"), System.getenv("ZILLIT_SENDER_UDID"),
                System.getenv("ZILLIT_UDID"), Config.get("zillit.udid"), "emulator-5554");
        // Own system ports, clear of single-device sessions and TwoUserChatTest (8260/8270).
        a = DriverFactory.create(aUdid, 8280);
        b = DriverFactory.create(bUdid, 8290);
        pkg = com.zillit.automation.core.Config.configuredAppPackage();
        if (pkg == null || pkg.isEmpty()) pkg = a.getCurrentPackage();
        ready = enterProject(a) && enterProject(b);
        System.out.println(">>> Info two-user setup: A=" + aUdid + " B=" + bUdid + " ready=" + ready);
    }

    @AfterAll
    void tearDown() {
        for (AndroidDriver d : new AndroidDriver[]{a, b}) {
            try { if (d != null) d.quit(); } catch (Exception ignored) { }
        }
    }

    // ------------------------------------------------------------------ live updates

    // [WEB info.spec.js:89 / :219] [WEB unit-feed.suite.js:1802]
    @Test
    @Order(1)
    @DisplayName("[G-LIVE-INFO] B's post, edit, comment and delete appear live on A's open Info")
    void liveUpdatesInfo() {
        liveUpdates("Info");
    }

    // [WEB CI sheet-bugs :135 CONF-10] [WEB unit-feed.suite.js:1802]
    @Test
    @Order(2)
    @DisplayName("[G-LIVE-CI] B's post, edit, comment and delete appear live on A's open Confidential Info")
    void liveUpdatesConfidentialInfo() {
        liveUpdates("Confidential Info");
    }

    private void liveUpdates(String module) {
        assumeTrue(ready, "Both devices are not inside the project");
        InfoFeedPage pa = open(a, module), pb = open(b, module);
        assumeTrue(pb.isInputBarVisible(), "B has no posting rights in " + module);
        String m = marker(module, "live");
        String edited = m + "-E", comment = m + "-c";
        try {
            pb.sendMessage(m);
            assumeTrue(pb.waitMessageSent(m, DELIVER), "B's message did not reach the server");
            assertTrue(waitText(pa, m, DELIVER), "A should see B's new post live in " + module);

            assertTrue(pb.longPressMessage(m) && pb.tapMenuOption("Edit") && pb.isEditDialogOpen(Duration.ofSeconds(8)),
                    "B could not open Edit on its own post");
            pb.setEditDialogText(edited);
            pb.saveEditDialog();
            assumeTrue(pb.waitMessageSent(edited, DELIVER), "B's edit did not reach the server");
            assertTrue(waitText(pa, edited, DELIVER), "A should see B's edit live in " + module);

            assertTrue(pb.longPressMessage(edited) && pb.tapMenuOption("Reply"), "B could not reply");
            pb.sendMessage(comment);
            assumeTrue(pb.waitMessageSent(comment, DELIVER), "B's comment did not reach the server");
            assertTrue(waitText(pa, comment, DELIVER), "A should see B's comment live in " + module);

            assertTrue(deleteOwn(pb, edited), "B could not delete its post");
            assertTrue(waitGone(pa, edited, DELIVER), "B's delete should remove the post from A's open " + module + " live");
        } finally {
            pb.cancelReplyPreview();
            if (pb.messageShown(edited, Duration.ofSeconds(1))) deleteOwn(pb, edited);
            else if (pb.messageShown(m, Duration.ofSeconds(1))) deleteOwn(pb, m);
        }
    }

    // ------------------------------------------------------------------ another member's rows

    // [WEB unit-feed.suite.js:1638] [WEB sheet-bugs INFO-16 / CONF-24]
    @Test
    @Order(3)
    @DisplayName("[G-OTHER] Info: editing another member's message is refused")
    void otherMembersMessageEditRefused() {
        assumeTrue(ready, "Both devices are not inside the project");
        InfoFeedPage pa = open(a, "Info"), pb = open(b, "Info");
        assumeTrue(pb.isInputBarVisible(), "B has no posting rights in Info");
        String m = marker("Info", "other");
        try {
            pb.sendMessage(m);
            assumeTrue(pb.waitMessageSent(m, DELIVER) && waitText(pa, m, DELIVER), "B's message did not reach A");
            assertTrue(pa.longPressMessage(m), "A could not long-press B's message");
            List<String> opts = pa.menuOptions();
            System.out.println(">>> A's menu on B's message: " + opts);
            if (opts.stream().noneMatch(o -> o.trim().equalsIgnoreCase("Edit"))) {
                pa.closeMenu();
                return;   // web behaviour: Edit not offered at all
            }
            assertTrue(pa.tapMenuOption("Edit"), "Could not tap Edit");
            boolean refused = pa.messageAppears("Permission To Edit", Duration.ofSeconds(4));
            boolean dialog = pa.visible("editTextInput", Duration.ofSeconds(1));
            if (dialog) pa.cancelEditDialog();
            assertTrue(refused && !dialog, "A must not be able to edit B's message ('"
                    + InfoFeedPage.NO_EDIT_PERMISSION + "' and no dialog)");
        } finally {
            deleteOwn(pb, m);
        }
    }

    @Test
    @Order(4)
    @Tag("known-bug")
    @DisplayName("[G-OTHER] INFO-16: another member's comment offers no Edit (Info)")
    void otherMembersCommentNoEditInfo() {
        otherMembersCommentNoEdit("Info", "INFO-16");
    }

    @Test
    @Order(5)
    @Tag("known-bug")
    @DisplayName("[G-OTHER] CONF-24: another member's comment offers no Edit (Confidential Info)")
    void otherMembersCommentNoEditCi() {
        otherMembersCommentNoEdit("Confidential Info", "CONF-24");
    }

    private void otherMembersCommentNoEdit(String module, String bug) {
        assumeTrue(ready, "Both devices are not inside the project");
        InfoFeedPage pa = open(a, module), pb = open(b, module);
        assumeTrue(pb.isInputBarVisible(), "B has no posting rights in " + module);
        String m = marker(module, "ocm"), c = m + "-c";
        List<String> opts;
        boolean editRefused = false;
        try {
            pb.sendMessage(m);
            assumeTrue(pb.waitMessageSent(m, DELIVER), "B's post did not reach the server");
            assumeTrue(pb.longPressMessage(m) && pb.tapMenuOption("Reply"), "B could not reply");
            pb.sendMessage(c);
            assumeTrue(pb.waitMessageSent(c, DELIVER) && waitText(pa, c, DELIVER), "B's comment did not reach A");
            pa.hideKeyboardSafely();
            assumeTrue(pa.openCommentMenu(c), "A could not open the menu on B's comment (no chevron)");
            opts = pa.menuOptions();
            System.out.println(">>> A's menu on B's comment in " + module + ": " + opts);
            if (opts.stream().anyMatch(o -> o.trim().equalsIgnoreCase("Edit"))) {
                pa.tapMenuOption("Edit");
                editRefused = pa.messageAppears("edit other users", Duration.ofSeconds(4));
                if (pa.visible("editTextInput", Duration.ofSeconds(1))) pa.cancelEditDialog();
            } else {
                pa.closeMenu();
            }
        } finally {
            pb.cancelReplyPreview();
            deleteOwn(pb, m);
        }
        System.out.println(">>> " + bug + ": Edit offered then refused=" + editRefused);
        assertTrue(opts.stream().noneMatch(o -> o.trim().equalsIgnoreCase("Edit")),
                bug + ": another member's comment should not offer Edit (it is offered"
                        + (editRefused ? " and then refused" : "") + "). Options: " + opts);
    }

    // ------------------------------------------------------------------ badge / leak

    // [WEB info.spec.js:88 / :165]
    @Test
    @Order(6)
    @DisplayName("[G-BADGE] B's Info post badges A's Info tile; opening Info clears it")
    void infoTileBadge() {
        assumeTrue(ready, "Both devices are not inside the project");
        InfoFeedPage pb = open(b, "Info");
        assumeTrue(pb.isInputBarVisible(), "B has no posting rights in Info");
        open(a, "Info");                 // read everything first
        toTools(a, "Info");
        String m = marker("Info", "badge");
        try {
            pb.sendMessage(m);
            assumeTrue(pb.waitMessageSent(m, DELIVER), "B's post did not reach the server");
            int badge = -1;
            long end = System.currentTimeMillis() + DELIVER.toMillis();
            while (System.currentTimeMillis() < end && badge <= 0) { badge = tileBadge(a, "Info"); sleep(1000); }
            System.out.println(">>> A's Info tile badge after B's post: " + badge);
            assertTrue(badge > 0, "A's Info tile should show a badge after B posts");
            InfoFeedPage pa = open(a, "Info");
            assertTrue(waitText(pa, m, DELIVER), "A should see the post");
            toTools(a, "Info");
            sleep(2000);
            int after = tileBadge(a, "Info");
            System.out.println(">>> A's Info tile badge after opening Info: " + after);
            assertTrue(after <= 0, "Opening Info should clear its tile badge, still " + after);
        } finally {
            InfoFeedPage again = open(b, "Info");
            deleteOwn(again, m);
        }
    }

    // [WEB CI sheet-bugs CONF-07]
    @Test
    @Order(7)
    @DisplayName("[G-LIVE-CI] CONF-07: a Confidential Info post's text never shows outside the tool")
    void confidentialTextNotLeaked() {
        assumeTrue(ready, "Both devices are not inside the project");
        InfoFeedPage pb = open(b, "Confidential Info");
        assumeTrue(pb.isInputBarVisible(), "B has no posting rights in Confidential Info");
        toTools(a, null);
        String m = marker("Confidential Info", "leak");
        boolean leaked = false;
        try {
            pb.sendMessage(m);
            assumeTrue(pb.waitMessageSent(m, DELIVER), "B's post did not reach the server");
            long end = System.currentTimeMillis() + 15000;
            while (System.currentTimeMillis() < end && !leaked) {
                leaked = !a.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"" + m + "\")")).isEmpty();
                sleep(500);
            }
        } finally {
            deleteOwn(pb, m);
        }
        assertTrue(!leaked, "CONF-07: the confidential post's text appeared on A outside Confidential Info");
    }

    // ------------------------------------------------------------------ helpers

    private InfoFeedPage open(AndroidDriver d, String module) {
        InfoFeedPage p = new InfoFeedPage(d);
        if (!d.findElements(byId("rvChat")).isEmpty() && d.findElements(byId("bottomNavigation")).isEmpty()
                && module.equalsIgnoreCase(header(d))) {
            return p;
        }
        for (int i = 0; i < 4 && d.findElements(byId("bottomNavigation")).isEmpty(); i++) {
            try { d.navigate().back(); } catch (WebDriverException ignored) { }
            sleep(700);
        }
        ToolsPage tools = new ToolsPage(d);
        assumeTrue(tools.openToolsTab() && tools.isLoaded(Duration.ofSeconds(15)), "Tools did not load");
        tools.searchTools(module);
        assumeTrue(tools.isModuleListed(module, Duration.ofSeconds(6)), module + " not listed");
        assumeTrue(tools.openModule(module) && tools.isModuleChatOpen(Duration.ofSeconds(15)), module + " did not open");
        sleep(1500);
        return p;
    }

    private String header(AndroidDriver d) {
        try { return d.findElement(byId("headerText")).getText().trim(); } catch (WebDriverException e) { return ""; }
    }

    /** Back to the Tools tab, optionally filtered to one module. */
    private void toTools(AndroidDriver d, String filter) {
        for (int i = 0; i < 4 && d.findElements(byId("bottomNavigation")).isEmpty(); i++) {
            try { d.navigate().back(); } catch (WebDriverException ignored) { }
            sleep(700);
        }
        ToolsPage tools = new ToolsPage(d);
        tools.openToolsTab();
        tools.isLoaded(Duration.ofSeconds(10));
        if (filter != null) tools.searchTools(filter); else tools.clearSearch();
        sleep(1000);
    }

    /** The badge on the Tools row titled exactly {@code name}; 0 when hidden, -1 when no row. */
    private int tileBadge(AndroidDriver d, String name) {
        String x = "//*[@resource-id='" + pkg + ":id/commonListItemHolder'][.//*[@resource-id='" + pkg
                + ":id/nameTv' and @text='" + name + "']]//*[@resource-id='" + pkg + ":id/badge']";
        try {
            List<WebElement> rows = d.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg
                    + ":id/nameTv' and @text='" + name + "']"));
            if (rows.isEmpty()) return -1;
            List<WebElement> badge = d.findElements(AppiumBy.xpath(x));
            if (badge.isEmpty() || !badge.get(0).isDisplayed()) return 0;
            return Integer.parseInt(badge.get(0).getText().replaceAll("\\D", ""));
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private boolean deleteOwn(InfoFeedPage p, String text) {
        try {
            p.hideKeyboardSafely();
            if (!p.messageShown(text, Duration.ofSeconds(3))) return false;
            if (!p.longPressMessage(text) || !p.tapMenuOption("Delete")) return false;
            p.tapDeleteActionBar(Duration.ofSeconds(6));
            boolean yes = p.confirmPopup().accept(Duration.ofSeconds(6));
            sleep(1500);
            return yes && !p.messageShown(text, Duration.ofSeconds(2));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean waitText(InfoFeedPage p, String text, Duration t) {
        long end = System.currentTimeMillis() + t.toMillis();
        while (System.currentTimeMillis() < end) {
            if (p.messageShown(text, Duration.ofMillis(700))) return true;
        }
        return false;
    }

    private boolean waitGone(InfoFeedPage p, String text, Duration t) {
        long end = System.currentTimeMillis() + t.toMillis();
        while (System.currentTimeMillis() < end) {
            if (!p.messageShown(text, Duration.ofMillis(700))) return true;
            sleep(500);
        }
        return false;
    }

    private boolean enterProject(AndroidDriver d) {
        try {
            long end = System.currentTimeMillis() + 90_000;
            while (System.currentTimeMillis() < end) {
                if (!d.findElements(byId("bottomNavigation")).isEmpty()
                        || !d.findElements(byId("projectListRecyclerView")).isEmpty()) break;
                try { d.navigate().back(); } catch (WebDriverException ignored) { }
                sleep(1500);
                if (!pkg.equals(d.getCurrentPackage())) d.executeScript("mobile: activateApp", Map.of("appId", pkg));
            }
            if (!d.findElements(byId("projectListRecyclerView")).isEmpty()) {
                ProjectListPage list = new ProjectListPage(d);
                List<String> names = list.visibleProjectNames();
                if (names.isEmpty() || !list.tapProject(names.get(0))) return false;
            }
            return new BottomNavPage(d).waitUntilLoaded(Duration.ofSeconds(60));
        } catch (WebDriverException e) {
            return false;
        }
    }

    private org.openqa.selenium.By byId(String id) {
        return AppiumBy.id(pkg + ":id/" + id);
    }

    private static String marker(String module, String kind) {
        return "ZL_AUTO-2u-" + module.toLowerCase().replaceAll("[^a-z]+", "") + "-" + kind + "-"
                + (System.currentTimeMillis() % 1000000);
    }

    private static String first(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
