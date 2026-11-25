package com.zillit.automation.pages.drive;

import com.zillit.automation.pages.common.ToolsPage;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.List;

import java.time.Duration;

/**
 * Multi-screen Drive journeys shared by the editor / new-feature cases: reach Drive home,
 * make sure a fixture is uploaded, upload a run-unique {@code QA_} copy, open a file's detail,
 * and remove a {@code QA_} item for good (row menu → Delete → Trash → Delete permanently).
 */
public final class DriveFlows {

    private final AndroidDriver driver;

    public DriveFlows(AndroidDriver driver) {
        this.driver = driver;
    }

    /**
     * Drive home from wherever the app is. The Tools row is labelled per project
     * ({@code toolsInfo.unitName}) — "Z-Drive" on the automation project — so it is opened by
     * CONTAINS, never by an exact "Drive" (that exact match is what skipped whole suites).
     */
    public boolean openDrive() {
        DriveHomePage drive = new DriveHomePage(driver);
        new DriveEditorPage(driver).forceExit();
        drive.exitSelectMode();
        if (drive.isDriveActivity() && drive.isHome(Duration.ofSeconds(3))) return true;
        if (drive.isDriveActivity() && drive.returnHome()) return true;
        for (int i = 0; i < 8 && !onBottomNav(); i++) {
            if (drive.isDriveActivity() && drive.isHome(Duration.ofSeconds(2))) return true;
            try { driver.navigate().back(); sleep(900); } catch (WebDriverException ignored) { }
        }
        ToolsPage tools = new ToolsPage(driver);
        tools.openToolsTab();
        if (!tools.isLoaded(Duration.ofSeconds(15))) return false;
        tools.searchTools("Drive");
        sleep(1500);
        return tools.openModuleContaining("Drive") && drive.isLoaded(Duration.ofSeconds(20));
    }

    private boolean onBottomNav() {
        try {
            return !driver.findElements(io.appium.java_client.AppiumBy.id(
                    driver.getCurrentPackage() + ":id/bottomNavigation")).isEmpty();
        } catch (WebDriverException e) { return false; }
    }

    /** Uploads {@code name} from the device's Download folder unless Files already lists it. */
    public boolean ensureFile(String name) {
        DriveHomePage drive = new DriveHomePage(driver);
        if (drive.hasFile(name)) return true;
        if (!drive.openFab() || !drive.tapUploadFile()) return false;
        if (!new DriveUploadPage(driver).uploadFile("Document", name)) return false;
        return waitListed(name, Duration.ofSeconds(40));
    }

    /** Uploads the fixture {@code fixture} under the name {@code uploadAs}. */
    public boolean uploadAs(String fixture, String uploadAs) {
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFiles();
        if (drive.hasFile(uploadAs)) return true;
        if (!drive.openFab() || !drive.tapUploadFile()) return false;
        if (!new DriveUploadPage(driver).uploadFileAs("Document", fixture, uploadAs)) return false;
        return waitListed(uploadAs, Duration.ofSeconds(40));
    }

    public boolean waitListed(String name, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        DriveHomePage drive = new DriveHomePage(driver);
        while (System.currentTimeMillis() < deadline) {
            sleep(4000);
            if (drive.hasFile(name)) return true;
            drive.pullToRefresh();
        }
        return false;
    }

    /** Files tab → tap the row → File details, waiting for its async action buttons. */
    public DriveFileDetailPage openDetail(String name) {
        DriveFileDetailPage d = new DriveFileDetailPage(driver);
        for (int attempt = 0; attempt < 3; attempt++) {
            if (d.isOpen(Duration.ofSeconds(2)) && name.equalsIgnoreCase(d.fileName())) break;
            DriveHomePage drive = new DriveHomePage(driver);
            if (!drive.isHome(Duration.ofSeconds(3))) drive.returnHome();
            drive.showFiles();
            if (!drive.openItem(name)) { drive.pullToRefresh(); sleep(3000); continue; }
            // A just-uploaded file's details can sit on a spinner for a while.
            if (d.isOpen(Duration.ofSeconds(45))) break;
        }
        if (!(d.isOpen(Duration.ofSeconds(3)) && name.equalsIgnoreCase(d.fileName()))) return null;
        d.waitFor("btnDownload", Duration.ofSeconds(25));
        return d;
    }


