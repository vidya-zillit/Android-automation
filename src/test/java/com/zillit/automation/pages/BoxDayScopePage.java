package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;

import java.time.Duration;

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
 * <p>Built in code with radio cards and no ids, so everything here goes by the app's own
 * wording.
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

    public boolean offersThisDateForEdit() { return textShown(BTN_EDIT_THIS); }
    public boolean offersCompleteForEdit() { return textShown(BTN_EDIT_ALL); }

    public boolean chooseThisDateForEdit() { return tapText(BTN_EDIT_THIS); }
    public boolean chooseCompleteForEdit() { return tapText(BTN_EDIT_ALL); }
    public boolean chooseThisDateForDelete() { return tapText(BTN_DELETE_THIS); }
    public boolean chooseCompleteForDelete() { return tapText(BTN_DELETE_ALL); }

    public boolean cancel() { return tapText("Cancel"); }

    private boolean waitForText(String label, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (textShown(label)) return true;
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }
}
