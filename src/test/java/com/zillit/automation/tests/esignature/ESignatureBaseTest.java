package com.zillit.automation.tests.esignature;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.common.ProjectListPage;
import com.zillit.automation.pages.common.ToolsPage;
import com.zillit.automation.pages.esignature.ESignaturePage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared entry for the Z-E-Signature suite. Same policy as the Schedule Distribution base:
 * the app is opened once, a project entered once, nothing relaunches between cases.
 *
 * <p>Module-specific recovery rules:
 * <ul>
 *   <li><b>The Tools label is server-supplied.</b> On ZL-Android-Automation the tile reads
 *       "E-Signature"; the module's own header reads "Z-E-Signature". The search matches both,
 *       and never "Documents &amp; Signature" (that is the separate Forms &amp; Signature tool).</li>
 *   <li><b>Everything lives in ONE activity</b> ({@code DocSignMainActivity}): the editor,
 *       placement and detail are fragments, so "inside the module" is the activity and "on the
 *       landing screen" is the segment chips being on screen.</li>
 *   <li><b>Back from the editor discards silently</b> (no "discard changes?" prompt), so a
 *       half-built envelope is never left behind by stepping back.</li>
 *   <li>Dialogs here are framework/AppCompat AlertDialogs ({@code button1}/{@code button2})
 *       and bottom sheets — BACK closes all of them.</li>
 * </ul>
 */
abstract class ESignatureBaseTest extends BaseTest {

    /** Matches the tile whether the server calls it "E-Signature" or "Z-E-Signature". */
    static final String TOOL_REGEX = "(Z-)?E-Signature";

    @Override protected boolean requiresLoggedOutStart() { return false; }
    @Override protected boolean relaunchBetweenTests()   { return false; }

    // ─────────────────────────── navigation ───────────────────────────

