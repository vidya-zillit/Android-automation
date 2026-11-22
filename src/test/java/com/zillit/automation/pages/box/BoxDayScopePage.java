package com.zillit.automation.pages.box;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * BOX — the "this date only vs the complete schedule" prompt.
 *
 * <p>Shown before Edit and before Delete on any multi-day schedule
 * ({@code BoxScheduleDayScopePrompt}). It is the one thing standing between picking a
 * single date and changing — or wiping — every other day in the block, and the source
 * records that Day Detail once got this wrong in a way that destroyed data silently: Edit
 * acted on one day while Delete removed the whole block, with no warning.
 *
 * <p>A one-day schedule has no meaningful choice, so the prompt is skipped and COMPLETE is
 * assumed. That short-circuit is deliberate, so a missing dialog there is correct — but a
 * delete still has to confirm.
 *
 * <p><b>The two options and the confirm button are different things.</b> This page object
 * used to treat them as one, and that is why option two could never be chosen: the strings
 * "Edit complete schedule" / "Delete complete schedule" are the CONFIRM BUTTON's caption,
 * and the button only reads that way AFTER the second option is selected. Tapping that text
 * on a freshly opened dialog looks for something that is not on screen yet. Worse, the
 * opposite pair — "Edit this date" / "Delete this date" — is the caption the button opens
 * with, so tapping it appeared to work while really doing nothing but confirm the default.
 *
 * <p>The options themselves read "This date only" and "Complete schedule", and they are
 * real views with real ids ({@code optScopeThisDate} / {@code optScopeComplete} in
 * {@code new_box_dialog_schedule_scope.xml}). Their RadioButtons are declared
 * {@code clickable="false"} and {@code focusable="false"} — the click listener sits on the
 * option's container — so a tap aimed at the radio itself is dropped. Everything here goes
 * by id for that reason, and the radios are read only to confirm what got selected.
 *
 * <p>The dialog is a ScrollView, so on a short screen its footer can sit below the fold and
 * out of the accessibility tree entirely. {@link #confirm()} scrolls to it first.
 */
public class BoxDayScopePage extends BoxSchedulePage {

    public static final String TITLE_EDIT    = "Edit schedule";
    public static final String TITLE_DELETE  = "Delete schedule";
    public static final String SUBTITLE      = "This schedule covers more than one day.";
    public static final String ASK_EDIT      = "What do you want to edit?";
    public static final String ASK_DELETE    = "What do you want to delete?";

    public static final String BTN_EDIT_THIS     = "Edit this date";
    public static final String BTN_EDIT_ALL      = "Edit complete schedule";
    public static final String BTN_DELETE_THIS   = "Delete this date";
    public static final String BTN_DELETE_ALL    = "Delete complete schedule";

    /** The promise the "this date" option makes, which the numbers must then honour. */
    public static final String PROMISE_OTHERS_STAY = "Every other day";

    public BoxDayScopePage(AndroidDriver driver) {
        super(driver);
    }

    public boolean shownForEdit(Duration timeout)   { return waitForText(TITLE_EDIT, timeout); }
    public boolean shownForDelete(Duration timeout) { return waitForText(TITLE_DELETE, timeout); }

    public boolean saysOthersStay()  { return textShown(PROMISE_OTHERS_STAY); }
    public boolean saysMultiDay()    { return textShown(SUBTITLE); }
    public boolean asksWhatToEdit()  { return textShown(ASK_EDIT); }
    public boolean asksWhatToDelete(){ return textShown(ASK_DELETE); }

    // ───────────────────────── the two options ─────────────────────────

    public static final String OPT_THIS_DATE = "This date only";
    public static final String OPT_COMPLETE  = "Complete schedule";

    /**
     * How many days the block being decided about actually covers.
     *
     * <p>Read from the dialog's own meta line, which the app builds as
     * {@code "%1$s · %2$s – %3$s"} — "15 day(s) · Nov 2 – Nov 16, 2026". That number is the
     * authority on the block's size, and it is the only one available at the moment of the
     * decision: the project's total for a type says nothing about THIS block, because a
     * project can hold several blocks of the same type.
     *
     * @return the block's day count, or -1 when the line cannot be read
     */
    public int blockDayCount() {
        String meta = text("tvScheduleScopeMeta");
        if (meta == null) return -1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(meta);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** The meta line verbatim, for a failure message worth reading. */
    public String metaLine() { return text("tvScheduleScopeMeta"); }

    public boolean offersThisDate() { return displayed("optScopeThisDate"); }
    public boolean offersComplete() { return displayed("optScopeComplete"); }

    /** Selects "This date only". The container carries the listener, not the radio. */
    public boolean pickThisDate() { return pickOption("optScopeThisDate", OPT_THIS_DATE); }

    /** Selects "Complete schedule". */
    public boolean pickComplete() { return pickOption("optScopeComplete", OPT_COMPLETE); }

    private boolean pickOption(String id, String titleText) {
        // The option container carries the click listener, which is also what keeps it in
        // the accessibility tree — a LinearLayout with no listener gets pruned out, as the
        // Sides script card's lay_actions does. If it is missing anyway, tapping the
        // option's own title works just as well: the tap lands on the TextView and bubbles
        // up to the clickable container behind it.
        if (scrollDialogTo(id) && clickIfPresent(id)) {
            pause(700);
            return true;
        }
        if (tapText(titleText)) {
            pause(700);
            return true;
        }
        return false;
    }

    public boolean thisDateSelected() { return radioChecked("rbScopeThisDate"); }
    public boolean completeSelected() { return radioChecked("rbScopeComplete"); }

    private boolean radioChecked(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                return "true".equalsIgnoreCase(String.valueOf(e.getAttribute("checked")));
            }
        } catch (Exception ignored) { }
        return false;
    }

    // ───────────────────────── the confirm button ─────────────────────────

    /**
     * The confirm button's caption, which names the scope that is actually about to run.
     *
     * <p>Worth asserting on its own: it is the last thing the user reads before a tap that
     * can wipe a whole block, so "Delete this date" must not be on a button that is about
     * to delete thirteen.
     */
    public String confirmLabel() { return text("btnScheduleScopeConfirm"); }

    public boolean confirm() {
        if (!scrollDialogTo("btnScheduleScopeConfirm")) return false;
        boolean tapped = clickIfPresent("btnScheduleScopeConfirm");
        if (tapped) pause(2500);
        return tapped;
    }

    /** Selects a scope and confirms it, checking the radio took before committing. */
    public boolean chooseThisDate() {
        if (!pickThisDate() || !thisDateSelected()) return false;
        return confirm();
    }

    public boolean chooseComplete() {
        if (!pickComplete() || !completeSelected()) return false;
        return confirm();
    }

    /**
     * Scrolls the dialog until the view is on screen.
     *
     * <p>The dialog's own ScrollView has no id, so this swipes within the dialog's bounds,
     * taken from a header view that is always visible.
     */
    private boolean scrollDialogTo(String id) {
        if (displayed(id)) return true;
        for (int pass = 0; pass < 6; pass++) {
            if (!dragInsideDialog()) return false;
            if (displayed(id)) return true;
        }
        return false;
    }

    private boolean dragInsideDialog() {
        try {
            List<WebElement> head = driver.findElements(byId("tvScheduleScopeTitle"));
            if (head.isEmpty()) return false;
            org.openqa.selenium.Rectangle r = head.get(0).getRect();
            org.openqa.selenium.Dimension screen = driver.manage().window().getSize();
            int x = r.getX() + r.getWidth() / 2;
            int from = (int) (screen.getHeight() * 0.72);
            int to = (int) (screen.getHeight() * 0.40);
            org.openqa.selenium.interactions.PointerInput f =
                    new org.openqa.selenium.interactions.PointerInput(
                            org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence seq =
                    new org.openqa.selenium.interactions.Sequence(f, 1);
            seq.addAction(f.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, from));
            seq.addAction(f.createPointerDown(0));
            for (int i = 1; i <= 8; i++) {
                seq.addAction(f.createPointerMove(Duration.ofMillis(45),
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                        x, from + (to - from) * i / 8));
            }
            seq.addAction(f.createPointerUp(0));
            driver.perform(List.of(seq));
            pause(600);
            return true;
        } catch (Exception e) { return false; }
    }

    /** Cancels by id — "Cancel" as text collides with whatever is behind the dialog. */
    public boolean cancel() {
        boolean tapped = clickIfPresent("btnScheduleScopeCancel");
        if (tapped) pause(1500);
        return tapped;
    }

    public boolean close() {
        boolean tapped = clickIfPresent("btnScheduleScopeClose");
        if (tapped) pause(1500);
        return tapped;
    }

    public boolean shown() { return displayed("optScopeThisDate"); }

    private boolean waitForText(String label, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (textShown(label)) return true;
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }
}
