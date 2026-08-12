package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * DOCUMENT DISTRIBUTION — the EMAIL COMPOSER ({@code emailComposerFragment} /
 * fragment_email_composer.xml), including its PREVIEW stage.
 *
 * <p>Three app behaviours (from EmailComposerFragment) shape this page:
 *
 * <ul>
 *   <li><b>Recipients commit on comma / IME-done / focus-loss</b> — typing alone leaves
 *       the text uncommitted, so {@link #addRecipient} always types a trailing comma.
 *       (The app itself had a bug where tapping Attach with uncommitted text lost the
 *       recipient — commit explicitly, never rely on focus theft.)</li>
 *   <li><b>An INVALID email still becomes a chip</b> — just styled as invalid
 *       (red background). Rejection happens at send time, not at chip time. So a case
 *       about invalid input asserts "chip appeared" + "send stays blocked", not
 *       "no chip".</li>
 *   <li><b>Send and Preview are DISABLED until a recipient exists</b>
 *       ({@code ddSend.isEnabled = … && hasRecipient}) — F-24's "send with no recipient
 *       is rejected" is assertable on the button state, no error text needed.</li>
 * </ul>
 *
 * <p>Compose ⇄ preview is ONE fragment swapping stages: {@code dd_compose_stage} vs
 * {@code dd_preview_stage}; Cancel/Preview belong to compose, Edit/Send to preview.
 * BACK inside preview only exits preview (goBack(): {@code exitPreview()} first).
 */
public class DocDistComposerPage extends BasePage {

    public DocDistComposerPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- identity ----------------

    public boolean isOpen(Duration timeout) {
        return isVisible("dd_compose_stage", timeout) || isVisible("dd_preview_stage", Duration.ofSeconds(1));
    }

    public boolean isComposeStage(Duration timeout) { return isVisible("dd_compose_stage", timeout); }
    public boolean isPreviewStage(Duration timeout) { return isVisible("dd_preview_stage", timeout); }

    // ---------------- recipients ----------------

    /**
     * Types an address into To/Cc/Bcc and commits it with a trailing comma.
     * fieldId: dd_to_input · dd_cc_input · dd_bcc_input
     */
    public boolean addRecipient(String fieldId, String email) {
        try {
            WebElement f = driver.findElement(byId(fieldId));
            f.click();
            f.sendKeys(email + ",");
            sleep(350);
            hideKeyboardIfShown();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean addTo(String email)  { return addRecipient("dd_to_input", email); }
    public boolean addCc(String email)  { return addRecipient("dd_cc_input", email); }
    public boolean addBcc(String email) { return addRecipient("dd_bcc_input", email); }

    /** Chip count inside a ChipGroup: dd_to_chips · dd_cc_chips · dd_bcc_chips. */
    public int chipCount(String groupId) {
        try {
            return driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/" + groupId + "']/*")).size();
        } catch (WebDriverException e) { return 0; }
    }

    public List<String> chipTexts(String groupId) {
        try {
            return driver.findElements(AppiumBy.xpath(
                            "//*[@resource-id='" + appPackage() + ":id/" + groupId + "']/*"))
                    .stream().map(WebElement::getText)
                    .filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    /**
     * Removes a chip by tapping its CLOSE icon — the icon is part of the Chip view, not
     * a separate node, so this taps near the chip's right edge rather than clicking the
     * element (which would only trigger the chip itself).
     */
    public boolean removeChip(String groupId, String text) {
        try {
            for (WebElement chip : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/" + groupId + "']/*"))) {
                String t = chip.getText();
                if (t != null && t.contains(text)) {
                    Rectangle r = chip.getRect();
                    int x = r.getX() + r.getWidth() - Math.min(30, r.getHeight() / 2);
                    int y = r.getY() + r.getHeight() / 2;
                    driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
                    sleep(350);
                    return true;
                }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** The Cc/Bcc toggle — reveals dd_cc_block (both Cc and Bcc live inside it). */
    public boolean toggleCcBcc()  { return click("dd_btn_ccbcc"); }
    public boolean ccBlockShown() { return isVisible("dd_cc_block", Duration.ofSeconds(3)); }

    // ---------------- subject / body ----------------

    public boolean setSubject(String text) {
        try {
            WebElement f = driver.findElement(byId("dd_subject"));
            f.click(); f.clear(); f.sendKeys(text);
            hideKeyboardIfShown();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public String subject() { return textOf("dd_subject"); }

    public boolean hasRichTextToolbar() { return isVisible("dd_rt_toolbar", Duration.ofSeconds(2)); }

    // ---------------- attachments ----------------

    public boolean openAttachFromLibrary() { return click("dd_btn_attach_library"); }
    public boolean openUploadAttachment()  { return click("dd_btn_upload"); }

    /** Empty-attachments CTAs (F-17). */
    public boolean hasEmptyUploadCta()  { return isVisible("dd_empty_upload", Duration.ofSeconds(2)); }
    public boolean hasEmptyLibraryCta() { return isVisible("dd_empty_library", Duration.ofSeconds(2)); }

    public int attachmentCount() {
        try {
            return driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/dd_attachments_recycler']"
                            + "//*[@resource-id='" + appPackage() + ":id/dd_name']")).size();
        } catch (WebDriverException e) { return 0; }
    }

    public List<String> attachmentNames() {
        try {
            return driver.findElements(AppiumBy.xpath(
                            "//*[@resource-id='" + appPackage() + ":id/dd_attachments_recycler']"
                                    + "//*[@resource-id='" + appPackage() + ":id/dd_name']"))
                    .stream().map(WebElement::getText)
                    .filter(t -> t != null && !t.isBlank()).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    /** Removes the attachment row containing this name via its dd_remove button. */
    public boolean removeAttachment(String name) {
        try {
            driver.findElement(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/dd_attachments_recycler']"
                            + "//*[.//*[@resource-id='" + appPackage() + ":id/dd_name' and contains(@text,'"
                            + name + "')]]//*[@resource-id='" + appPackage() + ":id/dd_remove']")).click();
            sleep(350);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- footer: cancel / preview / send ----------------

    /**
     * <b>Send lives on the PREVIEW stage only.</b>
     * {@code ddSend.visibility = if (preview) VISIBLE else GONE}, and Cancel/Preview are
     * the mirror image — so the compose stage has no Send button at all and the route to
     * sending is compose → Preview → Send.
     *
     * <p>Worth stating plainly because it invalidates a natural-looking assertion: "Send
     * is disabled without a recipient" cannot be tested on the compose stage, and an
     * {@code isSendEnabled()} that swallowed the not-found case reported a MISSING button
     * as a DISABLED one — a false failure that looked like an app bug.
     */
    public boolean hasSendButton() {
        try { return !driver.findElements(byId("dd_send")).isEmpty(); }
        catch (WebDriverException e) { return false; }
    }

    /** Enabled state of Send. Only meaningful on the preview stage — see {@link #hasSendButton()}. */
    public boolean isSendEnabled() {
        try { return driver.findElement(byId("dd_send")).isEnabled(); }
        catch (WebDriverException e) { return false; }
    }

    /** The compose stage's real gate: Preview is what a missing recipient disables. */
    public boolean isPreviewEnabled() {
        try { return driver.findElement(byId("dd_preview_btn")).isEnabled(); }
        catch (WebDriverException e) { return false; }
    }

    public boolean tapPreview() { return click("dd_preview_btn"); }
    public boolean tapEdit()    { return click("dd_edit"); }
    public boolean tapSend()    { return click("dd_send"); }
    public boolean tapCancel()  { return click("dd_cancel"); }

    /** Preview-stage fields (F-22). */
    public String previewTo()      { return textOf("dd_preview_to"); }
    public String previewSubject() { return textOf("dd_preview_subject"); }
    public String previewDocs()    { return textOf("dd_preview_docs"); }

    public String footerSummary()  { return textOf("dd_footer_summary"); }

    /** The send-in-flight overlay (trap 3.5 — its absence is NOT proof of a send). */
    public boolean busyOverlayShown() { return isVisible("dd_busy_overlay", Duration.ofSeconds(2)); }

    // ---------------- helpers ----------------

    private String textOf(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); sleep(350); return true; }
        catch (WebDriverException e) { return false; }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
