package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.ConfirmPopupPage;
import com.zillit.automation.pages.DocDistCreateFolderSheet;
import com.zillit.automation.pages.DocDistLibraryPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.SystemFilePickerPage;
import com.zillit.automation.pages.ToolsPage;
import io.appium.java_client.AppiumBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared plumbing for the Document Distribution CONTENT suites (files, composer, …):
 * reaching the library, seeding a work folder, and uploading a fixture into it.
 *
 * <p>The navigation is the same as {@link DocDistributionTest} / {@link DocDistFoldersTest}
 * grew organically: the activity is NOT exported, so the only way in is
 * Tools → the module row, and the row's visible label is server-supplied — matched
 * against {@link DocDistLibraryPage#TOOL_LABELS} rather than one guess.
 *
 * <p>Upload (the app side) is {@code OpenMultipleDocuments} straight into the SYSTEM
 * picker — no in-app source sheet like Drive — followed by a framework
 * "Upload N file(s)?" MaterialAlertDialog that {@link ConfirmPopupPage} knows how to
 * accept. The picker itself is driven by {@link SystemFilePickerPage} — a foreign
 * package, so it matches on text/description and never on ids, and it must leave the
 * picker's "Recent" tab because adb-pushed fixtures keep their original mtime and so are
 * not recent at all.
 */
abstract class DocDistBaseTest extends BaseTest {

    private boolean insideProject;

    @Override protected boolean relaunchBetweenTests()   { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    /** Call from @BeforeEach: lands on the library ROOT, or skips the case. */
    protected void reachLibraryOrSkip() {
        long t0 = System.currentTimeMillis();
        try {
            reachLibraryOrSkipInner();
        } finally {
            long ms = System.currentTimeMillis() - t0;
            // Printed for EVERY case: this is the gap between one case finishing and the
            // next starting, and it is invisible in the Gradle output otherwise. If it is
            // consistently high, the fast path is missing and something is leaving the app
            // off the library root at the end of a case.
            System.out.println(">>> setup took " + ms + "ms" + (ms > 4000 ? "  [SLOW]" : ""));
        }
    }

    private void reachLibraryOrSkipInner() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects");
                assumeTrue(list.tapProject(names.get(0)), "Could not enter a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "No project");
            insideProject = true;
        }
        // FAST PATH: the previous case almost always leaves us on the library root, and
        // re-running the full navigation there costs seconds per case for nothing. Only
        // pay for permission sweeps, popup clearing and re-navigation when we are NOT
        // already where we need to be.
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        if (lib.isDocDistActivity() && lib.isLibrary(Duration.ofSeconds(2)) && lib.isAtRoot()) {
            return;
        }
        dismissPermissionPrompts();
        clearBlockingPopup();
        assumeTrue(openModule(), "Could not open Document Distribution");
    }

    protected boolean openModule() {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        // isLibraryNow() rather than isLibrary(3s): on the happy path both answer yes
        // instantly, and on a miss the waiting version burned five seconds before the
        // recovery ladder below even started. A false negative here is harmless — the
        // ladder's first round re-asks (twice, 500ms apart) before it presses anything.
        if (lib.isDocDistActivity() && lib.isLibraryNow()) {
            lib.returnToRoot();
            return true;
        }
        // ALREADY INSIDE the module but not on the library: recover HERE. Going out to
        // Tools is impossible from this activity — it has no bottom nav, so openToolsTab()
        // can only fail, and a whole suite then skipped with the app sitting in Document
        // Distribution the entire time. Clear whatever is covering the library (a popup
        // menu, a sheet, a dialog) and step back one screen at a time.
        if (lib.isDocDistActivity()) {
            for (int i = 0; i < 8; i++) {
                clearBlockingPopup();
                // Presence-only, then ONE short retry for a screen still animating in.
                // The old isLibrary(2s) spent ~4s per round saying "no" — with two or
                // three rounds typical, that was ~25s of the setup time on its own.
                if (lib.isLibraryNow()) { lib.returnToRoot(); return true; }
                sleep(500);
                if (lib.isLibraryNow()) { lib.returnToRoot(); return true; }
                // Do NOT break on a single "not in DD" reading: currentActivity() reports
                // the transition mid-BACK, which used to abort the recovery after one
                // press and drop us into a Tools navigation that cannot work from here.
                // getPageSource() is seconds on a busy screen, so only dump the ids on the
                // first round (where the diagnostic earns its keep) and once the ladder
                // starts looking stuck.
                if (i == 0 || i >= 3) {
                    System.out.println(">>> in-module recovery " + (i + 1) + ": not the library yet, "
                            + "on screen = " + onScreenDdIds());
                }
                if (!lib.isDocDistActivity()) {
                    sleep(600);
                    if (!lib.isDocDistActivity()) break;   // genuinely left the module
                }
                try { driver.navigate().back(); Thread.sleep(700); } catch (Exception e) { break; }
            }
            if (lib.isDocDistActivity() && lib.isLibrary(Duration.ofSeconds(2))) {
                lib.returnToRoot();
                return true;
            }
        }
        // Get OUT to the bottom nav so Tools is reachable, dismissing anything in the way.
        forceExitToBottomNav();
        ToolsPage tools = new ToolsPage(driver);
        // Try the label that ALREADY worked before anything else. The Tools row's caption
        // is server-supplied, so this list is three guesses — and re-walking all three on
        // every navigation meant each miss cost a Tools load (up to 15s), a search and a
        // 4s listing wait before the right one was even tried. The winner never changes
        // within a run, so remember it: after the first success this loop is one pass.
        List<String> labels = new ArrayList<>();
        if (knownToolLabel != null) labels.add(knownToolLabel);
        for (String l : DocDistLibraryPage.TOOL_LABELS) {
            if (!l.equals(knownToolLabel)) labels.add(l);
        }
        for (String label : labels) {
            boolean tab = tools.openToolsTab();
            boolean loaded = tools.isLoaded(Duration.ofSeconds(8));
            if (!loaded) {
                System.out.println(">>> nav['" + label + "']: toolsTab=" + tab + " loaded=false"
                        + " pkg=" + safePackage() + " — the Tools screen never appeared");
                sleep(500);
                continue;
            }
            // SCROLL to the row instead of searching for it. Typing carries state: a
            // query left in the box filters the list for the next navigation, hides the
            // row we want, and removes the hint other code used to recognise the screen —
            // which is exactly how a whole suite ended up skipping with Tools on display.
            // Scrolling reads the list as-is and leaves nothing behind.
            // Two steps, cheapest first — and deliberately NO scrolling. The Tools list is
            // long, so hunting it by swiping costs several seconds and drags the list up
            // and down for every single navigation.
            //   1. The row is often already on screen (a previous navigation frequently
            //      leaves the list filtered to exactly this module) — just tap it.
            //   2. Otherwise SEARCH for it, which jumps straight to it regardless of how
            //      far down the list it lives.
            // searchTools() clears the box before typing, so a query left behind by an
            // earlier run cannot corrupt the new one.
            boolean opened = tools.openIfVisible(label);
            if (!opened) {
                tools.searchTools(label);
                sleep(400);
                opened = tools.openIfVisible(label) || tools.openModule(label);
            }
            if (!opened) {
                System.out.println(">>> nav['" + label + "']: loaded=true but the row was "
                        + "neither on screen nor found by searching for this caption");
                continue;
            }
            boolean library = lib.isLibrary(Duration.ofSeconds(20));
            System.out.println(">>> nav['" + label + "']: opened=" + opened
                    + " library=" + library + " pkg=" + safePackage()
                    + " activity=" + safeActivity());
            if (library) {
                knownToolLabel = label;
                return true;
            }
        }
        // LAST RESORT: restart the app. Reaching here means the module could not be
        // opened from wherever we are — and without this, ONE stuck screen turns into
        // every remaining case skipping, which is exactly what a whole suite did. A
        // relaunch costs a few seconds and guarantees a known starting point, so a single
        // bad case can no longer poison the rest of the run.
        System.out.println(">>> nav failed for every label " + labels
                + " — app is on " + safeActivity() + "; RESTARTING the app and retrying once");
        if (!relaunched) {
            relaunched = true;
            try {
                driver.executeScript("mobile: terminateApp", java.util.Map.of("appId", appPackage));
                sleep(1200);
                driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
            } catch (Exception e) {
                System.out.println(">>> relaunch failed: " + e.getMessage());
            }
            // A cold start lands on SPLASH, then possibly the PROJECT LIST, and only then
            // the bottom nav. Attacking Tools before that settles is what turned this
            // fallback into a restart loop: each retry hit the splash, failed, and
            // restarted again. Wait for a usable screen and re-enter the project first.
            boolean ready = settleAfterRelaunch();
            System.out.println(">>> relaunched; settled=" + ready + " on " + safeActivity());
            boolean ok = ready && openModule();
            relaunched = false;
            return ok;
        }
        return false;
    }

    /**
     * The Tools caption that actually opened the module, remembered for the whole run.
     * Static so every suite in the same Gradle invocation benefits from the first
     * suite's discovery instead of re-deriving it.
     */
    private static String knownToolLabel;

    /** Guards the relaunch retry so a failure cannot recurse. */
    private boolean relaunched;

    /** The dd_ ids currently on screen — names the state an in-module recovery is stuck in. */
    private String onScreenDdIds() {
        // Read the page SOURCE, not a per-element sweep: the sweep threw on every call
        // (each getAttribute is a round trip and any stale node aborts the lot), so this
        // diagnostic only ever printed "?" — useless exactly when it was needed.
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile(":id/(dd_[a-z_0-9]+)")
                    .matcher(driver.getPageSource());
            java.util.Set<String> ids = new java.util.TreeSet<>();
            while (m.find() && ids.size() < 18) ids.add(m.group(1));
            return ids.isEmpty() ? "(no dd_ ids — not a Document Distribution screen)" : ids.toString();
        } catch (Exception e) { return "unreadable: " + e.getMessage(); }
    }

    private String safePackage() {
        try { return driver.getCurrentPackage(); } catch (Exception e) { return "?"; }
    }

    private String safeActivity() {
        try { return driver.currentActivity(); } catch (Exception e) { return "?"; }
    }

    /**
     * Leaves whatever screen we are on until the bottom nav is showing, so Tools — and
     * therefore Document Distribution — can be reached again.
     *
     * <p>Plain BACK is not enough. The app has a dialog shape that BACK cannot dismiss:
     * {@code promptToolNotEnabled} is built with {@code setCancelable(false)} and NO left
     * button (trap 3.33), so it swallows every BACK and there is nothing to decline. A
     * recovery that only declines therefore stalls in front of it for ever — and because
     * the escape stalls, every remaining case in the run skips.
     *
     * <p>So each round: decline a dialog if it HAS a decline, otherwise accept an OK-only
     * one (accepting an informational dialog consents to nothing), then press BACK.
     */
    /**
     * Waits out a cold start: SPLASH → (PROJECT LIST) → bottom nav.
     *
     * <p>Re-enters the project when the relaunch drops us on the project list — without
     * that, every retry after a restart is doomed, because Tools only exists inside a
     * project.
     */
    protected boolean settleAfterRelaunch() {
        for (int i = 0; i < 40; i++) {           // ~60s budget for a cold start
            if (onBottomNavScreen()) return true;
            try {
                if (!driver.findElements(byAppId("projectListRecyclerView")).isEmpty()) {
                    ProjectListPage list = new ProjectListPage(driver);
                    List<String> names = list.visibleProjectNames();
                    if (!names.isEmpty() && list.tapProject(names.get(0))) {
                        new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(45));
                        return onBottomNavScreen();
                    }
                }
            } catch (Exception ignored) { }
            sleep(1500);
        }
        return onBottomNavScreen();
    }

    protected boolean forceExitToBottomNav() {
        ConfirmPopupPage popup = new ConfirmPopupPage(driver);
        for (int i = 0; i < 8; i++) {
            if (onBottomNavScreen()) return true;
            if (popup.isShown(Duration.ofMillis(600))) {
                boolean declined = popup.cancel(Duration.ofMillis(600));
                if (!declined) {
                    // No decline available — an OK-only informational dialog. Accepting is
                    // the ONLY exit and commits to nothing.
                    popup.accept(Duration.ofMillis(600));
                }
                sleep(500);
                continue;   // re-check before spending a BACK
            }
            try { driver.navigate().back(); } catch (Exception e) { break; }
            sleep(700);
        }
        boolean out = onBottomNavScreen();
        if (!out) {
            System.out.println(">>> could not reach the bottom nav — still on " + safeActivity()
                    + " with " + onScreenDdIds());
        }
        return out;
    }

    protected boolean onBottomNavScreen() {
        try { return !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
        catch (Exception e) { return false; }
    }

    /** Creates the named folder from the root, tolerant of the empty-state CTA path. */
    protected boolean createFolder(String name) {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        DocDistCreateFolderSheet sheet = new DocDistCreateFolderSheet(driver);
        for (int attempt = 0; attempt < 3; attempt++) {
            if ((lib.openCreateFolder() && sheet.waitOpen(Duration.ofSeconds(8)))
                    || (lib.hasEmptyCta() && clickAppId("dd_empty_cta")
                        && sheet.waitOpen(Duration.ofSeconds(8)))) {
                return sheet.createNamed(name);
            }
            sleep(1500);
        }
        return false;
    }

    /**
     * Uploads an adb-pushed fixture into the CURRENTLY OPEN folder. The caller must
     * already be inside one — Upload does not exist at the root (LibraryFragment:
     * visible only when {@code state.isInFolder}).
     *
     * @return false at the first step that did not appear, so the caller's message
     *         can say where the pipeline broke.
     */
    protected boolean uploadFixtureIntoOpenFolder(String fileName) {
        return uploadFixturesIntoOpenFolder(new String[]{fileName});
    }

    /**
     * Uploads one or more adb-pushed fixtures in a SINGLE picker session.
     *
     * <p>The app launches {@code OpenMultipleDocuments}, so the picker genuinely supports
     * a multi-file pick, and the confirm dialog is plural ("Upload N file(s)?"). Passing
     * several names here is therefore the honest way to exercise the batch path — and the
     * app's own allow-list ({@code FileTypeDetector}) silently drops anything unsupported
     * with a "skipped unsupported" snackbar, which is why a rejected type can be mixed in
     * deliberately.
     */
    protected boolean uploadFixturesIntoOpenFolder(String[] fileNames) {
        return pickForUpload(fileNames) && confirmUploadIfAsked();
    }

    /**
     * Opens Upload and hands the named fixtures to the app — WITHOUT confirming.
     *
     * <p>Split from the confirm step on purpose. The app only raises its
     * "Upload N file(s)?" dialog for files that pass {@code FileTypeDetector}'s
     * allow-list; an unsupported file is partitioned into {@code skipped} and merely
     * snackbarred, so NO dialog appears. If picking and confirming stayed fused, "the
     * picker never found the file" and "the app refused the file" would be the same
     * false return — and the .txt rejection case would PASS whenever the picker was
     * simply broken, which is exactly what happened before this split.
     *
     * @return true when the files were handed back to the app (the picker is gone).
     */
    protected boolean pickForUpload(String[] fileNames) {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        if (!lib.openUpload()) {
            System.out.println(">>> Upload chip not reachable");
            return false;
        }
        SystemFilePickerPage picker = new SystemFilePickerPage(driver);
        if (!picker.isOpen(Duration.ofSeconds(20))) {
            System.out.println(">>> the system picker never opened");
            return false;
        }
        // The first pick has to LEAVE "Recent": adb push preserves each fixture's original
        // mtime, so freshly pushed files are not recent and simply are not listed there.
        // pickFiles() also knows that a plain tap SELECTS-AND-RETURNS, so a batch has to
        // start with a long-press to enter the picker's multi-select mode.
        int picked = picker.pickFiles(fileNames, Duration.ofSeconds(60));
        System.out.println(">>> picker selected " + picked + " of " + fileNames.length + " file(s)");
        if (picked == 0) return false;
        picker.confirmIfWaiting();

        // The pick only counts once control is back in the app under test.
        for (int i = 0; i < 20; i++) {
            try {
                String pkg = driver.getCurrentPackage();
                if (pkg != null && pkg.contains("zillitapp")) return true;
            } catch (Exception ignored) { }
            sleep(500);
        }
        return false;
    }

    /**
     * Accepts the app's "Upload N file(s)?" confirmation.
     *
     * @return true if the dialog appeared AND was accepted; false when the app never
     *         asked — which for an unsupported file is the CORRECT outcome, not an error.
     */
    protected boolean confirmUploadIfAsked() {
        ConfirmPopupPage confirm = new DocDistLibraryPage(driver).confirmPopup();
        if (!confirm.isShown(Duration.ofSeconds(12))) {
            System.out.println(">>> no upload confirmation appeared (app accepted nothing)");
            return false;
        }
        System.out.println(">>> upload confirm says: '" + confirm.text() + "'");
        return confirm.accept(Duration.ofSeconds(5));
    }


    /** Deletes a root-level item through its row menu, confirming the dialog. */
    /**
     * Deletes a row and reports whether it is GONE — not whether the taps went through.
     *
     * <p>NEST-7 failed while the folder was successfully deleted: the screenshot taken at
     * the failure shows the root list with {@code QA_NEST1_436} absent from its alphabetical
     * slot, yet this returned false because the final {@code accept()} ends in a
     * {@code waitGone(10s)} on the DIALOG. Deleting a non-empty folder can raise a second
     * confirmation ("and everything inside it"), so a dialog was still up and the probe
     * called a completed delete a failure.
     *
     * <p>The outcome is the only thing worth asserting — the same rule the folder editor
     * page learned about Save. Accept every confirmation the app raises, then answer the
     * real question by searching for the row (which is authoritative right after a
     * mutation, per {@code waitForItem}).
     */
    protected boolean deleteItem(String name) {
        DocDistLibraryPage lib = new DocDistLibraryPage(driver);
        if (!lib.waitForItem(name, Duration.ofSeconds(15))) return true;   // already gone
        if (!lib.openRowMenu(name)) {
            System.out.println(">>> deleteItem('" + name + "'): the row's ⋮ was not clickable");
            return false;
        }
        sleep(1200);
        if (!lib.tapMenuItem("Delete")) {
            System.out.println(">>> deleteItem('" + name + "'): no Delete in the row menu, "
                    + "offered = " + lib.visibleTexts(12));
            return false;
        }
        int confirmations = 0;
        for (int i = 0; i < 3; i++) {
            if (!lib.confirmPopup().isShown(Duration.ofSeconds(i == 0 ? 8 : 2))) break;
            lib.confirmPopup().accept(Duration.ofSeconds(2));
            confirmations++;
            sleep(600);
        }
        boolean gone = !lib.waitForItem(name, Duration.ofSeconds(8));
        System.out.println(">>> deleteItem('" + name + "'): confirmations=" + confirmations
                + " gone=" + gone);
        return gone;
    }

    protected boolean clickAppId(String id) {
        try { driver.findElement(byAppId(id)).click(); sleep(800); return true; }
        catch (Exception e) { return false; }
    }

    protected void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
