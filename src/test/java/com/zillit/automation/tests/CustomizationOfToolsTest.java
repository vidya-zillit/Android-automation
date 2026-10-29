package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CustomizationOfToolsPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.ToolsPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Settings → Admin Settings → "Customization of tools" — enable/disable every
 * tool as a batch (toggle each one locally, then a single Update saves them
 * all), verified against the Tools tab's own visible list.
 *
 * DESTRUCTIVE / GLOBAL-STATE: this disables EVERY tool in the project (a
 * bigger blast radius than {@code ToolsManagementTest}'s single-tool toggle),
 * so it is ordered in the 90-99 "always last" range via
 * {@link TestOrder#CUSTOMIZATION_OF_TOOLS_ALL} and ends by re-enabling every
 * tool, restoring the project to an all-enabled baseline.
 */
@Order(TestOrder.CUSTOMIZATION_OF_TOOLS_ALL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomizationOfToolsTest extends BaseTest {

    private static final String DISABLE_WARNING =
            "Disabling the tool will deactivate all associated communication and data in it";

    private boolean insideProject;
    private List<String> toolNames;
    /** Set once Test Case 5 has re-enabled everything and saved successfully. */
    private boolean toolsAlreadyRestored;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns the Customization of Tools screen, navigating (or recovering) to it as needed. */
    private CustomizationOfToolsPage customization() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects");
                // This suite switches OFF every tool in whichever project it enters,
                // so "first row in the list" is not a good enough answer — that is
                // decided by list ordering on the day. Pin it with
                //   -Dzillit.toolsProject="QA Flow 47884"
                // and fall back to the old behaviour with a loud warning.
                String wanted = System.getProperty("zillit.toolsProject", "").trim();
                String target;
                if (!wanted.isEmpty()) {
                    target = names.stream().filter(n -> n.equalsIgnoreCase(wanted)).findFirst().orElse(null);
                    if (target == null) {
                        list.search(wanted);
                        target = list.visibleProjectNames().stream()
                                .filter(n -> n.toLowerCase().contains(wanted.toLowerCase()))
                                .findFirst().orElse(null);
                    }
                    assumeTrue(target != null, "Project not found: " + wanted);
                } else {
                    target = names.get(0);
                    System.out.println(">>> WARNING: no -Dzillit.toolsProject set — this DESTRUCTIVE suite "
                            + "will disable every tool in \"" + target + "\" (just the first project listed).");
                }
                System.out.println(">>> target project: " + target);
                assumeTrue(list.tapProject(target), "Could not enter " + target);
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "No project");
            insideProject = true;
        }
        CustomizationOfToolsPage page = new CustomizationOfToolsPage(driver);
        if (page.isLoaded()) {
            return page;
        }

        // On the SETTINGS TAB: open Admin Settings first, then the screen. Without
        // this the caller falls through to the unwind below, which tries to back out
        // to the bottom nav — but we are ALREADY on it, so the loop exits at once and
        // the recovery never really happens.
        if (page.isOnSettingsTab(Duration.ofSeconds(2))) {
            assumeTrue(page.openAdminSettings(), "Admin Settings row not present on the Settings tab");
            assumeTrue(page.isOnAdminSettings(Duration.ofSeconds(10)), "Admin Settings did not open");
            assumeTrue(page.openCustomizationOfTools(), "Customization of tools row not present");
            assumeTrue(page.isLoaded(), "Customization of tools screen did not open");
            return page;
        }

        for (int i = 0; i < 6 && !new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(1)); i++) {
            try {
                driver.navigate().back();
            } catch (Exception ignored) {
            }
            sleep(500);
        }
        // Log every hop. A bare assumeTrue reports "skipped" with no indication of
        // WHICH hop gave up, and JUnit does not persist the assumption message to
        // the XML — so a skip here used to be a dead end diagnostically.
        boolean settings = page.openSettingsTab();
        System.out.println(">>> nav: openSettingsTab=" + settings);
        assumeTrue(settings, "Settings tab not present");

        boolean admin = page.openAdminSettings();
        System.out.println(">>> nav: openAdminSettings=" + admin);
        assumeTrue(admin, "Admin Settings not present");

        boolean row = page.openCustomizationOfTools();
        System.out.println(">>> nav: openCustomizationOfTools=" + row
                + " | rows on this screen=" + page.toolNames());
        assumeTrue(row, "Customization of tools not present");

        boolean loaded = page.isLoaded();
        System.out.println(">>> nav: isLoaded=" + loaded + " | rows=" + page.toolNames());
        assumeTrue(loaded, "Customization of tools screen did not open");
        return page;
    }

    /** Tool names captured by Test Case 1, or empty when that case never ran. */
    private List<String> capturedTools() {
        return toolNames == null ? List.of() : toolNames;
    }

    /**
     * Every tool name in the Tools tab, collected by scrolling the whole list.
     *
     * <p>Deliberately thorough rather than quick. {@code ToolsPage.collectScrolled()}
     * gives up after TWO swipes that add no new names, and in a GROUPED list a swipe
     * can land on a group header and add nothing — so it stops early and silently
     * returns a short list. That is how "Crew List" and later "Transportation" were
     * each reported missing while being present. This version requires FOUR quiet
     * rounds before believing it has reached the bottom, and allows many more swipes.
     */
    private List<String> collectAllToolsFromTab(ToolsPage tools) {
        // Gestures must START LOW. The top ~40% of this screen is toolbar, the
        // "Missing Permissions" banner and the search box — none of them scrollable.
        // A drag beginning up there never touches the list, so "scroll to top" did
        // nothing and collection began wherever TC5 left the list, silently losing
        // the first rows.
        for (int i = 0; i < 12; i++) {
            swipeVertical(0.55, 0.90);          // pull DOWN -> travel toward the top
        }
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        int quiet = 0;
        for (int i = 0; i < 40 && quiet < 4; i++) {
            int before = seen.size();
            seen.addAll(tools.toolNames());
            quiet = (seen.size() == before) ? quiet + 1 : 0;
            swipeVertical(0.80, 0.50);          // short push UP -> travel down a little
        }
        return new java.util.ArrayList<>(seen);
    }

    /** One vertical drag, from {@code fromPct} to {@code toPct} of screen height. */
    private void swipeVertical(double fromPct, double toPct) {
        var size = driver.manage().window().getSize();
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        int x = size.getWidth() / 2;
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                x, (int) (size.getHeight() * fromPct)));
        seq.addAction(finger.createPointerDown(0));
        // Slow drag (800ms), not a flick. A fast gesture triggers Android's fling
        // physics and the list keeps travelling after the finger lifts, skipping
        // rows between samples — the collector then never sees them.
        seq.addAction(finger.createPointerMove(Duration.ofMillis(800),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                x, (int) (size.getHeight() * toPct)));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
        sleep(500);
    }

    /** Ends the Tools-tab checks back on Admin Settings, where the flow started. */
    private void returnToAdminSettings() {
        CustomizationOfToolsPage nav = new CustomizationOfToolsPage(driver);
        assertTrue(nav.openSettingsTab(), "Settings tab should be reachable from the Tools tab");
        assertTrue(nav.openAdminSettings(), "Admin Settings should open from the Settings tab");
        assertTrue(nav.isOnAdminSettings(Duration.ofSeconds(10)),
                "Flow should finish on Admin Settings");
        screenshot("customization-back-on-admin-settings");
    }

    private ToolsPage toolsTab() {
        ToolsPage p = new ToolsPage(driver);
        // Back out to the BOTTOM NAV, not to "ToolsPage.isLoaded()".
        // ToolsPage.isLoaded() accepts textContains("ADMIN"), which the
        // "ADMIN SETTINGS" header satisfies — so gating this loop on it meant the
        // loop exited immediately while still on Admin Settings, and openToolsTab()
        // then failed there (no bottomNavigation on that screen, and its text
        // fallback is a FULL match, so "Customization of tools" != "Tools").
        // That is why the two Tools-tab verification cases skipped rather than ran.
        // Bottom Nav is an unambiguous marker; open the tab from there.
        for (int i = 0; i < 8 && !new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(1)); i++) {
            try {
                driver.navigate().back();
            } catch (Exception ignored) {
            }
            sleep(500);
        }
        assumeTrue(p.openToolsTab(), "Tools tab not present");
        assumeTrue(p.isLoaded(Duration.ofSeconds(15)), "Tools list did not load");
        // Let the list settle before anyone reads it. isLoaded() only proves the
        // tab's chrome is up ("Search tools" / group headers); the rows themselves
        // refresh from the API a beat later. Reading immediately after a tool's
        // enabled-state changed caught the list mid-refresh and reported "Crew List
        // missing" when it was in fact there — confirmed by checking the device by
        // hand while the assertion was failing.
        sleep(2000);
        return p;
    }

    /**
     * Safety net: leave the project with every tool switched back on, whatever
     * happened above.
     *
     * <p>Restoration used to live only in Test Case 5, so when that case failed
     * partway the project was simply left with tools disabled — which is exactly
     * how a shared project ended up stranded with 13 of 15 tools off. A suite
     * that disables everything owns putting it back, in teardown, not in a test
     * case that might not run.
     *
     * <p>Best-effort by design: it never throws, so it cannot turn a clean run
     * red or mask the real failure. Runs before {@code BaseTest.stopSession()}
     * (JUnit runs subclass @AfterAll first), so the driver is still alive.
     */
    @AfterAll
    void restoreAllToolsEnabled() {
        if (toolNames == null || toolNames.isEmpty()) {
            return;                       // nothing was ever captured — nothing to restore
        }
        if (toolsAlreadyRestored) {
            // Test Case 5 finished cleanly, so every tool is already on. Returning
            // here leaves the session on ADMIN SETTINGS, where Test Case 6 left it —
            // reopening Customization just to count ticks would undo that.
            System.out.println(">>> RESTORE not needed — Test Case 5 re-enabled every tool.");
            return;
        }
        try {
            CustomizationOfToolsPage page = new CustomizationOfToolsPage(driver);
            if (!page.isLoaded()) {
                for (int i = 0; i < 8 && !new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(1)); i++) {
                    try {
                        driver.navigate().back();
                    } catch (Exception ignored) {
                    }
                    sleep(500);
                }
                if (!page.openSettingsTab() || !page.openAdminSettings()
                        || !page.openCustomizationOfTools() || !page.isLoaded()) {
                    System.out.println("!!! RESTORE SKIPPED — could not reach Customization of tools. "
                            + "Re-enable the tools manually before the next run.");
                    return;
                }
            }
            int reEnabled = 0;
            for (String name : toolNames) {
                if (page.isToolEnabled(name)) {
                    continue;
                }
                if (page.tapTool(name)) {
                    // Enabling is instant, but if this tool was actually enabled
                    // already the tap opens the DISABLE confirmation — back out of
                    // it rather than switching the tool off.
                    if (page.isDisableConfirmDialogOpen(Duration.ofSeconds(2))) {
                        page.tapCancel();
                    } else {
                        reEnabled++;
                    }
                }
            }
            if (reEnabled > 0) {
                page.tapUpdate();
                page.isOnAdminSettings(Duration.ofSeconds(15));
            }
            System.out.println(">>> RESTORE: re-enabled " + reEnabled + " tool(s) of " + toolNames.size());
        } catch (Exception e) {
            System.out.println("!!! RESTORE FAILED (" + e.getClass().getSimpleName() + ": " + e.getMessage()
                    + ") — check the project's tools manually.");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test Case 1: Customization of Tools opens with the tool list, reached via Admin Settings")
    void customizationOpens() {
        CustomizationOfToolsPage page = customization();
        toolNames = page.allToolNamesScrolled();
        System.out.println(">>> tools=" + toolNames);
        assertTrue(!toolNames.isEmpty(), "The customization screen should list tools");
        screenshot("customization-of-tools-opened");
    }

    @Test
    @Order(2)
    @DisplayName("Test Case 2: Disabling a tool prompts for confirmation; Cancel leaves it enabled")
    void disableConfirmationCancel() {
        CustomizationOfToolsPage page = customization();
        assumeTrue(!capturedTools().isEmpty(), "No tools captured — Test Case 1 did not run");
        String first = capturedTools().get(0);
        assumeTrue(page.isToolEnabled(first), first + " should start enabled");

        assertTrue(page.tapTool(first), "Should be able to tap " + first);
        assertTrue(page.isDisableConfirmDialogOpen(Duration.ofSeconds(5)),
                "Expected: " + DISABLE_WARNING);
        screenshot("customization-disable-confirm-dialog");

        assertTrue(page.tapCancel(), "Cancel should be tappable");
        assertTrue(page.isToolEnabled(first), first + " should still be enabled after Cancel");
        screenshot("customization-cancel-preserves-tick");
    }

    @Test
    @Order(3)
    @DisplayName("Test Case 3: Disable every tool, then a single Update saves them all")
    void disableAllTools() {
        CustomizationOfToolsPage page = customization();
        assumeTrue(!capturedTools().isEmpty(), "No tools captured — Test Case 1 did not run");

        for (String name : capturedTools()) {
            if (!page.isToolEnabled(name)) continue;
            assertTrue(page.tapTool(name), "Should be able to tap " + name);
            assertTrue(page.isDisableConfirmDialogOpen(Duration.ofSeconds(5)), "Expected: " + DISABLE_WARNING);
            assertTrue(page.tapYes(), "Yes should be tappable for " + name);
            assertFalse(page.isToolEnabled(name), name + " tick should be removed after Yes");
        }
        screenshot("customization-all-disabled-pending");

        assertTrue(page.tapUpdate(), "Update should be tappable");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(15)),
                "Saving real changes should redirect to Admin Settings");
        screenshot("customization-update-disabled-all");
    }

    @Test
    @Order(4)
    @DisplayName("Test Case 4: Every disabled tool is gone from the Tools list")
    void disabledToolsNotInToolsList() {
        assumeTrue(!capturedTools().isEmpty(), "No tools captured — Test Case 1 did not run");
        ToolsPage tools = toolsTab();
        List<String> visible = collectAllToolsFromTab(tools);
        System.out.println(">>> Tools list after disable-all (" + visible.size() + "): " + visible);
        assertTrue(capturedTools().stream().noneMatch(visible::contains),
                "None of the disabled tools should remain visible. Still showing: "
                        + visible.stream().filter(capturedTools()::contains).toList());
        screenshot("customization-tools-list-empty");

        // Back to Admin Settings so the next case starts from the same place the
        // flow started, instead of resuming on the Tools tab.
        returnToAdminSettings();
    }

    @Test
    @Order(5)
    @DisplayName("Test Case 5: Re-enable every tool, then a single Update saves them all")
    void reEnableAllTools() {
        assumeTrue(!capturedTools().isEmpty(), "No tools captured — Test Case 1 did not run");
        CustomizationOfToolsPage page = customization();

        for (String name : capturedTools()) {
            if (page.isToolEnabled(name)) continue;
            assertTrue(page.tapTool(name), "Should be able to tap " + name);
            assertTrue(page.isToolEnabled(name), name + " should be enabled immediately (no confirm needed)");
        }
        screenshot("customization-all-reenabled-pending");

        assertTrue(page.tapUpdate(), "Update should be tappable");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(15)),
                "Saving real changes should redirect to Admin Settings");
        screenshot("customization-update-reenabled-all");
        // Everything is back on and saved, so the teardown has nothing to do and
        // must not reopen the Customization screen after the flow has finished.
        toolsAlreadyRestored = true;
    }

    @Test
    @Order(6)
    @DisplayName("Test Case 6: Every re-enabled tool is back in the Tools list")
    void reEnabledToolsBackInToolsList() {
        assumeTrue(!capturedTools().isEmpty(), "No tools captured — Test Case 1 did not run");
        ToolsPage tools = toolsTab();
        List<String> visible = collectAllToolsFromTab(tools);
        System.out.println(">>> Tools list after re-enable-all (" + visible.size() + "): " + visible);
        screenshot("customization-tools-list-restored");

        assertTrue(capturedTools().stream().allMatch(visible::contains),
                "Every tool should be visible again. Missing: "
                        + capturedTools().stream().filter(n -> !visible.contains(n)).toList());

        // Finish where the flow began. Leaving the session on the Tools tab is what
        // stopped the @AfterAll restore from finding its way back to Customization.
        returnToAdminSettings();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