    /**
     * Deletes a file or folder this run created, then removes it from Trash as well, so nothing
     * named {@code QA_…} piles up. Refuses anything that is not a {@code QA_}/{@code ZL_AUTO_}
     * name — a cleanup helper must never be able to remove a fixture or a person's file.
     */
    public boolean purge(String name, boolean isFolder) {
        if (!(name.startsWith("QA_") || name.startsWith("ZL_AUTO_"))) return false;
        if (!openDrive()) return false;
        DriveHomePage home = new DriveHomePage(driver);
        if (isFolder) home.showFolders(); else home.showFiles();
        boolean listed = home.allItemNamesScrolled().stream().anyMatch(n -> n.equalsIgnoreCase(name));
        if (listed && home.openItemMenu(name)) {
            DriveItemMenuPage menu = new DriveItemMenuPage(driver);
            if (menu.isOpen() && menu.tapDelete()) {
                sleep(1200);
                home.confirmDeleteDialog(Duration.ofSeconds(8));
                sleep(3000);
            } else {
                menu.dismiss();
            }
        }
        // Second step: out of Trash too.
        if (!home.openDrawer()) return !listed;
        DriveItemMenuPage drawer = new DriveItemMenuPage(driver);
        if (!drawer.openTrash()) return false;
        DriveTrashPage trash = new DriveTrashPage(driver);
        if (!trash.isOpen(Duration.ofSeconds(10))) return false;
        sleep(2000);
        boolean purged = !trash.hasItem(name) || trash.deleteForever(name);
        if (purged) purged = !trash.hasItem(name);
        trash.back();
        new DriveHomePage(driver).returnHome();
        return purged;
    }

    // ───────────────────────── web-gap helpers ─────────────────────────

    /** Creates a folder in the list that is open (root or a FolderActivity). */
    public boolean createFolder(String name) {
        DriveHomePage drive = new DriveHomePage(driver);
        if (!drive.openFab() || !drive.tapCreateFolder()) return false;
        CreateFolderSheet sheet = new CreateFolderSheet(driver);
        if (!sheet.waitOpen(Duration.ofSeconds(10))) return false;
        boolean ok = sheet.createNamed(name);
        sleep(2500);
        return ok;
    }

    /** Opens a folder row by name (Folders tab) → FolderActivity. */
    public boolean openFolder(String name) {
        DriveHomePage drive = new DriveHomePage(driver);
        drive.showFolders();
        if (!drive.openItem(name)) return false;
        sleep(2500);
        return new DriveFolderPage(driver).isLoaded(Duration.ofSeconds(20));
    }

    /** Back out of a FolderActivity to Drive home. */
    public void backToHome() {
        for (int i = 0; i < 4 && new DriveFolderPage(driver).isFolderActivity(); i++) {
            try { driver.navigate().back(); } catch (Exception ignored) { }
            sleep(1200);
        }
        new DriveHomePage(driver).returnHome();
    }

    /** Row ⋯ → Delete → confirm: the item goes to Trash. */
    public boolean deleteToTrash(String name, boolean folder) {
        DriveHomePage home = new DriveHomePage(driver);
        if (folder) home.showFolders(); else home.showFiles();
        if (!home.openItemMenu(name)) return false;
        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        if (!menu.isOpen() || !menu.tapDelete()) { menu.dismiss(); return false; }
        sleep(1200);
        boolean ok = home.confirmDeleteDialog(Duration.ofSeconds(8));
        sleep(3000);
        return ok;
    }

    /** Drawer → Trash. */
    public DriveTrashPage openTrash() {
        DriveHomePage home = new DriveHomePage(driver);
        if (!home.openDrawer()) return null;
        if (!new DriveItemMenuPage(driver).openTrash()) return null;
        DriveTrashPage trash = new DriveTrashPage(driver);
        if (!trash.isOpen(Duration.ofSeconds(15))) return null;
        sleep(2500);
        return trash;
    }

    /** Result of an Edit Info save. */
    public static final class EditInfoResult {
        public boolean opened, saved, dialogStillOpen, inlineError;
        public String errorText = "";
        @Override public String toString() {
            return "opened=" + opened + " saved=" + saved + " stillOpen=" + dialogStillOpen
                    + " inlineError=" + inlineError + " '" + errorText + "'";
        }
    }

