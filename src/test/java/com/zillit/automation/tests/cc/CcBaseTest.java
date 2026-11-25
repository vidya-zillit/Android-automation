package com.zillit.automation.tests.cc;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.pages.common.BottomNavPage;
import com.zillit.automation.pages.cc.CcChatThreadPage;
import com.zillit.automation.pages.cc.CcCreateGroupPage;
import com.zillit.automation.pages.cc.CcLandingPage;
import com.zillit.automation.pages.common.ProjectListPage;
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
@org.junit.jupiter.api.extension.ExtendWith(com.zillit.automation.core.ReasonLogger.class)
abstract class CcBaseTest extends BaseTest {

    protected static final String TEST_GROUP_PREFIX = "QA Cc Grp";
    /**
     * The OTHER test account (the Pixel user). Groups and 1-1 chats created by the web-gap
     * cases use only this person, so nothing reaches a real project member.
     */
    protected static final String PEER = peerName();

    private static String peerName() {
        String v = System.getenv("ZILLIT_CC_PEER");
        return v == null || v.isBlank() ? "Vidya Pixel" : v.trim();
    }
    private boolean onCc;

    @Override protected boolean relaunchBetweenTests() { return false; }
    @Override protected boolean requiresLoggedOutStart() { return false; }

    @BeforeEach
    void reachCc() {
        // clearBlockingPopup(): a leftover modal confirmation (leave group, delete
        // chat, end call) swallows taps, so recovery must clear it first.
        if (onCc) { dismissPermissionPrompts(); clearBlockingPopup(); recoverToCcLanding(); return; }
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
        // The other test account first; only when it is not listed fall back to the
        // historic "first two in the picker".
        if (g.tapSelectMember() && g.isMemberPickerOpen(Duration.ofSeconds(8))
                && !g.selectMemberNamed(PEER)
                && g.tapSelectMember() && g.isMemberPickerOpen(Duration.ofSeconds(8))) {
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

    /** Opens the 1-1 chat with {@link #PEER} through SearchUsersPage; null when not found. */
    protected CcChatThreadPage openPeerChat() {
        CcLandingPage p = landing();
        p.tapChip("Members");
        sleep(600);
        if (!p.openSearchUsers()) return null;
        p.searchUsers(PEER);
        if (p.searchUserNames().stream().noneMatch(n -> n.contains(PEER))) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) {}
            return null;
        }
        if (!p.openFirstSearchUser()) return null;
        CcChatThreadPage t = new CcChatThreadPage(driver);
        if (!t.isLoaded(Duration.ofSeconds(15))) return null;
        return t.title().contains(PEER) ? t : null;
    }

    /**
     * Creates a C&C group called {@code name} whose only other member is {@link #PEER}.
     * Ends on the landing. Returns null when the form could not be set up (no FAB, or
     * {@link #PEER} not offered in the picker), false when Submit left the form open,
     * true when it saved.
     */
    protected Boolean createPeerGroup(String name) {
        recoverToCcLanding();
        CcLandingPage p = landing();
        p.tapChip("All");
        sleep(500);
        if (!p.isCreateGroupVisible()) return null;
        CcCreateGroupPage g = p.tapCreateGroup();
        if (!g.isLoaded(Duration.ofSeconds(12))) return null;
        g.setName(name);
        if (!g.tapSelectMember() || !g.isMemberPickerOpen(Duration.ofSeconds(8))
                || !g.selectMemberNamed(PEER)) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) {}
            recoverToCcLanding();
            return null;
        }
        g.tapSubmit();
        boolean closed = g.waitClosed(Duration.ofSeconds(20));
        if (!closed) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) {}
        }
        recoverToCcLanding();
        return closed;
    }

    /** Polls the Groups chip (re-tapping it refetches the rooms) until {@code name} is listed. */
    protected boolean waitGroupListed(CcLandingPage p, String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!p.isLoaded(Duration.ofSeconds(2))) recoverToCcLanding();
            p.tapChip("Groups");
            sleep(1500);
            p.typeInlineSearch(name);   // the Groups chip filters inline — no scrolling needed
            // No keyboard hide here: a BACK that lands after the IME already closed leaves C&C.
            if (p.hasRowNamed(name)) return true;
            p.tapChip("All");
            sleep(800);
        }
        return false;
    }

    /**
     * Closes the soft keyboard with ESCAPE, never BACK: driver.hideKeyboard() presses BACK,
     * and when the IME is already closing that BACK leaves C&C for the Home tab.
     */
    protected void hideKeyboardIfUp() {
        try {
            if (driver.isKeyboardShown()) {
                driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                        io.appium.java_client.android.nativekey.AndroidKey.ESCAPE));
                sleep(600);
            }
        } catch (WebDriverException ignored) {
        }
    }

    /** Groups chip → open the group with this exact-ish name; null when not listed. */
    protected CcChatThreadPage openGroupNamed(String name) {
        recoverToCcLanding();
        CcLandingPage p = landing();
        p.tapChip("Groups");
        sleep(800);
        p.typeInlineSearch(name);
        if (!p.openRow(name, Duration.ofSeconds(10))) return null;
        CcChatThreadPage t = new CcChatThreadPage(driver);
        return t.isLoaded(Duration.ofSeconds(15)) ? t : null;
    }

    /** Best-effort cleanup of a group this run created (landing long-press → Delete). */
    protected void deleteGroupQuietly(String name) {
        try {
            recoverToCcLanding();
            CcLandingPage p = landing();
            p.tapChip("Groups");
            sleep(1000);
            p.typeInlineSearch(name);
            if (p.hasRowNamed(name)) {
                boolean ok = p.deleteGroupViaLongPress(name);
                System.out.println(">>> cleanup: deleted group '" + name + "' = " + ok);
            }
            p.tapChip("All");
        } catch (Exception e) {
            System.out.println(">>> cleanup of '" + name + "' failed: " + e.getMessage());
        }
    }

    protected static String stamp() {
        return String.valueOf(System.currentTimeMillis() % 1000000);
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
        if (p.isLoaded(Duration.ofSeconds(3))) return;
        // Still not on the list (seen stuck on group info after a member change): say where
        // we are, then restart the app and walk back in rather than skip every later case.
        String act;
        try { act = driver.currentActivity(); } catch (WebDriverException e) { act = "?"; }
        System.out.println(">>> recoverToCcLanding: stuck on " + act + " — relaunching the app");
        try {
            relaunchApp();
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                if (!names.isEmpty()) list.tapProject(names.get(0));
            }
            new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60));
            tapCcTab();
            sleep(1500);
            dismissPermissionPrompts();
        } catch (RuntimeException e) {
            System.out.println(">>> recoverToCcLanding relaunch failed: " + e.getMessage());
        }
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
