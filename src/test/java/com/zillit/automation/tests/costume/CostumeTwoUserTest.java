package com.zillit.automation.tests.costume;

import com.zillit.automation.core.Config;
import com.zillit.automation.core.DriverFactory;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import com.zillit.automation.pages.costume.CostumeFormPage;
import com.zillit.automation.pages.costume.CostumePage;
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
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Costume (Main Cast) across two members. A = the emulator (Vidya Testing) creates and
 * deletes; B = the Pixel (Vidya Pixel, project admin) watches and posts in the unit chat.
 * Nothing is typed into the Pixel's clipboard or gallery; B only reads, chats and deletes
 * its own chat message.
 *
 * <pre>
 * ZILLIT_FLAVOR=dev ZILLIT_UDID=emulator-5554 ZILLIT_RECEIVER_UDID=34281FDH2005XC \
 *   ./run-and-report.sh --only CostumeTwoUserTest
 * </pre>
 */
@Order(TestOrder.COSTUME_TWO_USER)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(ReasonLogger.class)
class CostumeTwoUserTest {

    private static final String TOOL = "Costume (Main Cast)";
    private static final Duration LIVE = Duration.ofSeconds(45);
    private static final String LINK = "https://example.com/zl-auto-costume-2u";

    private final String run = String.valueOf(1000 + (System.currentTimeMillis() / 1000) % 9000);
    private final String ep = "7" + run.substring(1);
    private final String character = CostumeBaseTest.PREFIX + " " + run + "two";
    private final String desc = CostumeBaseTest.PREFIX + " 2u " + run;

    private AndroidDriver a;   // creator (emulator)
    private AndroidDriver b;   // watcher (Pixel)
    private String pkg;
    private boolean ready;
    private boolean created;

    @BeforeAll
    void setUp() {
        String bUdid = first(Config.get("zillit.receiver.udid"), System.getenv("ZILLIT_RECEIVER_UDID"));
        assumeTrue(bUdid != null && !bUdid.isBlank(), "Two-device cases need ZILLIT_RECEIVER_UDID (the second device) — skipping");
        String aUdid = first(Config.get("zillit.sender.udid"), System.getenv("ZILLIT_SENDER_UDID"),
                System.getenv("ZILLIT_UDID"), Config.get("zillit.udid"), "emulator-5554");
        // Own system ports, clear of the other two-device suites (8260–8290).
        a = DriverFactory.create(aUdid, 8300);
        b = DriverFactory.create(bUdid, 8310);
        pkg = Config.configuredAppPackage();
        if (pkg == null || pkg.isEmpty()) pkg = a.getCurrentPackage();
        ready = enterProject(a) && enterProject(b);
        System.out.println(">>> Costume two-user setup: A=" + aUdid + " B=" + bUdid + " ready=" + ready + " episode=" + ep);
    }

    @AfterAll
    void tearDown() {
        try {
            if (created && a != null) deleteCharacterFolder(a);
        } catch (Exception ignored) { }
        for (AndroidDriver d : new AndroidDriver[]{a, b}) {
            try { if (d != null) { toBottomNav(d); d.quit(); } } catch (Exception ignored) { }
        }
    }

    // ───────────────────────── cases ─────────────────────────

    @Test
    @Order(1)
    @DisplayName("COS-2U-01 A record A creates in Selects appears on B's open Costume tool without reopening it")
    void createdRecordAppearsLive() {
        assumeTrue(ready, "Both devices are not inside the project");
        CostumePage pb = openTool(b);
        pb.selectTab(CostumePage.TAB_SELECTS);
        assumeTrue(!pb.hasFolder(CostumeBaseTest.episodeRow(ep)), "The run episode already exists");

        CostumePage pa = openTool(a);
        pa.selectTab(CostumePage.TAB_SELECTS);
        assumeTrue(pa.tapAdd(), "A: '+' not tappable");
        CostumeFormPage f = new CostumeFormPage(a);
        assumeTrue(f.isOpen(Duration.ofSeconds(10)), "A: add form did not open");
        assumeTrue(f.addLink(LINK), "A: link could not be attached");
        assumeTrue(f.openCharacterPicker() && f.createTempCharacter(character, CostumeBaseTest.PREFIX + " Talent " + run, "Female"),
                "A: temporary character could not be set");
        f.setField(CostumeFormPage.FIELD_EPISODE, ep);
        f.setField(CostumeFormPage.FIELD_SCENE, "911");
        f.setField(CostumeFormPage.FIELD_DESC, desc);
        f.submit();
        created = pa.isLoaded(Duration.ofSeconds(30));
        assumeTrue(created, "A: the record was not created");
        assumeTrue(pa.waitFolder(CostumeBaseTest.episodeRow(ep), Duration.ofSeconds(30)), "A: own list does not show the new episode");

        // B never leaves the page: the socket (wardrobeObserver → getList) has to bring it.
        assertTrue(waitFolderNoSearch(pb, CostumeBaseTest.episodeRow(ep), LIVE),
                "B's open Selects should list '" + CostumeBaseTest.episodeRow(ep) + "' live — saw " + pb.folderNames());
    }

