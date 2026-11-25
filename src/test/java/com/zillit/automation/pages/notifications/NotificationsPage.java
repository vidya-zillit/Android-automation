package com.zillit.automation.pages.notifications;

import com.zillit.automation.pages.common.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The in-app notification list ({@code NotificationActivity}, activity_notification.xml), opened
 * from the bottom-nav toolbar logo {@code imgLogo} (BottomNavigationActivity.kt:553-555).
 *
 * <p>Ids: {@code rvHelper} (list), {@code progress} / {@code linearProgress} (loaders),
 * {@code noDataFound} (empty state), {@code mainVw}. Row (received_notification_item.xml):
 * {@code mainView}, {@code sos_notification_message}, {@code sos_notification_time},
 * {@code sosNotificationDelete}. A row tap calls {@code onNotificationItemTap}, whose body is
 * empty (NotificationActivity.kt:173-175).
 */
public class NotificationsPage extends BasePage {

    public static final String ACTIVITY = "NotificationActivity";
    public static final String APPROVE_ACTIVITY = "ApproveNewUsersListActivity";

    public NotificationsPage(AndroidDriver driver) {
        super(driver);
    }

    /** Taps the toolbar logo on a bottom-nav screen. */
    public boolean openFromToolbarLogo() {
        try {
            driver.findElement(byId("imgLogo")).click();
        } catch (WebDriverException e) {
            return false;
        }
        return isOpen(Duration.ofSeconds(10));
    }

    public boolean isOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (activity().endsWith(ACTIVITY)) return true;
            if (!driver.findElements(byId("rvHelper")).isEmpty()
                    || !driver.findElements(byId("noDataFound")).isEmpty()) return true;
            pause(300);
        }
        return false;
    }

    /** Waits until neither loader is on screen. Returns false if one is still showing at the deadline. */
    public boolean waitSettled(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        pause(800);
        while (System.currentTimeMillis() < deadline) {
            if (!shown("progress") && !shown("linearProgress")) return true;
            pause(400);
        }
        return false;
    }

    public boolean isEmptyStateShown() {
        return shown("noDataFound");
    }

    /** Message + time of each row on screen, top first ("message | time"). */
    public List<String> rows() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement row : driver.findElements(byId("mainView"))) {
                String msg = textIn(row, "sos_notification_message");
                String time = textIn(row, "sos_notification_time");
                if (!msg.isBlank() || !time.isBlank()) out.add(msg + " | " + time);
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    /** Taps the message text of the row at {@code index}. */
    public boolean tapRow(int index) {
        try {
            List<WebElement> rows = driver.findElements(byId("sos_notification_message"));
            if (index >= rows.size()) return false;
            rows.get(index).click();
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** Index of the first on-screen row whose message matches {@code regex} (case-insensitive), or -1. */
    public int indexOfRow(String regex) {
        List<String> r = rows();
        for (int i = 0; i < r.size(); i++) {
            if (r.get(i).toLowerCase().matches("(?s).*(" + regex.toLowerCase() + ").*")) return i;
        }
        return -1;
    }

    public String activity() {
        try {
            return String.valueOf(driver.currentActivity());
        } catch (WebDriverException e) {
            return "";
        }
    }

    // ---------------------------------------------------------------- system shade

    /**
     * Opens the shade and taps the first Zillit notification whose text matches {@code regex}.
     * Returns the tapped text, or null when none is there (the shade is closed again).
     */
    public String tapShadeNotification(String regex) {
        try {
            driver.openNotifications();
            pause(1500);
            List<WebElement> hits = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textMatches(\"(?is).*(" + regex + ").*\")"));
            for (WebElement h : hits) {
                String t = h.getText();
                h.click();
                return t;
            }
        } catch (WebDriverException ignored) {
        }
        try { driver.navigate().back(); } catch (WebDriverException ignored) { }
        return null;
    }

    // ---------------------------------------------------------------- helpers

    private boolean shown(String id) {
        try {
            return driver.findElements(byId(id)).stream().anyMatch(WebElement::isDisplayed);
        } catch (WebDriverException e) {
            return false;
        }
    }

    private String textIn(WebElement row, String id) {
        try {
            String t = row.findElement(byId(id)).getText();
            return t == null ? "" : t.trim();
        } catch (WebDriverException e) {
            return "";
        }
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
