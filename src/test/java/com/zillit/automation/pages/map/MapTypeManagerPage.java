package com.zillit.automation.pages.map;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Map &rarr; <b>location types</b> ({@code headerManagerFragment}), reached from
 * {@code btnTypes} and from the location form's "add new type".
 *
 * <p>The list is a normal layout ({@code fragment_header_manager.xml} +
 * {@code item_header_card.xml}), but the <b>form is built entirely in code</b> — an
 * {@code EditText}, a "Select Icon" label, a preview line and an emoji {@code GridLayout},
 * none of which carry resource ids. So the form has to be driven by TEXT: the field by its
 * hint "Type name (e.g., Hotel)", the buttons by their captions ("Create"/"Update",
 * "Cancel"), the icon state by the preview reading "No icon selected" or "Selected: X".
 *
 * <p>That is also why the two confirmations matter more than usual here: changing or
 * deleting a type rewrites the classification of every location already using it, and the
 * dialogs are the only place that is said.
 */
public class MapTypeManagerPage extends BasePage {

    public static final String HINT_TYPE_NAME = "Type name (e.g., Hotel)";
    public static final String LABEL_SELECT_ICON = "Select Icon";
    public static final String NO_ICON = "No icon selected";
    public static final String TITLE_ADD = "Add Type";
    public static final String TITLE_EDIT = "Edit Type";
    public static final String BTN_CREATE = "Create";
    public static final String BTN_UPDATE = "Update";
    public static final String BTN_CANCEL = "Cancel";
    public static final String HINT_SUBTYPE = "Add a subtype...";
    public static final String BTN_ADD_SUBTYPE = "+ Add";

    public static final String TITLE_UPDATE_CONFIRM = "Update Location Type";
    public static final String TITLE_DELETE_CONFIRM = "Delete Location Type";

    public MapTypeManagerPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- list ----------------

    public boolean isLoaded(Duration timeout) {
        return isVisible("rvHeaders", timeout) || isVisible("etTypeSearch", timeout);
    }

    public String countText() { return text("tvCount"); }

    /** Add is hidden entirely without posting access. */
    public boolean addShown() { return present("btnAdd"); }

    public boolean openAdd() { return clickIfPresent("btnAdd"); }

    public boolean search(String term) { return fill("etTypeSearch", term); }

    public List<String> typeNames()    { return textsIn("rvHeaders", "tvName"); }
    public List<String> typeIcons()    { return textsIn("rvHeaders", "tvIcon"); }
    public List<String> typeSubtypes() { return textsIn("rvHeaders", "tvSubtypes"); }

    public int typeCount() { return typeNames().size(); }