    /**
     * Row ⋯ → Edit Info → set name (null = keep) and description (null = keep) → Save Changes.
     * A duplicate name reopens the dialog with {@code tvEditNameError} (HomeFragment).
     */
    public EditInfoResult editInfo(String name, boolean folder, String newName, String description) {
        EditInfoResult r = new EditInfoResult();
        DriveHomePage home = new DriveHomePage(driver);
        if (folder) home.showFolders(); else home.showFiles();
        if (!home.openItemMenu(name)) return r;
        DriveItemMenuPage menu = new DriveItemMenuPage(driver);
        if (!menu.isOpen() || !menu.tapEditInfo()) { menu.dismiss(); return r; }
        sleep(1500);
        try {
            WebElement et = driver.findElement(byApp("etEditName"));
            r.opened = true;
            if (newName != null) { et.clear(); if (!newName.isEmpty()) et.sendKeys(newName); }
            if (description != null) {
                WebElement d = driver.findElement(byApp("etEditDescription"));
                d.clear(); if (!description.isEmpty()) d.sendKeys(description);
            }
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"android:id/button1\")")).click();
            r.saved = true;
        } catch (WebDriverException e) { return r; }
        // A server refusal (duplicate name) REOPENS the dialog with tvEditNameError once the
        // reply arrives — poll for it rather than reading once.
        for (int i = 0; i < 12; i++) {
            sleep(1000);
            try {
                List<WebElement> err = driver.findElements(byApp("tvEditNameError"));
                if (!err.isEmpty() && err.get(0).isDisplayed()) { r.inlineError = true; r.errorText = err.get(0).getText(); break; }
            } catch (WebDriverException ignored) { }
        }
        r.dialogStillOpen = !driver.findElements(byApp("etEditName")).isEmpty();
        return r;
    }

    /** Closes an open dialog with its negative button (Cancel). */
    public void cancelDialog() {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"android:id/button2\")")).click();
            sleep(800);
        } catch (WebDriverException ignored) { }
    }

    /** The subtitle line under a row's name ("2 files, 1 folder · …"), or null when absent. */
    public String rowSubtitle(String name) {
        try {
            List<WebElement> names = driver.findElements(byApp("tvName"));
            for (WebElement n : names) {
                if (!name.equalsIgnoreCase(n.getText())) continue;
                org.openqa.selenium.Rectangle r = n.getRect();
                for (WebElement sub : driver.findElements(byApp("tvSubtitle"))) {
                    org.openqa.selenium.Rectangle sr = sub.getRect();
                    if (sr.getY() >= r.getY() && sr.getY() <= r.getY() + r.getHeight() * 3) return sub.getText();
                }
            }
        } catch (WebDriverException ignored) { }
        return null;
    }

    /** Taps the row's own star ({@code btnFavorite}). */
    public boolean tapRowStar(String name) {
        try {
            // The star is the name's sibling in item_drive_file — pair them through the row, not
            // by list index (a row without a star, e.g. an outbox row, shifts every index).
            for (WebElement n : driver.findElements(byApp("tvName"))) {
                if (!name.equalsIgnoreCase(n.getText())) continue;
                List<WebElement> star = n.findElements(org.openqa.selenium.By.xpath(
                        "./../*[contains(@resource-id,':id/btnFavorite')]"));
                if (star.isEmpty()) return false;
                star.get(0).click(); sleep(2000); return true;
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    /** chipFilter → "Filter by Tag" → pick {@code tag} ("All" clears). */
    public boolean filterByTag(String tag) {
        DriveHomePage home = new DriveHomePage(driver);
        if (!home.openFilter()) return false;
        try {
            List<WebElement> hits = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text(\"" + tag + "\"))"));
            if (hits.isEmpty()) hits = driver.findElements(AppiumBy.androidUIAutomator("new UiSelector().text(\"" + tag + "\")"));
            if (hits.isEmpty()) { driver.navigate().back(); return false; }
            hits.get(0).click();
            sleep(3500);
            return true;
        } catch (WebDriverException e) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            return false;
        }
    }

    /** chipSort → pick {@code option} (Name / Date modified / Size / Type). */
    public boolean sortBy(String option) {
        DriveHomePage home = new DriveHomePage(driver);
        if (!home.openSort()) return false;
        try {
            driver.findElement(AppiumBy.androidUIAutomator("new UiSelector().text(\"" + option + "\")")).click();
            sleep(3000);
            return true;
        } catch (WebDriverException e) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            return false;
        }
    }

    private org.openqa.selenium.By byApp(String id) {
        return AppiumBy.id(driver.getCurrentPackage() + ":id/" + id);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
