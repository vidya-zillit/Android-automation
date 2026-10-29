package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Settings → "Zillit Help" → "Tutorial for Admin" — the guided coach-mark tour.
 *
 * <p>Opening it drops the app on the Home tab and lays a tooltip over it. The tour
 * is eleven stops long and always runs in the same order; each stop carries its own
 * descriptive text and points at one part of the Home screen.
 *
 * <p>Ids confirmed on the device: {@code tooltip_title} (always empty — the app
 * never sets a heading), {@code tooltip_desc}, {@code btn_stop} and
 * {@code btn_next}. On the LAST stop {@code btn_stop} is gone and {@code btn_next}
 * relabels itself to "Close", so the same id serves both.
 *
 * <p><b>The highlight itself is not in the accessibility tree.</b> There is no
 * spotlight/mask node — the overlay draws it, and only the four ids above are
 * exposed. What IS measurable is where the tooltip sits: it is pinned exactly
 * 132px BELOW targets at the top of the screen, and ABOVE the bottom navigation
 * bar for targets down there. So "X is highlighted" is checked as: this stop's
 * text is X's text, X is on screen, and the tooltip is anchored on the correct
 * side of X. The tooltip does NOT track horizontally (it is a fixed-width panel),
 * so same-row items are told apart by their text alone — which is unique per stop.
 *
 * <p><b>The text uses curly quotes</b> (U+2018/U+2019), not ASCII apostrophes.
 * They are written as {@code \\u2018}/{@code \\u2019} escapes below so the file's
 * own encoding cannot change what is compared.
 */
public class TutorialForAdminPage extends BasePage {

    /** The Settings row that leads here. */
    public static final String SETTINGS_ROW = "Zillit Help";
    /** The guide row that starts the tour. */
    public static final String GUIDE_ROW = "Tutorial for Admin";

    /** Where the tooltip sits relative to the element the stop is about. */
    public enum Anchor { BELOW, ABOVE, NONE }

    /** One stop on the tour. */
    public static final class Step {
        /** Human name, used in assertion messages. */
        public final String label;
        /** The tooltip text, character for character. */
        public final String desc;
        /** Id of the element this stop points at, or null for the opening stop. */
        public final String targetId;
        /** Text that picks the right one when the id is shared, else null. */
        public final String targetText;
        public final Anchor anchor;

        Step(String label, String desc, String targetId, String targetText, Anchor anchor) {
            this.label = label;
            this.desc = desc;
            this.targetId = targetId;
            this.targetText = targetText;
            this.anchor = anchor;
        }
    }

    /**
     * The tour, in order. Read off the device — the wording differs in small ways
     * from how it is usually transcribed ("calendar IS based on", "messageS from
     * Users", "individualS and groups"), so it is kept exactly as the app renders it.
     *
     * <p>{@code menuText} is shared by the top tab row AND the bottom navigation
     * bar, hence the text column; every label here is unique across the two.
     */
    public static final List<Step> STEPS = List.of(
            new Step("Home page",
                    "This is your Home page.",
                    null, null, Anchor.NONE),
            new Step("Bulletin",
                    "Currently highlighted, is your default opening. You can change this in "
                            + "\u2018Settings\u2019. This tab allows you to post and comment on texts, "
                            + "attachments, videos, photos. As Admin you can post anything. Whatever is "
                            + "posted here is visible/readable to all Users of the project.",
                    "menuText", "Bulletin", Anchor.BELOW),
            new Step("Calendar",
                    "Create an event and send invites. Time and date, shown on the event calendar "
                            + "is based on your current location. Events created are editable. You can "
                            + "also see who has accepted and who has rejected your event.",
                    "menuText", "Calendar", Anchor.BELOW),
            new Step("Call Sheet",
                    "Allows you to see the call sheet once it is posted. Only the Asst. Directors "
                            + "are allowed to post here. No one can comment on it.",
                    "menuText", "Call Sheet", Anchor.BELOW),
            new Step("Zillit icon",
                    "See messages that Zillit sends out as information. As Admin you will also "
                            + "receive messages from Users and project is requested to be deleted.",
                    "imgLogo", null, Anchor.BELOW),
            new Step("Project name",
                    "This allows you to start your own project, join a project or switch between "
                            + "multiple projects. \u2018Remote Unit\u2019 will also show up here.",
                    "tvProjectName", null, Anchor.BELOW),
            new Step("SOS",
                    "To be used in case of emergency. Connect immediately with all whose details "
                            + "you have filled with one touch.",
                    "tvSOS", null, Anchor.BELOW),
            new Step("Email",
                    "Communicate with your private email created just for this project, with other "
                            + "users on the project, or with the others outside the project.",
                    "menuText", "Email", Anchor.ABOVE),
            new Step("Tools",
                    "Various tools offered by Zillit.",
                    "menuText", "Tools", Anchor.ABOVE),
            new Step("C&C",
                    "Ability to \u2018Chat\u2019 & \u2018Call\u2019 with individuals and groups. You can "
                            + "also make calls to anyone in your phone diary.",
                    "menuText", "C&C", Anchor.ABOVE),
            new Step("Settings",
                    "For you to personalize your Zillit. Also all Admin functionality is done here "
                            + "thru Admin Settings.",
                    "menuText", "Settings", Anchor.ABOVE));