    /** Lands on the module's landing screen (segment chips), Manage Envelopes selected or not. */
    void openESign() {
        bringAppToFront();
        if (onLanding()) return;
        if (insideModule()) { stepBackToLanding(); if (onLanding()) return; }

        BottomNavPage nav = new BottomNavPage(driver);
        if (nav.waitUntilLoaded(Duration.ofSeconds(3))) { openFromTools(); return; }
        for (int i = 0; i < 6; i++) {
            if (onLanding()) return;
            if (nav.waitUntilLoaded(Duration.ofSeconds(2))) { openFromTools(); return; }
            if (onProjectList()) break;
            closeAnyDialog();
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(900);
        }
        if (onProjectList()) {
            ProjectListPage projects = new ProjectListPage(driver);
            List<String> names = projects.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "The project list is open but shows no projects to enter.");
            assumeTrue(projects.tapProject(names.get(0)),
                    "Could not enter \"" + names.get(0) + "\". Projects listed: " + names);
        }
        if (!nav.waitUntilLoaded(Duration.ofSeconds(60))) {
            assumeTrue(false, "The bottom navigation never loaded. On screen instead: " + whereAreWe());
        }
        openFromTools();
    }

    void openFromTools() {
        ToolsPage tools = new ToolsPage(driver);
        assumeTrue(tools.openToolsTab(), "The Tools tab could not be opened");
        assumeTrue(tools.isLoaded(Duration.ofSeconds(30)), "The Tools tab did not load");
        tools.searchTools("Signature");
        sleep(1200);
        if (!tools.openIfVisible(TOOL_REGEX)) {
            assumeTrue(false, "No \"E-Signature\" / \"Z-E-Signature\" tile in Tools (tool not enabled"
                    + " for this project or user). Tools visible: " + tools.toolNames());
        }
        ESignaturePage page = new ESignaturePage(driver);
        if (!page.isLoaded(Duration.ofSeconds(25))) {
            if (page.isReceivedOnly()) {
                assumeTrue(false, "This user has view-only rights: the module opened straight on the"
                        + " received inbox, so the sender cases cannot run.");
            }
            assumeTrue(false, "Z-E-Signature did not open. Without view/posting rights it shows a"
                    + " \"no viewing rights\" prompt and closes. On screen instead: " + whereAreWe());
        }
    }

    /** Back to Manage Envelopes → Z-E-Signature → the given sub-tab. */
    ESignaturePage openManage(String subTab) {
        openESign();
        ESignaturePage page = new ESignaturePage(driver);
        if (!page.segmentSelected(ESignaturePage.SEG_MANAGE)) page.openSegment(ESignaturePage.SEG_MANAGE);
        assumeTrue(page.listShown(Duration.ofSeconds(15)), "Manage Envelopes list did not show");
        page.openTopTab(ESignaturePage.TOP_ESIGN);
        if (subTab != null) page.openSubTab(subTab);
        return page;
    }

    void bringAppToFront() {
        String pkg;
        try { pkg = String.valueOf(driver.getCurrentPackage()); } catch (Exception e) { pkg = ""; }
        if (appPackage.equals(pkg)) return;
        for (int i = 0; i < 3; i++) {   // the system picker / an external viewer usually leaves on BACK
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1000);
            try { if (appPackage.equals(driver.getCurrentPackage())) return; } catch (Exception ignored) { }
        }
        try { driver.activateApp(appPackage); }
        catch (Exception e) {
            try { driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage)); }
            catch (Exception ignored) { }
        }
        sleep(3000);
        waitAndDismissPermissions(Duration.ofSeconds(3));
    }

    boolean insideModule() {
        return activityIs("DocSignMainActivity") || activityIs("GalleryItemView");
    }

    /** The landing screen: the segment chips are on screen. */
    boolean onLanding() {
        return activityIs("DocSignMainActivity") && present("dsEntryChipManage") && !dialogUp();
    }

    boolean dialogUp() {
        return present("alertTitle") || present("design_bottom_sheet") || present("btnUse")
                || present("parentPanel");
    }

    boolean activityIs(String simpleName) {
        try {
            String a = String.valueOf(driver.currentActivity());
            return a.endsWith("." + simpleName) || a.endsWith(simpleName);
        } catch (Exception e) {
            return false;
        }
    }

    /** Bounded; never calls {@link #openESign()}. */
    void stepBackToLanding() {
        for (int i = 0; i < 8; i++) {
            bringAppToFront();
            if (onLanding()) return;
            if (onProjectList()) return;
            if (!insideModule()) return;
            if (closeAnyDialog()) continue;
            // GalleryItemView (the picked-PDF preview) asks "Are you sure you want to exit?".
            if (activityIs("GalleryItemView")) {
                try { driver.navigate().back(); } catch (Exception ignored) { }
                sleep(800);
                tapTextIfShown("(?i)yes");
                continue;
            }
            if (!tapId("btnBack")) {
                try { driver.navigate().back(); } catch (Exception ignored) { }
            }
            sleep(1200);
        }
    }

    /** Closes an AlertDialog with its NEGATIVE button, or a bottom sheet with BACK. */
    boolean closeAnyDialog() {
        if (present("button2") && present("alertTitle")) return tapId("button2");
        if (present("btnUse") || present("cardType")) return tapId("btnClose");
        if (present("design_bottom_sheet")) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(800);
            return true;
        }
        return false;
    }

    boolean tapId(String id) {
        try {
            List<WebElement> e = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/" + id + "\")"));
            if (e.isEmpty()) return false;
            e.get(0).click();
            sleep(1000);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    void tapTextIfShown(String regex) {
        try {
            List<WebElement> e = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"" + regex + "\")"));
            if (!e.isEmpty()) { e.get(0).click(); sleep(800); }
        } catch (Exception ignored) { }
    }

    boolean onProjectList() { return present("projectListRecyclerView"); }

    boolean present(String id) {
        try {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/" + id + "\")")).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    String whereAreWe() {
        String pkg;
        try { pkg = String.valueOf(driver.getCurrentPackage()); }
        catch (Exception e) { pkg = ""; }
        if (!appPackage.equals(pkg)) return "a different app (" + (pkg.isBlank() ? "unreadable" : pkg) + ")";
        if (onProjectList()) return "the project list";
        try { return "activity " + driver.currentActivity(); }
        catch (Exception e) { return "an unreadable activity"; }
    }

    void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @BeforeEach
    void clearLeftovers() {
        bringAppToFront();
        if (insideModule()) for (int i = 0; i < 3 && closeAnyDialog(); i++) { /* drain */ }
    }

    @AfterAll
    void leaveModule() {
        for (int i = 0; i < 10 && insideModule(); i++) {
            if (closeAnyDialog()) continue;
            if (activityIs("GalleryItemView")) {
                try { driver.navigate().back(); } catch (Exception ignored) { }
                sleep(800);
                tapTextIfShown("(?i)yes");
                continue;
            }
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(800);
        }
    }
}
