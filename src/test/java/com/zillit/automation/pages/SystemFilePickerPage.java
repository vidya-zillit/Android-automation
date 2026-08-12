package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * The ANDROID SYSTEM file picker ({@code com.google.android.documentsui}), driven for
 * Document Distribution's {@code OpenMultipleDocuments} upload.
 *
 * <p>Deliberately separate from {@link DriveUploadPage}, which drives the same picker for
 * Drive: that one is proven against Drive's flow and its own fixtures, and widening it to
 * fix a Doc-Distribution problem would put every Drive suite at risk for no benefit.
 *
 * <p><b>Everything here is text/description matching.</b> The picker is a FOREIGN package,
 * so {@code byId()} — which prefixes the app-under-test's package — cannot match a single
 * node in it.
 *
 * <p><b>The trap that made this class necessary.</b> The picker opens on <i>Recent</i>,
 * and Recent is ordered by the file's MODIFICATION time. {@code adb push} preserves the
 * source file's mtime, so a fixture pushed today can carry a date from weeks ago and
 * never appear in Recent at all — the file is plainly on the device, in the right folder,
 * and the picker still does not show it. Reaching it therefore ALWAYS means leaving
 * Recent, and this class tries three independent ways to do that before giving up, each
 * of which fails differently:
 * <ol>
 *   <li>the roots drawer ("Show roots") → a Downloads root,</li>
 *   <li>the picker's own Search, which is unaffected by the drawer's labelling,</li>
 *   <li>the "More options" overflow → "Show internal storage", then the storage root —
 *       needed on builds that hide Downloads until internal storage is revealed.</li>
 * </ol>
 */
