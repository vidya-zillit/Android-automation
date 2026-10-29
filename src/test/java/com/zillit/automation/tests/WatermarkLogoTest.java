package com.zillit.automation.tests;

import com.zillit.automation.core.BaseTest;
import com.zillit.automation.core.TestOrder;
import com.zillit.automation.pages.BottomNavPage;
import com.zillit.automation.pages.ProjectListPage;
import com.zillit.automation.pages.WatermarkLogoPage;
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
 * Settings → Admin Settings → "Watermark Logo of Company"
 * (header "UPDATE WATERMARK LOGO").
 *
 * <p>Runs the whole watermark lifecycle twice over, once per image source: empty
 * validations, capture from the in-app camera, save, reopen, delete with its
 * confirmation, then upload again from the in-app gallery.
 *
 * <p><b>The suite normalises its own starting state.</b> The specified flow
 * begins with no watermark, but it ends by saving one — so a second run would
 * start dirty and the two empty-state validations would be meaningless. TC1
 * therefore removes any existing watermark first, which makes every run
 * identical and repeatable.
 *
 * <p>Behaviour confirmed on the device while building this:
 * <ul>
 *   <li>camera and gallery are both IN-APP, so no system picker is involved;</li>
 *   <li>snackbars linger, and a stale one is read unless it is waited out —
 *       this masked the "Please choose an image" message on the first attempt;</li>
 *   <li>whether a watermark exists is NOT visible in the tree: {@code imageView}
 *       always renders, showing a default Zillit logo when empty, and the
 *       placeholder measured larger than a real photo — so the app is asked
 *       instead, via the delete probe.</li>
 * </ul>
 */
