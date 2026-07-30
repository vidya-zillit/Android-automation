package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;

/**
 * C&C calling (CallActivity, activity_call.xml) + the line-selection dialog.
 * Start: chat toolbar audioCall/videoCall → line dialog (Line 1 Mediasoup /
 * Line 2 Agora) → CallActivity. Controls: btnMute/btnVideo/btnSpeaker/
 * btnSwitchCamera/btnMore/btnEndCall; incoming btnAnswer/btnDecline/btnMessage;
 * status tvCallStatus/tvCallTimer/tvCallerName.
 */
public class CcCallPage extends BasePage {

    public CcCallPage(AndroidDriver driver) {
        super(driver);
    }

    /** The Line 1 / Line 2 selection dialog after tapping a call button. */
    public boolean isLineDialogShown(Duration timeout) {
        return textShown("Line 1", timeout) || textShown("Line", timeout);
    }

    public boolean pickLine() {
        for (String pat : new String[]{"Line 1", "Line 2", "Line"}) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().textMatches(\"(?i).*" + pat + ".*\")")).click();
                return true;
            } catch (WebDriverException ignored) {
            }
        }
        return false;
    }

    /**
     * The in-call screen is up. Poll for the WHOLE timeout because an audio call
     * sits on a "Connecting…" screen for a few seconds before every control is
     * laid out — in that state only btnEndCall / controlBar / tvCallStatus /
     * tvCallerName / avatarContainer are present (no btnMute yet).
     */
    public boolean isCallScreenShown(Duration timeout) {
        long end = System.nanoTime() + timeout.toNanos();
        String[] ids = {"btnEndCall", "controlBar", "tvCallStatus",
                        "tvCallerName", "avatarContainer", "btnMute"};
        do {
            for (String id : ids) {
                if (isVisible(id, Duration.ofMillis(400))) return true;
            }
        } while (System.nanoTime() < end);
        return false;
    }

    public String callStatus() {
        try { return driver.findElement(byId("tvCallStatus")).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    public boolean hasControl(String id) {   // btnMute/btnVideo/btnSpeaker/btnMore/btnEndCall
        return isVisible(id, Duration.ofSeconds(3));
    }

    public boolean tapMute()      { return tap("btnMute"); }
    public boolean tapSpeaker()   { return tap("btnSpeaker"); }
    public boolean tapMore()      { return tap("btnMore"); }

    public boolean endCall() {
        if (tap("btnEndCall")) return true;
        // fallback: back a couple times
        try { driver.navigate().back(); } catch (WebDriverException ignored) {}
        return false;
    }

    private boolean tap(String id) {
        try { driver.findElement(byId(id)).click(); return true; }
        catch (WebDriverException e) { return false; }
    }
}