public class SystemFilePickerPage extends BasePage {

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
            return tapFile(fileNames[0]) ? 1 : 0;
        }
        int picked = 1;
        for (int i = 1; i < fileNames.length; i++) {
            long each = System.currentTimeMillis() + 25_000;
            // scrollOnly: once a multi-selection is under way we are already in the right
            // folder, and re-entering it through the roots drawer CLEARS the selection —
            // which is why a four-file batch kept coming back as two.
            if (reachFile(fileNames[i], each, false) && tapFile(fileNames[i])) {
                picked++;
            } else {
                System.out.println(">>> could not add '" + fileNames[i] + "' to the selection");
            }
        }
        return picked;
    }

    /** Enters the picker's multi-select mode on a file. */
    private boolean longPressFile(String fileName) {
        // Text only — the cell's "Preview the file <name>" description is a viewer
        // affordance that opens the document instead of selecting it (see tapFile).
        for (String locator : new String[]{
                "new UiSelector().text(\"" + fileName + "\")",
                "new UiSelector().textContains(\"" + fileName + "\")"}) {
            try {
                WebElement el = driver.findElement(AppiumBy.androidUIAutomator(locator));
                var r = el.getRect();
                driver.executeScript("mobile: longClickGesture", Map.of(
                        "x", r.getX() + r.getWidth() / 2,
                        "y", r.getY() + r.getHeight() / 2,
                        "duration", 900));
                sleep(500);
                return true;
            } catch (WebDriverException ignored) { }
        }
        return false;
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
        if (onScreen(fileName)) return true;
        if (allowNavigate) openDownloadsRoot();
        if (onScreen(fileName)) return true;
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).setMaxSearchSwipes(30)"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + fileName + "\"))"));
        } catch (WebDriverException ignored) { }
        for (int i = 0; i < 10 && System.currentTimeMillis() < deadline; i++) {
            if (onScreen(fileName)) return true;
            swipeUp();
        }
        return onScreen(fileName);
    }

    private boolean onScreen(String fileName) {
        try {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + fileName + "\")")).isEmpty();
        } catch (WebDriverException e) { return false; }
    }

    /**
     * Selects a file by name, trying each navigation strategy in turn.
     *
     * @return true once the file has been tapped (selected or returned).
     */
    public boolean pickFile(String fileName, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        if (tapFile(fileName)) return true;
        if (openDownloadsRoot() && findAndTap(fileName, deadline)) return true;
        if (searchFor(fileName) && findAndTap(fileName, deadline)) return true;
        if (showInternalStorage() && openDownloadsRoot() && findAndTap(fileName, deadline)) return true;

        System.out.println(">>> picker could not reach '" + fileName + "'. On screen now: "
                + visibleNames());
        return false;
    }

    /** Scrolls the current listing looking for the file until the budget runs out. */
    private boolean findAndTap(String fileName, long deadline) {
        if (tapFile(fileName)) return true;
        // Downloads is sorted by modified date and holds every fixture this rig has ever
        // pushed, so the wanted file can be far down the list. scrollIntoView drives the
        // container itself; a manual sweep follows in case the grid is not reported as
        // scrollable (which some picker layouts do).
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).setMaxSearchSwipes(30)"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + fileName + "\"))"));
            if (tapFile(fileName)) return true;
        } catch (WebDriverException ignored) { }

        for (int i = 0; i < 12 && System.currentTimeMillis() < deadline; i++) {
            swipeUp();
            if (tapFile(fileName)) return true;
        }
        return false;
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
     * Taps the file's NAME label, which is what selects a document.
     *
     * <p><b>Never match on content-desc here.</b> Each grid cell also carries a child
     * described as {@code "Preview the file <name>"}, and tapping THAT opens the document
     * in an external viewer instead of selecting it. A `descriptionContains` fallback did
     * exactly that: the upload seed ended up inside Google Drive's viewer showing
     * "Add to Drive", the picker never returned a URI, and the case skipped with a
     * completely unrelated-looking reason.
     */
    private boolean tapFile(String fileName) {
        for (String locator : new String[]{
                "new UiSelector().text(\"" + fileName + "\")",
                "new UiSelector().textContains(\"" + fileName + "\")"}) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(locator)).click();
                sleep(350);
                returnFromExternalViewer();
                return true;
            } catch (WebDriverException ignored) { }
        }
        return false;
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

    /** Opens the roots drawer and picks a Downloads-like root. */
    public boolean openDownloadsRoot() {
        if (!openRootsDrawer()) return false;
        for (String root : new String[]{"Downloads", "Download"}) {
            try {
                List<WebElement> matches = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"" + root + "\")"));
                if (matches.isEmpty()) continue;
                // "Downloads" appears TWICE once the picker is already in that folder:
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
                sleep(300);
                return true;
            } catch (WebDriverException ignored) { }
        }
        System.out.println(">>> roots drawer opened but no Downloads root found; roots on screen: "
                + visibleNames());
        return false;
    }

    private boolean openRootsDrawer() {
        try {
            driver.findElement(AppiumBy.accessibilityId("Show roots")).click();
            sleep(600);
            return true;
        } catch (WebDriverException ignored) { }
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"roots\")")).click();
            sleep(600);
            return true;
        } catch (WebDriverException ignored) { }
        // Last resort: the hamburger's usual place, top-left of the picker's app bar.
        try {
            var size = driver.manage().window().getSize();
            driver.executeScript("mobile: clickGesture",
                    Map.of("x", (int) (size.width * 0.065), "y", (int) (size.height * 0.08)));
            sleep(600);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** The picker's own search — independent of how the roots are labelled. */
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
            box.sendKeys(fileName);
            try { driver.executeScript("mobile: performEditorAction", Map.of("action", "search")); }
            catch (Exception ignored) { }
            sleep(350);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /** Some builds hide Downloads until "Show internal storage" is toggled on. */
    private boolean showInternalStorage() {
        try {
            driver.findElement(AppiumBy.accessibilityId("More options")).click();
            sleep(500);
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"internal storage\")")).click();
            sleep(600);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
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
        try {
            WebElement el = driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + fileName + "\")"));
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