    @Test
    @Order(2)
    @DisplayName("COS-2U-02 On B the record shows 'Uploaded by: Vidya Testing'; B (admin, not uploader) gets the admin menu")
    void otherMembersRecordOnB() {
        assumeTrue(created, "No record (2U-01)");
        CostumePage pb = openRecords(b);
        assumeTrue(pb != null && pb.waitRecord(desc, Duration.ofSeconds(15)), "B cannot reach the record");
        String txt = pb.recordText(pb.record(desc));
        System.out.println(">>> 2U-02 B sees: " + txt);
        assertTrue(txt.contains("Uploaded by: Vidya Testing"), "B should see the uploader: " + txt);
        List<String> items = pb.openRecordMenu(desc);
        pb.dismissPopup();
        assertEquals(List.of("View Details", "Edit", "Forward", "Download", "Image Reply", "Move To Shortlist",
                "Move To Final", "Read By", "Delete"), items,
                "An admin gets the full menu on another member's Selects record (WardrobeListingPage: uploaded_by == me || isAdmin)");
    }

    @Test
    @Order(3)
    @DisplayName("COS-2U-03 B's message in the Selects unit chat appears live in A's open unit chat")
    void unitChatLive() {
        assumeTrue(ready, "Both devices are not inside the project");
        CostumePage pa = openTool(a);
        pa.selectTab(CostumePage.TAB_SELECTS);
        assumeTrue(pa.unitChatShown() && pa.openUnitChat(), "A: unit chat did not open");
        CostumePage pb = openTool(b);
        pb.selectTab(CostumePage.TAB_SELECTS);
        assumeTrue(pb.unitChatShown() && pb.openUnitChat(), "B: unit chat did not open");
        CostumeChat ca = new CostumeChat(a), cb = new CostumeChat(b);
        assumeTrue(cb.inputShown(), "B has no posting rights in the unit chat");
        String msg = CostumeBaseTest.PREFIX + " 2u chat " + run;
        boolean sent = false;
        try {
            cb.send(msg);
            sent = cb.waitMessage(msg, Duration.ofSeconds(20));
            assumeTrue(sent, "B's message did not reach the server");
            long end = System.currentTimeMillis() + LIVE.toMillis();
            boolean seen = false;
            while (!seen && System.currentTimeMillis() < end) seen = ca.messageShown(msg);
            assertTrue(seen, "A's open unit chat should show B's message live");
        } finally {
            if (sent) cb.deleteOwn(msg);
            back(a); back(b);
        }
    }

    @Test
    @Order(4)
    @DisplayName("COS-2U-04 A deletes the character folder; B's open Selects drops the episode live")
    void deleteSyncsToB() {
        assumeTrue(created, "No record (2U-01)");
        CostumePage pb = openTool(b);
        pb.selectTab(CostumePage.TAB_SELECTS);
        assumeTrue(pb.waitFolder(CostumeBaseTest.episodeRow(ep), Duration.ofSeconds(20)), "B does not list the episode");
        assertTrue(deleteCharacterFolder(a), "A should be able to delete its character folder (admin)");
        created = false;
        long end = System.currentTimeMillis() + LIVE.toMillis();
        boolean gone = false;
        while (!gone && System.currentTimeMillis() < end) {
            gone = !pb.folderNames().contains(CostumeBaseTest.episodeRow(ep));
            if (!gone) CostumeBaseTest.sleep(2000);
        }
        assertTrue(gone, "B's open Selects should drop '" + CostumeBaseTest.episodeRow(ep) + "' live after A's delete");
    }

    // ───────────────────────── helpers ─────────────────────────