    /**
     * Taps a per-row button on the row identified by name.
     *
     * <p>XPath first, then an index pairing. {@code ancestor::*[...][1]} is evaluated
     * inconsistently by UiAutomator's XPath — it can return the outermost matching
     * ancestor rather than the nearest — which taps another row's button, or nothing at
     * all, with no error to show for it.
     */
    private boolean tapRowButton(String typeName, String buttonId) {
        try {
            driver.findElement(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/tvName' and @text='" + typeName + "']"
                            + "/ancestor::*[.//*[@resource-id='" + appPackage() + ":id/" + buttonId + "']][1]"
                            + "//*[@resource-id='" + appPackage() + ":id/" + buttonId + "']")).click();
            pause(1300);
            return true;
        } catch (Exception ignored) { }
        try {
            List<String> names = typeNames();
            int idx = names.indexOf(typeName);
            if (idx < 0) return false;
            List<WebElement> btns = driver.findElements(byId(buttonId));
            if (idx >= btns.size()) return false;
            btns.get(idx).click();
            pause(1300);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean editType(String name)   { return tapRowButton(name, "btnEdit"); }
    public boolean deleteType(String name) { return tapRowButton(name, "btnDelete"); }

    // ---------------- the code-built form ----------------

    /** The form is identified by its name field's hint, since it carries no ids. */
    public boolean formShown(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (byText(HINT_TYPE_NAME) != null) return true;
            if (byText(TITLE_ADD) != null || byText(TITLE_EDIT) != null) return true;
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    public String formTitle() {
        if (byText(TITLE_EDIT) != null) return TITLE_EDIT;
        if (byText(TITLE_ADD) != null) return TITLE_ADD;
        return null;
    }

    public boolean iconGridShown() {
        return byText(LABEL_SELECT_ICON) != null;
    }

    /** "No icon selected" until one is picked, then "Selected: &lt;emoji&gt;". */
    public boolean noIconSelected() {
        return byText(NO_ICON) != null;
    }

    public boolean setTypeName(String value) {
        WebElement f = nameField();
        if (f == null) return false;
        try {
            f.click();
            f.clear();
            f.sendKeys(value);
            pause(900);
            // CLOSE THE KEYBOARD before anyone reaches for Cancel/Create.
            //
            // Typing leaves the IME up, and it covers the bottom of the dialog — which is
            // exactly where those two buttons sit. Worse, while the IME holds the active
            // window the dialog's own views stop coming back from the lookups: a search
            // for android.widget.Button returned an EMPTY list on a screen that plainly
            // had Cancel and Create on it. That empty list was the clue; the buttons were
            // never missing, they were behind the keyboard.
            hideKeyboard();
            pause(600);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean submitForm() {
        return tapText(BTN_CREATE) || tapText(BTN_UPDATE);
    }

    public boolean cancelForm() {
        return tapText(BTN_CANCEL);
    }

    // ---------------- helpers ----------------

    private WebElement byText(String t) {
        try {
            List<WebElement> els = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + t + "\")"));
            return els.isEmpty() ? null : els.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * The field carrying {@code hint}, or null.
     *
     * <p>The fallback used to be hard-coded to {@code contains(@hint,'Type name')} AND to
     * "otherwise the first EditText on screen". Both were wrong for anything but the name
     * field: asking for the subtype hint returned the NAME field, so `addSubtype` cleared
     * the type's name and typed the subtype over it. T-11 then failed reporting that the
     * subtype field "still holds ZL AUTO Sub 16600" — which was the type name, sitting in
     * the field the helper had wandered into.
     *
     * <p>Now it matches the hint it was ACTUALLY given, and returns null rather than
     * guessing. Callers that have a safe positional fallback say so themselves.
     */
    private WebElement byHint(String hint) {
        // The hint shows as `text` while the field is empty and moves to `content-desc`
        // (or disappears) once it has a value, so both are tried.
        WebElement e = byText(hint);
        if (e != null) return e;
        try {
            // Hints are often ellipsised on screen, so match on a stable leading chunk
            // rather than the whole string.
            String needle = hint.replace("...", "").replace("…", "").trim();
            List<WebElement> els = driver.findElements(AppiumBy.xpath(
                    "//android.widget.EditText[contains(@hint," + quote(needle) + ")]"));
            if (!els.isEmpty()) return els.get(0);
        } catch (Exception ignored) {
        }
        return null;
    }

    /** All EditTexts in the sheet, in layout order: name first, subtype last. */
    private List<WebElement> editFields() {
        try {
            return driver.findElements(AppiumBy.className("android.widget.EditText"));
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    /**
     * The type-name field. Safe to fall back to the FIRST EditText: the form builds the
     * name input before the icon grid and the subtype row ({@code HeaderManagerFragment:156}).
     */
    private WebElement nameField() {
        WebElement e = byHint(HINT_TYPE_NAME);
        if (e != null) return e;
        List<WebElement> all = editFields();
        return all.isEmpty() ? null : all.get(0);
    }

    /**
     * The subtype field. Falls back to the LAST EditText — the add-subtype row is built
     * after the name and the icon grid (:296) — and only when there is more than one, so
     * it can never land on the name field.
     */
    private WebElement subtypeField() {
        WebElement e = byHint(HINT_SUBTYPE);
        if (e != null) return e;
        List<WebElement> all = editFields();
        return all.size() > 1 ? all.get(all.size() - 1) : null;
    }

    /**
     * Drags a BottomSheetDialog up to its expanded height.
     *
     * <p>{@code HeaderManagerFragment:318} shows the add-type form in a
     * {@code BottomSheetDialog}, which opens at its peek height. Its Cancel/Create row
     * sits under the name field, the "Select Icon" emoji grid and the subtype rows, so at
     * peek height the buttons are off screen — present in the hierarchy, and untappable.
     * Scrolling the CONTENT does not help when it is the sheet itself that is short.
     */
    public void expandSheet() {
        // Only when it is actually needed. With the keyboard down this dialog comes up at
        // full height already, and a stray drag across it scrolls the icon grid or pulls
        // the sheet toward dismissal — doing harm in the common case to fix a rare one.
        try {
            hideKeyboard();
            pause(400);
            WebElement create = byText(BTN_CREATE);
            if (create != null && displayed(create)) return;
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            int x = size.getWidth() / 2;
            swipe(x, (int) (size.getHeight() * 0.80), x, (int) (size.getHeight() * 0.35));
            pause(900);
        } catch (Exception ignored) {
            // Not a sheet, or already expanded — submitForm() still scrolls and re-reads.
        }
    }

    /**
     * Every non-empty label on screen, whatever widget carries it.
     *
     * <p>Was limited to {@code android.widget.Button} and came back EMPTY while the sheet
     * was demonstrably open with its name field typed into — so the Create button is not
     * reported as a Button. Guessing at the right class twice is enough; this reads the
     * text of everything and lets the log say what is actually there.
     */
    public List<String> visibleButtonTexts() {
        List<String> out = new java.util.ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath("//*[string-length(@text)>0]"))) {
                String t = e.getText();
                if (t != null && !t.isBlank() && t.length() < 40) out.add(t.trim());
                // 60, not 25. At 25 the list stopped inside the emoji grid and the very
                // labels being hunted — Subtypes, Cancel, Create — were cut off by the cap
                // itself, which made an absent-looking dialog out of a present one. A
                // diagnostic that truncates the answer is worse than none.
                if (out.size() >= 60) break;
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /**
     * Types a subtype name and taps "+ Add".
     *
     * <p>The add row is built in code ({@code HeaderManagerFragment:296-314}): an EditText
     * hinted "Add a subtype..." and — note — a <b>TextView</b> reading "+ Add", not a
     * Button. That is part of why a search by button class found nothing here.
     *
     * @return true when the field CLEARED, which is how the app signals it accepted the
     *         name: the handler adds it, clears the field and re-renders only when the
     *         name is non-blank and not already in the list.
     */
    public boolean addSubtype(String name) {
        try {
            WebElement f = subtypeField();
            if (f == null) return false;
            f.click();
            f.clear();
            if (!name.isEmpty()) f.sendKeys(name);
            hideKeyboard();
            pause(500);
            if (!tapText(BTN_ADD_SUBTYPE)) return false;
            pause(800);
            return subtypeFieldText().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /** What is left sitting in the subtype input. Empty after an accepted add. */
    public String subtypeFieldText() {
        try {
            WebElement f = subtypeField();
            if (f == null) return "";
            String t = f.getText();
            if (t == null || t.equals(HINT_SUBTYPE)) return "";
            return t.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean tapText(String t) {
        WebElement e = byText(t);
        // BELOW THE FOLD. This form is built in code — name field, then the "Select Icon"
        // emoji grid, then the subtype rows, and only then the Cancel/Create row — so on a
        // phone the buttons sit under the bottom edge. byText() still FINDS the button,
        // because a ScrollView keeps its children in the hierarchy, but clicking it taps
        // coordinates that are off screen. That is why T-05 skipped with "Create button
        // not found" while the button was plainly there.
        if (e == null || !displayed(e)) {
            scrollTextIntoView(t);
            e = byText(t);
        }
        if (e == null) return false;
        try {
            e.click();
            pause(1200);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean displayed(WebElement e) {
        try { return e.isDisplayed(); } catch (Exception ex) { return false; }
    }

    /**
     * Brings {@code t} onto the screen by dragging the sheet up, and reports whether it
     * got there.
     *
     * <p>UiScrollable was tried first and does NOT work here: it binds to the first
     * {@code scrollable(true)} container it finds, which is the type list BEHIND the
     * bottom sheet, so it scrolled the wrong thing and the button stayed where it was.
     * This reads the button's own bounds instead — the element IS in the hierarchy, just
     * below the bottom edge — and swipes by exactly the shortfall.
     */
    private boolean scrollTextIntoView(String t) {
        try {
            List<WebElement> els = driver.findElements(
                    AppiumBy.xpath("//*[@text=" + quote(t) + "]"));
            if (els.isEmpty()) return false;
            org.openqa.selenium.Rectangle r = els.get(0).getRect();
            int screenH = driver.manage().window().getSize().getHeight();
            int centreY = r.getY() + r.getHeight() / 2;
            // Already comfortably on screen — nothing to do.
            if (centreY > 0 && centreY < screenH - r.getHeight()) return true;
            int shortfall = centreY - (int) (screenH * 0.6);
            if (shortfall <= 0) return true;
            int x = driver.manage().window().getSize().getWidth() / 2;
            swipe(x, (int) (screenH * 0.75), x, (int) (screenH * 0.75) - shortfall);
            pause(800);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Single-finger swipe, used to move the sheet rather than the list behind it. */
    private void swipe(int fromX, int fromY, int toX, int toY) {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence seq =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(java.time.Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), fromX, fromY));
        seq.addAction(finger.createPointerDown(0));
        seq.addAction(finger.createPointerMove(java.time.Duration.ofMillis(700),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), toX, toY));
        seq.addAction(finger.createPointerUp(0));
        driver.perform(java.util.List.of(seq));
    }

    private static String quote(String v) {
        return v.contains("'") ? "concat('" + v.replace("'", "',\"'\",'") + "')" : "'" + v + "'";
    }

    private boolean present(String id) {
        try { return !driver.findElements(byId(id)).isEmpty(); }
        catch (Exception e) { return false; }
    }

    private String text(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            return els.isEmpty() ? null : els.get(0).getText();
        } catch (Exception e) { return null; }
    }

    private List<String> textsIn(String containerId, String childId) {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(
                    "//*[@resource-id='" + appPackage() + ":id/" + containerId + "']"
                            + "//*[@resource-id='" + appPackage() + ":id/" + childId + "']"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    private boolean clickIfPresent(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            if (els.isEmpty()) return false;
            els.get(0).click();
            pause(1300);
            return true;
        } catch (Exception e) { return false; }
    }

    private boolean fill(String id, String value) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            if (els.isEmpty()) return false;
            WebElement f = els.get(0);
            f.click();
            f.clear();
            f.sendKeys(value);
            pause(1100);
            return true;
        } catch (Exception e) { return false; }
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
