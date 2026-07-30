package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CcCreateGroupPage;
import com.zillit.automation.pages.CcLandingPage;
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
 * C&C — landing + CREATE GROUP with worst-to-worst validation, then a real
 * group creation. The created group name is shared (static) so the group-manage
 * suite can act on it.
 */
@Order(40)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CcGroupCreateTest extends BaseTest {

    private boolean onCc;
    static String createdGroup;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachCc() {
        if (onCc) { dismissPermissionPrompts(); recoverToCcLanding(); return; }
        if (landing == Landing.PROJECT_LIST) {
            ProjectListPage list = new ProjectListPage(driver);
            List<String> names = list.visibleProjectNames();
            assumeTrue(!names.isEmpty(), "No projects to enter");
            assumeTrue(list.tapProject(names.get(0)), "Could not tap a project");
        }
        assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "No project");
        assumeTrue(tapCcTab(), "Could not open the C&C tab");
        for (int i = 0; i < 3; i++) { if (!dismissPermissionPrompts()) break; sleep(500); }
        assumeTrue(new CcLandingPage(driver).isLoaded(Duration.ofSeconds(30)), "C&C landing did not load");
        onCc = true;
    }

    private CcLandingPage landing() {
        return new CcLandingPage(driver);
    }

    // ================================================================ landing

    @Test
    @Order(1)
    @DisplayName("C&C landing shows the chat/group list, filter chips, search and Create-Group FAB")
    void landingLoads() {
        CcLandingPage p = landing();
        assertTrue(p.isLoaded(Duration.ofSeconds(10)), "C&C landing (list) should load");
        assertTrue(p.isSearchVisible(), "Search should be present");
        assertTrue(p.isCreateGroupVisible(), "Create-Group FAB should be present");
        System.out.println(">>> C&C chips: " + p.chipLabels() + " | rows=" + p.rowCount());
        screenshot("cc-landing");
    }

    @Test
    @Order(2)
    @DisplayName("C&C filter chips (All/Members/Groups) switch the list")
    void filterChips() {
        CcLandingPage p = landing();
        List<String> chips = p.chipLabels();
        assumeTrue(!chips.isEmpty(), "No filter chips — skipping");
        assertTrue(chips.stream().anyMatch(c -> c.equalsIgnoreCase("Groups"))
                        || chips.stream().anyMatch(c -> c.equalsIgnoreCase("All")),
                "Chips should include Groups/All. Got: " + chips);
        p.tapChip("Groups");
        assertTrue(p.isLoaded(Duration.ofSeconds(6)), "List should stay usable after chip switch");
        p.tapChip("All");
    }

    // ============================================================ create group

    @Test
    @Order(3)
    @DisplayName("Create Group validation: empty name, <3 chars, and no members are rejected")
    void createGroupValidation() {
        CcLandingPage p = landing();
        assumeTrue(p.isCreateGroupVisible(), "Create-Group FAB missing — skipping");
        CcCreateGroupPage g = p.tapCreateGroup();
        assumeTrue(g.isLoaded(Duration.ofSeconds(12)), "Create Group did not open");
        // Empty name → blocked.
        g.tapSubmit();
        assertTrue(g.isStillOpen(), "Empty group name should be rejected (form stays open)");
        // Too-short name (<3) → blocked.
        g.setName("ab");
        g.tapSubmit();
        assertTrue(g.isStillOpen(), "A <3-char group name should be rejected");
        // Valid name but no members → blocked.
        g.setName("QA Cc Group " + (System.currentTimeMillis() % 100000));
        g.tapSubmit();
        assertTrue(g.isStillOpen(), "A group with no members should be rejected");
        screenshot("cc-create-validation");
        g.waitClosed(Duration.ofSeconds(1)); // no-op; leave form
        driver.navigate().back(); // exit create-group
        recoverToCcLanding();
    }

    @Test
    @Order(4)
    @DisplayName("Create a group with a name + members — it appears in the C&C list")
    void createGroup() {
        CcLandingPage p = landing();
        assumeTrue(p.isCreateGroupVisible(), "Create-Group FAB missing — skipping");
        CcCreateGroupPage g = p.tapCreateGroup();
        assumeTrue(g.isLoaded(Duration.ofSeconds(12)), "Create Group did not open");
        String name = "QA Cc Grp " + (System.currentTimeMillis() % 1000000);
        g.setName(name);
        assumeTrue(g.tapSelectMember(), "Select-member not present");
        assumeTrue(g.isMemberPickerOpen(Duration.ofSeconds(8)), "Member picker did not open");
        int picked = g.selectMembers(2);
        System.out.println(">>> members picked: " + picked);
        assumeTrue(g.selectedMemberCount() > 0 || picked > 0, "Could not select any member — skipping");
        assumeTrue(g.tapSubmit(), "Submit not tappable");
        // Success returns to the list (or opens the new group thread).
        boolean created = g.waitClosed(Duration.ofSeconds(20));
        System.out.println(">>> group create closed=" + created + " name=" + name);
        assertTrue(created || landing().isRowShown(name, Duration.ofSeconds(10)),
                "The created group should be saved (form closes / group appears)");
        createdGroup = name;
        screenshot("cc-group-created");
        recoverToCcLanding();
    }

    // ============================================================ helpers

    private boolean tapCcTab() {
        try {
            var nav = driver.findElement(byAppId("bottomNavigation"));
            for (var it : nav.findElements(byAppId("menuText"))) {
                String t = it.getText();
                if (t != null && (t.equalsIgnoreCase("C&C") || t.contains("C&C"))) { it.click(); return true; }
            }
        } catch (WebDriverException ignored) {
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"C&C\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private void recoverToCcLanding() {
        CcLandingPage p = new CcLandingPage(driver);
        if (p.isLoaded(Duration.ofSeconds(1))) return;
        try {
            String pkg = driver.getCurrentPackage();
            if (pkg == null || !pkg.equals(appPackage)) {
                driver.executeScript("mobile: activateApp", java.util.Map.of("appId", appPackage));
                sleep(1200);
            }
        } catch (WebDriverException ignored) {
        }
        for (int i = 0; i < 6 && !p.isLoaded(Duration.ofSeconds(1)); i++) {
            dismissPermissionPrompts();
            boolean bottomNav;
            try { bottomNav = !driver.findElements(byAppId("bottomNavigation")).isEmpty(); }
            catch (WebDriverException ex) { bottomNav = false; }
            if (bottomNav && !p.isLoaded(Duration.ofMillis(500))) {
                tapCcTab();
                sleep(1200);
                if (p.isLoaded(Duration.ofSeconds(2))) return;
            }
            try { driver.navigate().back(); } catch (WebDriverException ignored) {}
            sleep(700);
        }
        if (!p.isLoaded(Duration.ofSeconds(1))) tapCcTab();
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
    }
}
