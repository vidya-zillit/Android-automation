package com.zillit.automation.pages.esignature;

import com.zillit.automation.pages.common.VisiblePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The envelope editor: {@code DocuSignComposeFragment} ("Prepare" — document, title, signers,
 * CC, settings, "Next: Place Fields") and {@code DocuSignPlacementFragment} (PDF pager, field
 * toolbar, "Save as Draft" / "Send").
 *
 * <p>Read from the source:
 * <ul>
 *   <li>The document row {@code docPickerRow} opens the host's media picker
 *       ({@code openMediaPicker(PICKER_ITEM_PDF_DOCUMENT)}): a "Choose below" sheet with one
 *       "Document" tile, then the Android system picker. The picked name replaces
 *       "Upload PDF Document" in {@code tvPickedFile}.</li>
 *   <li>"Next: Place Fields" never disables: a missing piece shows {@code tvNextHint} —
 *       "Upload a PDF to continue." → "Add a title to continue." → "Add at least one signer to
 *       continue.", in that order.</li>
 *   <li>Signers come from "Inside project" ({@code btnSelectSigners}) — the
 *       {@code DocuSignPickUsersBottomSheet}: rows {@code tvName}/{@code tvEmail}/{@code cb},
 *       "Done" — or the "I need to sign this document" switch.</li>
 *   <li>Placement: the bottom toolbar {@code fieldChipRow} holds id-less chips ("Signature",
 *       "Initial", …). Tap a chip to arm it, then tap the page → the field drops for the
 *       active signer and its editor opens. "Send" with no signature field toasts
 *       "Place at least one signature field before sending — …". "Save as Draft" has no field
 *       requirement and pops back to the list.</li>
 * </ul>
 */
public class ESignComposePage extends VisiblePage {

    public static final String UPLOAD_PDF = "Upload PDF Document";
    public static final String UPLOADING = "Uploading…";
    public static final String NEXT = "Next: Place Fields";
    public static final String HINT_NEED_PDF = "Upload a PDF to continue.";
    public static final String HINT_NEED_TITLE = "Add a title to continue.";
    public static final String HINT_NEED_SIGNER = "Add at least one signer to continue.";
    public static final String SELF_SIGNER = "I need to sign this document";
    public static final String PICK_SIGNERS_TITLE = "Select Signers";
    public static final String SEND_NO_FIELDS =
            "Place at least one signature field before sending";
    public static final String SEND_CONFIRM_TITLE = "Send this document?";
    public static final String SAVE_DRAFT = "Save as Draft";

    public ESignComposePage(AndroidDriver driver) {
        super(driver);
    }

    // ─────────────────────────── Prepare ───────────────────────────

    public boolean isLoaded(Duration timeout) {
        return isVisible("docPickerRow", timeout) || isVisible("btnNext", Duration.ofSeconds(1));
    }

    public String pickedFileLabel() { return firstText("tvPickedFile"); }

    public boolean hasDocument() {
        String t = pickedFileLabel();
        return t != null && !t.isEmpty() && !UPLOAD_PDF.equals(t) && !UPLOADING.equals(t);
    }

    public boolean tapDocumentRow() { return tapFirst("docPickerRow"); }

