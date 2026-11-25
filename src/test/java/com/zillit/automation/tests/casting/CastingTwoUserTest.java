package com.zillit.automation.tests.casting;

import com.zillit.automation.core.Config;
import com.zillit.automation.core.DriverFactory;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.casting.CastingPage;
import com.zillit.automation.pages.casting.CastingPage.Tool;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import com.zillit.automation.pages.homechat.HomeChatPage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Casting with two accounts on two devices. A = the emulator (Vidya Testing) creates, moves,
 * chats and deletes; B = the Pixel (Vidya Pixel, a project admin) watches. Nothing changes
 * either user's rights; B never writes anything.
 *
 * <pre>
 * ZILLIT_FLAVOR=dev ZILLIT_UDID=emulator-5558 ZILLIT_RECEIVER_UDID=34281FDH2005XC \
 *   ./run-and-report.sh --only CastingTwoUserTest
 * </pre>
 */
@Order(TestOrder.CASTING_TWO_USER)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(ReasonLogger.class)
class CastingTwoUserTest {

    private static final Duration SYNC = Duration.ofSeconds(60);

    private AndroidDriver a;   // creator (emulator)
    private AndroidDriver b;   // watcher (Pixel)
    private String pkg;
    private boolean ready;

    /** What A created per tool, and the tabs it sits in. */
    private static final class Fx {
        final String n, character, talent, episode, link;
        final Set<String> tabs = new LinkedHashSet<>();
        Fx(Tool t) {
            n = "T" + (t == Tool.MAIN ? "M" : "B") + (System.currentTimeMillis() % 100000);
            character = "ZL_AUTO_Char " + n;
            talent = "ZL_AUTO_Talent " + n;
            episode = String.valueOf(900 + (int) (System.currentTimeMillis() % 90) + (t == Tool.MAIN ? 0 : 1));
            link = "https://example.com/zl-auto-cast-" + n.toLowerCase();
        }
    }

    private final Map<Tool, Fx> fx = new EnumMap<>(Tool.class);

    @BeforeAll
    void setUp() {
        String bUdid = first(Config.get("zillit.receiver.udid"), System.getenv("ZILLIT_RECEIVER_UDID"));
        assumeTrue(bUdid != null && !bUdid.isBlank(),
                "Two-device cases need ZILLIT_RECEIVER_UDID (the second device) — skipping");
        String aUdid = first(Config.get("zillit.sender.udid"), System.getenv("ZILLIT_SENDER_UDID"),
                System.getenv("ZILLIT_UDID"), Config.get("zillit.udid"), "emulator-5554");
        // Own system ports, clear of single-device sessions and the other two-user suites.
        a = DriverFactory.create(aUdid, 8300);
        b = DriverFactory.create(bUdid, 8310);
        pkg = Config.configuredAppPackage();
        if (pkg == null || pkg.isEmpty()) pkg = a.getCurrentPackage();
        ready = enterProject(a) && enterProject(b);
        System.out.println(">>> Casting two-user setup: A=" + aUdid + " B=" + bUdid + " ready=" + ready);
    }

    @AfterAll
    void tearDown() {
        try {
            for (Map.Entry<Tool, Fx> e : fx.entrySet()) {
                for (String tab : List.of(CastingPage.TAB_FINAL, CastingPage.TAB_SHORTLIST, CastingPage.TAB_SELECTS)) {
                    try { deleteFolderIfListed(e.getKey(), tab, e.getValue().character); }
                    catch (Throwable t) { System.out.println(">>> sweep " + e.getKey() + "/" + tab + ": " + t); }
                }
            }
        } finally {
            for (AndroidDriver d : new AndroidDriver[]{a, b}) {
                try { if (d != null) { toBottomNav(d); d.quit(); } } catch (Exception ignored) { }
            }
        }
    }

    // ───────────────────────── cases ─────────────────────────

    @Test
    @Order(1)
    @DisplayName("[CAST-2U-01] Main: a cast A creates in Selects shows on B's Selects")
    void createdCastReachesOtherMember() {
        assumeTrue(ready, "Both devices are not inside the project");
        Fx f = create(Tool.MAIN);
        assertTrue(waitFolderOn(b, Tool.MAIN, CastingPage.TAB_SELECTS, f.character, SYNC),
                "B should see A's new cast '" + f.character + "' in Selects within " + SYNC.toSeconds() + "s");
    }

