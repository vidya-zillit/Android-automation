package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;

/**
 * Drive's "More" screen ({@code SettingsFragment} / fragment_drive_settings.xml):
 * profile card, Trash and Activity rows, the team-members card and the diagnostics
 * block (user / project / device / environment / version).
 *
 * <p><b>Not reachable in this build.</b> The destination exists in
 * {@code drive_nav_graph.xml}, but the ONE call site that navigated to it —
 * {@code DriveDrawerFragment}'s profile row — is commented out and now starts
 * {@code EditProfileDataActivity} instead. Nothing else in the drive module references
 * {@code R.id.settingsFragment}, so the screen is built but unshipped.
 *
 * <p>This class therefore exists to prove the screen is ABSENT ({@code DriveSettingsTest}
 * asserts {@link #isOpen} is false after tapping the profile row). The moment the
 * navigation is restored, that assertion fails — which is the signal to switch area N
 * on and use the accessors below.
 */
public class DriveSettingsPage extends BasePage {

    public DriveSettingsPage(AndroidDriver driver) {
        super(driver);
    }

    /**
     * True only on Drive's More screen. {@code rowTrash} and {@code cardTeamMembers}
     * are unique to it — the drawer's Trash entry is {@code navTrash}, a different id.
     */
    public boolean isOpen(Duration timeout) {
        return isVisible("rowTrash", timeout)
                || isVisible("cardTeamMembers", Duration.ofSeconds(2))
                || isVisible("tvVersion", Duration.ofSeconds(2));
    }

    public boolean hasProfileName()  { return isVisible("tvProfileName", Duration.ofSeconds(3)); }
    public boolean hasProfileEmail() { return isVisible("tvProfileEmail", Duration.ofSeconds(2)); }
    public boolean hasDesignation()  { return isVisible("tvProfileDesignation", Duration.ofSeconds(2)); }

    public boolean hasTrashRow()     { return isVisible("rowTrash", Duration.ofSeconds(3)); }
    public boolean hasActivityRow()  { return isVisible("rowActivity", Duration.ofSeconds(2)); }
    public boolean hasTeamMembers()  { return isVisible("cardTeamMembers", Duration.ofSeconds(3)); }

    public boolean openTrash()    { return click("rowTrash"); }
    public boolean openActivity() { return click("rowActivity"); }

    public String profileName() { return text("tvProfileName"); }
    public String userName()    { return text("tvUserName"); }
    public String userEmail()   { return text("tvUserEmail"); }
    public String projectId()   { return text("tvProjectId"); }
    public String deviceId()    { return text("tvDeviceId"); }
    public String environment() { return text("tvEnvironment"); }
    public String version()     { return text("tvVersion"); }

    private String text(String id) {
        try { return driver.findElement(byId(id)).getText(); }
        catch (WebDriverException e) { return ""; }
    }

    private boolean click(String id) {
        try { driver.findElement(byId(id)).click(); return true; }
        catch (WebDriverException e) { return false; }
    }
}
