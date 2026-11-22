package com.zillit.automation.pages.common;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * The ANDROID SYSTEM file picker ({@code com.google.android.documentsui}) — the one place
 * in the suite that drives it. Every module's upload goes through here: the single-file
 * pick every page reaches through {@link BasePage#pickSystemFile(String, Duration)}
 * ({@link #pickAndReturn}), and Document Distribution's multi-file upload
 * ({@link #pickFiles}).
 *
 * <p>Drive's own {@code DriveUploadPage} still drives the same picker separately: it is
 * proven against Drive's flow and fixtures, and moving it is its own change.
 *
 * <p><b>Everything here is text/description matching.</b> The picker is a FOREIGN package,
 * so {@code byId()} — which prefixes the app-under-test's package — cannot match a single
 * node in it.
 *
 * <p><b>The trap that made this class necessary.</b> The picker opens on <i>Recent</i>,
 * and Recent is ordered by the file's MODIFICATION time. {@code adb push} preserves the
 * source file's mtime, so a fixture pushed today can carry a date from weeks ago and
 * never appear in Recent at all — the file is plainly on the device, in the right folder,
 * and the picker still does not show it. (The runner now touches every fixture after
 * pushing it, but a device prepared some other way still has the problem.) A name still
 * showing in Recent can also be a stale entry for a file that has since been replaced.
 * So the real routes leave Recent — single picks visit it only as a last resort, and
 * Document Distribution's batch only as a fast path — and each route fails differently:
 * <ol>
 *   <li>the device storage root's {@code Documents} folder — a real directory listing, so
 *       it holds every file on disk, including types MediaStore never indexed
 *       ({@code .fdx}). The runner pushes the Sides, chat and Schedule fixtures here;</li>
 *   <li>the roots drawer's Downloads root — where Document Distribution's fixtures go;</li>
 *   <li>the picker's own Search, which is unaffected by how the roots are labelled;</li>
 *   <li>"More options" → "Show internal storage", then both folders again — needed on
 *       builds that hide the device root until internal storage is revealed.</li>
 * </ol>
 */
public class SystemFilePickerPage extends BasePage {

    /** Roots-drawer entries that are MediaStore collections, not the device's own storage. */
    private static final List<String> COLLECTIONS = List.of("recent", "documents", "downloads",
            "download", "images", "audio", "video", "videos", "photos", "drive", "browse");

    /** What a tap on a file name achieved. */
    private enum Tap {
        /** The file was handed to the app (or, without the close check, tapped). */
        CHOSEN,
        /** The row was tapped and the picker stayed open: its MIME filter refuses the file. */
        REFUSED,
        /** No such row on screen. */
        ABSENT
    }

    /** Which folder to try first: where the caller's fixtures are pushed. */
    private enum Folder { DOCUMENTS, DOWNLOADS }

    public SystemFilePickerPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                String pkg = driver.getCurrentPackage();
                if (pkg != null && pkg.contains("documentsui")) return true;
            } catch (WebDriverException ignored) { }
            sleep(400);
        }
        return false;
    }

    // ─────────────────────────────── single-file picks ───────────────────────────────

    /**
     * Picks ONE file whose name contains {@code namePart} and hands it to the app — and
     * never strands the run when the file is not there.
     *
     * <p>For the app's single-document flows (chat attachments, Sides, Call Sheet,
     * Schedule upload, email). Two rules, both learnt the hard way:
     * <ul>
     *   <li><b>A tap is not a pick.</b> A row the picker's MIME filter does not allow is
     *       still drawn and still clickable, and tapping it does nothing. The picker
     *       closing is the only proof the file was handed over, so that is what counts.</li>
     *   <li><b>Failure must not leave the picker open.</b> It is a different app; with it
     *       still in front, every later case looks for the app's ids in someone else's
     *       activity, finds nothing, and skips — one missing fixture takes the whole
     *       suite with it. So a failed pick always backs out to the app.</li>
     * </ul>
     *
     * <p><b>Recent is the LAST route here, not the first.</b> Once a run has picked a
     * fixture it sits at the top of Recent, and that entry can be stale — a file since
     * re-pushed or replaced — so tapping it closes the picker and hands the app a URI it
     * cannot read. That is how Sides' SI-13 picked a script and still showed an empty
     * card. The folder listings are read from disk, so they are tried first. Recent
     * stays as the fallback for media that lives in neither folder.
     *
     * @param timeout time spent LOOKING once the picker has left Recent; navigation is
     *        not counted against it.
     */
    public boolean pickAndReturn(String namePart, Duration timeout) {
        if (pick(namePart, timeout, true, Folder.DOCUMENTS, false)) return true;
        System.out.println(">>> system picker: '" + namePart + "' not picked within "
                + timeout.toSeconds() + "s of looking — backing out so the next case can run."
                + " On screen: " + visibleNames());
        returnToAppUnderTest();
        return false;
    }

    /**
     * Taps a file by name for Document Distribution's {@code OpenMultipleDocuments}
     * upload. Unlike {@link #pickAndReturn}, a tap counts as success even if the picker
     * stays open — that picker may be in multi-select mode, where a tap selects and a
     * button confirms ({@link #confirmIfWaiting}) — and the picker is left open on
     * failure for the caller to {@link #cancel}.
     *
     * @return true once the file has been tapped (selected or returned).
     */
    public boolean pickFile(String fileName, Duration timeout) {
        // Recent first, as this always did: the runner touches the zl_dd_* fixtures so
        // they top Recent, and Document Distribution's suites have passed that way.
        if (pick(fileName, timeout, false, Folder.DOWNLOADS, true)) return true;
        System.out.println(">>> picker could not reach '" + fileName + "'. On screen now: "
                + visibleNames());
        return false;
    }

    /**
     * The one navigation sequence behind both single-file picks: the caller's folder,
     * the other folder, search, internal storage — with Recent either first or last.
     *
     * @param mustClose whether success means the picker closed (see {@link #pickAndReturn}).
     * @param recentFirst try where the picker opened (Recent) before anything else; when
     *        false Recent is the last resort instead (see {@link #pickAndReturn}).
     */
    private boolean pick(String name, Duration timeout, boolean mustClose, Folder first,
                         boolean recentFirst) {
        Tap t;
        if (recentFirst) {
            // Scrolls the list once but does not sweep it: a Recent entry is either near
            // the top or not worth trusting.
            t = tapHere(name, mustClose);
            if (t != Tap.ABSENT) return t == Tap.CHOSEN;
        }

        // The deadline starts after the first walk, so the timeout is time spent LOOKING
        // rather than navigating. Later routes share it, but each still gets its look.
        Folder second = first == Folder.DOCUMENTS ? Folder.DOWNLOADS : Folder.DOCUMENTS;
        boolean walked = open(first);
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        if (walked && (t = findAndTap(name, deadline, mustClose)) != Tap.ABSENT) return t == Tap.CHOSEN;
        if (open(second) && (t = findAndTap(name, deadline, mustClose)) != Tap.ABSENT) return t == Tap.CHOSEN;
        if (searchFor(name) && (t = pollTap(name, deadline, mustClose)) != Tap.ABSENT) return t == Tap.CHOSEN;
        if (showInternalStorage()) {
            for (Folder f : new Folder[]{first, second}) {
                if (open(f) && (t = findAndTap(name, deadline, mustClose)) != Tap.ABSENT) {
                    return t == Tap.CHOSEN;
                }
            }
        }
        if (!recentFirst && openRoot("Recent") && (t = tapHere(name, mustClose)) != Tap.ABSENT) {
            return t == Tap.CHOSEN;
        }
        return false;
    }

    private boolean open(Folder folder) {
        return folder == Folder.DOCUMENTS ? openStorageDocuments() : openDownloadsRoot();
    }

    /** A tap where the picker is now, after one scroll of its list. */
    private Tap tapHere(String name, boolean mustClose) {
        Tap t = tapFile(name, mustClose);
        if (t != Tap.ABSENT) return t;
        scrollIntoView(name);
        return tapFile(name, mustClose);
    }

    /** Scrolls the current listing looking for the file until the budget runs out. */
    private Tap findAndTap(String name, long deadline, boolean mustClose) {
        // A folder holds every fixture this rig has ever pushed plus the app's own output,
        // so the wanted file can be far down. scrollIntoView drives the container itself;
        // a manual sweep follows in case the list is not reported as scrollable (which
        // some picker layouts do). Always at least one look, even past the deadline.
        Tap t = tapHere(name, mustClose);
        if (t != Tap.ABSENT) return t;
        for (int i = 0; i < 12 && System.currentTimeMillis() < deadline; i++) {
            swipeUp();
            if ((t = tapFile(name, mustClose)) != Tap.ABSENT) return t;
        }
        return Tap.ABSENT;
    }

    /** Search results arrive asynchronously, so poll rather than look once. */
    private Tap pollTap(String name, long deadline, boolean mustClose) {
        do {
            Tap t = tapFile(name, mustClose);
            if (t != Tap.ABSENT) return t;
            sleep(500);
        } while (System.currentTimeMillis() < deadline);
        return Tap.ABSENT;
    }

    private void scrollIntoView(String name) {
        for (String target : new String[]{
                "new UiSelector().resourceId(\"android:id/title\").textContains(\"" + name + "\")",
                "new UiSelector().className(\"android.widget.TextView\").textContains(\"" + name + "\")"}) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true)).setMaxSearchSwipes(30)"
                                + ".scrollIntoView(" + target + ")"));
                return;
            } catch (WebDriverException ignored) {
                // Nothing scrollable, or no match while scrolling.
            }
        }
    }

    /**
     * Taps the file's NAME label, which is what selects a document.
     *
     * <p>The file row's {@code android:id/title} first. A bare {@code textContains} also
     * matches the breadcrumb, the section headers and the search box holding the query
     * just typed, and tapping one of those reports a pick that never touched the file.
     * The fallback is still limited to TextViews for the same reason.
     *
     * <p><b>Never match on content-desc here.</b> Each grid cell also carries a child
     * described as {@code "Preview the file <name>"}, and tapping THAT opens the document
     * in an external viewer instead of selecting it. A {@code descriptionContains}
     * fallback did exactly that: the upload seed ended up inside Google Drive's viewer
     * showing "Add to Drive", the picker never returned a URI, and the case skipped with
     * a completely unrelated-looking reason.
     */
    private Tap tapFile(String fileName, boolean mustClose) {
        WebElement row = fileRow(fileName, true);
        if (row == null) return Tap.ABSENT;
        try {
            row.click();
        } catch (WebDriverException e) {
            return Tap.ABSENT; // went stale while the list was still drawing
        }
        sleep(350);
        returnFromExternalViewer();
        if (!mustClose) return Tap.CHOSEN;
        if (waitForPickerToClose(Duration.ofSeconds(8))) return Tap.CHOSEN;
        System.out.println(">>> system picker: tapped \"" + fileName + "\" and the picker"
                + " stayed open — that row is not selectable under this picker's MIME"
                + " filter, so no file was handed to the app");
        return Tap.REFUSED;
    }

    /** The displayed name label for a file, selectable or not, or null. */
    private WebElement fileRow(String fileName) {
        return fileRow(fileName, false);
    }

    /**
     * The displayed name label for a file, or null.
     *
     * @param selectableOnly skip rows the picker shows greyed out ({@code enabled="false"}):
     *        files its MIME filter refuses. A name fragment like {@code zl_test} matches
     *        both {@code zl_test_doc.pdf} and {@code zl_test_1.jpg}, and under an image
     *        filter the PDF is drawn first and disabled — taking it would end the search
     *        on a row that can never be chosen. DocumentsUI disables a refused row's
     *        children too, so the label's own flag is enough.
     */
    private WebElement fileRow(String fileName, boolean selectableOnly) {
        for (String locator : new String[]{
                "new UiSelector().resourceId(\"android:id/title\").textContains(\"" + fileName + "\")",
                "new UiSelector().className(\"android.widget.TextView\").text(\"" + fileName + "\")",
                "new UiSelector().className(\"android.widget.TextView\").textContains(\"" + fileName + "\")"}) {
            try {
                for (WebElement el : driver.findElements(AppiumBy.androidUIAutomator(locator))) {
                    if (!el.isDisplayed()) continue;
                    if (selectableOnly && "false".equalsIgnoreCase(el.getAttribute("enabled"))) continue;
                    return el;
                }
            } catch (WebDriverException ignored) { }
        }
        return null;
    }

    /** Whether the app under test is back in front, i.e. the picker handed over and closed. */
    private boolean waitForPickerToClose(Duration timeout) {
        String app = appPackage();
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                if (app.equals(driver.getCurrentPackage())) return true;
            } catch (WebDriverException ignored) {
                // Transition in flight — poll again.
            }
            sleep(400);
        }
        return false;
    }

    // ─────────────────────────────── multi-file picks ────────────────────────────────

    /**
     * Selects SEVERAL files in one picker session.
     *
     * <p>A plain tap in {@code OpenMultipleDocuments} selects the file <b>and returns to
     * the app immediately</b> — so tapping names one after another does not build a
     * multi-selection, it picks the first and then taps into whatever the app draws next.
     * That is exactly how a four-file upload silently became a one-file upload here.
     *
     * <p>The real gesture is: LONG-PRESS the first file to enter the picker's selection
     * mode, then single-tap each remaining file, then confirm. This does that, and falls
     * back to the single-file path when only one name is given.
     *
     * @return the number of files actually selected.
     */
    public int pickFiles(String[] fileNames, Duration firstTimeout) {
        if (fileNames.length == 0) return 0;
        if (fileNames.length == 1) {
            return pickFile(fileNames[0], firstTimeout) ? 1 : 0;
        }
        long deadline = System.currentTimeMillis() + firstTimeout.toMillis();
        if (!reachFile(fileNames[0], deadline)) return 0;
        if (!longPressFile(fileNames[0])) {
            // Could not enter multi-select — take the first file alone rather than
            // pretending the batch worked.
            return tapFile(fileNames[0], false) == Tap.CHOSEN ? 1 : 0;
        }
        int picked = 1;
        for (int i = 1; i < fileNames.length; i++) {
            long each = System.currentTimeMillis() + 25_000;
            // scrollOnly: once a multi-selection is under way we are already in the right
            // folder, and re-entering it through the roots drawer CLEARS the selection —
            // which is why a four-file batch kept coming back as two.
            if (reachFile(fileNames[i], each, false) && tapFile(fileNames[i], false) == Tap.CHOSEN) {
                picked++;
            } else {
                System.out.println(">>> could not add '" + fileNames[i] + "' to the selection");
            }
        }
        return picked;
    }

    /** Enters the picker's multi-select mode on a file. */
    private boolean longPressFile(String fileName) {
        // Name label only — the cell's "Preview the file <name>" description is a viewer
        // affordance that opens the document instead of selecting it (see tapFile).
        WebElement el = fileRow(fileName);
        if (el == null) return false;
        try {
            var r = el.getRect();
            driver.executeScript("mobile: longClickGesture", Map.of(
                    "x", r.getX() + r.getWidth() / 2,
                    "y", r.getY() + r.getHeight() / 2,
                    "duration", 900));
            sleep(500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private boolean reachFile(String fileName, long deadline) {
        return reachFile(fileName, deadline, true);
    }

    /**
     * Brings the file on screen without tapping it.
     *
     * @param allowNavigate when false, scroll only — never re-enter the folder through
     *        the roots drawer. Re-navigating resets a multi-selection already in
     *        progress, silently turning a four-file batch into a two-file one.
     */
    private boolean reachFile(String fileName, long deadline, boolean allowNavigate) {
        if (fileRow(fileName) != null) return true;
        if (allowNavigate) openDownloadsRoot();
        if (fileRow(fileName) != null) return true;
        scrollIntoView(fileName);
        for (int i = 0; i < 10 && System.currentTimeMillis() < deadline; i++) {
            if (fileRow(fileName) != null) return true;
            swipeUp();
        }
        return fileRow(fileName) != null;
    }

    /**
     * Commits a multi-select pick if the picker is waiting for confirmation.
     *
     * <p>{@code OpenMultipleDocuments} runs the picker in multi-select mode on some
     * versions (tap selects, a button confirms) and in tap-to-return mode on others.
     */
    public boolean confirmIfWaiting() {
        for (int i = 0; i < 5; i++) {
            if (!isOpen(Duration.ofSeconds(1))) return true;
            boolean tapped = false;
            for (String label : new String[]{"Select", "SELECT", "Open", "OPEN", "Done", "Allow"}) {
                try {
                    driver.findElement(AppiumBy.androidUIAutomator(
                            "new UiSelector().text(\"" + label + "\")")).click();
                    tapped = true;
                    break;
                } catch (WebDriverException ignored) { }
            }
            if (!tapped) return true;   // nothing to confirm — single-tap mode
            sleep(500);
        }
        return !isOpen(Duration.ofSeconds(1));
    }

    // ─────────────────────────────── navigation ───────────────────────────────

    /**
     * Walks the picker to the device storage root's {@code Documents} folder, where the
     * runner puts most fixtures.
     *
     * <p>The roots drawer lists the MediaStore collections (Recent, Documents, Downloads,
     * Images…) and then the device itself. The device is the one we want, because only
     * that root is a real directory tree. It has no stable id and its label is the
     * hardware model (e.g. {@code sdk_gphone64_arm64}), so it is identified by
     * elimination: the one root that is not a known collection name. Failing that, the
     * Documents collection is used — it reaches the same files whenever MediaStore has
     * indexed them.
     *
     * @return whether the picker moved to one of those.
     */
    public boolean openStorageDocuments() {
        if (!openRootsDrawer()) return false;
        WebElement storage = null;
        WebElement documents = null;
        try {
            for (WebElement root : driver.findElements(AppiumBy.id("android:id/title"))) {
                String label = String.valueOf(root.getText()).trim();
                if (label.isEmpty()) continue;
                if (label.equalsIgnoreCase("Documents")) {
                    documents = root;
                } else if (!COLLECTIONS.contains(label.toLowerCase())) {
                    storage = root;
                }
            }
        } catch (WebDriverException ignored) { }

        WebElement root = storage != null ? storage : documents;
        if (root == null) return false;
        try {
            root.click();
            sleep(2000);
        } catch (WebDriverException e) {
            return false;
        }
        // Inside the root, descend into Documents: on the storage root that is the real
        // /sdcard/Documents, and on the Documents collection it is the folder of the same
        // name. If it is absent we are already looking at files, so this is not an error.
        try {
            List<WebElement> folders = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"android:id/title\").text(\"Documents\")"));
            if (!folders.isEmpty()) {
                folders.get(0).click();
                sleep(2000);
            }
        } catch (WebDriverException ignored) { }
        return true;
    }

    /** Opens the roots drawer and picks a Downloads-like root. */
    public boolean openDownloadsRoot() {
        return openRoot("Downloads", "Download");
    }

    /** Opens the roots drawer and picks the first root carrying one of these labels. */
    private boolean openRoot(String... labels) {
        if (!openRootsDrawer()) return false;
        for (String root : labels) {
            try {
                List<WebElement> matches = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"" + root + "\")"));
                if (matches.isEmpty()) continue;
                // A root's name appears TWICE once the picker is already in that folder:
                // the breadcrumb behind the scrim, and the drawer's root entry. Clicking
                // the first match hits the breadcrumb under the scrim and does nothing.
                // The drawer list sits lower on screen, so take the LOWEST match.
                WebElement lowest = matches.get(0);
                for (WebElement e : matches) {
                    try {
                        if (e.getRect().getY() > lowest.getRect().getY()) lowest = e;
                    } catch (WebDriverException ignored) { }
                }
                lowest.click();
                sleep(600);
                return true;
            } catch (WebDriverException ignored) { }
        }
        System.out.println(">>> roots drawer opened but no " + String.join("/", labels)
                + " root found; roots on screen: " + visibleNames());
        return false;
    }

    private boolean openRootsDrawer() {
        try {
            driver.findElement(AppiumBy.accessibilityId("Show roots")).click();
            sleep(1000);
            return true;
        } catch (WebDriverException ignored) { }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"roots\")")).click();
            sleep(1000);
            return true;
        } catch (WebDriverException ignored) { }
        // Last resort: the hamburger's usual place, top-left of the picker's app bar.
        try {
            var size = driver.manage().window().getSize();
            driver.executeScript("mobile: clickGesture",
                    Map.of("x", (int) (size.width * 0.065), "y", (int) (size.height * 0.08)));
            sleep(1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * The picker's own search — independent of how the roots are labelled. Clears the box
     * first: re-typing into a box that still holds the last query concatenates them into
     * something that matches nothing.
     */
    public boolean searchFor(String fileName) {
        try {
            driver.findElement(AppiumBy.accessibilityId("Search")).click();
            sleep(500);
        } catch (WebDriverException e) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\"Search\")")).click();
                sleep(500);
            } catch (WebDriverException e2) { return false; }
        }
        try {
            WebElement box = driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.EditText\")"));
            box.clear();
            box.sendKeys(fileName);
            try { driver.executeScript("mobile: performEditorAction", Map.of("action", "search")); }
            catch (Exception ignored) { }
            sleep(350);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /** Some builds hide the device root until "Show internal storage" is toggled on. */
    private boolean showInternalStorage() {
        try {
            driver.findElement(AppiumBy.accessibilityId("More options")).click();
            sleep(500);
        } catch (WebDriverException e) {
            return false;
        }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"internal storage\")")).click();
            sleep(600);
            return true;
        } catch (WebDriverException e) {
            // The menu opened without the entry (already shown, or not offered): close
            // the menu, which BACK does without leaving the picker.
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            sleep(400);
            return false;
        }
    }

    private void swipeUp() {
        try {
            var size = driver.manage().window().getSize();
            driver.executeScript("mobile: swipeGesture", Map.of(
                    "left", size.width / 2 - 100, "top", (int) (size.height * 0.35),
                    "width", 200, "height", (int) (size.height * 0.45),
                    "direction", "up", "percent", 0.85));
            Thread.sleep(300);
        } catch (Exception ignored) { }
    }

    /**
     * If a tap escaped into a third-party viewer (Drive, Photos, a PDF reader), come back.
     * Leaving the run parked in another app poisons every later case.
     */
    private void returnFromExternalViewer() {
        for (int i = 0; i < 4; i++) {
            String pkg;
            try { pkg = driver.getCurrentPackage(); } catch (WebDriverException e) { return; }
            if (pkg == null || pkg.contains("documentsui") || pkg.contains("zillitapp")) return;
            System.out.println(">>> tap escaped into " + pkg + " — backing out");
            try { driver.navigate().back(); } catch (WebDriverException ignored) { return; }
            sleep(500);
        }
    }

    // ─────────────────────────────── other checks ───────────────────────────────

    /**
     * Whether a listed file can actually be chosen.
     *
     * <p>The app launches the picker with {@code FileTypeDetector.PICKER_MIME_TYPES}
     * (its whole allow-list), and DocumentsUI still DISPLAYS non-matching files — greyed
     * out, with {@code enabled="false"}. So an unsupported type is refused by the OS
     * picker before the app ever sees it, and "tapping it did nothing" is the correct
     * behaviour rather than a broken test.
     *
     * @return TRUE selectable, FALSE listed-but-disabled, null when not listed at all.
     */
    public Boolean fileSelectable(String fileName) {
        long deadline = System.currentTimeMillis() + 30_000;
        if (!reachFile(fileName, deadline)) return null;
        WebElement el = fileRow(fileName);
        if (el == null) return null;
        try {
            // The row's enabled state lives on the cell, not always on the label — walk
            // up one level when the label itself reports enabled.
            String own = el.getAttribute("enabled");
            if ("false".equalsIgnoreCase(own)) return false;
            try {
                WebElement parent = el.findElement(AppiumBy.xpath("./../.."));
                String up = parent.getAttribute("enabled");
                if ("false".equalsIgnoreCase(up)) return false;
            } catch (WebDriverException ignored) { }
            return true;
        } catch (WebDriverException e) {
            return null;
        }
    }

    /** Leaves the picker without choosing anything. */
    public boolean cancel() {
        for (int i = 0; i < 4 && isOpen(Duration.ofSeconds(1)); i++) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            sleep(400);
        }
        return !isOpen(Duration.ofSeconds(1));
    }

    /** For failure messages: what the picker is actually showing. */
    public List<String> visibleNames() {
        try {
            return driver.findElements(AppiumBy.androidUIAutomator(
                            "new UiSelector().className(\"android.widget.TextView\")"))
                    .stream().map(WebElement::getText)
                    .filter(t -> t != null && !t.isBlank()).limit(25).toList();
        } catch (WebDriverException e) { return List.of(); }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
