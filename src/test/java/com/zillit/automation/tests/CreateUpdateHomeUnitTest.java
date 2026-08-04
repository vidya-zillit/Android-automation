package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.CreateUpdateHomeUnitPage;
import com.zillit.automation.pages.HomeChatPage;
import com.zillit.automation.pages.ProjectListPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Settings → Admin Settings → "Create/Update Home Units.".
 *
 * A ladder walked on ONE screen instance (relaunchBetweenTests()=false), but
 * unlike the other Admin Settings suites this one genuinely LEAVES the
 * feature screen mid-ladder to check the Home tab's own top-tab strip, then
 * comes back — {@link #homeUnit()}'s existing recovery logic (back out until
 * Bottom Nav, then re-navigate) already handles the "come back" half, so only
 * the "leave" half needs its own explicit back-button/tab-tap steps.
 *
 * The name typed ("ZOO") is not what the app displays afterward — it
 * normalizes to "Zoo" (capitalize-first) — so every lookup after creation
 * uses the DISPLAYED form, not the typed one.
 */
@Order(TestOrder.CREATE_UPDATE_HOME_UNIT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CreateUpdateHomeUnitTest extends BaseTest {

    private static final String EMPTY_NAME_TOAST = "Please Enter Unit Name";
    private static final String SHORT_NAME_TOAST =
            "Home unit name is required. Please provide a valid name, it must be between 2 and 100 characters long.";
    private static final String DUPLICATE_NAME_TOAST = "Unit name already assigned";
    private static final String TYPED_NAME = "ZOO";
    private static final String DISPLAY_NAME = "Zoo";

    private boolean insideProject;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns the Create/Update Home Units list screen, navigating (or recovering) to it as needed. */
    private CreateUpdateHomeUnitPage homeUnit() {
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
        CreateUpdateHomeUnitPage page = new CreateUpdateHomeUnitPage(driver);
        if (page.isLoaded()) {
            return page;
        }
        // Recover: back out until Bottom Nav is visible again, then re-navigate.
        for (int i = 0; i < 6 && !new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(1)); i++) {
            try {
                driver.navigate().back();
            } catch (Exception ignored) {
            }
            sleep(500);
        }
        assumeTrue(page.openSettingsTab(), "Settings tab not present");
        assumeTrue(page.openAdminSettings(), "Admin Settings not present");
        assumeTrue(page.openCreateUpdateHomeUnits(), "Create/Update Home Units not present");
        assumeTrue(page.isLoaded(), "Create/Update Home Units screen did not open");
        return page;
    }

    /** Same menuText-in-bottomNavigation idiom used by every page's openSettingsTab(), for the Home tab. */
    private HomeChatPage tapHomeTab() {
        try {
            WebElement navBar = driver.findElement(AppiumBy.id(appPackage + ":id/bottomNavigation"));
            for (WebElement it : navBar.findElements(AppiumBy.id(appPackage + ":id/menuText"))) {
                if ("Home".equalsIgnoreCase(it.getText())) {
                    it.click();
                    break;
                }
            }
        } catch (WebDriverException ignored) {
        }
        return new HomeChatPage(driver);
    }

    /**
     * {@link HomeChatPage#visibleUnitNames()} fetches the chip elements, then
     * reads each one's text in a separate step; toggling a unit's enabled state
     * right before this call leaves the recTab strip mid re-render, so the
     * cached elements can go stale between those two steps. Retry the whole
     * fetch-then-read on StaleElementReferenceException rather than touching
     * that shared, pre-existing page object.
     */
    private List<String> stableUnitNames(HomeChatPage home) {
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                return home.visibleUnitNames();
            } catch (StaleElementReferenceException stale) {
                sleep(400);
            }
        }
        return home.visibleUnitNames();
    }

    @Test
    @Order(1)
    @DisplayName("Test Case 1: Verify empty Unit Name validation")
    void emptyUnitNameValidation() {
        CreateUpdateHomeUnitPage page = homeUnit();

        assertTrue(page.tapAddFab(), "+ icon should open the add-unit form");
        assertTrue(page.isAddFormOpen(), "Add-unit form should be open");

        page.tapSubmit();

        screenshot("home-unit-empty-name");
        assertTrue(page.hasToastMessage(EMPTY_NAME_TOAST, Duration.ofSeconds(5)), "Expected: " + EMPTY_NAME_TOAST);
    }

    @Test
    @Order(2)
    @DisplayName("Test Case 2: Verify Unit Name minimum length validation")
    void minimumUnitNameLength() {
        CreateUpdateHomeUnitPage page = homeUnit();

        page.setUnitName("O");
        page.tapSubmit();

        screenshot("home-unit-min-length");
        assertTrue(page.hasToastMessage(SHORT_NAME_TOAST, Duration.ofSeconds(5)), "Expected: " + SHORT_NAME_TOAST);
    }

    @Test
    @Order(3)
    @DisplayName("Test Case 3: Verify successful creation of a home unit")
    void validHomeUnitCreated() {
        CreateUpdateHomeUnitPage page = homeUnit();

        page.setUnitName(TYPED_NAME);
        page.tapSubmit();

        assertTrue(page.isUnitListed(DISPLAY_NAME, Duration.ofSeconds(15)),
                DISPLAY_NAME + " should appear in the Create/Update Home Units list");
        // The list keeps settling for a moment after the new row appears; give it
        // a minimum 3s before Test Case 4 taps the + FAB again.
        sleep(3000);
        screenshot("home-unit-created");
    }

    @Test
    @Order(4)
    @DisplayName("Test Case 4: Verify duplicate Unit Name validation")
    void duplicateUnitNameValidation() {
        CreateUpdateHomeUnitPage page = homeUnit();

        assertTrue(page.tapAddFab(), "+ icon should open the add-unit form again");
        page.setUnitName("Bulletin");
        page.tapSubmit();

        screenshot("home-unit-duplicate-name");
        assertTrue(page.hasToastMessage(DUPLICATE_NAME_TOAST, Duration.ofSeconds(5)),
                "Expected: " + DUPLICATE_NAME_TOAST);
    }

    @Test
    @Order(5)
    @DisplayName("Test Case 5: New unit is visible as its own tab on Home while enabled")
    void newUnitVisibleOnHomeWhileEnabled() {
        CreateUpdateHomeUnitPage page = homeUnit();

        // Test Case 4's duplicate-name Submit failed, so it left the add form open
        // rather than returning to the list — one extra back-tap closes that first.
        assertTrue(page.tapBackButton(), "Back button should be tappable from the add-unit form");
        assertFalse(page.isAddFormOpen(), "Back button should close the add-unit form, returning to the units list");

        assertTrue(page.tapBackButton(), "Back button should be tappable from the units list");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(10)), "Back button should return to Admin Settings");

        assertTrue(page.tapBackButton(), "Back button should be tappable from Admin Settings");
        assertTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(10)),
                "Back button should return to the project's Bottom Navigation");

        HomeChatPage home = tapHomeTab();
        sleep(5000);
        assertTrue(home.isLoaded(), "Home tab should load");
        List<String> unitNames = stableUnitNames(home);
        assertTrue(unitNames.stream().anyMatch(n -> n.equalsIgnoreCase(DISPLAY_NAME)),
                DISPLAY_NAME + " should be visible on Home while enabled. Visible tabs: " + unitNames);
        screenshot("home-unit-visible-on-home");
    }

    @Test
    @Order(6)
    @DisplayName("Test Case 6: Search finds the unit, and disabling it requires confirmation")
    void searchAndDisableUnit() {
        CreateUpdateHomeUnitPage page = homeUnit();

        page.searchUnit(TYPED_NAME);
        assertTrue(page.isUnitListed(DISPLAY_NAME, Duration.ofSeconds(10)),
                DISPLAY_NAME + " should appear after searching");
        screenshot("home-unit-search");

        assertTrue(page.toggleUnit(DISPLAY_NAME), "Toggle should be tappable");
        assertTrue(page.isDisableConfirmDialogOpen(Duration.ofSeconds(5)), "Disable confirmation dialog should open");
        assertTrue(page.confirmDisableYes(), "Yes should be tappable on the disable confirmation");
        assertFalse(page.waitUnitEnabled(DISPLAY_NAME, false, Duration.ofSeconds(5)),
                DISPLAY_NAME + " should now be disabled");
    }

    @Test
    @Order(7)
    @DisplayName("Test Case 7: Disabled unit is no longer visible on Home")
    void disabledUnitNotVisibleOnHome() {
        CreateUpdateHomeUnitPage page = homeUnit();

        assertTrue(page.tapBackButton(), "Back button should be tappable from the units list");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(10)), "Back button should return to Admin Settings");

        assertTrue(page.tapBackButton(), "Back button should be tappable from Admin Settings");
        assertTrue(new BottomNavPage(driver).waitUntilLoaded(Duration.ofSeconds(10)),
                "Back button should return to the project's Bottom Navigation");

        HomeChatPage home = tapHomeTab();
        assertTrue(home.isLoaded(), "Home tab should load");
        List<String> unitNames = stableUnitNames(home);
        assertTrue(unitNames.stream().noneMatch(n -> n.equalsIgnoreCase(DISPLAY_NAME)),
                DISPLAY_NAME + " should NOT be visible on Home while disabled. Visible tabs: " + unitNames);
        screenshot("home-unit-not-visible-on-home");
    }

    @Test
    @Order(8)
    @DisplayName("Test Case 8: Re-enabling and deleting the unit")
    void reEnableThenDeleteUnit() {
        CreateUpdateHomeUnitPage page = homeUnit();

        assertTrue(page.toggleUnit(DISPLAY_NAME), "Toggle should be tappable to re-enable");
        assertTrue(page.waitUnitEnabled(DISPLAY_NAME, true, Duration.ofSeconds(5)),
                DISPLAY_NAME + " should be enabled again (no confirmation needed)");
        screenshot("home-unit-re-enabled");

        assertTrue(page.tapDeleteIcon(DISPLAY_NAME), "Delete icon should open the confirmation snackbar");
        assertTrue(page.confirmDeleteYes(), "Yes action on the confirmation snackbar should be tappable");
        assertTrue(page.isUnitRemoved(DISPLAY_NAME, Duration.ofSeconds(10)),
                DISPLAY_NAME + " should no longer appear in the Create/Update Home Units list");
        screenshot("home-unit-deleted");
    }

    @Test
    @Order(9)
    @DisplayName("Test Case 9: Back button from top returns to Admin Settings")
    void backButtonReturnsToAdminSettings() {
        CreateUpdateHomeUnitPage page = homeUnit();

        assertTrue(page.tapBackButton(), "Back button should be tappable from the units list");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(10)), "Back button should return to Admin Settings");
        screenshot("home-unit-back-to-admin-settings");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
