package com.zillit.automation.pages.drive;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * FILE DETAIL ({@code FileDetailFragment} / fragment_file_detail.xml) — pushed when a
 * FILE row is tapped in Drive. Metadata, the action row (Download / Open / Edit / Share
 * / Delete / Manage access), the share-link card, tags, versions and the COMMENTS
 * section this class mostly exists for.
 *
 * <p><b>Comment rows have no resource ids.</b> {@code CommentsAdapter} builds every row
 * in code — a horizontal LinearLayout holding a text column (author / text / date) and
 * two ImageButtons. Only the buttons are identifiable, via their
 * {@code contentDescription} ("Edit comment" / "Delete comment"), and those are the SAME
 * on every row. So a per-comment action has to be anchored on the comment TEXT and walked
 * back up to its row — see {@link #tapEditComment} — never taken from a list index.
 *
 * <p>Detail shows only the <b>latest two</b> comments; the rest live behind
 * {@code btnViewAllComments} ({@link DriveCommentsPage}).
 */
public class DriveFileDetailPage extends BasePage {

    /** contentDescription set by the adapter on every comment row's edit button. */
    private static final String CD_EDIT = "Edit comment";
    private static final String CD_DELETE = "Delete comment";

    public DriveFileDetailPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isOpen(Duration timeout) {
        return isVisible("tvFileName", timeout) || isVisible("btnShare", Duration.ofSeconds(2));
    }

    public String fileName() { return text("tvFileName"); }
    public String fileSize() { return text("tvFileSize"); }
    public String mimeType() { return text("tvMimeType"); }

    public boolean hasDownload()      { return isVisible("btnDownload", Duration.ofSeconds(3)); }
    public boolean hasOpen()          { return isVisible("btnOpen", Duration.ofSeconds(2)); }
    public boolean hasShare()         { return isVisible("btnShare", Duration.ofSeconds(2)); }
    public boolean hasManageAccess()  { return isVisible("btnManageAccess", Duration.ofSeconds(2)); }

    /** A detail view with this id is on screen (scrolling to it first). */
    public boolean isVisibleId(String id) {
        if (isVisible(id, Duration.ofSeconds(3))) return true;
        scrollToId(id);
        return isVisible(id, Duration.ofSeconds(3));
    }

    public boolean hasEdit()          { return isVisible("btnEdit", Duration.ofSeconds(2)); }

    /**
     * Waits for an action button. The detail screen sets them from an async state flow (the
     * permissions arrive after the screen is drawn), so a fixed sleep reports a missing button.
     */
    public boolean waitFor(String id, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try { if (!driver.findElements(byId(id)).isEmpty()) return true; }
            catch (WebDriverException ignored) { }
            sleep(700);
        }
        return false;
    }

    /** {@code btnEdit} → the office editor in EDIT mode (txt/md: the text editor, editing). */
    public boolean tapEdit()     { return click("btnEdit"); }
    /** {@code btnOpen} → the office editor in VIEW mode (txt/md: the text viewer). */
    public boolean tapOpen()     { return click("btnOpen"); }
    public boolean tapDownload() { return click("btnDownload"); }

    /** Waits until the Download button is back from "Downloading…". */
    public boolean waitDownloadDone(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        sleep(1500);
        while (System.currentTimeMillis() < deadline) {
            String t = text("btnDownload");
            if (t != null && !t.toLowerCase().contains("downloading")) return true;
            sleep(800);
        }
        return false;
    }

    // ---------------- version history (2e8cd2801) ----------------

    /** Matches a version row label: "v3", "v3 · Autosaved", "v3 · Autosaved · Name". */
    public static final java.util.regex.Pattern VERSION_LABEL = java.util.regex.Pattern.compile(
            "^v(\\d+)( · (Created|Autosaved|Saved|Saved on close|Restored|Uploaded|Earlier version))?( · .+)?$");

    public boolean hasVersionsCard() {
        if (isVisible("cardVersions", Duration.ofSeconds(2))) return true;
        scrollToId("tvVersionsHeader");
        return isVisible("tvVersionsHeader", Duration.ofSeconds(2));
    }

    /** "Versions (N)" → N, or -1 when the card is not there. */
    public int versionCount() {
        if (!hasVersionsCard()) return -1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(text("tvVersionsHeader"));
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** Rendered version-row labels ("v3 · Autosaved"), top (latest) first. */
    public List<String> versionLabels() {
        List<String> out = new ArrayList<>();
        scrollToId("rvVersions");
        try {
            WebElement rv = driver.findElement(byId("rvVersions"));
            for (WebElement tv : rv.findElements(By.className("android.widget.TextView"))) {
                String t = tv.getText();
                if (t != null && t.matches("^v\\d+.*")) out.add(t);
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    /** Every text in the versions list (labels, "Saved by …", summaries, Open/Download/Restore). */
    public List<String> versionTexts() {
        List<String> out = new ArrayList<>();
        try {
            WebElement rv = driver.findElement(byId("rvVersions"));
            for (WebElement tv : rv.findElements(By.className("android.widget.TextView"))) {
                String t = tv.getText();
                if (t != null && !t.isBlank()) out.add(t);
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    /**
     * The action TextViews ("Open" / "Download" / "Restore") of the version row whose label is
     * {@code label}. Rows have no ids, so the row is the band between this label and the next one.
     */
    public List<String> versionRowActions(int rowIndex) {
        List<String> out = new ArrayList<>();
        try {
            List<WebElement> labels = versionLabelElements();
            if (rowIndex >= labels.size()) return out;
            int top = labels.get(rowIndex).getRect().getY();
            int bottom = rowIndex + 1 < labels.size() ? labels.get(rowIndex + 1).getRect().getY() : Integer.MAX_VALUE;
            WebElement rv = driver.findElement(byId("rvVersions"));
            for (WebElement tv : rv.findElements(By.className("android.widget.TextView"))) {
                int y = tv.getRect().getY();
                String t = tv.getText();
                if (y > top && y < bottom && t != null
                        && (t.equals("Open") || t.equals("Download") || t.equals("Restore"))) out.add(t);
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    public boolean tapVersionAction(int rowIndex, String action) {
        try {
            List<WebElement> labels = versionLabelElements();
            if (rowIndex >= labels.size()) return false;
            int top = labels.get(rowIndex).getRect().getY();
            int bottom = rowIndex + 1 < labels.size() ? labels.get(rowIndex + 1).getRect().getY() : Integer.MAX_VALUE;
            WebElement rv = driver.findElement(byId("rvVersions"));
            for (WebElement tv : rv.findElements(By.className("android.widget.TextView"))) {
                int y = tv.getRect().getY();
                if (y > top && y < bottom && action.equals(tv.getText())) { tv.click(); return true; }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    private List<WebElement> versionLabelElements() {
        List<WebElement> out = new ArrayList<>();
        WebElement rv = driver.findElement(byId("rvVersions"));
        for (WebElement tv : rv.findElements(By.className("android.widget.TextView"))) {
            String t = tv.getText();
            if (t != null && t.matches("^v\\d+.*")) out.add(tv);
        }
        return out;
    }

    /** Brings the versions list on screen (it sits below the actions and tags). */
    public void scrollToVersions() {
        for (int i = 0; i < 5 && !isVisible("rvVersions", Duration.ofSeconds(1)); i++) swipeUp();
        scrollToId("rvVersions");
    }

    // ---------------- Share button = copy-link expiry (0c909b7fb) ----------------

    /** File details Share → "Share Link Expiry" list (1 hour / 24 hours / 7 days). */
    public List<String> openShareLinkExpiryOptions() {
        List<String> out = new ArrayList<>();
        if (!click("btnShare")) return out;
        if (!textShown("Share Link Expiry", Duration.ofSeconds(6))) return out;
        try {
            for (WebElement e : driver.findElements(AppiumBy.id("android:id/text1"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t);
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    // ---------------- description / download (M02, M06) ----------------

    public String description() {
        scrollToId("tvDescription");
        return text("tvDescription");
    }

    public String downloadButtonText() { return text("btnDownload"); }

    // ---------------- tags (M03) ----------------

    /** Tag chips (the "+ Add Tag" chip excluded). */
    public List<String> tagChips() {
        List<String> out = new ArrayList<>();
        scrollToId("tagsContainer");
        try {
            WebElement c = driver.findElement(byId("tagsContainer"));
            for (WebElement e : c.findElements(By.xpath(".//*"))) {
                String t = e.getText();
                if (t != null && !t.isBlank() && !t.startsWith("+") && !out.contains(t)) out.add(t);
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    public boolean noTagsShown() { scrollToId("tvNoTags"); return isVisible("tvNoTags", Duration.ofSeconds(2)); }

    public boolean tapAddTag() {
        scrollToId("tagsContainer");
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textStartsWith(\"+ Add Tag\")")).click();
            sleep(1500);
            return textShown("Add Tag", Duration.ofSeconds(6));
        } catch (WebDriverException e) { return false; }
    }

    /** Add Tag → "+ Create New Tag" → type → Create. */
    public boolean createTag(String name) {
        if (!tapAddTag()) return false;
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"+ Create New Tag\")")).click();
            sleep(1200);
            WebElement field = driver.findElement(By.className("android.widget.EditText"));
            field.click();
            field.sendKeys(name);
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"android:id/button1\")")).click();
            sleep(3000);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /** Add Tag → pick an existing tag by name. */
    public boolean assignExistingTag(String name) {
        if (!tapAddTag()) return false;
        try {
            driver.findElement(AppiumBy.androidUIAutomator("new UiSelector().text(\"" + name + "\")")).click();
            sleep(3000);
            return true;
        } catch (WebDriverException e) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            return false;
        }
    }

    /**
     * Removes a tag chip with its close icon. Material exposes the icon as a virtual child
     * ("Remove …"); when it is not in the tree, the icon is tapped at the chip's right edge.
     */
    public boolean removeTag(String name) {
        scrollToId("tagsContainer");
        try {
            List<WebElement> icons = driver.findElements(AppiumBy.xpath(
                    "//*[contains(@content-desc,'Remove') and contains(@content-desc,'" + name + "')]"));
            if (!icons.isEmpty()) { icons.get(0).click(); sleep(2500); return true; }
            WebElement chip = driver.findElement(AppiumBy.androidUIAutomator("new UiSelector().text(\"" + name + "\")"));
            org.openqa.selenium.Rectangle r = chip.getRect();
            driver.executeScript("mobile: clickGesture", java.util.Map.of(
                    "x", r.getX() + r.getWidth() - Math.max(18, r.getHeight() / 3), "y", r.getY() + r.getHeight() / 2));
            sleep(2500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- Copy link card (M04) ----------------

    /** Share → "Share Link Expiry" → pick {@code option}; the link appears in cardShareLink. */
    public boolean makeCopyLink(String option) {
        if (openShareLinkExpiryOptions().isEmpty()) return false;
        try {
            driver.findElement(AppiumBy.androidUIAutomator("new UiSelector().text(\"" + option + "\")")).click();
        } catch (WebDriverException e) { return false; }
        for (int i = 0; i < 20; i++) {
            sleep(1000);
            scrollToId("cardShareLink");
            if (!text("tvShareLink").isBlank()) return true;
        }
        return false;
    }

    public String shareLink()       { scrollToId("cardShareLink"); return text("tvShareLink"); }
    public String shareLinkExpiry() { scrollToId("cardShareLink"); return text("tvShareExpiry"); }
    public boolean copyShareLink()  { scrollToId("btnCopyLink"); return click("btnCopyLink"); }

    // ---------------- comments section ----------------

    /** "Comments" or "Comments (N)" — the section header. */
    public String commentsHeader() { return text("tvCommentsHeader"); }

    /**
     * How many comments the file has, from the header ("Comments (3)" → 3, bare
     * "Comments" → 0).
     *
     * <p>This is the ONLY stable count. The rendered rows are not: the preview binds
     * lazily, so two reads a second apart can legitimately return one row and then two,
     * which makes a before/after comparison of row text fail for no reason. The header is
     * driven straight off {@code state.comments.size()}.
     */
    public int commentCount() {
        String header = commentsHeader();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(header);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    public boolean hasCommentsSection() {
        return isVisible("tvCommentsHeader", Duration.ofSeconds(6));
    }

    /**
     * True when the section renders one of its two valid states: the list, or the
     * "no comments" placeholder. Either is a pass for P-1; NEITHER means the section
     * failed to bind.
     */
    public boolean commentsStateShown() {
        return isVisible("rvComments", Duration.ofSeconds(4))
                || isVisible("tvNoComments", Duration.ofSeconds(2));
    }

    public boolean noCommentsShown() { return isVisible("tvNoComments", Duration.ofSeconds(3)); }

    /**
     * Waits until the comments section has actually LOADED.
     *
     * <p>Comments are fetched after the file itself, and the "No comments yet"
     * placeholder is what the screen shows in the meantime — an empty
     * {@code state.comments} renders identically before and after the fetch. So the
     * placeholder alone is NOT proof the load finished: this waits for rows, and only
     * accepts the placeholder once a settle window has passed with nothing arriving.
     *
     * <p>Reading too early is what made a saved comment look lost, and what made an
     * empty-comment guard "grow" the list from 0 rows to 2 between two reads.
     */
    public boolean waitForCommentsLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        long settled = System.currentTimeMillis() + SETTLE_MS;
        while (System.currentTimeMillis() < deadline) {
            if (!commentTexts().isEmpty()) return true;
            if (System.currentTimeMillis() > settled && noCommentsShown()) return true;
            sleep(500);
        }
        return false;
    }

    /** How long an empty section must stay empty before we believe it. */
    private static final long SETTLE_MS = 8000;

    /** The comment composer — hidden entirely for view-only recipients. */
    public boolean hasCommentInput() {
        return isVisible("etComment", Duration.ofSeconds(4))
                && isVisible("btnSendComment", Duration.ofSeconds(2));
    }

    public boolean typeComment(String text) {
        try {
            WebElement field = driver.findElement(byId("etComment"));
            scrollToId("etComment");
            field.click();
            field.clear();
            if (!text.isEmpty()) field.sendKeys(text);
            hideKeyboard();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean tapSend() { return click("btnSendComment"); }

    /** Types and sends in one go. Returns false if either half failed. */
    public boolean postComment(String text) {
        return typeComment(text) && tapSend();
    }

    /** The text currently in the composer — used to prove a rejected send did not clear it. */
    public String composerText() { return text("etComment"); }

    /**
     * Every TextView inside the comments list: author, body and date for each rendered
     * row. Rows carry no ids, so this is deliberately coarse — use it to ask whether a
     * body is present, not to count rows.
     */
    public List<String> commentTexts() {
        List<String> out = new ArrayList<>();
        try {
            WebElement list = driver.findElement(byId("rvComments"));
            for (WebElement tv : list.findElements(By.className("android.widget.TextView"))) {
                String t = tv.getText();
                if (t != null && !t.isBlank()) out.add(t);
            }
        } catch (WebDriverException ignored) { }
        return out;
    }

    public boolean hasComment(String body) {
        return commentTexts().stream().anyMatch(t -> t.equals(body));
    }

    /** Posting is a server round-trip, so poll rather than sleep-then-peek. */
    public boolean waitForComment(String body, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (hasComment(body)) return true;
            sleep(1000);
        }
        return false;
    }

    public boolean waitForCommentGone(String body, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!hasComment(body)) return true;
            sleep(1000);
        }
        return false;
    }

    // ---------------- per-comment actions ----------------

    public boolean tapEditComment(String body)   { return tapRowAction(body, CD_EDIT); }
    public boolean tapDeleteComment(String body) { return tapRowAction(body, CD_DELETE); }

    /**
     * Finds the row whose body matches and taps ITS edit/delete button.
     *
     * <p>The row is {@code [ textColumn[author, body, date], btnEdit, btnDelete ]}, so
     * from the body TextView the row is two levels up. Tapping the first element with
     * the right contentDescription instead would hit whichever comment happens to be
     * rendered first — silently editing the wrong one.
     */
    private boolean tapRowAction(String body, String contentDesc) {
        String xpath = "//android.widget.TextView[@text=" + quote(body) + "]"
                + "/parent::*/parent::*//*[@content-desc=" + quote(contentDesc) + "]";
        try {
            driver.findElement(AppiumBy.xpath(xpath)).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- edit / delete dialogs ----------------

    public boolean editDialogOpen() {
        return textShown("Edit Comment", Duration.ofSeconds(6));
    }

    public boolean deleteDialogOpen() {
        return textShown("Delete Comment", Duration.ofSeconds(6))
                || textShown("delete this comment", Duration.ofSeconds(2));
    }

    /**
     * Replaces the text in the edit dialog's field.
     *
     * <p>The dialog's EditText is created in code with NO id, and the page behind it
     * still holds {@code etComment} — so "the first EditText" is ambiguous. Two signals,
     * in order: the field PREFILLED with the comment being edited (unmistakable), then
     * the one without a resource-id.
     *
     * <p>Each candidate is tried in its own try/catch. Sharing one meant a single
     * exception on the covered {@code etComment} behind the dialog abandoned the search
     * altogether, and the edit case skipped with the dialog sitting open on screen.
     */
    public boolean setEditedText(String currentText, String newText) {
        List<WebElement> candidates;
        try {
            candidates = driver.findElements(By.className("android.widget.EditText"));
        } catch (WebDriverException e) {
            return false;
        }
        for (WebElement field : candidates) {
            try {
                String t = field.getText();
                if (t != null && t.equals(currentText) && replaceText(field, newText)) return true;
            } catch (WebDriverException ignored) { }
        }
        for (WebElement field : candidates) {
            try {
                String rid = field.getAttribute("resource-id");
                if ((rid == null || rid.isBlank()) && replaceText(field, newText)) return true;
            } catch (WebDriverException ignored) { }
        }
        return false;
    }

    private boolean replaceText(WebElement field, String newText) {
        try {
            field.click();
            field.clear();
            field.sendKeys(newText);
            hideKeyboard();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Confirms whichever dialog is open (Save / Delete are both android:id/button1). */
    public boolean confirmDialog(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceId(\"android:id/button1\")")).click();
                return true;
            } catch (WebDriverException ignored) { }
            sleep(400);
        }
        return false;
    }

    public boolean cancelDialog() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"android:id/button2\")")).click();
            return true;
        } catch (WebDriverException e) { return false; }
    }

    public boolean isDialogOpen() {
        try {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"android:id/button1\")")).isEmpty();
        } catch (WebDriverException e) { return false; }
    }

    // ---------------- view all ----------------

    /**
     * Only rendered once there are MORE than two comments — and it sits UNDER the two
     * preview rows, at the very bottom of a long scroll view, so it is off screen when
     * the detail first renders. Scroll before judging it missing.
     */
    public boolean hasViewAllComments() {
        return visibleAfterScroll("btnViewAllComments");
    }

    public String viewAllLabel() { return text("btnViewAllComments"); }

    public boolean tapViewAllComments() {
        visibleAfterScroll("btnViewAllComments");
        return click("btnViewAllComments");
    }

    /** True when the control is on screen, scrolling the detail to reach it if needed. */
    private boolean visibleAfterScroll(String id) {
        if (isVisible(id, Duration.ofSeconds(3))) return true;
        scrollToId(id);
        if (isVisible(id, Duration.ofSeconds(2))) return true;
        for (int i = 0; i < 4; i++) {
            swipeUp();
            if (isVisible(id, Duration.ofSeconds(1))) return true;
        }
        return false;
    }

    private void swipeUp() {
        try {
            var size = driver.manage().window().getSize();
            driver.executeScript("mobile: swipeGesture", java.util.Map.of(
                    "left", size.width / 2 - 100,
                    "top", (int) (size.height * 0.35),
                    "width", 200,
                    "height", (int) (size.height * 0.40),
                    "direction", "up",
                    "percent", 0.8));
            Thread.sleep(600);
        } catch (Exception ignored) { }
    }

    // ---------------- helpers ----------------

    /** Scrolls the detail's NestedScrollView far enough to reach the comments block. */
    public void scrollToComments() {
        scrollToId("tvCommentsHeader");
    }

    public boolean back() {
        try { driver.navigate().back(); Thread.sleep(1200); return true; }
        catch (Exception e) { return false; }
    }

    /** XPath string literal that survives an apostrophe in the text. */
    private static String quote(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        return "concat('" + s.replace("'", "',\"'\",'") + "')";
    }

    private String text(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); return true; }
        catch (WebDriverException e) { return false; }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