@Order(TestOrder.WATERMARK_LOGO)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WatermarkLogoTest extends BaseTest {

    private static final Duration MESSAGE_WAIT = Duration.ofSeconds(15);
    private static final Duration UPLOAD_WAIT = Duration.ofSeconds(60);

    private boolean insideProject;

    @Override
    protected boolean relaunchBetweenTests() {
        return false;
    }

    @Override
    protected boolean requiresLoggedOutStart() {
        return false;
    }

    /** Returns the watermark screen, navigating (or recovering) to it as needed. */
    private WatermarkLogoPage watermark() {
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
        WatermarkLogoPage page = new WatermarkLogoPage(driver);
        assumeTrue(page.ensureOnWatermark(Duration.ofSeconds(60)),
                "Could not reach Settings -> Admin Settings -> Watermark Logo of Company");
        return page;
    }

    @Test
    @Order(1)
    @DisplayName("TC1: Watermark Logo of Company opens, and any existing watermark is cleared")
    void opensAndStartsEmpty() {
        WatermarkLogoPage page = watermark();
        assertTrue(page.isLoaded(Duration.ofSeconds(20)), "UPDATE WATERMARK LOGO should be open");
        screenshot("watermark-open");

        // The flow ends by saving a watermark, so without this a rerun would begin
        // dirty and TC2/TC3 would assert nothing.
        assertTrue(page.clearWatermarkIfPresent(UPLOAD_WAIT),
                "Should be able to clear any watermark left from a previous run");
    }

    @Test
    @Order(2)
    @DisplayName("TC2: Delete with no watermark reports \"No watermark available\"")
    void deleteWithNoWatermark() {
        WatermarkLogoPage page = watermark();

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapDelete(), "Delete icon should be tappable");
        assertTrue(page.snackbarShown(WatermarkLogoPage.NO_WATERMARK, MESSAGE_WAIT),
                "Expected \"" + WatermarkLogoPage.NO_WATERMARK + "\"");
        screenshot("watermark-delete-empty");
    }

    @Test
    @Order(3)
    @DisplayName("TC3: Submit with no image reports \"Please choose an image to update watermark\"")
    void submitWithNoImage() {
        WatermarkLogoPage page = watermark();

        // Load-bearing: TC2's snackbar is still up, and reading without waiting
        // returns that instead of this one.
        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "TC2's snackbar should clear first");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(WatermarkLogoPage.CHOOSE_IMAGE, MESSAGE_WAIT),
                "Expected \"" + WatermarkLogoPage.CHOOSE_IMAGE + "\"");
        screenshot("watermark-submit-empty");
    }

    @Test
    @Order(4)
    @DisplayName("TC4: The edit icon offers Photo and Gallery")
    void editOffersBothSources() {
        WatermarkLogoPage page = watermark();

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapEdit(), "Edit icon should be tappable");
        assertTrue(page.isChooserOpen(Duration.ofSeconds(15)), "The \"Choose below\" sheet should open");

        List<String> options = page.chooserOptions();
        screenshot("watermark-chooser");
        assertTrue(options.contains("Photo"), "Chooser should offer Photo; got " + options);
        assertTrue(options.contains("Gallery"), "Chooser should offer Gallery; got " + options);
    }

    @Test
    @Order(5)
    @DisplayName("TC5: Photo opens the camera, and a capture returns to the form")
    void captureFromCamera() {
        WatermarkLogoPage page = new WatermarkLogoPage(driver);
        assumeTrue(page.isChooserOpen(Duration.ofSeconds(10)), "Chooser is not open — TC4 did not leave it");

        assertTrue(page.chooseSource("Photo"), "Photo should be selectable");
        assertTrue(page.isOnCamera(Duration.ofSeconds(20)), "The in-app camera should open");
        assertTrue(page.tapCapture(), "Capture should be tappable");

        assertTrue(page.isOnPreview(Duration.ofSeconds(20)), "A preview should follow the capture");
        assertTrue(page.tapPreviewSend(), "Send should be tappable on the preview");

        assertTrue(page.isLoaded(Duration.ofSeconds(30)),
                "Sending should return to UPDATE WATERMARK LOGO");
        assertTrue(page.imageShown(), "The captured image should be rendered on the form");
        screenshot("watermark-captured");
    }

    @Test
    @Order(6)
    @DisplayName("TC6: Submitting the capture saves it")
    void submitCapturedImage() {
        WatermarkLogoPage page = new WatermarkLogoPage(driver);
        assumeTrue(page.isLoaded(Duration.ofSeconds(10)), "Not on the watermark screen");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(WatermarkLogoPage.PROJECT_UPDATED, UPLOAD_WAIT),
                "Expected \"" + WatermarkLogoPage.PROJECT_UPDATED + "\"");
        assertTrue(page.imageShown(), "The saved watermark should still be rendered");
        screenshot("watermark-saved");
    }

    @Test
    @Order(7)
    @DisplayName("TC7: The watermark survives leaving and reopening the screen")
    void watermarkPersists() {
        WatermarkLogoPage page = new WatermarkLogoPage(driver);
        assumeTrue(page.isLoaded(Duration.ofSeconds(10)), "Not on the watermark screen");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapBack(), "Back should be tappable");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(20)), "Back should land on ADMIN SETTINGS");

        assertTrue(page.openWatermarkLogo(), "Watermark row should be tappable");
        assertTrue(page.isLoaded(Duration.ofSeconds(20)), "The watermark screen should reopen");
        assertTrue(page.imageShown(), "The saved watermark should still be shown");
        screenshot("watermark-persisted");
    }

    @Test
    @Order(8)
    @DisplayName("TC8: Delete asks for confirmation, and No keeps the watermark")
    void deleteConfirmationCancelled() {
        WatermarkLogoPage page = watermark();

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapDelete(), "Delete icon should be tappable");
        // The dialog only appears when a watermark exists, so reaching it also
        // proves TC6 really saved one.
        assertTrue(page.isConfirmDialogShown(Duration.ofSeconds(15)),
                "Deleting a saved watermark should ask for confirmation");
        assertEquals(WatermarkLogoPage.REMOVE_CONFIRM, page.dialogMessage(),
                "Confirmation wording should match the app");
        screenshot("watermark-delete-confirm");

        assertTrue(page.tapDialogNo(), "No should be tappable");
        assertTrue(page.isLoaded(Duration.ofSeconds(15)), "Declining should stay on the watermark screen");
        assertTrue(page.imageShown(), "Declining should keep the watermark");
    }

    @Test
    @Order(9)
    @DisplayName("TC9: Confirming the delete removes the watermark")
    void deleteConfirmed() {
        WatermarkLogoPage page = watermark();

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapDelete(), "Delete icon should be tappable");
        assertTrue(page.isConfirmDialogShown(Duration.ofSeconds(15)), "The dialog should appear again");
        assertTrue(page.tapDialogYes(), "Yes should be tappable");

        assertTrue(page.snackbarShown(WatermarkLogoPage.WATERMARK_DELETED, UPLOAD_WAIT),
                "Expected \"" + WatermarkLogoPage.WATERMARK_DELETED + "\"");
        screenshot("watermark-deleted");

        // The image view keeps rendering a default logo, so absence is confirmed by
        // asking the app rather than by looking at the picture.
        assertEquals(WatermarkLogoPage.DeleteProbe.NO_WATERMARK, page.probeDelete(),
                "After deleting, the app should report no watermark");
    }

    @Test
    @Order(10)
    @DisplayName("TC10: Gallery offers folders, and a photo can be picked and sent")
    void pickFromGallery() {
        WatermarkLogoPage page = watermark();

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapEdit(), "Edit icon should be tappable");
        assertTrue(page.isChooserOpen(Duration.ofSeconds(15)), "The chooser should open");
        assertTrue(page.chooseSource("Gallery"), "Gallery should be selectable");

        assertTrue(page.isOnGallery(Duration.ofSeconds(20)), "The in-app gallery should open");
        assumeTrue(page.openFirstFolder(), "No gallery folder available on this device");
        assumeTrue(page.selectFirstPhoto(), "No photo available in the first folder");

        assertTrue(page.tapGallerySend(), "The gallery send button should appear once a photo is ticked");
        assertTrue(page.isOnPreview(Duration.ofSeconds(20)), "A preview should follow the pick");
        assertTrue(page.tapPreviewSend(), "Send should be tappable on the preview");

        assertTrue(page.isLoaded(Duration.ofSeconds(30)),
                "Sending should return to UPDATE WATERMARK LOGO");
        assertTrue(page.imageShown(), "The picked image should be rendered on the form");
        screenshot("watermark-gallery-picked");
    }

    @Test
    @Order(11)
    @DisplayName("TC11: Submitting the gallery image saves it")
    void submitGalleryImage() {
        WatermarkLogoPage page = new WatermarkLogoPage(driver);
        assumeTrue(page.isLoaded(Duration.ofSeconds(10)), "Not on the watermark screen");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapSubmit(), "Submit should be tappable");
        assertTrue(page.snackbarShown(WatermarkLogoPage.PROJECT_UPDATED, UPLOAD_WAIT),
                "Expected \"" + WatermarkLogoPage.PROJECT_UPDATED + "\"");
        assertTrue(page.imageShown(), "The saved watermark should be rendered");
        screenshot("watermark-gallery-saved");
    }

    @Test
    @Order(12)
    @DisplayName("TC12: Back returns to Admin Settings")
    void finishOnAdminSettings() {
        WatermarkLogoPage page = new WatermarkLogoPage(driver);
        assumeTrue(page.isLoaded(Duration.ofSeconds(10)), "Not on the watermark screen");

        assertTrue(page.waitForSnackbarToClear(MESSAGE_WAIT), "Previous snackbar should clear first");
        assertTrue(page.tapBack(), "Back should be tappable");
        assertTrue(page.isOnAdminSettings(Duration.ofSeconds(20)),
                "Back should land on ADMIN SETTINGS");
        screenshot("watermark-finished");
    }
}
