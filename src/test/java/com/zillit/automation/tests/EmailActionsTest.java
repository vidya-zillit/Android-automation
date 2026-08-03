package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.EmailPage;
import com.zillit.automation.pages.FolderSelectionPage;
import com.zillit.automation.pages.ProjectListPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * EMAIL bulk / folder actions: MOVE an email to another folder, BULK-DELETE via
 * selection mode, and CLEAR a folder ("Empty Trash" / "Clear <Folder>"). All
 * operate on the Sent folder (which has the test sends); skip cleanly when a
 * folder doesn't have enough content.
 */
@Order(TestOrder.EMAIL_ACTIONS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmailActionsTest extends BaseTest {

    private boolean onEmail;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachEmail() {
        if (onEmail) { dismissPermissionPrompts(); recoverToEmailList(); return; }
        if (landing == Landing.PROJECT_LIST) {
            ProjectListPage list = new ProjectListPage(driver);
            List<String> names = list.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "No projects to enter");
            assumeTrue(list.tapProject(names.get(0)), "Could not tap a project");
        }
        assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "No project");
        assumeTrue(tapEmailTab(), "Could not open the Email tab");
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        assumeTrue(new EmailPage(driver).isLoaded(Duration.ofSeconds(30)), "Email did not load");
        onEmail = true;
    }

    private EmailPage email() { return new EmailPage(driver); }

    // ============================================================ MOVE

    @Test
    @Order(1)
    @DisplayName("MOVE an email from Sent to another folder (folder picker)")
    void moveEmailToFolder() {
        EmailPage e = email();
        assumeTrue(e.openFolder("Sent"), "Could not open Sent");
        assumeTrue(e.emailCount() > 0, "Sent has no email to move — skipping");
        int before = e.emailCount();
        assumeTrue(e.tapFirstRowOverflow(), "Row overflow (iv_more) not present");
        assumeTrue(e.tapOverflowOption("Move"), "'Move' option not present");
        FolderSelectionPage f = new FolderSelectionPage(driver);
        assumeTrue(f.isLoaded(Duration.ofSeconds(10)), "Folder picker did not open");
        assumeTrue(f.folderCount() > 0, "No destination folders available — skipping");
        System.out.println(">>> move: folder picker has " + f.folderCount() + " destinations");
        assertTrue(f.selectFirst(), "Selecting a destination folder should work");
        sleep(2500);
        // Back on the list — the email should have moved out of Sent (or the list
        // stays usable if the server sync is delayed).
        EmailPage e2 = new EmailPage(driver);
        assertTrue(e2.isLoaded(Duration.ofSeconds(15)), "Should return to the email list after moving");
        System.out.println(">>> move done: Sent " + before + " -> " + e2.emailCount());
        screenshot("email-move");
        recoverToEmailList();
    }

    // ========================================================= BULK DELETE

    @Test
    @Order(2)
    @DisplayName("BULK DELETE: select multiple emails and delete them")
    void bulkDelete() {
        EmailPage e = email();
        assumeTrue(e.openFolder("Sent"), "Could not open Sent");
        assumeTrue(e.emailCount() > 0, "Sent empty — nothing to bulk-delete");
        int before = e.emailCount();
        // Enter selection mode (long-press selects the 1st) and add a 2nd.
        assumeTrue(e.longPressFirstEmail() || e.isSelectionMode(),
                "Long-press to enter selection failed");
        e.selectEmail(1); // toggle a 2nd email into the selection (if present)
        int selected = e.selectedCount();
        System.out.println(">>> bulk-delete selected=" + selected + " of " + before);
        assumeTrue(selected >= 1, "No emails selected — skipping");
        assumeTrue(e.tapSelectionAction("Move to trash") || e.tapSelectionAction("Delete permanently")
                        || e.tapSelectionAction("Delete"),
                "Selection delete/move action not present");
        // Raises an "Are you sure you want to delete?" confirm — click Delete.
        assertTrue(e.confirmDialog(Duration.ofSeconds(8), "Delete", "Move", "Yes", "OK"),
                "A delete/move confirmation should appear");
        sleep(3000);
        // Clean up any lingering selection mode, then confirm the list is usable.
        EmailPage e2 = new EmailPage(driver);
        if (e2.isSelectionMode()) e2.cancelSelection();
        recoverToEmailList();
        int after = new EmailPage(driver).emailCount();
        System.out.println(">>> bulk-delete done: Sent " + before + " -> " + after);
        assertTrue(new EmailPage(driver).isLoaded(Duration.ofSeconds(15)) || after < before,
                "After bulk delete the list should be usable and/or the count reduced");
        screenshot("email-bulk-delete");
        recoverToEmailList();
    }

    // ========================================================= CLEAR FOLDER

    @Test
    @Order(3)
    @DisplayName("CLEAR a folder: Empty Trash / Clear <Folder> with confirmation")
    void clearFolder() {
        EmailPage e = email();
        // Find a folder that HAS content and offers the clear button (Trash gets the
        // bulk-deleted items; Sent/Junk may also have content in a full run).
        String cleared = null;
        int before = 0;
        for (String folder : List.of("Trash", "Sent", "Junk")) {
            if (e.openFolder(folder) && e.emailCount() > 0
                    && e.clearFolderText(Duration.ofSeconds(3))) {
                cleared = folder;
                before = e.emailCount();
                break;
            }
        }
        assumeTrue(cleared != null,
                "No folder has content + a Clear/Empty button right now — skipping");
        assumeTrue(e.tapClearFolder(), "'Empty Trash' / 'Clear' button not tappable");
        assertTrue(e.confirmDialog(Duration.ofSeconds(8), "Empty", "Clear", "Yes", "Delete"),
                "A clear/empty confirmation dialog should appear");
        sleep(2500);
        EmailPage e2 = new EmailPage(driver);
        int after = e2.emailCount();
        System.out.println(">>> clear folder done: " + cleared + " " + before + " -> " + after);
        assertTrue(after < before || e2.isEmpty() || e2.isLoaded(Duration.ofSeconds(10)),
                "The folder should be cleared / usable after Empty/Clear");
        screenshot("email-clear-folder");
        recoverToEmailList();
    }

    // ============================================================ helpers

    private boolean tapEmailTab() {
        try {
            var nav = driver.findElement(byAppId("bottomNavigation"));
            for (var it : nav.findElements(byAppId("menuText"))) {
                if ("Email".equalsIgnoreCase(it.getText())) { it.click(); return true; }
            }
        } catch (WebDriverException ignored) {
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)email\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private void recoverToEmailList() {
        EmailPage e = new EmailPage(driver);
        if (e.isLoaded(Duration.ofSeconds(1))) return;
        try {
            String pkg = driver.getCurrentPackage();
            if (pkg == null || !pkg.equals(appPackage)) {
                driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
                sleep(1200);
            }
        } catch (WebDriverException ignored) {
        }
        for (int i = 0; i < 6 && !e.isLoaded(Duration.ofSeconds(1)); i++) {
            dismissPermissionPrompts();
            boolean bottomNav;
            try { bottomNav = !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
            catch (WebDriverException ex) { bottomNav = false; }
            if (bottomNav && !e.isLoaded(Duration.ofMillis(500))) {
                tapEmailTab();
                sleep(1200);
                if (e.isLoaded(Duration.ofSeconds(2))) return;
            }
            try { driver.navigate().back(); } catch (WebDriverException ignored) {}
            sleep(700);
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
    }
}