    private boolean waitFolderNoSearch(CostumePage p, String label, Duration t) {
        long end = System.currentTimeMillis() + t.toMillis();
        while (System.currentTimeMillis() < end) {
            if (p.folderNames().stream().anyMatch(n -> n.equalsIgnoreCase(label))) return true;
            if (p.hasFolder(label)) return true;
            CostumeBaseTest.sleep(2000);
        }
        return false;
    }

    private CostumePage openRecords(AndroidDriver d) {
        CostumePage p = openTool(d);
        p.selectTab(CostumePage.TAB_SELECTS);
        if (!p.waitFolder(CostumeBaseTest.episodeRow(ep), Duration.ofSeconds(20)) || !p.openFolder(CostumeBaseTest.episodeRow(ep))) return null;
        String row = CostumeBaseTest.characterRow(character);
        if (!p.waitFolder(row, Duration.ofSeconds(10)) || !p.openFolder(row)) return null;
        return p.onRecordList(Duration.ofSeconds(10)) ? p : null;
    }

    private boolean deleteCharacterFolder(AndroidDriver d) {
        CostumePage p = openTool(d);
        p.selectTab(CostumePage.TAB_SELECTS);
        if (!p.waitFolder(CostumeBaseTest.episodeRow(ep), Duration.ofSeconds(15)) || !p.openFolder(CostumeBaseTest.episodeRow(ep))) return false;
        String row = CostumeBaseTest.characterRow(character);
        if (!p.waitFolder(row, Duration.ofSeconds(10))) return false;
        p.openFolderMenu(row);
        if (!p.tapPopupItem("Delete")) { p.dismissPopup(); return false; }
        if (!p.dialogShown(Duration.ofSeconds(5))) return false;
        p.dialogYes();
        CostumeBaseTest.sleep(3000);
        back(d);
        return true;
    }

    private CostumePage openTool(AndroidDriver d) {
        CostumePage p = new CostumePage(d);
        if (p.isLoaded(Duration.ofSeconds(2)) && TOOL.equalsIgnoreCase(p.headerText().trim())) return p;
        assumeTrue(toBottomNav(d), "Could not get back to the bottom nav on " + d.getCapabilities().getCapability("udid"));
        ToolsPage tools = new ToolsPage(d);
        assumeTrue(tools.openToolsTab() && tools.isLoaded(Duration.ofSeconds(15)), "Tools did not open");
        tools.searchTools("Costume");
        CostumeBaseTest.sleep(1200);
        boolean tapped = false;
        long end = System.currentTimeMillis() + 8000;
        while (!tapped && System.currentTimeMillis() < end) {
            try {
                List<WebElement> rows = d.findElements(AppiumBy.xpath("//*[@resource-id='" + pkg + ":id/nameTv' and @text='" + TOOL + "']"));
                if (!rows.isEmpty()) { rows.get(0).click(); tapped = true; }
            } catch (WebDriverException ignored) { }
            CostumeBaseTest.sleep(500);
        }
        assumeTrue(tapped, "No '" + TOOL + "' tile");
        assumeTrue(p.isLoaded(Duration.ofSeconds(25)), TOOL + " did not open");
        CostumeBaseTest.sleep(2000);
        return p;
    }

    private boolean toBottomNav(AndroidDriver d) {
        for (int i = 0; i < 8; i++) {
            if (!d.findElements(AppiumBy.id(pkg + ":id/bottomNavigation")).isEmpty()) return true;
            CostumePage p = new CostumePage(d);
            if (p.dialogShown(Duration.ofMillis(500))) {
                if (p.dialogMessage().startsWith("Are you sure you want to go back")) p.dialogYes(); else p.dialogNo();
                CostumeBaseTest.sleep(800);
                continue;
            }
            back(d);
        }
        return !d.findElements(AppiumBy.id(pkg + ":id/bottomNavigation")).isEmpty();
    }

    private void back(AndroidDriver d) {
        try { d.navigate().back(); } catch (WebDriverException ignored) { }
        CostumeBaseTest.sleep(1000);
    }

    private boolean enterProject(AndroidDriver d) {
        try {
            long end = System.currentTimeMillis() + 90_000;
            while (System.currentTimeMillis() < end) {
                if (!d.findElements(AppiumBy.id(pkg + ":id/bottomNavigation")).isEmpty()
                        || !d.findElements(AppiumBy.id(pkg + ":id/projectListRecyclerView")).isEmpty()) break;
                back(d);
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
}
