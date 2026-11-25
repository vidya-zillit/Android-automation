package com.zillit.automation.tests.costume;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.Config;
import com.zillit.automation.core.ReasonLogger;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import com.zillit.automation.pages.costume.CostumeFormPage;
import com.zillit.automation.pages.costume.CostumePage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebElement;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared navigation, fixtures and cleanup for the Costume suites — Tools →
 * <b>Costume (Main Cast)</b> and <b>Costume (Background Cast)</b>. Both tiles open the same
 * activity ({@code WardrobePage}) with a different tool identifier
 * ({@code WARDROBE_MAIN_TOOL} / {@code WARDROBE_BACKGROUND_TOOL}), so every case body takes
 * the {@link Tool} it runs against and the suites run each case for both.
 *
 * <p><b>Fixtures.</b> Every record a case creates carries a temporary character named
 * {@code ZL_AUTO_Cos <run>…} (the app stores it as "Zl_auto_cos …") and sits under a
 * per-run episode number, so the "Episode No : &lt;run episode&gt;" row in each tab holds
 * nothing but this run's records. Attachments are a link (example.com) or a PNG this suite
 * generates and pushes into its own album {@code ZL_AUTO_Costume} — never a photo of the
 * device owner's. No Casting record is created or used: a temporary character (the "+" on
 * the character picker from a Selects create) lives only inside the costume record.
 */
@ExtendWith(ReasonLogger.class)
abstract class CostumeBaseTest extends BaseTest {

    /** The two tiles. Labels come from the server; these are what this project shows. */
    enum Tool {
        MAIN("Costume (Main Cast)"),
        BACKGROUND("Costume (Background Cast)");

        final String label;
        Tool(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    /**
     * The tools a run covers: both by default; {@code COSTUME_TOOL=MAIN} or {@code BACKGROUND}
     * (env or {@code -Dcostume.tool}) narrows it — a full run of both takes over an hour on a
     * slow emulator, and the two are the same code.
     */
    static List<Tool> tools() {
        String only = System.getProperty("costume.tool", System.getenv("COSTUME_TOOL"));
        if (only == null || only.isBlank()) return List.of(Tool.values());
        List<Tool> out = new ArrayList<>();
        for (String s : only.split(",")) out.add(Tool.valueOf(s.trim().toUpperCase(Locale.ROOT)));
        return out;
    }

    static final String PREFIX = "ZL_AUTO_Cos";
    static final String ALBUM = "ZL_AUTO_Costume";
    static final String LINK = "https://example.com/zl-auto-costume";
    static final String SCENE = "901";

    /** Run number shared by every fixture name and the episode, e.g. 4821. */
    final String run = String.valueOf(1000 + (System.currentTimeMillis() / 1000) % 9000);

    private boolean insideProject;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachProject() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects to enter");
                assumeTrue(list.tapProject(names.get(0)), "Could not enter a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "Never reached a project");
            insideProject = true;
        }
        dismissPermissionPrompts();
    }

    // ───────────────────────── names ─────────────────────────

    /** Episode for a tool's records this run: Main 8xxx, Background 9xxx (distinct per tool). */
    String episode(Tool t) { return (t == Tool.MAIN ? "8" : "9") + run.substring(1); }

    String character(Tool t, String tag) { return PREFIX + " " + run + (t == Tool.MAIN ? "m" : "b") + tag; }

    /** How the app shows a name after {@code capitalizeSentence()}: first letter of each word upper, rest lower. */
    static String stored(String s) {
        StringBuilder sb = new StringBuilder();
        for (String w : s.trim().split(" ")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(w.substring(0, 1).toUpperCase(Locale.ROOT)).append(w.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    static String episodeRow(String ep) { return "Episode No : " + ep; }
    static String characterRow(String name) { return "Character: " + stored(name); }

    // ───────────────────────── navigation ─────────────────────────

    CostumePage page() { return new CostumePage(driver); }
    CostumeFormPage form() { return new CostumeFormPage(driver); }

    /** Opens the tool on its first page from wherever the last step left the app. */
    CostumePage openTool(Tool t) {
        CostumePage p = page();
        if (p.isLoaded(Duration.ofSeconds(2)) && t.label.equalsIgnoreCase(p.headerText().trim())) return p;
        assumeTrue(recoverToBottomNav(), "The app could not be returned to a bottom-nav screen");
        ToolsPage tools = new ToolsPage(driver);
        if (!tools.openToolsTab()) { clearBlockingPopup(); sleep(600); }
        assumeTrue(tools.openToolsTab() || tools.isLoaded(Duration.ofSeconds(3)), "Could not open Tools");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(15)), "Tools list did not load");
        tools.searchTools("Costume");
        sleep(1200);
        assumeTrue(tapToolRow(t.label), "No '" + t.label + "' tile in Tools on this project (tiles seen: "
                + tools.visibleModuleNames() + ")");
        assumeTrue(p.isLoaded(Duration.ofSeconds(25)), t.label + " did not open (WardrobePage)");
        sleep(2000);
        return p;
    }

    /** Taps the Tools row whose label ({@code nameTv}) is exactly {@code label}. */
    boolean tapToolRow(String label) {
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline) {
            try {
                List<WebElement> rows = driver.findElements(AppiumBy.xpath(
                        "//*[@resource-id='" + appPackage + ":id/nameTv' and @text='" + label + "']"));
                if (!rows.isEmpty()) { rows.get(0).click(); sleep(2500); return true; }
            } catch (Exception ignored) { }
            sleep(500);
        }
        return false;
    }

