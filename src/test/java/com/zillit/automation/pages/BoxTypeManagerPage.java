package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Schedule types — Shoot, Prep, Day Off and whatever else a project defines.
 *
 * <p>Reached from the drawer's "Edit Types", and from the create-schedule form. Ids from
 * `new_box_dialog_type_manager.xml`: a list, a colour swatch, a name field, Add, an error
 * line and Done.
 */
public class BoxTypeManagerPage extends BoxSchedulePage {

    public BoxTypeManagerPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isLoaded(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (displayed("typesList") || displayed("btnAddType")) return true;
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    public boolean addShown() { return displayed("btnAddType"); }

    /**
     * Waits for the type manager to disappear.
     *
     * <p>A successful create closes this screen by itself: the list observer calls finish()
     * 150ms after the new type arrives from the server (TypeManagerActivity:134-139). So
     * "gone" is the app's confirmation, not a failure.
     */
    public boolean waitUntilGone(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (!displayed("typesList") && !displayed("btnAddType")) return true;
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    public boolean setName(String v) { return fill("etNewTypeName", v); }

    public String nameValue() { return text("etNewTypeName"); }

    public boolean add() { return clickIfPresent("btnAddType"); }

    public boolean done() { return clickIfPresent("btnDone"); }

    public boolean colourShown() { return displayed("newTypeColor"); }

    public boolean pickColour() { return clickIfPresent("newTypeColor"); }

    /**
     * Picks a colour no existing type is using, and says which one it picked.
     *
     * <p>This is not optional. TypeManagerActivity refuses a create whose colour any
     * existing type already holds (setupAddSection, third gate: tm_color_used), and the
     * form always opens on the same hard-coded #F39C12 — so the swatch must be driven
     * before Add can ever succeed in a project whose types already cover that colour.
     *
     * <p>The swatch opens a ColorPickerDialog: a colour area, a hex readout in color_info,
     * and "Next" as its confirm (rightButton). There is no hex field, so the colour is
     * chosen by tapping a point in the area; {@code variant} moves that point, which is
     * how a caller asks for a different colour after a clash.
     *
     * @return the #AARRGGBB the dialog reported, or null if the dialog could not be driven
     */
    public String chooseColour(int variant) {
        if (!pickColour()) {
            lastColourProblem = "the swatch newTypeColor is not in the tree or not"
                    + " clickable (it is a bare 28dp View, so it can be dropped)";
            return null;
        }
        pause(1200);
        try {
            List<WebElement> areas = driver.findElements(byId("color_picker_view"));
            if (areas.isEmpty()) {           // dialog never opened, or ids differ
                lastColourProblem = "the swatch was tapped but color_picker_view is absent."
                        + " Dialog ids on screen: " + colourDialogSnapshot();
                dismissColourPicker();
                return null;
            }
            org.openqa.selenium.Rectangle r = areas.get(0).getRect();
            // The hue comes from the ANGLE around the centre, and ColorPickerView changes
            // it on ACTION_MOVE only: ACTION_DOWN merely records whether the press was in
            // the middle, and ACTION_UP fires the listener only for a centre press
            // (ColorPickerView:64-97). So a tap can never pick a colour — this has to be a
            // drag along the ring, and it needs a stream of MOVE events, not one jump.
            dragRound(r, variant);
            pause(800);

            String hex = text("color_info");
            // "Next" is the confirm — it is the button that fires onColorChangedListener.
            if (!clickIfPresent("rightButton")) {
                lastColourProblem = "the colour area was tapped (hex read " + hex + ") but"
                        + " the confirm button rightButton is absent. Dialog ids on screen: "
                        + colourDialogSnapshot();
                dismissColourPicker();
                return null;
            }
            pause(1000);
            lastColourProblem = null;
            if (hex == null) {
                System.out.println(">>> type manager: the picker was driven but color_info"
                        + " read null. Dialog ids: " + colourDialogSnapshot());
            }
            return hex;
        } catch (Exception e) {
            lastColourProblem = "driving the picker threw " + e.getClass().getSimpleName();
            dismissColourPicker();
            return null;
        }
    }

    /** Which of the colour dialog's own ids are on screen — cheap, and names the culprit. */
    private String colourDialogSnapshot() {
        StringBuilder sb = new StringBuilder();
        for (String id : new String[] { "color_picker_view", "color_display", "color_info",
                                        "transparency_bar", "leftButton", "rightButton" }) {
            sb.append(id).append('=')
              .append(driver.findElements(byId(id)).isEmpty() ? "absent" : "present")
              .append(' ');
        }
        return sb.toString().trim();
    }

    /** Why the last chooseColour gave up, or null when it succeeded. */
    public String colourProblem() { return lastColourProblem; }

    private String lastColourProblem;

    private void dismissColourPicker() {
        if (!clickIfPresent("leftButton")) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
        }
        pause(600);
    }

    /**
     * Drags around the colour ring to a hue chosen by {@code variant}.
     *
     * <p>Stays outside the 40px centre circle, where a press is read as "tracking the
     * centre" and the hue is left alone, and well inside the edge so every sample lands on
     * the view.
     */
    private void dragRound(org.openqa.selenium.Rectangle r, int variant) {
        int cx = r.getX() + r.getWidth() / 2;
        int cy = r.getY() + r.getHeight() / 2;
        int radius = (int) (Math.min(r.getWidth(), r.getHeight()) * 0.32);
        if (radius < 60) radius = 60;          // clear of CENTER_RADIUS = 40px

        // Six well-separated hues: start at the same place, stop a different way round.
        double start = Math.PI;                                  // left of centre
        double sweep = Math.PI / 3.0 * (1 + (Math.abs(variant) % 6));

        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence drag =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        drag.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                (int) (cx + radius * Math.cos(start)), (int) (cy + radius * Math.sin(start))));
        drag.addAction(finger.createPointerDown(0));
        int steps = 12;
        for (int i = 1; i <= steps; i++) {
            double a = start + sweep * i / steps;
            drag.addAction(finger.createPointerMove(Duration.ofMillis(60),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                    (int) (cx + radius * Math.cos(a)), (int) (cy + radius * Math.sin(a))));
        }
        drag.addAction(finger.createPointerUp(0));
        driver.perform(List.of(drag));
    }

    private void tapAt(int x, int y) {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence tap =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(0));
        tap.addAction(new org.openqa.selenium.interactions.Pause(finger, Duration.ofMillis(120)));
        tap.addAction(finger.createPointerUp(0));
        driver.perform(List.of(tap));
    }

    /**
     * Fills the name, picks a free colour and presses Add, retrying on a colour clash.
     *
     * @return "" when the type was created, otherwise the app's own refusal text
     */
    public String createType(String name) {
        for (int variant = 0; variant < 6; variant++) {
            String hex = chooseColour(variant);
            if (hex == null && variant == 0) {
                // Could not drive the picker — still worth trying the default colour, which
                // succeeds in a project that is not already using #F39C12.
                System.out.println(">>> type manager: could not pick a colour — "
                        + lastColourProblem + "; falling back to the form's default");
            }
            if (!setName(name)) return "Could not type a name";
            if (!add()) return "Add was not available";
            pause(2500);

            String err = errorText();
            if (err.isEmpty()) return "";
            // Only a colour clash is worth another attempt; a name clash or empty name
            // will say the same thing however many colours we try.
            if (!err.toLowerCase().contains("color") && !err.toLowerCase().contains("colour")) {
                return err;
            }
            System.out.println(">>> type manager: colour " + hex + " was taken (" + err
                    + "); trying another");
        }
        return errorText();
    }

    /** The validation line under the field — empty when there is nothing to say. */
    public String errorText() {
        String t = text("tvTypeError");
        return t == null ? "" : t.trim();
    }

    public boolean errorShown() { return displayed("tvTypeError") && !errorText().isEmpty(); }

    /**
     * Every type name in the list.
     *
     * <p>Settles first. Counting straight after the screen opens caught the list
     * half-built — TY-02b read 2 names before an action and 12 after, and reported a
     * blank type as "accepted" on the strength of that difference. A count that depends
     * on when it was taken cannot support an assertion.
     */
    public List<String> typeNames() {
        settle();
        List<String> out = new ArrayList<>();
        try {
            List<WebElement> lists = driver.findElements(byId("typesList"));
            if (lists.isEmpty()) return out;
            for (WebElement e : lists.get(0).findElements(
                    io.appium.java_client.AppiumBy.className("android.widget.TextView"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    /**
     * Is this type in the list?
     *
     * <p>Scrolls before answering no. A type just created is appended at the END of the
     * list, which on a phone is below the fold — TY-02 failed reporting that "ZL AUTO Type
     * 70508 was added but is not in the list" while listing only the five types that
     * happened to be on screen.
     */
    /** Waits until the list stops growing, so a count means something. */
    private void settle() {
        int last = -1;
        for (int i = 0; i < 6; i++) {
            int now = rawCount();
            if (now == last && now > 0) return;
            last = now;
            pause(500);
        }
    }

    private int rawCount() {
        try {
            List<WebElement> lists = driver.findElements(byId("typesList"));
            if (lists.isEmpty()) return 0;
            return lists.get(0).findElements(
                    io.appium.java_client.AppiumBy.className("android.widget.TextView")).size();
        } catch (Exception e) { return 0; }
    }

    public boolean hasType(String name) {
        if (typeNames().stream().anyMatch(n -> n != null && n.contains(name))) return true;
        try {
            driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".setMaxSearchSwipes(4)"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + name + "\"))"));
            pause(600);
        } catch (Exception ignored) {
            // Not scrollable, or genuinely absent — the re-read below decides.
        }
        return typeNames().stream().anyMatch(n -> n != null && n.contains(name));
    }

    /**
     * Taps a row's delete control.
     *
     * <p>Rows are built entirely in code, with no ids at all (TypeManagerActivity's
     * TypeViewHolder): a LinearLayout holding a colour dot, the name TextView, a "SYSTEM"
     * badge, then an edit and a delete control — both {@code ImageButton}, in that order,
     * and both GONE for system-defined types, which therefore cannot be deleted at all.
     *
     * <p>So the row has to be found by the name its TextView carries and then walked as a
     * whole: a search for the name that lands on the TextView finds no controls, because
     * the TextView is the name's own node and has no children. Delete is the LAST
     * ImageButton — the first is edit, and clicking that opens the wrong dialog.
     */
    public boolean deleteType(String name) {
        try {
            List<WebElement> lists = driver.findElements(byId("typesList"));
            if (lists.isEmpty()) return false;
            // Direct children of the list are the row containers.
            for (WebElement row : lists.get(0).findElements(
                    io.appium.java_client.AppiumBy.xpath("./*"))) {
                boolean isThisRow = false;
                for (WebElement label : row.findElements(
                        io.appium.java_client.AppiumBy.className("android.widget.TextView"))) {
                    String t = label.getText();
                    if (t != null && t.contains(name)) { isThisRow = true; break; }
                }
                if (!isThisRow) continue;

                List<WebElement> buttons = row.findElements(
                        io.appium.java_client.AppiumBy.className("android.widget.ImageButton"));
                if (buttons.isEmpty()) {
                    System.out.println(">>> type manager: \"" + name + "\" has no edit/delete"
                            + " controls, which is how a system-defined type renders");
                    return false;
                }
                WebElement del = buttons.get(buttons.size() - 1);   // edit first, delete last
                if (!del.isDisplayed()) return false;
                del.click();
                pause(1200);
                return true;
            }
            System.out.println(">>> type manager: no row carries \"" + name + "\"; rows: "
                    + typeNames());
            return false;
        } catch (Exception e) { return false; }
    }

    /** Confirms the delete dialog — "Delete Schedule" / Delete, built by AlertDialog. */
    public boolean confirmDelete() {
        for (String label : new String[] { "Delete", "DELETE", "Yes", "OK" }) {
            if (tapText(label)) { pause(2000); return true; }
        }
        return false;
    }

    private boolean fill(String id, String value) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (!e.isDisplayed()) continue;
                e.click();
                e.clear();
                if (!value.isEmpty()) e.sendKeys(value);
                hideKeyboard();
                pause(600);
                return true;
            }
            return false;
        } catch (Exception e) { return false; }
    }
}
