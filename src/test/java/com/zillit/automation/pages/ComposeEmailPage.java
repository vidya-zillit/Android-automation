package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * Compose / Reply / Forward screen (ComposeActivity, activity_compose.xml).
 * Recipients: et_to / et_cc / et_bcc (+ chip_group_to/cc/bcc, row_to/cc/bcc,
 * expanded_to/cc/bcc, collapsed_*). Subject: et_subject. Body: web_view_body.
 * Format toolbar: btn_bold/italic/underline/strikethrough/bullet_list/number_list/
 * font_size_decrease/increase/text_color/clear_format (tv_font_size). Actions:
 * btn_attach_file, btn_send, progress_send. Chip close: iv_chip_close.
 *
 * Validation (from ComposeViewModel): Send enabled only with ≥1 To recipient and
 * no attachment uploading; empty subject → "No subject" confirm dialog.
 */
public class ComposeEmailPage extends BasePage {

    public ComposeEmailPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        return isVisible("et_to", timeout) && isVisible("btn_send", Duration.ofSeconds(3));
    }

    // ---------------- recipients ----------------

    /** Types a recipient into To and commits it as a chip (comma triggers a chip). */
    public void addTo(String email) {
        addRecipient("et_to", email);
    }

    public void addCc(String email) {
        expandCcBcc();
        addRecipient("et_cc", email);
    }

    public void addBcc(String email) {
        expandCcBcc();
        addRecipient("et_bcc", email);
    }

    private void addRecipient(String fieldId, String email) {
        WebElement f = waitVisible(fieldId);
        f.click();
        f.sendKeys(email);
        // A real ENTER key press commits the chip (verified live). Appium's
        // sendKeys("\n") does NOT trigger it, nor does a comma.
        try {
            driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                    io.appium.java_client.android.nativekey.AndroidKey.ENTER));
        } catch (WebDriverException e) {
            f.sendKeys("\n");
        }
        sleep(700);
    }

    /** Cc/Bcc rows may be collapsed behind the To row — tap To's expander. */
    private void expandCcBcc() {
        if (isVisible("et_cc", Duration.ofMillis(800))) return;
        // Tapping the To row typically reveals Cc/Bcc; try known expander ids.
        for (String id : new String[]{"row_to", "expanded_to", "row_cc"}) {
            try {
                driver.findElement(byId(id)).click();
                if (isVisible("et_cc", Duration.ofSeconds(2))) return;
            } catch (WebDriverException ignored) {
            }
        }
    }

    // ---------- recipient SEARCH + select (autocomplete of project users) ----------

    /** Types a partial query into a recipient field WITHOUT committing a chip,
     *  so the search-suggestion dropdown (project users/contacts/groups) shows. */
    public void typeRecipientQuery(String fieldId, String query) {
        WebElement f = waitVisible(fieldId);
        f.click();
        f.sendKeys(query);
        sleep(1200); // let the autocomplete filter + dropdown render
    }

    /** Clears a recipient field (e.g. between search queries). */
    public void clearField(String fieldId) {
        try {
            WebElement f = driver.findElement(byId(fieldId));
            f.clear();
            sleep(300);
        } catch (WebDriverException ignored) {
        }
    }

    /** Suggestion rows in the autocomplete dropdown (row = tv_text + subTextTv). */
    public int suggestionCount() {
        try {
            return driver.findElements(byId("tv_text")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    public boolean isSuggestionShown(Duration timeout) {
        return isVisible("tv_text", timeout);
    }

    public List<String> suggestionNames() {
        try {
            return driver.findElements(byId("tv_text")).stream()
                    .map(WebElement::getText).filter(t -> t != null && !t.isBlank())
                    .toList();
        } catch (WebDriverException e) {
            return List.of();
        }
    }

    /** Taps the first suggestion in the dropdown → creates a chip from that user. */
    public boolean selectFirstSuggestion() {
        var rows = driver.findElements(byId("tv_text"));
        if (rows.isEmpty()) return false;
        try {
            rows.get(0).click();
            sleep(600);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /**
     * Search-and-select a project user: type a partial query, then pick the first
     * suggestion. The autocomplete dropdown is a PopupWindow that is NOT in the
     * accessibility tree (findElements can't see it), so we drive it by KEYS —
     * DPAD_DOWN highlights the first suggestion, ENTER/DPAD_CENTER selects it,
     * which creates a recipient chip. Returns true if a chip was created.
     */
    public boolean searchAndSelectRecipient(String fieldId, String query) {
        int before = recipientChipCount();
        WebElement f = waitVisible(fieldId);
        f.click();
        f.sendKeys(query);
        sleep(1800); // let the project-user suggestions load + dropdown render
        // The dropdown's first item sits just BELOW the field. Tap it by coordinate
        // (the PopupWindow isn't in the accessibility tree). Try a couple offsets.
        try {
            var r = f.getRect();
            int x = r.getX() + r.getWidth() / 2;
            for (int dy : new int[]{(int) (r.getHeight() * 1.6), (int) (r.getHeight() * 2.6)}) {
                int yBefore = recipientChipCount();
                tapAt(x, r.getY() + r.getHeight() + dy);
                sleep(800);
                if (recipientChipCount() > yBefore) break;
            }
        } catch (WebDriverException ignored) {
        }
        return recipientChipCount() > before;
    }

    private void tapAt(int x, int y) {
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.Collections.singletonList(seq));
    }

    public int recipientChipCount() {
        try {
            return driver.findElements(byId("tv_chip_name")).size();
        } catch (WebDriverException e) {
            return 0;
        }
    }

    public boolean removeFirstChip() {
        var closes = driver.findElements(byId("iv_chip_close"));
        if (closes.isEmpty()) return false;
        try {
            closes.get(0).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isCcVisible() {
        return isVisible("et_cc", Duration.ofSeconds(2));
    }

    public boolean isBccVisible() {
        return isVisible("et_bcc", Duration.ofSeconds(2));
    }

    // ---------------- subject / body ----------------

    public void setSubject(String subject) {
        WebElement f = waitVisible("et_subject");
        f.click();
        f.clear();
        f.sendKeys(subject);
    }

    public String subjectText() {
        try {
            return driver.findElement(byId("et_subject")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    /** The body is a contenteditable WebView (no native id) — type via the editor. */
    public void setBody(String text) {
        try {
            // The WebView exposes its contenteditable as an EditText; tap it and type.
            WebElement body = driver.findElement(By.className("android.webkit.WebView"));
            body.click();
            sleep(400);
            body.sendKeys(text);
        } catch (WebDriverException ignored) {
            // WebView typing varies by device; body is optional for most flows.
        }
    }

    /** Body is present when the rich-text toolbar (bound to the WebView) is shown. */
    public boolean isBodyPresent() {
        return isVisible("format_toolbar", Duration.ofSeconds(3))
                || !driver.findElements(By.className("android.webkit.WebView")).isEmpty();
    }

    // ---------------- rich-text format toolbar ----------------

    public boolean hasFormatToolbar() {
        return isVisible("btn_bold", Duration.ofSeconds(3))
                && isVisible("btn_italic", Duration.ofSeconds(2));
    }

    /** Taps a format button by its id suffix, e.g. "btn_bold". Returns tapped. */
    public boolean tapFormat(String btnId) {
        try {
            driver.findElement(byId(btnId)).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public String fontSize() {
        try {
            return driver.findElement(byId("tv_font_size")).getText();
        } catch (WebDriverException e) {
            return "";
        }
    }

    // ---------------- attach ----------------

    public boolean tapAttach() {
        try {
            waitVisible("btn_attach_file").click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** The attachment picker sheet is open (offers Gallery/Document/etc.). */
    public boolean isAttachPickerOpen(Duration timeout) {
        return textShown("Document", timeout) || textShown("Gallery", Duration.ofSeconds(2))
                || textShown("Audio", Duration.ofSeconds(2));
    }

    /** Picks an attachment-picker option by label (Gallery/Document/Audio/…). */
    public boolean tapAttachOption(String label) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?i)" + label + "\")")).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Picks a file in the Android system picker by (partial) name. */
    public boolean pickSystemFileByName(String namePart, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        By loc = AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + namePart + "\")");
        while (System.currentTimeMillis() < deadline) {
            try {
                var els = driver.findElements(loc);
                if (!els.isEmpty()) {
                    els.get(els.size() - 1).click();
                    return true;
                }
            } catch (WebDriverException ignored) {
            }
            sleep(500);
        }
        return false;
    }

    /**
     * After picking media the shared gallery PREVIEW opens with a Send button
     * (id sendBtn). It must be CONFIRMED for the attachment to actually upload —
     * backing out here instead pops a "Do You Really want to Exit?" dialog and
     * discards the attachment. Best-effort: documents may attach with no preview.
     */
    public boolean confirmMediaPreview(Duration timeout) {
        if (isVisible("sendBtn", timeout)) {
            try {
                driver.findElement(byId("sendBtn")).click();
                sleep(1400); // let the preview close + the attachment attach
                return true;
            } catch (WebDriverException ignored) {
            }
        }
        return false;
    }

    /**
     * Handles the exit/discard confirmation that a back-press raises ("Do You
     * Really want to Exit?" / "leave this page without saving" / "go back? Unsaved
     * changes will be lost"). confirmExit=true → leave; false → stay. Returns true
     * if such a dialog was present and dismissed.
     */
    public boolean handleExitPopup(boolean confirmExit, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        String[] buttons = confirmExit
                ? new String[]{"yes", "exit", "discard", "leave", "ok"}
                : new String[]{"no", "cancel", "stay"};
        while (System.currentTimeMillis() < deadline) {
            boolean popup = textShown("exit", Duration.ofMillis(300))
                    || textShown("leave this page", Duration.ofMillis(300))
                    || textShown("Unsaved changes", Duration.ofMillis(300))
                    || textShown("discard", Duration.ofMillis(300))
                    || textShown("go back", Duration.ofMillis(300));
            if (!popup) return false;
            for (String b : buttons) {
                try {
                    driver.findElement(AppiumBy.androidUIAutomator(
                            "new UiSelector().textMatches(\"(?i)" + b + "\")")).click();
                    sleep(600);
                    return true;
                } catch (WebDriverException ignored) {
                }
            }
        }
        return false;
    }

    public int attachmentCount() {
        try {
            return driver.findElement(byId("attachments_container"))
                    .findElements(byId("tv_file_name")).size();
        } catch (WebDriverException e) {
            return driver.findElements(byId("tv_file_name")).size();
        }
    }

    public boolean isAttachmentShown(Duration timeout) {
        return isVisible("tv_file_name", timeout);
    }

    /** Removes the first attachment (its close control is iv_download in the row). */
    public boolean removeFirstAttachment() {
        var closes = driver.findElements(byId("iv_download"));
        if (closes.isEmpty()) return false;
        try {
            closes.get(0).click();
            sleep(600);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- send ----------------

    public boolean isSendEnabled() {
        try {
            return driver.findElement(byId("btn_send")).isEnabled();
        } catch (WebDriverException e) {
            return false;
        }
    }

    public void tapSend() {
        waitVisible("btn_send").click();
    }

    /** After Send, an empty subject raises a "No subject" confirm dialog. */
    public boolean isNoSubjectDialogShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (textShown("subject", Duration.ofMillis(600))
                    && (textShown("Send", Duration.ofMillis(400)) || textShown("without", Duration.ofMillis(400)))) {
                return true;
            }
        }
        return false;
    }

    /** Confirms the "No subject" dialog (Send anyway). */
    public boolean confirmSendWithoutSubject() {
        for (String pat : new String[]{"Send", "Yes", "OK"}) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().textMatches(\"(?i)" + pat + "\")")).click();
                return true;
            } catch (WebDriverException ignored) {
            }
        }
        return false;
    }

    /** Compose closes (returns to the list) once the email is sent/queued. */
    public boolean waitClosed(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!isVisible("btn_send", Duration.ofMillis(700))) return true;
        }
        return false;
    }

    public void close() {
        try {
            driver.navigate().back();
        } catch (WebDriverException ignored) {
        }
        // Compose with content pops a "Do You Really want to Exit?" dialog on back —
        // confirm it so the automation doesn't stall waiting for a manual tap.
        handleExitPopup(true, Duration.ofSeconds(3));
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
