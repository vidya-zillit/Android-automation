package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CalendarPage;
import com.zillit.automation.pages.EmailContactsPage;
import com.zillit.automation.pages.EmailPage;
import com.zillit.automation.pages.EmailSettingsPage;
import com.zillit.automation.pages.EmailSignaturesPage;
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
 * EMAIL drawer SECONDARY screens — everything reachable from the folder drawer
 * beyond the inbox: the in-drawer Calendar and Contacts, Signatures (via
 * Settings), folder management (create / rename / delete a custom folder), and
 * folder-specific actions (Empty Trash, Junk). Chains on the email module.
 *
 * Run: ./run-and-report.sh --email  (this class runs alongside EmailTest)
 */
@Order(31)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmailDrawerTest extends BaseTest {

    private boolean onEmail;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachEmail() {
        if (onEmail) {
            dismissPermissionPrompts();
            recoverToEmailList();
            return;
        }
        if (landing == Landing.PROJECT_LIST) {
            ProjectListPage list = new ProjectListPage(driver);
            List<String> names = list.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "No projects to enter");
            assumeTrue(list.tapProject(names.get(0)), "Could not tap a project");
        }
        assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)),
                "Could not enter a project");
        assumeTrue(tapEmailTab(), "Could not open the Email tab");
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        assumeTrue(new EmailPage(driver).isLoaded(Duration.ofSeconds(30)), "Email did not load");
        onEmail = true;
    }

    private EmailPage email() {
        return new EmailPage(driver);
    }

    // ============================================================ Calendar

    @Test
    @Order(1)
    @DisplayName("Drawer → Calendar opens the email calendar")
    void drawerCalendar() {
        EmailPage e = email();
        assumeTrue(e.openDrawer(), "Could not open the drawer");
        assumeTrue(e.tapDrawerCalendar(), "Calendar nav item not present");
        boolean opened = new CalendarPage(driver).isCalendarLoaded()
                || new EmailPage(driver).textShown("Calendar", Duration.ofSeconds(8));
        assertTrue(opened, "The email Calendar screen should open");
        screenshot("email-calendar");
        driver.navigate().back();
        recoverToEmailList();
    }

    // ============================================================ Contacts

    @Test
    @Order(2)
    @DisplayName("Drawer → Contacts opens the contacts list")
    void drawerContacts() {
        EmailPage e = email();
        assumeTrue(e.openDrawer(), "Could not open the drawer");
        assumeTrue(e.tapDrawerContacts(), "Contacts nav item not present");
        EmailContactsPage c = new EmailContactsPage(driver);
        assertTrue(c.isLoaded(Duration.ofSeconds(15)), "Contacts list should open");
        System.out.println(">>> contacts: count=" + c.contactCount() + " empty=" + c.isEmpty());
        screenshot("email-contacts");
        driver.navigate().back();
        recoverToEmailList();
    }

    @Test
    @Order(3)
    @DisplayName("Add contact: email required (save blocked), then a valid contact saves")
    void addContact() {
        EmailContactsPage c = openContacts();
        assumeTrue(c.tapAdd(), "Add-contact FAB not present");
        assumeTrue(c.isFormLoaded(Duration.ofSeconds(10)), "Add-contact form did not open");
        // Validation: no email → save blocked (form stays open).
        c.tapSave();
        assertTrue(c.isFormStillOpen(), "Saving with no email should be blocked");
        c.setFirstName("QA Contact " + (System.currentTimeMillis() % 100000));
        c.setEmail("qa.contact@zillit.com");
        c.tapSave();
        assertTrue(c.waitFormClosed(Duration.ofSeconds(20)) || c.isLoaded(Duration.ofSeconds(5)),
                "A valid contact (with email) should save and return to the list");
        screenshot("email-contact-added");
        recoverToEmailList();
    }

    @Test
    @Order(4)
    @DisplayName("Edit contact opens the prefilled form")
    void editContact() {
        EmailContactsPage c = openContacts();
        assumeTrue(c.contactCount() > 0, "No contact to edit — skipping");
        assumeTrue(c.tapEditFirst(), "Edit button not present");
        assertTrue(c.isFormLoaded(Duration.ofSeconds(10)), "Edit form should open prefilled");
        screenshot("email-contact-edit");
        driver.navigate().back();
        recoverToEmailList();
    }

    @Test
    @Order(5)
    @DisplayName("Delete contact (confirm dialog)")
    void deleteContact() {
        EmailContactsPage c = openContacts();
        assumeTrue(c.contactCount() > 0, "No contact to delete — skipping");
        int before = c.contactCount();
        assumeTrue(c.tapDeleteFirst(), "Delete button not present");
        assertTrue(c.confirmDelete(Duration.ofSeconds(8)),
                "The 'Delete Contact' dialog should appear and be confirmable");
        sleep(1500);
        System.out.println(">>> contact delete: before=" + before + " after=" + new EmailContactsPage(driver).contactCount());
        screenshot("email-contact-deleted");
        driver.navigate().back();
        recoverToEmailList();
    }

    // ============================================================ Signatures

    @Test
    @Order(6)
    @DisplayName("Settings → Signatures opens the signatures list")
    void drawerSignatures() {
        EmailSignaturesPage s = openSignatures();
        assertTrue(s.isLoaded(Duration.ofSeconds(15)), "Signatures list should open");
        System.out.println(">>> signatures: count=" + s.signatureCount() + " addVisible=" + s.isAddVisible());
        screenshot("email-signatures");
        backTo(2);
        recoverToEmailList();
    }

    @Test
    @Order(7)
    @DisplayName("Add signature: name required (save blocked), then a named signature saves")
    void addSignature() {
        EmailSignaturesPage s = openSignatures();
        assumeTrue(s.isLoaded(Duration.ofSeconds(10)), "Signatures did not open");
        assumeTrue(s.isAddVisible(), "Add disabled (one signature already exists) — skipping");
        assumeTrue(s.tapAdd(), "Add-signature FAB not present");
        assumeTrue(s.isEditorLoaded(Duration.ofSeconds(10)), "Signature editor did not open");
        // Validation: empty name → save blocked.
        s.tapSave();
        assertTrue(s.isEditorStillOpen(), "Saving a nameless signature should be blocked");
        s.setName("QA Sig " + (System.currentTimeMillis() % 100000));
        s.setContent("Best regards, QA");
        s.tapSave();
        assertTrue(s.waitEditorClosed(Duration.ofSeconds(20)) || s.isLoaded(Duration.ofSeconds(5)),
                "A named signature should save");
        screenshot("email-signature-added");
        backTo(2);
        recoverToEmailList();
    }

    @Test
    @Order(8)
    @DisplayName("Delete signature (confirm dialog)")
    void deleteSignature() {
        EmailSignaturesPage s = openSignatures();
        assumeTrue(s.signatureCount() > 0, "No signature to delete — skipping");
        assumeTrue(s.tapDeleteFirst(), "Delete button not present");
        assertTrue(s.confirmDelete(Duration.ofSeconds(8)),
                "The 'Delete Signature' dialog should appear and be confirmable");
        sleep(1500);
        screenshot("email-signature-deleted");
        backTo(2);
        recoverToEmailList();
    }

    // ============================================================ Folders

    @Test
    @Order(9)
    @DisplayName("Create a custom folder from the drawer")
    void createFolder() {
        EmailPage e = email();
        assumeTrue(e.openDrawer(), "Could not open the drawer");
        assumeTrue(e.tapCreateFolder(), "Create-folder (+) not present");
        assumeTrue(e.isFolderDialogOpen(Duration.ofSeconds(8)), "Create Folder dialog did not open");
        createdFolder = "QAFolder" + (System.currentTimeMillis() % 100000);
        e.typeFolderName(createdFolder);
        assertTrue(e.tapDialogButton("Create"), "Create button should be present");
        sleep(2000);
        // Reopen the drawer and verify the folder now appears.
        EmailPage e2 = new EmailPage(driver);
        e2.openDrawer();
        assertTrue(e2.folderNames().stream().anyMatch(f -> f.equalsIgnoreCase(createdFolder))
                        || e2.textShown(createdFolder, Duration.ofSeconds(6)),
                "The created folder '" + createdFolder + "' should appear in the drawer");
        System.out.println(">>> created folder: " + createdFolder);
        screenshot("email-folder-created");
        driver.navigate().back();
        recoverToEmailList();
    }

    private static String createdFolder;

    @Test
    @Order(10)
    @DisplayName("Rename the custom folder")
    void renameFolder() {
        assumeTrue(createdFolder != null, "No folder was created — skipping");
        EmailPage e = email();
        assumeTrue(e.openDrawer(), "Could not open the drawer");
        assumeTrue(e.tapFolderOverflow(createdFolder), "Folder overflow (btn_more) not present");
        assumeTrue(e.tapDialogButton("Rename"), "Rename option not present");
        assumeTrue(e.isFolderDialogOpen(Duration.ofSeconds(6)), "Rename dialog did not open");
        String renamed = createdFolder + "R";
        e.typeFolderName(renamed);
        assertTrue(e.tapDialogButton("Rename"), "Rename confirm should be present");
        sleep(2000);
        createdFolder = renamed;
        screenshot("email-folder-renamed");
        driver.navigate().back();
        recoverToEmailList();
    }

    @Test
    @Order(11)
    @DisplayName("Delete the custom folder (confirm dialog)")
    void deleteFolder() {
        assumeTrue(createdFolder != null, "No folder to delete — skipping");
        EmailPage e = email();
        assumeTrue(e.openDrawer(), "Could not open the drawer");
        assumeTrue(e.tapFolderOverflow(createdFolder), "Folder overflow not present");
        assumeTrue(e.tapDialogButton("Delete"), "Delete option not present");
        assertTrue(e.tapDialogButton("Delete"),
                "The 'Delete Folder' confirm should appear");
        sleep(2000);
        screenshot("email-folder-deleted");
        driver.navigate().back();
        recoverToEmailList();
    }

    // ============================================================ Trash / Junk

    @Test
    @Order(12)
    @DisplayName("Trash folder shows the 'Empty Trash' action")
    void trashFolder() {
        EmailPage e = email();
        assumeTrue(e.openFolder("Trash"), "Could not open the Trash folder");
        // Empty-Trash button appears only when Trash has content; assert the
        // folder opened and (if any content) the clear button is present.
        boolean ok = e.isLoaded(Duration.ofSeconds(10));
        if (e.emailCount() > 0) {
            ok = ok && e.clearFolderText(Duration.ofSeconds(5));
        }
        assertTrue(ok, "Trash folder should open (with Empty Trash when it has content)");
        System.out.println(">>> trash: count=" + e.emailCount());
        screenshot("email-trash");
        recoverToEmailList();
    }

    @Test
    @Order(13)
    @DisplayName("Junk folder opens")
    void junkFolder() {
        EmailPage e = email();
        assumeTrue(e.openFolder("Junk"), "Could not open the Junk folder");
        assertTrue(e.isLoaded(Duration.ofSeconds(10)), "Junk folder should open");
        System.out.println(">>> junk: count=" + e.emailCount());
        screenshot("email-junk");
        recoverToEmailList();
    }

    // ============================================================ helpers

    private EmailContactsPage openContacts() {
        EmailPage e = email();
        assumeTrue(e.openDrawer(), "Could not open the drawer");
        assumeTrue(e.tapDrawerContacts(), "Contacts nav item not present");
        EmailContactsPage c = new EmailContactsPage(driver);
        assumeTrue(c.isLoaded(Duration.ofSeconds(15)), "Contacts did not open");
        return c;
    }

    private EmailSignaturesPage openSignatures() {
        EmailPage e = email();
        assumeTrue(e.openDrawer(), "Could not open the drawer");
        assumeTrue(e.tapDrawerSettings(), "Settings nav item not present");
        EmailSettingsPage set = new EmailSettingsPage(driver);
        assumeTrue(set.isLoaded(Duration.ofSeconds(15)), "Settings did not open");
        assumeTrue(set.tapSignatures(), "Signatures card not present");
        return new EmailSignaturesPage(driver);
    }

    private boolean tapEmailTab() {
        try {
            var nav = driver.findElement(byAppId("bottomNavigation"));
            var items = nav.findElements(byAppId("menuText"));
            for (var it : items) {
                if ("Email".equalsIgnoreCase(it.getText())) {
                    it.click();
                    return true;
                }
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

    private void backTo(int times) {
        for (int i = 0; i < times; i++) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) {}
            sleep(600);
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
            if (isOnHome()) {
                tapEmailTab();
                sleep(1200);
                if (e.isLoaded(Duration.ofSeconds(2))) return;
            }
            try { driver.navigate().back(); } catch (WebDriverException ignored) {}
            sleep(700);
        }
        if (!e.isLoaded(Duration.ofSeconds(1)) && isOnHome()) {
            tapEmailTab();
            sleep(1200);
        }
    }

    private boolean isOnHome() {
        boolean bottomNav;
        try {
            bottomNav = !driver.findElements(byAppId("bottomNavigation")).isEmpty();
        } catch (WebDriverException ex) {
            bottomNav = false;
        }
        return bottomNav && !new EmailPage(driver).isLoaded(Duration.ofMillis(500));
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
