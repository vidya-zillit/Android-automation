package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CcChatThreadPage;
import com.zillit.automation.pages.CcCreateGroupPage;
import com.zillit.automation.pages.CcLandingPage;
import com.zillit.automation.pages.ProjectListPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared navigation for the C&C suites: reach the C&C tab, recover to the landing,
 * and open (or create) a "QA Cc Grp" test group so group tests always have one.
 */
abstract class CcBaseTest extends BaseTest {

    protected static final String TEST_GROUP_PREFIX = "QA Cc Grp";
    private boolean onCc;

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

    protected CcLandingPage landing() {
        return new CcLandingPage(driver);
    }

    /** Ensures a test group exists and returns its thread page (creates one if needed). */
    protected CcChatThreadPage openOrCreateTestGroup() {
        CcLandingPage p = landing();
        p.tapChip("Groups");
        if (p.openRow(TEST_GROUP_PREFIX, Duration.ofSeconds(6))) {
            return new CcChatThreadPage(driver);
        }
        // None exists → create one.
        if (!p.isCreateGroupVisible()) return null;
        CcCreateGroupPage g = p.tapCreateGroup();
        if (!g.isLoaded(Duration.ofSeconds(12))) return null;
        g.setName(TEST_GROUP_PREFIX + " " + (System.currentTimeMillis() % 1000000));
        if (g.tapSelectMember() && g.isMemberPickerOpen(Duration.ofSeconds(8))) {
            g.selectMembers(2);
        }
        g.tapSubmit();
        g.waitClosed(Duration.ofSeconds(20));
        // Opening the group from the list (creation may land on list or thread).
        CcLandingPage p2 = landing();
        if (p2.isLoaded(Duration.ofSeconds(3))) {
            p2.tapChip("Groups");
            p2.openRow(TEST_GROUP_PREFIX, Duration.ofSeconds(8));
        }
        return new CcChatThreadPage(driver);
    }

    protected boolean tapCcTab() {
        try {
            var nav = driver.findElement(byAppId("bottomNavigation"));
            for (var it : nav.findElements(byAppId("menuText"))) {
                String t = it.getText();
                if (t != null && t.contains("C&C")) { it.click(); return true; }
            }
        } catch (WebDriverException ignored) {
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator("new UiSelector().textContains(\"C&C\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    protected void recoverToCcLanding() {
        CcLandingPage p = new CcLandingPage(driver);
        if (p.isLoaded(Duration.ofSeconds(1))) return;
        // Fast path: the Call/Contacts screens are sibling TOP-tabs of the Chat
        // landing — tapping "Chat" returns instantly, avoiding the back-loop.
        try {
            if (p.tapTopTab("Chat")) {
                sleep(1000);
                if (p.isLoaded(Duration.ofSeconds(3))) return;
            }
        } catch (WebDriverException ignored) {
        }
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

    /** Opens a 1-1 chat with the first project user (via SearchUsersPage). */
    protected CcChatThreadPage openOneToOne() {
        CcLandingPage p = landing();
        p.tapChip("Members");
        sleep(600);
        if (!p.openSearchUsers()) return null;
        if (!p.openFirstSearchUser()) return null;
        CcChatThreadPage t = new CcChatThreadPage(driver);
        return t.isLoaded(Duration.ofSeconds(15)) ? t : null;
    }

    protected void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
    }
}