    boolean toolListed(String label) {
        try {
            return !driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage + ":id/nameTv' and @text='" + label + "']")).isEmpty();
        } catch (Exception e) { return false; }
    }

    CostumePage openTab(Tool t, String tab) {
        CostumePage p = openTool(t);
        if (!CostumePage.TAB_SELECTS.equals(tab) || !tabSelected(tab)) {
            assumeTrue(p.selectTab(tab), "Could not select the " + tab + " tab");
        }
        sleep(1500);
        return p;
    }

    private boolean tabSelected(String tab) {
        try {
            List<WebElement> el = driver.findElements(AppiumBy.xpath("//*[@resource-id='" + appPackage
                    + ":id/tabLayout']//*[@selected='true']//android.widget.TextView[@text='" + tab + "']"));
            return !el.isEmpty();
        } catch (Exception e) { return false; }
    }

    /**
     * Tab → "Episode No : ep" → "Character: name" (→ "Scene No. scene" when the character has
     * several) → the record list. Returns null (without failing) when the path is not there.
     */
    CostumePage openRecords(Tool t, String tab, String ep, String characterName, String scene, Duration wait) {
        long end = System.currentTimeMillis() + wait.toMillis();
        do {
            CostumePage p = openTab(t, tab);
            if (p.waitFolder(episodeRow(ep), Duration.ofSeconds(4)) && p.openFolder(episodeRow(ep))) {
                String row = characterName == null ? null : characterRow(characterName);
                boolean opened = row == null ? openUnknownCharacter(p) : p.waitFolder(row, Duration.ofSeconds(6)) && p.openFolder(row);
                if (opened) {
                    if (p.onRecordList(Duration.ofSeconds(8))) return p;
                    if (scene != null && p.waitFolder("Scene No. " + scene, Duration.ofSeconds(4))
                            && p.openFolder("Scene No. " + scene) && p.onRecordList(Duration.ofSeconds(8))) return p;
                }
            }
            sleep(2500);
        } while (System.currentTimeMillis() < end);
        return null;
    }

    /** The "no character" row on the character page reads "Character: Unknown" (unknown_label). */
    boolean openUnknownCharacter(CostumePage p) {
        for (String n : p.folderNames()) {
            if (!n.toLowerCase(Locale.ROOT).startsWith("character: zl_auto") && n.startsWith("Character:")) {
                return p.openFolder(n);
            }
        }
        return false;
    }

    /** Back to the tool's first page (or out), never past the bottom nav. */
    void backToToolHome(Tool t) {
        CostumePage p = page();
        for (int i = 0; i < 6; i++) {
            if (p.isLoaded(Duration.ofSeconds(1))) return;
            if (onBottomNavScreen()) return;
            clearBlockingPopup();
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1200);
            if (form().isOpen(Duration.ofSeconds(1)) || p.dialogShown(Duration.ofSeconds(1))) {
                // "Are you sure you want to go back?" — leave without saving.
                if (p.dialogShown(Duration.ofSeconds(2)) && p.dialogMessage().startsWith("Are you sure you want to go back")) p.dialogYes();
            }
        }
    }

    // ───────────────────────── recovery ─────────────────────────

    boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); } catch (Exception e) { return false; }
    }

    boolean onProjectList() {
        try { return !driver.findElements(byAppId("projectListRecyclerView")).isEmpty(); } catch (Exception e) { return false; }
    }

    boolean recoverToBottomNav() {
        try { clearBlockingPopup(); } catch (Exception ignored) { }
        if (onBottomNavScreen()) return true;
        for (int i = 0; i < 8 && !onBottomNavScreen(); i++) {
            CostumePage p = page();
            if (p.dialogShown(Duration.ofMillis(500))) {
                if (p.dialogMessage().startsWith("Are you sure you want to go back")) p.dialogYes(); else p.dialogNo();
                sleep(800);
                continue;
            }
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }
        if (onBottomNavScreen() || enterProjectFromList()) return true;
        try {
            String pkg = driver.getCurrentPackage();
            if (pkg == null || !pkg.contains("zillit")) {
                driver.executeScript("mobile: activateApp", Map.of("appId", appPackage));
                sleep(2500);
            }
        } catch (Exception ignored) { }
        if (onBottomNavScreen() || enterProjectFromList()) return true;
        try {
            driver.executeScript("mobile: terminateApp", Map.of("appId", appPackage));
            driver.executeScript("mobile: activateApp", Map.of("appId", appPackage));
        } catch (Exception e) { return false; }
        landing = settleLanding(90, false);
        return onBottomNavScreen() || enterProjectFromList();
    }

    private boolean enterProjectFromList() {
        if (!onProjectList()) return false;
        ProjectListPage list = new ProjectListPage(driver);
        List<String> names = list.visibleProjectNames();
        if (names.isEmpty() || !list.tapProject(names.get(0))) return false;
        return new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(45));
    }

    @AfterAll
    void parkOnBottomNav() {
        try { clearBlockingPopup(); } catch (Exception ignored) { }
        for (int i = 0; i < 10; i++) {
            if (onBottomNavScreen()) return;
            CostumePage p = page();
            if (p.dialogShown(Duration.ofMillis(500)) && p.dialogMessage().startsWith("Are you sure you want to go back")) {
                p.dialogYes();
                sleep(800);
                continue;
            }
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }
    }

    // ───────────────────────── fixture creation ─────────────────────────

    /**
     * Creates a record from a tab's "+" with the given attachment ("link" or "photo"),
     * optional temporary character, episode, scene and description. Returns true when the
     * form closed after Submit.
     */
    boolean createRecord(Tool t, String tab, String attachment, String characterName, String talent,
                         String gender, String ep, String scene, String desc) {
        return createRecord(t, tab, attachment, characterName, talent, gender, ep, scene, desc, false);
    }

    /** As above; with {@code pickExisting} the character is picked from the Casting Final list instead of typed in. */
    boolean createRecord(Tool t, String tab, String attachment, String characterName, String talent,
                         String gender, String ep, String scene, String desc, boolean pickExisting) {
        CostumePage p = openTab(t, tab);
        if (!p.tapAdd()) { System.out.println(">>> create: '+' not tapped"); return false; }
        CostumeFormPage f = form();
        if (!f.isOpen(Duration.ofSeconds(10))) { System.out.println(">>> create: the form did not open"); return false; }
        boolean attached = "photo".equals(attachment) ? f.addGalleryPhoto(ALBUM)
                : "link".equals(attachment) ? f.addLink(LINK) : true;
        if (!attached) { System.out.println(">>> attachment '" + attachment + "' could not be added"); leaveForm(); return false; }
        if (characterName != null) {
            if (!f.openCharacterPicker()) { System.out.println(">>> create: the picker did not open"); leaveForm(); return false; }
            if (pickExisting) {
                if (!f.pickCast(characterName)) { leaveForm(); return false; }
            } else if (!f.createTempCharacter(characterName, talent, gender)) {
                System.out.println(">>> temp character could not be created");
                leaveForm();
                return false;
            }
        }
        if (ep != null) f.setField(CostumeFormPage.FIELD_EPISODE, ep);
        if (scene != null) f.setField(CostumeFormPage.FIELD_SCENE, scene);
        if (desc != null) f.setField(CostumeFormPage.FIELD_DESC, desc);
        f.submit();
        long end = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < end) {
            if (p.isLoaded(Duration.ofSeconds(2))) return true;
            String s = f.snackbar();
            if (!s.isEmpty() && f.isOpen(Duration.ofMillis(300))) {
                System.out.println(">>> create refused: " + s);
                leaveForm();
                return false;
            }
        }
        boolean back = p.isLoaded(Duration.ofSeconds(3));
        if (!back) {
            System.out.println(">>> create: still not back on the tool 30 s after Submit — form open: "
                    + f.isOpen(Duration.ofSeconds(1)) + ", header '" + f.header() + "', snackbar '" + f.snackbar() + "'");
            if (f.isOpen(Duration.ofSeconds(1))) leaveForm();
        }
        return back;
    }

    /** Leaves the costume form (or the picker / temp form above it), confirming "go back". */
    void leaveForm() {
        CostumePage p = page();
        for (int i = 0; i < 5; i++) {
            if (p.isLoaded(Duration.ofSeconds(1)) || p.onRecordList(Duration.ofMillis(500)) || p.onInnerFolderPage(Duration.ofMillis(500))) return;
            if (p.dialogShown(Duration.ofSeconds(1))) { p.dialogYes(); sleep(1200); continue; }
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1200);
            if (p.dialogShown(Duration.ofSeconds(2))) { p.dialogYes(); sleep(1200); }
        }
    }

    // ───────────────────────── cleanup ─────────────────────────

    /**
     * Deletes this run's character folders for one tool in one tab — the admin-only
     * "Delete" on the character row ({@code WardrobeCharacterListingPage}), which removes
     * every record of that character + episode + tab. Then any "Unknown" character folder
     * under the run's episode (only this run's records live under it).
     *
     * @return how many folders were deleted
     */
    int deleteRunFolders(Tool t, String tab, String ep) {
        int deleted = 0;
        for (int round = 0; round < 6; round++) {
            CostumePage p = openTab(t, tab);
            if (!p.waitFolder(episodeRow(ep), Duration.ofSeconds(3))) break;
            if (!p.openFolder(episodeRow(ep))) break;
            sleep(1500);
            List<String> rows = p.folderNames();
            String target = null;
            for (String n : rows) if (n.startsWith("Character:")) { target = n; break; }
            if (target == null) break;
            List<String> items = p.openFolderMenu(target);
            if (!items.contains("Delete")) { p.dismissPopup(); System.out.println(">>> no Delete on " + target + ": " + items); break; }
            p.tapPopupItem("Delete");
            if (p.dialogShown(Duration.ofSeconds(5))) { p.dialogYes(); deleted++; }
            sleep(3000);
        }
        backToToolHome(t);
        return deleted;
    }

    /**
     * Sweeps leftovers of any earlier run in one tab: searches "ZL_AUTO_Cos", opens each episode
     * row the search keeps, and deletes the character folders in it that are ours — a
     * "Zl_auto_cos …" character, or "Not Assigned" when the episode is one of this suite's
     * (7xxx / 8xxx / 9xxx) and also holds a Zl_auto_cos folder.
     */
    int sweepOldFolders(Tool t, String tab) {
        int n = sweepTab(openTab(t, tab), () -> openTab(t, tab), t);
        // Episodes of an interrupted run whose only leftover is a "Not Assigned" folder are not
        // found by a character search: name them in COSTUME_SWEEP_EPISODES (e.g. "8544,9544").
        String eps = System.getProperty("costume.sweep.episodes", System.getenv("COSTUME_SWEEP_EPISODES"));
        if (eps != null && !eps.isBlank()) {
            for (String ep : eps.split(",")) {
                ep = ep.trim();
                if (!ep.matches("[789]\\d{3}")) continue;
                for (int round = 0; round < 4; round++) {
                    CostumePage p = openTab(t, tab);
                    if (!p.waitFolder(episodeRow(ep), Duration.ofSeconds(4)) || !p.openFolder(episodeRow(ep))) break;
                    sleep(1500);
                    String target = null;
                    for (String r : p.folderNames())
                        if (r.equals("Character: Not Assigned") || r.toLowerCase(Locale.ROOT).contains(PREFIX.toLowerCase(Locale.ROOT))) { target = r; break; }
                    if (target == null) break;
                    p.openFolderMenu(target);
                    if (!p.tapPopupItem("Delete")) { p.dismissPopup(); break; }
                    if (p.dialogShown(Duration.ofSeconds(5))) { p.dialogYes(); n++; System.out.println(">>> sweep: deleted '" + target + "' in episode " + ep); }
                    sleep(3000);
                }
            }
            recoverToBottomNav();
        }
        return n;
    }

    /** Same sweep on Casting → tab, deleting only "Zl_auto_cos" folders (never "Not Assigned"). */
    int sweepCasting(Tool t, String tab) {
        java.util.function.Supplier<CostumePage> open = () -> { CostumePage c = openCasting(t); c.selectTab(tab); sleep(1500); return c; };
        return sweepTab(open.get(), open, null);
    }

    private int sweepTab(CostumePage first, java.util.function.Supplier<CostumePage> reopen, Tool costumeTool) {
        int deleted = 0;
        String mine = PREFIX.toLowerCase(Locale.ROOT);
        CostumePage p = first;
        for (int round = 0; round < 12; round++) {
            if (round > 0) p = reopen.get();
            p.search(PREFIX);
            String target = null;
            String episodeOpened = null;
            for (String n : p.folderNames()) {
                if (n.toLowerCase(Locale.ROOT).contains(mine)) { target = n; break; }
                if (episodeOpened == null && n.matches("Episode No : [789]\\d{3}")) episodeOpened = n;
            }
            if (target == null && episodeOpened != null) {
                if (!p.openFolder(episodeOpened)) break;
                sleep(1500);
                List<String> rows = p.folderNames();
                boolean ours = rows.stream().anyMatch(r -> r.toLowerCase(Locale.ROOT).contains(mine));
                // "Not Assigned" first: once the last Zl_auto_cos folder is gone the search no longer
                // finds this episode, and a character-less folder would be stranded.
                if (costumeTool != null && ours && rows.contains("Character: Not Assigned")) target = "Character: Not Assigned";
                else for (String r : rows) if (r.toLowerCase(Locale.ROOT).contains(mine)) { target = r; break; }
            }
            if (target == null) break;
            List<String> items = p.openFolderMenu(target);
            if (!p.tapPopupItem("Delete")) { p.dismissPopup(); System.out.println(">>> sweep: no Delete on '" + target + "': " + items); break; }
            if (p.dialogShown(Duration.ofSeconds(5))) { p.dialogYes(); deleted++; System.out.println(">>> sweep: deleted '" + target + "'"); }
            sleep(3000);
        }
        try { p.clearSearch(); } catch (Exception ignored) { }
        recoverToBottomNav();
        return deleted;
    }

    // ───────────────────────── Casting fixture ─────────────────────────
    //
    // A costume record can leave Selects only with a character that exists in the matching
    // Casting tool (server: "costumes with cast names created via the Costume module are not
    // allowed…"). So the Shortlist / Final cases need ONE cast of our own in Casting → Final:
    // photo (Final refuses links), episode, character, talent, gender. It is created here and
    // deleted again by name — nothing of the Casting suite's is read or touched.

    static String castingTile(Tool t) { return t == Tool.MAIN ? "Casting (Main Cast)" : "Casting (Background Cast)"; }

    CostumePage openCasting(Tool t) {
        CostumePage p = page();
        if (p.isLoaded(Duration.ofSeconds(1)) && castingTile(t).equalsIgnoreCase(p.headerText().trim())) return p;
        assumeTrue(recoverToBottomNav(), "The app could not be returned to a bottom-nav screen");
        ToolsPage tools = new ToolsPage(driver);
        if (!tools.openToolsTab()) { clearBlockingPopup(); sleep(600); }
        assumeTrue(tools.openToolsTab() || tools.isLoaded(Duration.ofSeconds(3)), "Could not open Tools");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(15)), "Tools list did not load");
        tools.searchTools("Casting");
        sleep(1200);
        assumeTrue(tapToolRow(castingTile(t)), "No '" + castingTile(t) + "' tile — cannot make the cast a Shortlist/Final costume needs");
        assumeTrue(p.isLoaded(Duration.ofSeconds(25)), castingTile(t) + " did not open");
        sleep(2000);
        return p;
    }

    /** Creates the ZL_AUTO cast in Casting → Final (photo, episode, character, talent, gender). */
    boolean createCastingFixture(Tool t, String name, String gender, String ep) {
        CostumePage p = openCasting(t);
        if (!p.selectTab(CostumePage.TAB_FINAL)) { System.out.println(">>> casting: no Final tab"); return false; }
        if (!p.tapAdd()) { System.out.println(">>> casting: '+' not tapped"); return false; }
        CostumeFormPage f = form();
        if (!f.isOpen(Duration.ofSeconds(10))) { System.out.println(">>> casting: the cast form did not open"); return false; }
        if (!f.addGalleryPhoto(ALBUM)) { System.out.println(">>> casting: the photo could not be attached"); leaveForm(); return false; }
        f.setField(CostumeFormPage.FIELD_EPISODE, ep);
        f.setField("characterName", name);
        f.setField("talentName", PREFIX + " Talent " + run);
        if (!f.pickGender(gender)) System.out.println(">>> casting: gender not picked");
        f.submit();
        long end = System.currentTimeMillis() + 40_000;
        while (System.currentTimeMillis() < end) {
            if (p.isLoaded(Duration.ofSeconds(2))) return true;
            String s = f.snackbar();
            if (!s.isEmpty() && f.isOpen(Duration.ofMillis(300))) {
                System.out.println(">>> casting create refused: " + s);
                leaveForm();
                return false;
            }
        }
        System.out.println(">>> casting: not back on the Casting home after Submit");
        leaveForm();
        return false;
    }

    /** Deletes every Casting folder whose label names {@code name}, in all three tabs. */
    int deleteCastingFixture(Tool t, String name, String ep) {
        int n = 0;
        String want = stored(name).toLowerCase(Locale.ROOT);
        for (String tab : CostumePage.TABS) {
            for (int round = 0; round < 3; round++) {
                CostumePage p = openCasting(t);
                p.selectTab(tab);
                sleep(1500);
                if (p.waitFolder(episodeRow(ep), Duration.ofSeconds(4))) {
                    if (!p.openFolder(episodeRow(ep))) break;
                    sleep(1500);
                }
                String target = null;
                for (String r : p.folderNames()) if (r.toLowerCase(Locale.ROOT).contains(want)) { target = r; break; }
                if (target == null) break;
                List<String> items = p.openFolderMenu(target);
                if (!p.tapPopupItem("Delete")) { p.dismissPopup(); System.out.println(">>> casting delete not offered on " + target + ": " + items); break; }
                if (p.dialogShown(Duration.ofSeconds(5))) { p.dialogYes(); n++; }
                sleep(3000);
            }
        }
        recoverToBottomNav();
        return n;
    }

    // ───────────────────────── photo fixture ─────────────────────────

    private String pushedPhoto;

    /** Generates a PNG and pushes it into the device album ZL_AUTO_Costume; true when the media store has it. */
    boolean pushTestPhoto() {
        if (pushedPhoto != null) return true;
        try {
            BufferedImage img = new BufferedImage(480, 640, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(40, 90, 160));
            g.fillRect(0, 0, 480, 640);
            g.setColor(Color.WHITE);
            g.drawString("ZL_AUTO costume test " + run, 40, 320);
            g.dispose();
            Path f = Files.createTempFile("zl_auto_costume_", ".png");
            javax.imageio.ImageIO.write(img, "png", f.toFile());
            String remoteDir = "/sdcard/Pictures/" + ALBUM;
            String remote = remoteDir + "/zl_auto_costume_" + run + ".png";
            adb("shell", "mkdir", "-p", remoteDir);
            adb("push", f.toString(), remote);
            adb("shell", "am", "broadcast", "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE", "-d", "file://" + remote);
            adb("shell", "content", "call", "--method", "scan_volume", "--uri", "content://media", "--arg", "external_primary");
            sleep(3000);
            String q = adb("shell", "content", "query", "--uri", "content://media/external/images/media",
                    "--projection", "_display_name", "--where", "\"_display_name LIKE 'zl_auto_costume_%'\"");
            pushedPhoto = remote;
            System.out.println(">>> pushed " + remote + "; media store: " + q.trim());
            return q.contains("zl_auto_costume_");
        } catch (Exception e) {
            System.out.println(">>> could not push the test photo: " + e);
            return false;
        }
    }

    void removeTestPhotos() {
        adb("shell", "rm", "-rf", "/sdcard/Pictures/" + ALBUM);
        adb("shell", "content", "call", "--method", "scan_volume", "--uri", "content://media", "--arg", "external_primary");
        pushedPhoto = null;
    }

    String adb(String... args) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(adbBinary());
            String udid = Config.get("zillit.udid");
            if (udid == null || udid.isBlank()) udid = System.getenv("ZILLIT_UDID");
            if (udid != null && !udid.isBlank()) { cmd.add("-s"); cmd.add(udid); }
            cmd.addAll(List.of(args));
            ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
            pb.environment().put("ADB_MDNS", "0");
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes());
            p.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
            return out;
        } catch (Exception e) {
            return "";
        }
    }

    private static String adbBinary() {
        for (String var : new String[]{"ANDROID_HOME", "ANDROID_SDK_ROOT"}) {
            String root = System.getenv(var);
            if (root == null || root.isBlank()) continue;
            File c = Path.of(root, "platform-tools", "adb").toFile();
            if (c.canExecute()) return c.toString();
        }
        File home = Path.of(System.getProperty("user.home"), "Library", "Android", "sdk", "platform-tools", "adb").toFile();
        return home.canExecute() ? home.toString() : "adb";
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
