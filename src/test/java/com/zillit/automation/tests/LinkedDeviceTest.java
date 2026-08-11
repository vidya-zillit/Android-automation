package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.LinkedDevicePage;
import com.zillit.automation.pages.ProjectListPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Settings → "Connect to Zillit from your laptop" (header "LINKED DEVICE").
 *
 * <p>Read-only: it checks the linking instructions are present, prints them, and
 * confirms the Link Device button opens the QR scanner with a live camera. It
 * never actually links a device, since that needs a QR code from a browser.
 *
 * <p>The instruction paragraph is asserted as FRAGMENTS rather than one exact
 * string. The app writes "Step 1 :" with a space before the colon, says "link
 * YOUR mobile device", and embeds newlines — so an exact comparison against the
 * written steps fails on formatting while the screen is perfectly correct.
 */
@Order(TestOrder.LINKED_DEVICE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LinkedDeviceTest extends BaseTest {

    private static final Duration WAIT = Duration.ofSeconds(20);
    private static final Duration LOAD_WAIT = Duration.ofSeconds(60);

    private boolean insideProject;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    private LinkedDevicePage linkedDevice() {
        if (!insideProject) {
            if (landing == Landing.PROJECT_LIST) {
                ProjectListPage list = new ProjectListPage(driver);
                List<String> names = list.visibleProjectNames();
                assumeTrue(!names.isEmpty(), "No projects");
                assumeTrue(list.tapProject(names.get(0)), "Could not enter a project");
            }
            assumeTrue(new BottomNavPage(driver).waitUntilLoaded(LOAD_WAIT), "No project");
            insideProject = true;
        }
        LinkedDevicePage page = new LinkedDevicePage(driver);
        assumeTrue(page.ensureOnLinkedDevice(LOAD_WAIT),
                "Could not reach Settings -> " + LinkedDevicePage.SETTINGS_ROW);
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("TC1: The screen opens and shows every linking instruction")
    void showsInstructions() {
        LinkedDevicePage page = linkedDevice();
        assertTrue(page.isLoaded(WAIT), "LINKED DEVICE should be open");
        screenshot("linked-device-open");

        String text = page.instructions();
        System.out.println(">>> linking instructions:");
        System.out.println(text);

        assertTrue(!text.isBlank(), "The instruction paragraph should not be empty");
        for (String fragment : LinkedDevicePage.INSTRUCTION_FRAGMENTS) {
            assertTrue(text.contains(fragment),
                    "The instructions should mention \"" + fragment + "\"; got: " + text);
        }
    }

    @Test
    @Order(2)
    @DisplayName("TC2: The Link Device button is visible")
    void linkDeviceButtonVisible() {
        LinkedDevicePage page = linkedDevice();

        assertTrue(page.isLinkDeviceButtonShown(WAIT), "The Link Device button should be visible");
        assertEquals("Link Device", page.linkDeviceButtonLabel(),
                "The button should be labelled Link Device");
        screenshot("linked-device-button");
    }

    @Test
    @Order(3)
    @DisplayName("TC3: Link Device opens the QR scanner with the camera running")
    void linkDeviceOpensScanner() {
        LinkedDevicePage page = linkedDevice();

        assertTrue(page.tapLinkDevice(), "Link Device should be tappable");
        // Keyed on the camera preview itself, not just a screen change.
        assertTrue(page.isOnScanner(Duration.ofSeconds(30)),
                "The QR scanner should open with a camera preview");
        screenshot("linked-device-scanner");

        System.out.println(">>> scanner header: " + page.scannerHeader());
        System.out.println(">>> scanner subtitle: " + page.scannerSubtitle());
        assertEquals("SCAN ZILLIT QR CODE", page.scannerHeader(),
                "The scanner header should read SCAN ZILLIT QR CODE");
    }

    @Test
    @Order(4)
    @DisplayName("TC4: Back leaves the scanner, and back again returns to the Settings tab")
    void backOutToSettings() {
        LinkedDevicePage page = new LinkedDevicePage(driver);
        assumeTrue(page.isOnScanner(Duration.ofSeconds(10)),
                "Not on the scanner — TC3 did not leave it open");

        assertTrue(page.tapBack(), "Back should be tappable on the scanner");
        assertTrue(page.isLoaded(WAIT), "Back should return to LINKED DEVICE");
        screenshot("linked-device-back-from-scanner");

        assertTrue(page.tapBack(), "Back should be tappable on LINKED DEVICE");
        assertTrue(page.isOnSettingsTab(WAIT), "Back should return to the Settings tab");
        screenshot("linked-device-finished");
    }
}