    @Test
    @Order(2)
    @DisplayName("[CAST-2U-02] Main: A's Selects unit chat message reaches B, with an unread badge on B's home")
    void unitChatReachesOtherMember() {
        assumeTrue(ready, "Both devices are not inside the project");
        CastingPage pb = openTool(b, Tool.MAIN, true);
        pb.selectTab(CastingPage.TAB_SELECTS);
        for (int i = 0; i < 10 && !pb.unitChatIconShown(); i++) sleep(1000);
        CastingPage pa = openTool(a, Tool.MAIN, true);
        for (int i = 0; i < 10 && !pa.unitChatIconShown(); i++) sleep(1000);   // unit info loads after the list
        assumeTrue(pa.unitChatIconShown(), "A has no Selects unit chat");
        assertTrue(pa.openUnitChat(), "A's Selects unit chat should open");
        HomeChatPage ca = new HomeChatPage(a);
        String msg = "ZL_AUTO-cast-2u-" + (System.currentTimeMillis() % 1000000);
        ca.sendMessage(msg);
        assertTrue(ca.waitMessageSent(msg, Duration.ofSeconds(30)), "A's message should reach the server");
        // B is on the Selects home, not in the chat: the chat icon should carry a badge.
        boolean badge = false;
        long end = System.currentTimeMillis() + 30000;
        while (System.currentTimeMillis() < end && !badge) {
            badge = !b.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/castingBadge']"
                    + "//*[@resource-id='" + pkg + ":id/zBadge']")).isEmpty()
                    || !b.findElements(AppiumBy.id(pkg + ":id/zBadge")).isEmpty();
            if (!badge) sleep(2000);
        }
        assertTrue(pb.openUnitChat(), "B's Selects unit chat should open");
        HomeChatPage cb = new HomeChatPage(b);
        assertTrue(cb.messageShown(msg, Duration.ofSeconds(30)), "B should see A's unit chat message");
        try { b.navigate().back(); } catch (WebDriverException ignored) { }
        deleteOwn(ca, msg);
        assertTrue(badge, "B's Selects unit chat icon should show an unread badge for A's message");
    }

    @Test
    @Order(3)
    @DisplayName("[CAST-2U-03] Main: A's move to Shortlist shows on B's Shortlist")
    void moveReachesOtherMember() {
        assumeTrue(ready, "Both devices are not inside the project");
        Fx f = fx.get(Tool.MAIN);
        assumeTrue(f != null && !f.tabs.isEmpty(), "A's cast was not created");
        CastingPage pa = characterHome(a, Tool.MAIN, CastingPage.TAB_SELECTS, f.character);
        assertTrue(pa.folderMenu(f.character, CastingPage.M_MOVE_SHORTLIST), "A: Move To Shortlist should be in the folder menu");
        assertTrue(pa.formShown(Duration.ofSeconds(10)), "A: the move form should open");
        pa.submitForm();
        assertTrue(pa.waitFormGone(Duration.ofSeconds(20)), "A: the move should submit; snackbar '" + pa.snackbar() + "'");
        f.tabs.add(CastingPage.TAB_SHORTLIST);
        assertTrue(waitFolderOn(b, Tool.MAIN, CastingPage.TAB_SHORTLIST, f.character, SYNC),
                "B should see the moved cast in Shortlist");
    }

    @Test
    @Order(4)
    @DisplayName("[CAST-2U-04] Main: A's delete removes the cast from B's tabs")
    void deleteReachesOtherMember() {
        assumeTrue(ready, "Both devices are not inside the project");
        Fx f = fx.get(Tool.MAIN);
        assumeTrue(f != null && !f.tabs.isEmpty(), "A's cast was not created");
        for (String tab : List.of(CastingPage.TAB_SHORTLIST, CastingPage.TAB_SELECTS)) {
            assertTrue(deleteFolderIfListed(Tool.MAIN, tab, f.character), "A could not delete the cast in " + tab);
        }
        f.tabs.clear();
        for (String tab : List.of(CastingPage.TAB_SELECTS, CastingPage.TAB_SHORTLIST)) {
            assertFalse(waitFolderOn(b, Tool.MAIN, tab, f.character, Duration.ofSeconds(8)),
                    "B should no longer see the deleted cast in " + tab);
        }
    }

    @Test
    @Order(5)
    @DisplayName("[CAST-2U-05] Background: A's cast shows on B's Background Cast and not on B's Main Cast")
    void backgroundCastReachesOtherMemberOnly() {
        assumeTrue(ready, "Both devices are not inside the project");
        Fx f = create(Tool.BACKGROUND);
        try {
            assertTrue(waitFolderOn(b, Tool.BACKGROUND, CastingPage.TAB_SELECTS, f.character, SYNC),
                    "B should see A's Background cast in Background Cast → Selects");
            assertFalse(waitFolderOn(b, Tool.MAIN, CastingPage.TAB_SELECTS, f.character, Duration.ofSeconds(6)),
                    "B's Main Cast must not list a Background cast");
        } finally {
            if (deleteFolderIfListed(Tool.BACKGROUND, CastingPage.TAB_SELECTS, f.character)) f.tabs.clear();
        }
    }

    // ───────────────────────── flows ─────────────────────────

    /** A: Selects → + → link → fields → Submit; asserts A lists it. */
    private Fx create(Tool tool) {
        Fx f = new Fx(tool);
        CastingPage p = openTool(a, tool, true);
        assertTrue(p.openAddForm(), "A: '+' should open the add form");
        boolean submitted = false;
        try {
            assumeTrue(p.addLinkAttachment(f.link), "A: could not attach a link");
            p.fill(f.episode, f.character, f.talent, "Male", null, "ZL_AUTO two-user " + f.n);
            assertTrue("Male".equals(String.valueOf(p.gender()).trim()), "A: gender could not be set to Male");
            p.submitForm();
            submitted = p.waitFormGone(Duration.ofSeconds(16));
            assertTrue(submitted, "A: the cast should save; snackbar '" + p.snackbar() + "'");
        } finally {
            if (!submitted) p.backFromForm();
        }
        fx.put(tool, f);
        f.tabs.add(CastingPage.TAB_SELECTS);
        assertTrue(waitFolderOn(a, tool, CastingPage.TAB_SELECTS, f.character, SYNC), "A should list its new cast");
        return f;
    }

    private CastingPage characterHome(AndroidDriver d, Tool tool, String tab, String character) {
        CastingPage p = openTool(d, tool, true);
        p.selectTab(tab);
        assertTrue(p.groupBy(CastingPage.GROUP_CHARACTER, null), "Group By Character should apply");
        assertTrue(p.waitFolder(character, SYNC), tab + " should list '" + character + "': " + p.folders());
        return p;
    }

    /** Re-opens the tool on the device (a refetch) until the folder shows, or the wait ends. */
    private boolean waitFolderOn(AndroidDriver d, Tool tool, String tab, String character, Duration wait) {
        long end = System.currentTimeMillis() + wait.toMillis();
        do {
            CastingPage p = openTool(d, tool, true);
            p.selectTab(tab);
            p.groupBy(CastingPage.GROUP_CHARACTER, null);
            if (p.waitFolder(character, Duration.ofSeconds(10))) return true;
        } while (System.currentTimeMillis() < end);
        return false;
    }

    private boolean deleteFolderIfListed(Tool tool, String tab, String character) {
        CastingPage p = openTool(a, tool, true);
        p.selectTab(tab);
        p.groupBy(CastingPage.GROUP_CHARACTER, null);
        if (!p.waitFolder(character, Duration.ofSeconds(12))) return true;
        if (!p.folderMenu(character, CastingPage.M_DELETE)) return false;
        if (!p.dialogShown(Duration.ofSeconds(5))) return false;
        p.dialogRight();
        return p.waitFolderGone(character, Duration.ofSeconds(20));
    }

    private CastingPage openTool(AndroidDriver d, Tool tool, boolean fresh) {
        CastingPage p = new CastingPage(d);
        if (!fresh && p.onHomeOf(tool, Duration.ofSeconds(1))) return p;
        toBottomNav(d);
        ToolsPage tools = new ToolsPage(d);
        assumeTrue(tools.openToolsTab() && tools.isLoaded(Duration.ofSeconds(15)), "Tools did not load");
        tools.searchTools("Casting");
        sleep(1500);
        assumeTrue(p.tapToolRow(tool), "'" + tool.tile + "' is not listed in Tools");
        assumeTrue(p.onHomeOf(tool, Duration.ofSeconds(25)), tool.tile + " did not open");
        sleep(1500);
        return p;
    }

    private void toBottomNav(AndroidDriver d) {
        for (int i = 0; i < 7 && d.findElements(AppiumBy.id(pkg + ":id/bottomNavigation")).isEmpty(); i++) {
            try { d.navigate().back(); } catch (WebDriverException ignored) { }
            sleep(800);
        }
    }

    private void deleteOwn(HomeChatPage p, String text) {
        try {
            if (!p.longPressMessage(text) || !p.tapMenuOption("Delete")) return;
            p.tapDeleteActionBar(Duration.ofSeconds(6));
            p.confirmPopup().accept(Duration.ofSeconds(6));
            sleep(1500);
        } catch (RuntimeException ignored) { }
    }

    private boolean enterProject(AndroidDriver d) {
        try {
            long end = System.currentTimeMillis() + 90_000;
            while (System.currentTimeMillis() < end) {
                if (!d.findElements(AppiumBy.id(pkg + ":id/bottomNavigation")).isEmpty()
                        || !d.findElements(AppiumBy.id(pkg + ":id/projectListRecyclerView")).isEmpty()) break;
                try { d.navigate().back(); } catch (WebDriverException ignored) { }
                sleep(1500);
                if (!pkg.equals(d.getCurrentPackage())) d.executeScript("mobile: activateApp", Map.of("appId", pkg));
            }
            if (!d.findElements(AppiumBy.id(pkg + ":id/projectListRecyclerView")).isEmpty()) {
                ProjectListPage list = new ProjectListPage(d);
                List<String> names = list.visibleProjectNames();
                if (names.isEmpty() || !list.tapProject(names.get(0))) return false;
            }
            return new BottomNavPage(d).waitUntilLoaded(Duration.ofSeconds(60));
        } catch (WebDriverException e) {
            return false;
        }
    }

    private static String first(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
