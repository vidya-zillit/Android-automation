package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;

import java.time.Duration;

/**
 * The Replace / Extend / Overlap chooser, shown when a schedule lands on dates that
 * already carry one.
 *
 * <p>Raised by an HTTP 409 from the server on a multi-date save, and raised UP FRONT when
 * one day's type is changed from a calendar cell. Ids from `new_box_dialog_conflict.xml`.
 */
public class BoxConflictPage extends BoxSchedulePage {

    /** Option wording, from strings.xml. The descriptions say what each one does. */
    public static final String REPLACE = "Replace";
    public static final String EXTEND  = "Extend";
    public static final String OVERLAP = "Overlap";

    public static final String REPLACE_DESC =
            "Remove existing schedule on these dates and use your new schedule instead";
    public static final String EXTEND_DESC =
            "Keep existing schedule, extend it at the end to make room";
    public static final String OVERLAP_DESC =
            "Keep existing schedule and also add the new one on the same dates";

    public BoxConflictPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean shown(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (displayed("optionReplace") || displayed("tvConflictMessage")) return true;
            pause(350);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    /** "…N dates conflict…" — the message naming how many dates clash. */
    public String message() { return text("tvConflictMessage"); }

    public boolean replaceOffered() { return displayed("optionReplace"); }
    public boolean extendOffered()  { return displayed("optionExtend"); }
    public boolean overlapOffered() { return displayed("optionOverlap"); }

    public boolean chooseReplace() { return clickIfPresent("optionReplace"); }
    public boolean chooseExtend()  { return clickIfPresent("optionExtend"); }
    public boolean chooseOverlap() { return clickIfPresent("optionOverlap"); }

    public boolean back() { return clickIfPresent("btnBack"); }

    /** Does the sheet explain what each option does, rather than just naming it? */
    public boolean descriptionsShown() {
        return textShown("Remove existing schedule") || textShown("extend it at the end");
    }
}