    /** True while the host's "Choose below" sheet (or any picker tile) is up. */
    public boolean pickerSheetShown(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!elementsById("tvLabel").isEmpty()) return true;
            pause(300);
        }
        return false;
    }

    public List<String> pickerOptions() {
        List<String> out = new ArrayList<>();
        for (WebElement e : elementsById("tvLabel")) out.add(text(e));
        return out;
    }

    public boolean tapPickerOption(String label) {
        for (WebElement e : elementsById("tvLabel")) {
            if (label.equalsIgnoreCase(text(e))) {
                try { e.click(); pause(2000); return true; } catch (WebDriverException ignored) { }
            }
        }
        return false;
    }

    /**
     * Full upload: document row → "Document" → system picker → back in the app with the file
     * named in {@code tvPickedFile}. Accepts the storage-permission prompt on the way.
     */
    public boolean uploadPdf(String namePart, Duration timeout) {
        if (!tapDocumentRow()) return false;
        if (pickerSheetShown(Duration.ofSeconds(6))) {
            if (!tapPickerOption("Document")) {
                List<String> o = pickerOptions();
                if (o.isEmpty() || !tapPickerOption(o.get(0))) return false;
            }
        }
        allowPermissionIfAsked();
        waitForPicker(Duration.ofSeconds(10));
        boolean picked = pickViaDeviceStorage(namePart);
        if (!picked) {
            System.out.println(">>> device-storage route did not reach " + namePart + "; trying the shared picker");
            picked = pickSystemFile(namePart, timeout);
        }
        if (!picked) return false;
        confirmPreviewIfShown();
        return waitDocument(Duration.ofSeconds(30));
    }

    private String currentPackage() {
        try { return String.valueOf(driver.getCurrentPackage()); } catch (WebDriverException e) { return ""; }
    }

    private boolean inDocumentsUi() { return currentPackage().contains("documentsui"); }

    private void waitForPicker(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline && !inDocumentsUi()) pause(500);
    }

    /**
     * The system picker opens on Downloads (or Recent). The shared
     * {@code SystemFilePickerPage.openStorageDocuments} finds the device root "by elimination"
     * — the one drawer label that is not a known collection — which on the Pixel picks
     * "Bug reports" / "Adobe Scan" / a Drive account instead of "Pixel 7". The device root is
     * the one whose summary reads "… free" (its storage space), so take that, then Documents,
     * then the file. (Updated: the root is found by the device model — see {@link #deviceRoot}.)
     */
    private boolean pickViaDeviceStorage(String name) {
        if (!inDocumentsUi()) return false;
        try {
            driver.findElement(AppiumBy.accessibilityId("Show roots")).click();
            pause(1500);
            WebElement root = deviceRoot();
            if (root == null) {
                driver.navigate().back();   // close the drawer, leave the picker for the fallback
                pause(800);
                return false;
            }
            root.click();
            pause(2500);
            List<WebElement> docs = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"android:id/title\").text(\"Documents\")"));
            if (docs.isEmpty()) return false;
            docs.get(0).click();
            pause(2500);
            for (int i = 0; i < 8; i++) {
                List<WebElement> f = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceId(\"android:id/title\").text(\"" + name + "\")"));
                if (!f.isEmpty()) {
                    f.get(0).click();
                    pause(3000);
                    return !inDocumentsUi();
                }
                var size = driver.manage().window().getSize();
                swipeVertical(size.getWidth() / 2, (int) (size.getHeight() * 0.75), (int) (size.getHeight() * 0.35));
            }
        } catch (WebDriverException e) {
            System.out.println(">>> device-storage route failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * The device's own storage root in the open roots drawer. Its title is the device model
     * ("Pixel 7", "sdk_gphone64_arm64" — {@code mobile: deviceInfo}); some builds also give it
     * a "… GB free" summary, used as the fallback.
     */
    private WebElement deviceRoot() {
        String model = null;
        try {
            Object info = driver.executeScript("mobile: deviceInfo");
            if (info instanceof java.util.Map<?, ?> m && m.get("model") != null) model = String.valueOf(m.get("model"));
        } catch (WebDriverException ignored) { }
        try {
            if (model != null && !model.isBlank()) {
                List<WebElement> t = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceId(\"android:id/title\").text(\"" + model + "\")"));
                if (!t.isEmpty()) return t.get(t.size() - 1);
            }
            List<WebElement> free = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"android:id/summary\").textMatches(\"(?i).*\\\\bfree\\\\b.*\")"));
            if (!free.isEmpty()) return free.get(0);
        } catch (WebDriverException ignored) { }
        System.out.println(">>> no device root in the picker drawer (model=" + model + ")");
        return null;
    }

    private void swipeVertical(int x, int y1, int y2) {
        PointerInput f = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence s = new Sequence(f, 1);
        s.addAction(f.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y1));
        s.addAction(f.createPointerDown(0));
        s.addAction(f.createPointerMove(Duration.ofMillis(450), PointerInput.Origin.viewport(), x, y2));
        s.addAction(f.createPointerUp(0));
        try { driver.perform(List.of(s)); } catch (WebDriverException ignored) { }
        pause(800);
    }

    public boolean waitDocument(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (hasDocument()) return true;
            pause(600);
        }
        return false;
    }

    /**
     * The PDF tile asks for media permissions first (on the Pixel: "music and audio", then
     * "photos and videos"). "Allow all" where offered, else "Allow".
     */
    private void allowPermissionIfAsked() {
        pause(1200);
        for (int i = 0; i < 3; i++) {
            try {
                List<WebElement> b = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceIdMatches(\".*permission_allow_all_button\")"));
                if (b.isEmpty()) b = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceIdMatches(\".*permission_allow_button\")"));
                if (b.isEmpty()) return;
                b.get(0).click();
                pause(1500);
            } catch (WebDriverException ignored) { return; }
        }
    }

    /** The host's preview step (when it shows) confirms with its send / done button. */
    private void confirmPreviewIfShown() {
        pause(1500);
        for (String id : new String[]{"sendBtn", "btnSend", "ivSend", "doneBtn", "btnDone"}) {
            List<WebElement> e = elementsById(id);
            if (!e.isEmpty() && onScreen("docPickerRow").isEmpty()) {
                try { e.get(0).click(); pause(2000); return; } catch (WebDriverException ignored) { }
            }
        }
    }

    public void setTitle(String title) { setField("etTitle", title); }
    public String titleText() { return firstText("etTitle"); }

    public void setDescription(String d) { setField("etDescription", d); }

    /** Clears then types into an EditText by its own id; never presses BACK blindly. */
    private void setField(String id, String value) {
        scrollToId(id);
        List<WebElement> f = onScreen(id);
        if (f.isEmpty()) return;
        try {
            f.get(0).click();
            f.get(0).clear();
            if (value != null && !value.isEmpty()) f.get(0).sendKeys(value);
        } catch (WebDriverException ignored) { }
        pause(500);
        hideKeyboardIfShown();
    }

    /** "Next" is pinned under the scroll view, so no scrolling is needed (or possible). */
    public boolean tapNext() {
        return tapFirst("btnNext");
    }

    public String nextHint() {
        List<WebElement> h = onScreen("tvNextHint");   // pinned above Next, outside the scroll
        return h.isEmpty() ? null : text(h.get(0));
    }

    public String signersHeading() {
        scrollToId("tvSignersHeading");
        return firstText("tvSignersHeading");
    }

    public String ccHeading() {
        scrollToId("tvCcHeading");
        return firstText("tvCcHeading");
    }

    public boolean tapInsideProjectSigners() {
        scrollToId("btnSelectSigners");
        return tapFirst("btnSelectSigners");
    }

    public boolean tapInsideProjectCc() {
        scrollToId("btnSelectCc");
        return tapFirst("btnSelectCc");
    }

    public boolean toggleSelfSigner() {
        scrollToId("switchSelfSigner");
        return tapFirst("switchSelfSigner");
    }

    public boolean selfSignerOn() {
        List<WebElement> s = onScreen("switchSelfSigner");
        try { return !s.isEmpty() && "true".equals(s.get(0).getAttribute("checked")); }
        catch (WebDriverException e) { return false; }
    }

    public boolean tapBack() { return tapFirst("btnBack"); }

    /** "Replace" ({@code tvChangeFile}) — shown once a document is attached. */
    public boolean replaceShown() { return "Replace".equals(firstText("tvChangeFile")); }

    public boolean renameShown() { return !onScreen("btnEditFileName").isEmpty(); }

    // ── ADD MESSAGE (collapsible; its open/closed state is persisted) ──

    public boolean addMessageOpen() {
        return !onScreen("addMessageChildren").isEmpty();
    }

    public void openAddMessage() {
        if (addMessageOpen()) return;
        scrollToId("btnAddMessageToggle");
        tapFirst("btnAddMessageToggle");
    }

    public void setEmailSubject(String s) { setField("etEmailSubject", s); }
    public void setEmailBody(String s) { setField("etEmailBody", s); }
    public String emailSubject() { return firstText("etEmailSubject"); }
    public String emailBody() { return firstText("etEmailBody"); }

    // ─────────────────────── pick-users bottom sheet ───────────────────────

    public boolean pickUsersShown(Duration timeout) {
        return isVisible("rvUsers", timeout) && !onScreen("btnDone").isEmpty();
    }

    public String pickUsersTitle() { return firstText("tvTitle"); }

    public List<String> pickUserNames() {
        List<String> out = new ArrayList<>();
        for (WebElement e : onScreen("tvName")) out.add(text(e));
        return out;
    }

    public void searchPickUsers(String q) {
        List<WebElement> f = onScreen("etSearch");
        if (f.isEmpty()) return;
        try { f.get(0).click(); f.get(0).clear(); f.get(0).sendKeys(q); } catch (WebDriverException ignored) { }
        pause(1200);
        hideKeyboardIfShown();
    }

    public boolean pickUser(String name) {
        for (WebElement e : onScreen("tvName")) {
            if (text(e).equalsIgnoreCase(name) || text(e).startsWith(name)) {
                try { e.click(); pause(700); return true; } catch (WebDriverException ignored) { }
            }
        }
        return false;
    }

    public String pickedCountLabel() { return firstText("tvSelectedCount"); }
    public boolean pickUsersDone() { return tapFirst("btnDone"); }
    public boolean pickUsersCancel() { return tapFirst("btnCancel"); }

    // ─────────────────────────── Placement ───────────────────────────

    public boolean placementShown(Duration timeout) {
        return isVisible("btnSaveDraft", timeout) && !onScreen("pagerPdf").isEmpty();
    }

    /** The PDF page rendered: the spinner is gone and no error is shown. */
    public boolean documentRendered(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (onScreen("progressDoc").isEmpty() && onScreen("tvDocError").isEmpty()
                    && !onScreen("zoomHost").isEmpty()) return true;
            pause(700);
        }
        return false;
    }

    public String docError() { return firstText("tvDocError"); }
    public String fieldCountLabel() { return firstText("tvFieldCount"); }
    public String pageLabel() { return firstText("tvPageLabel"); }
    public String activeSignerLabel() { return firstText("tvActiveSigner"); }

    public List<String> fieldToolbarLabels() {
        List<String> out = new ArrayList<>();
        List<WebElement> row = onScreen("fieldChipRow");
        if (row.isEmpty()) return out;
        try {
            for (WebElement t : row.get(0).findElements(AppiumBy.className("android.widget.TextView"))) {
                String s = text(t);
                if (!s.isEmpty()) out.add(s);
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    public boolean armField(String label) {
        List<WebElement> row = onScreen("fieldChipRow");
        if (row.isEmpty()) return false;
        try {
            for (WebElement t : row.get(0).findElements(AppiumBy.className("android.widget.TextView"))) {
                if (label.equals(text(t))) { t.click(); pause(800); return true; }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** Taps the centre of the visible PDF page (the overlay catches it). */
    public boolean tapDocument(double fx, double fy) {
        List<WebElement> pages = onScreen("pageOverlay");
        if (pages.isEmpty()) pages = onScreen("zoomHost");
        if (pages.isEmpty()) return false;
        try {
            var r = pages.get(0).getRect();
            int x = r.getX() + (int) (r.getWidth() * fx);
            int y = r.getY() + (int) (r.getHeight() * fy);
            PointerInput f = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence s = new Sequence(f, 1);
            s.addAction(f.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
            s.addAction(f.createPointerDown(0));
            s.addAction(f.createPointerMove(Duration.ofMillis(80), PointerInput.Origin.viewport(), x, y));
            s.addAction(f.createPointerUp(0));
            driver.perform(List.of(s));
            pause(1500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** The field editor ({@code dialog_docusign_edit_field}) is up. */
    public boolean fieldEditorShown(Duration timeout) {
        return isVisible("switchRequired", timeout) || isVisible("sizeGroup", Duration.ofSeconds(1));
    }

    /** Closes the field editor / any sheet by its positive button, else BACK. */
    public void closeFieldEditor() {
        for (String label : new String[]{"Save", "Done", "OK", "Apply"}) {
            for (WebElement b : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceIdMatches(\".*:id/button[123]\")"))) {
                if (label.equalsIgnoreCase(text(b))) {
                    try { b.click(); pause(1000); return; } catch (WebDriverException ignored) { }
                }
            }
        }
        try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        pause(1000);
    }

    public boolean tapSend() { return tapFirst("btnSend"); }
    public boolean tapSaveDraft() { return tapFirst("btnSaveDraft"); }

    public boolean sendConfirmShown(Duration timeout) {
        return isVisible("sendConfirmRoot", timeout) || textShown(SEND_CONFIRM_TITLE, Duration.ofSeconds(1));
    }

    public String sendConfirmDoc() { return firstText("tvSendConfirmDoc"); }
    public String sendConfirmWho() { return firstText("tvSendConfirmWhoHeader"); }

    /** Negative button of the send-confirm AlertDialog — never the positive "Send". */
    public boolean cancelSendConfirm() {
        for (WebElement b : driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().resourceIdMatches(\".*:id/button2\")"))) {
            try { b.click(); pause(1000); return true; } catch (WebDriverException ignored) { }
        }
        return false;
    }

    public boolean placementBack() { return tapFirst("btnBack"); }

    // ─────────────────────── template authoring ───────────────────────

    /** Template mode swaps Save as Draft / Send for a single "Save Template". */
    public boolean templateModeShown(Duration timeout) {
        return isVisible("btnSaveTemplate", timeout);
    }

    public boolean tapSaveTemplate() { return tapFirst("btnSaveTemplate"); }

    public boolean saveTemplateDialogShown(Duration timeout) {
        return isVisible("btnSaveTemplateConfirm", timeout);
    }

    public String templateNameField() { return firstText("etTemplateName"); }

    public void setTemplateName(String n) { setField("etTemplateName", n); }

    public void setTemplateCategory(String c) {
        setField("etTemplateCategory", c);
        // the AutoCompleteTextView's suggestion popup may cover the buttons
        pause(400);
        hideKeyboardIfShown();
    }

    public boolean confirmSaveTemplate() { return tapFirst("btnSaveTemplateConfirm"); }
    public boolean cancelSaveTemplate() { return tapFirst("btnSaveTemplateCancel"); }
}