    /** The button label on every stop but the last. */
    public static final String NEXT = "Next";
    /** The label {@code btn_next} takes on the last stop. */
    public static final String CLOSE = "Close";
    /** The second button, present on every stop except the last. */
    public static final String STOP = "Stop";

    public TutorialForAdminPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- getting there ----------------

    public boolean openSettingsTab() {
        try {
            WebElement navBar = driver.findElement(byId("bottomNavigation"));
            for (WebElement it : navBar.findElements(byId("menuText"))) {
                String t = safeText(it);
                if (t != null && t.equalsIgnoreCase("Settings")) {
                    it.click();
                    sleep(2000);
                    return true;
                }
            }
        } catch (WebDriverException ignored) {
        }
        return false;
    }

    /** Taps a row in a settings-style list, scrolling it into view first. */
    private boolean tapListRow(String label) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".scrollIntoView(new UiSelector().textContains(\"" + label + "\"))"));
        } catch (WebDriverException notScrollable) {
            // Short list or already visible.
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath(
                            "//*[contains(@resource-id,'nameTv') and contains(@text,\"" + label
                                    + "\")]/ancestor::*[contains(@resource-id,'commonListItemHolder')]")))
                    .click();
            sleep(2000);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean isOnZillitGuide(Duration timeout) {
        return headerIs("ZILLIT GUIDE", timeout);
    }

    public boolean isOnSettingsTab(Duration timeout) {
        return onSettingsTabScreen(timeout);
    }

    /**
     * Gets to ZILLIT GUIDE from wherever the app is.
     *
     * <p>Deliberately does NOT step over a running tour: the tooltip swallows taps,
     * so if one is up it is closed first rather than tapped through.
     */
    public boolean ensureOnZillitGuide(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isOnZillitGuide(Duration.ofMillis(800))) {
                return true;
            }
            if (isTourShowing(Duration.ofMillis(300))) {
                closeTour();
                continue;
            }
            if (isOnSettingsTab(Duration.ofMillis(500))) {
                tapListRow(SETTINGS_ROW);
                sleep(1500);
                continue;
            }
            if (hasBottomNav()) {
                openSettingsTab();
                sleep(1500);
                continue;
            }
            try {
                driver.navigate().back();
            } catch (WebDriverException ignored) {
            }
            sleep(900);
        }
        return false;
    }

    /** Starts the tour from the guide. */
    public boolean openTutorial() {
        return tapListRow(GUIDE_ROW);
    }

    /** Walks a running tour to its end so the app is usable again. */
    public void closeTour() {
        for (int i = 0; i < STEPS.size() + 3 && isTourShowing(Duration.ofMillis(300)); i++) {
            if (!tapStop()) {
                tapNext();
            }
            sleep(1200);
        }
    }

    // ---------------- the tour ----------------

    /** True while a coach-mark is on screen. */
    public boolean isTourShowing(Duration timeout) {
        return presentWithin("tooltip_desc", timeout);
    }

    /** True once no coach-mark is left. */
    public boolean tourGone(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (!present("tooltip_desc") && !present("btn_next")) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(400);
        }
    }

    public String tooltipDesc() {
        return textOf("tooltip_desc");
    }

    /** Always empty on this tour — the app sets no heading. */
    public String tooltipTitle() {
        return textOf("tooltip_title");
    }

    /** "Next" on stops 1–10, "Close" on the last. */
    public String nextLabel() {
        return textOf("btn_next");
    }

    public boolean hasStopButton() {
        return present("btn_stop");
    }

    public boolean hasNextButton() {
        return present("btn_next");
    }

    /** Advances one stop. Same id as Close — the label is what changes. */
    public boolean tapNext() {
        return tapById("btn_next");
    }

    /** Ends the tour early. Never pressed by the walkthrough — only its presence is checked. */
    public boolean tapStop() {
        return present("btn_stop") && tapById("btn_stop");
    }

    /** Finishes the tour from the last stop. */
    public boolean tapClose() {
        return tapById("btn_next");
    }

    /** Waits for a given stop's text to be the one on screen. */
    public boolean waitForDesc(String expected, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (expected.equals(tooltipDesc())) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(350);
        }
    }

    // ---------------- what a stop points at ----------------

    /** The element a stop highlights, or null when it is not on screen. */
    public WebElement targetElement(Step step) {
        if (step.targetId == null) {
            return null;
        }
        try {
            for (WebElement e : driver.findElements(byId(step.targetId))) {
                if (step.targetText == null || step.targetText.equals(safeText(e))) {
                    return e;
                }
            }
        } catch (WebDriverException ignored) {
        }
        return null;
    }

    public boolean targetVisible(Step step) {
        return step.targetId == null || targetElement(step) != null;
    }

    /**
     * True when the tooltip sits on the side of the target the stop expects.
     *
     * <p>This is the closest thing to "is it highlighting the right place": the
     * overlay's own highlight is not exposed, but the panel is positioned off the
     * target, so the wrong element would put it on the wrong side of the screen.
     */
    public boolean anchoredToTarget(Step step) {
        if (step.anchor == Anchor.NONE) {
            return true;
        }
        WebElement target = targetElement(step);
        if (target == null) {
            return false;
        }
        try {
            Rectangle t = target.getRect();
            int top = panelTop();
            int bottom = panelBottom();
            if (top < 0 || bottom < 0) {
                return false;
            }
            return step.anchor == Anchor.BELOW
                    ? top >= t.getY() + t.getHeight()
                    : bottom <= t.getY();
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Top of the tooltip panel, or -1 when nothing is showing. */
    public int panelTop() {
        int title = topOf("tooltip_title");
        int desc = topOf("tooltip_desc");
        if (title < 0) {
            return desc;
        }
        return desc < 0 ? title : Math.min(title, desc);
    }

    /** Bottom of the tooltip panel — the buttons are its lowest part. */
    public int panelBottom() {
        try {
            List<WebElement> found = driver.findElements(byId("btn_next"));
            if (found.isEmpty()) {
                return -1;
            }
            Rectangle r = found.get(0).getRect();
            return r.getY() + r.getHeight();
        } catch (WebDriverException e) {
            return -1;
        }
    }

    private int topOf(String id) {
        try {
            List<WebElement> found = driver.findElements(byId(id));
            return found.isEmpty() ? -1 : found.get(0).getRect().getY();
        } catch (WebDriverException e) {
            return -1;
        }
    }

    // ---------------- Home tab ----------------

    /** The tour runs on Home, so this is what it should have landed on. */
    public boolean isOnHomeTab(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (hasBottomNav() && present("layTabs")) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(400);
        }
    }

    private boolean hasBottomNav() {
        try {
            return !driver.findElements(byId("bottomNavigation")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- shared ----------------

    public String headerText() {
        return textOf("headerText");
    }

    private boolean headerIs(String expected, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (expected.equalsIgnoreCase(headerText())) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    private boolean present(String id) {
        try {
            return !driver.findElements(byId(id)).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    private boolean presentWithin(String id, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (present(id)) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }

    private boolean tapById(String id) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId(id)))
                    .click();
            sleep(1500);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    private String textOf(String id) {
        try {
            List<WebElement> found = driver.findElements(byId(id));
            return found.isEmpty() ? "" : String.valueOf(safeText(found.get(0))).trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    private String safeText(WebElement e) {
        try {
            return e.getText();
        } catch (WebDriverException stale) {
            return "";
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
