package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CustomizationOfToolsPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.ToolsPage;
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
                assumeTrue(list.tapProject(names.get(0)), "Could not enter a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(60)), "No project");
            insideProject = true;
        }
        CustomizationOfToolsPage page = new CustomizationOfToolsPage(driver);
        if (page.isLoaded()) {
            return page;
        }
        for (int i = 0; i < 6 && !new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(1)); i++) {
            try {
                driver.navigate().back();
            } catch (Exception ignored) {
            }
            sleep(500);
        }
        assumeTrue(page.openSettingsTab(), "Settings tab not present");
        assumeTrue(page.openAdminSettings(), "Admin Settings not present");
        assumeTrue(page.openCustomizationOfTools(), "Customization of tools not present");
        assumeTrue(page.isLoaded(), "Customization of tools screen did not open");
        return page;
    }

    private ToolsPage toolsTab() {
        ToolsPage p = new ToolsPage(driver);
        for (int i = 0; i < 6 && !p.isLoaded(Duration.ofSeconds(1)); i++) {
            try {
                driver.navigate().back();
            } catch (Exception ignored) {
            }
            sleep(500);
        }
        assumeTrue(p.openToolsTab(), "Tools tab not present");
        assumeTrue(p.isLoaded(Duration.ofSeconds(15)), "Tools list did not load");
        return p;
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
        assumeTrue(!toolNames.isEmpty(), "No tools captured");
        String first = toolNames.get(0);
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
        assumeTrue(!toolNames.isEmpty(), "No tools captured");

        for (String name : toolNames) {
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
        ToolsPage tools = toolsTab();
        List<String> visible = tools.allToolNamesScrolled();
        System.out.println(">>> Tools list after disable-all: " + visible);
        assertTrue(toolNames.stream().noneMatch(visible::contains),
                "None of the disabled tools should remain visible. Still showing: "
                        + visible.stream().filter(toolNames::contains).toList());
        screenshot("customization-tools-list-empty");
    }

    @Test
    @Order(5)
    @DisplayName("Test Case 5: Re-enable every tool, then a single Update saves them all")
    void reEnableAllTools() {
        CustomizationOfToolsPage page = customization();

        for (String name : toolNames) {
            if (page.isToolEnabled(name)) continue;
            assertTrue(page.tapTool(name), "Should be able to tap " + name);
            assertTrue(page.isToolEnabled(name), name + " should be enabled immediately (no confirm needed)");
        }
        screenshot("customization-all-reenabled-pending");

        assertTrue(page.tapUpdate(), "Update should be tappable");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(15)),
                "Saving real changes should redirect to Admin Settings");
        screenshot("customization-update-reenabled-all");
    }

    @Test
    @Order(6)
    @DisplayName("Test Case 6: Every re-enabled tool is back in the Tools list")
    void reEnabledToolsBackInToolsList() {
        ToolsPage tools = toolsTab();
        List<String> visible = tools.allToolNamesScrolled();
        System.out.println(">>> Tools list after re-enable-all: " + visible);
        assertTrue(toolNames.stream().allMatch(visible::contains),
                "Every tool should be visible again. Missing: "
                        + toolNames.stream().filter(n -> !visible.contains(n)).toList());
        screenshot("customization-tools-list-restored");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
