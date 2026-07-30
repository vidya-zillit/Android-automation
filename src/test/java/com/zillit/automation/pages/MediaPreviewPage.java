package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Map;

/**
 * The app's OWN media flow (verified live on the emulator):
 *   attachment "Gallery" → FalleryActivity album list → grid → send FAB →
 *   GalleryItemView preview (caption + edit + send) → EditImageActivity
 *   (Paint / Crop / Add-Text; NO rotate — the editor has no rotate tool).
 *
 * All screens are app-rendered, so every control is a stable resource id.
 */
public class MediaPreviewPage extends BasePage {

    public MediaPreviewPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------- Fallery gallery ----------

    public boolean isGalleryOpen() {
        return isVisible("toolbarFalleryActivity", Duration.ofSeconds(15))
                || isVisible("recyclerViewBucketContent", Duration.ofSeconds(5))
                || isVisible("recyclerViewBuckets", Duration.ofSeconds(5));
    }

    /** Opens the "All Media" album if the album list is showing. */
    public void openAllMediaAlbumIfListed() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"All Media\")")).click();
            sleep(1200);
        } catch (WebDriverException ignored) {
            // Already in a grid (single-album device) — nothing to do.
        }
    }

    public int galleryThumbCount() {
        return driver.findElements(byId("imageViewPhotoMedia")).size();
    }

    public int galleryVideoCount() {
        return driver.findElements(byId("imageViewVideoMedia")).size();
    }

    /** Selects the first N video thumbnails; returns how many it tapped. */
    public int selectVideos(int n) {
        var thumbs = driver.findElements(byId("imageViewVideoMedia"));
        int count = Math.min(n, thumbs.size());
        for (int i = 0; i < count; i++) {
            thumbs.get(i).click();
            sleep(400);
        }
        return count;
    }

    /** Selects the first N photo thumbnails; returns how many it tapped. */
    public int selectPhotos(int n) {
        var thumbs = driver.findElements(byId("imageViewPhotoMedia"));
        int count = Math.min(n, thumbs.size());
        for (int i = 0; i < count; i++) {
            thumbs.get(i).click();
            sleep(400);
        }
        return count;
    }

    public boolean isSendFabVisible() {
        return isVisible("floatingButtonSendMedia", Duration.ofSeconds(5));
    }

    public void tapGallerySendFab() {
        waitVisible("floatingButtonSendMedia").click();
        sleep(1500);
    }

    // ---------- GalleryItemView preview ----------

    public boolean isPreviewOpen() {
        return isVisible("description", Duration.ofSeconds(15))
                && isVisible("sendBtn", Duration.ofSeconds(5));
    }

    public void setCaption(String text) {
        WebElement f = waitVisible("description");
        f.click();
        f.clear();
        if (!text.isEmpty()) {
            f.sendKeys(text);
        }
        hideKeyboard();
    }

    public String captionText() {
        try {
            return driver.findElement(byId("description")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    public boolean isEditButtonVisible() {
        return isVisible("edit", Duration.ofSeconds(5));
    }

    public void tapEdit() {
        waitVisible("edit").click();
        sleep(1500);
    }

    public boolean isHdToggleVisible() {
        return isVisible("hdImage", Duration.ofSeconds(3));
    }

    public boolean isDeleteVisible() {
        return isVisible("delete", Duration.ofSeconds(3));
    }

    public void tapDelete() {
        waitVisible("delete").click();
        sleep(800);
    }

    public void tapPreviewSend() {
        waitVisible("sendBtn").click();
    }

    public void tapPreviewBack() {
        waitVisible("cancel").click();
        confirmExitIfPrompted();
    }

    /** Leaving the preview raises "Are sure you want to exit?" — tap Yes. */
    public void confirmExitIfPrompted() {
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(
                            AppiumBy.androidUIAutomator("new UiSelector().textMatches(\"(?i)yes\")")))
                    .click();
            sleep(800);
        } catch (WebDriverException ignored) {
            // No exit dialog (e.g. nothing was edited) — fine.
        }
    }

    /** Waits until the preview closes (media sent / cancelled) and chat returns. */
    public boolean waitPreviewClosed(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!isVisible("sendBtn", Duration.ofSeconds(1))) return true;
        }
        return false;
    }

    // ---------- EditImageActivity ----------

    public boolean isEditorOpen() {
        return isVisible("main_image", Duration.ofSeconds(15))
                && (isVisible("btn_crop", Duration.ofSeconds(3))
                    || isVisible("save_btn", Duration.ofSeconds(3)));
    }

    public boolean hasTool(String id) {
        return isVisible(id, Duration.ofSeconds(3));
    }

    public void openPaint() { waitVisible("btn_paint").click(); sleep(1000); }
    public void openCrop()  { waitVisible("btn_crop").click();  sleep(1000); }
    public void openText()  { waitVisible("btn_text").click();  sleep(1000); }

    public boolean isPaintToolOpen() {
        return isVisible("paint_color_list", Duration.ofSeconds(5));
    }

    public boolean isCropToolOpen() {
        return isVisible("crop_panel", Duration.ofSeconds(5));
    }

    public boolean isTextToolOpen() {
        return isVisible("text_input", Duration.ofSeconds(5));
    }

    /** Draws a diagonal stroke across the image canvas (paint tool). */
    public void drawStroke() {
        Rectangle r = waitVisible("main_image").getRect();
        int x1 = r.getX() + r.getWidth() / 4;
        int y1 = r.getY() + r.getHeight() / 2;
        int x2 = r.getX() + r.getWidth() * 3 / 4;
        int y2 = r.getY() + r.getHeight() / 2 + 120;
        driver.executeScript("mobile: dragGesture", Map.of(
                "startX", x1, "startY", y1, "endX", x2, "endY", y2, "speed", 800));
        sleep(600);
    }

    /** Picks the Nth colour swatch in the paint palette (0-based). */
    public void pickPaintColor(int index) {
        var colors = driver.findElement(byId("paint_color_list"))
                .findElements(By.className("android.view.ViewGroup"));
        if (index < colors.size()) {
            colors.get(index).click();
        }
    }

    public void tapEraser() {
        waitVisible("paint_eraser").click();
    }

    public void typeEditorText(String text) {
        WebElement f = waitVisible("text_input");
        f.click();
        f.sendKeys(text);
    }

    public void tapApply() {
        waitVisible("apply").click();
        sleep(1000);
    }

    public void tapEditorBackToMenu() {
        waitVisible("back_to_main").click();
        sleep(800);
    }

    public boolean isUndoVisible() {
        return isVisible("uodo_btn", Duration.ofSeconds(4));
    }

    public void tapUndo() {
        waitVisible("uodo_btn").click();
        sleep(600);
    }

    public boolean isSaveVisible() {
        return isVisible("save_btn", Duration.ofSeconds(4));
    }

    public void tapSave() {
        waitVisible("save_btn").click();
        sleep(2000);
    }

    public void tapEditorBack() {
        waitVisible("back_btn").click();
        sleep(1000);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
