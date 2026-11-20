package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * EXTERNAL USERS — the listing.
 *
 * <p>Ids from {@code activity_external_user_listing_page.xml} and
 * {@code external_user_item.xml}, read back off a device to be sure.
 *
 * <p><b>One trap worth knowing.</b> A row's email is held by a TextView whose id is
 * {@code designationTv}, because it sits inside {@code emailLi} and was evidently copied
 * from the row above it. The id is misleading and the value is the email — the designation
 * is in {@code userDesignation}. Anything reading this screen has to know that.
 */
public class ExternalUserListPage extends BasePage {

    public static final String HEADER = "EXTERNAL USER";

    /** The filter menu's four choices, exactly as the app renders them. */
    public static final String FILTER_ALL    = "All";
    public static final String FILTER_CREW   = "Crew Member";
    public static final String FILTER_VENDOR = "Vendor";
    /**
     * Lower-case on purpose. Crew Member and Vendor come from server label keys and arrive
     * capitalised; this one is the raw constant {@code Constants.OTHERS = "others"} and is
     * shown as-is, so the menu reads "All / Crew Member / Vendor / others".
     */
    public static final String FILTER_OTHERS = "others";

    public ExternalUserListPage(AndroidDriver driver) {
        super(driver);
    }

    /** One row as the list prints it. */
    public static final class Row {
        public final String name;
        public final String gender;
        public final String userType;
        public final String department;
        public final String designation;
        public final String email;
        public final String contact;
        public final String createdBy;

        Row(String name, String gender, String userType, String department,
            String designation, String email, String contact, String createdBy) {
            this.name = name; this.gender = gender; this.userType = userType;
            this.department = department; this.designation = designation;
            this.email = email; this.contact = contact; this.createdBy = createdBy;
        }

        @Override public String toString() {
            return name + " " + gender + " [" + userType
                    + (department == null ? "" : " / " + department)
                    + (designation == null ? "" : " / " + designation)
                    + "] " + email + " " + contact;
        }
    }

    public boolean isLoaded(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            if (isVisible("recUsers", Duration.ofMillis(400))
                    || isVisible("add_fab", Duration.ofMillis(400))) {
                return true;
            }
        } while (System.currentTimeMillis() < end);
        return false;
    }

    public String header() { return text("headerText"); }

    public boolean addShown() { return displayed("add_fab"); }

    public boolean openAdd() { return clickIfPresent("add_fab"); }

    public boolean searchShown() { return displayed("search_widget_edit_textvw"); }

    public boolean search(String term) {
        try {
            for (WebElement e : driver.findElements(byId("search_widget_edit_textvw"))) {
                if (!e.isDisplayed()) continue;
                e.click();
                e.clear();
                if (!term.isEmpty()) e.sendKeys(term);
                hideKeyboard();
                pause(1800);
                return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    public String searchText() { return text("search_widget_edit_textvw"); }

    public boolean clearSearch() { return search(""); }

    // ───────────────────────────── filters ─────────────────────────────

    /** Opens the filter menu, which hangs off the toolbar's filter icon. */
    public boolean openFilters() { return clickIfPresent("settingImage"); }

    /** The filter choices on offer, read from the open menu. */
    public List<String> filterOptions() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(byId("title"))) {
                String t = e.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            }
        } catch (Exception ignored) { }
        return out;
    }

    public boolean chooseFilter(String label) {
        boolean tapped = tapText(label);
        if (tapped) pause(2500);
        return tapped;
    }

    // ───────────────────────────── rows ─────────────────────────────

    /**
     * Every row on screen.
     *
     * <p>Rows are found by their own container id rather than as children of the list, so
     * it does not matter whether the adapter nests anything inside {@code recUsers}.
     */
    public List<Row> rows() {
        List<Row> out = new ArrayList<>();
        try {
            for (WebElement row : driver.findElements(byId("usersLinearHolder"))) {
                out.add(new Row(
                        childText(row, "userNameTv"),
                        childText(row, "genderTv"),
                        childText(row, "userType"),
                        childText(row, "userDepartment"),
                        childText(row, "userDesignation"),
                        // The email really does live in designationTv — see the class note.
                        childText(row, "designationTv"),
                        childText(row, "contactTypeTv"),
                        childText(row, "createdByTv")));
            }
        } catch (Exception ignored) { }
        return out;
    }

    public List<String> names() {
        List<String> out = new ArrayList<>();
        for (Row r : rows()) if (r.name != null) out.add(r.name);
        return out;
    }

    public Row find(String name) {
        for (Row r : rows()) {
            if (r.name != null && r.name.contains(name)) return r;
        }
        return null;
    }

    /** Scrolls the list until a row carrying this text is on screen. */
    public boolean scrollTo(String textFragment) {
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                            + ".setMaxSearchSwipes(12)"
                            + ".scrollIntoView(new UiSelector().textContains(\""
                            + textFragment + "\"))"));
            pause(600);
            return true;
        } catch (Exception e) { return false; }
    }

    public boolean edit(String name)        { return tapRowControl(name, "btnEdit"); }
    public boolean delete(String name)      { return tapRowControl(name, "btnDel"); }
    public boolean viewDetails(String name) { return tapRowControl(name, "moreBtn"); }

    /** Taps a row's email, which should hand over to the Email module. */
    public boolean tapEmail(String name) { return tapRowControl(name, "designationTv"); }

    /** Taps a row's contact number. */
    public boolean tapContact(String name) { return tapRowControl(name, "contactTypeTv"); }

    private boolean tapRowControl(String name, String controlId) {
        try {
            for (WebElement row : driver.findElements(byId("usersLinearHolder"))) {
                String n = childText(row, "userNameTv");
                if (n == null || !n.contains(name)) continue;
                List<WebElement> controls = row.findElements(byId(controlId));
                if (controls.isEmpty()) {
                    System.out.println(">>> external users: row \"" + n + "\" has no "
                            + controlId);
                    return false;
                }
                controls.get(0).click();
                pause(1800);
                return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    // ───────────────────────── delete confirmation ─────────────────────────

    public static final String DELETE_CONFIRM = "Are you sure you want to delete";

    public boolean deleteConfirmShown(Duration timeout) {
        return textShown(DELETE_CONFIRM, timeout);
    }

    public boolean confirmDelete() {
        boolean tapped = tapText("Yes");
        if (tapped) pause(3000);
        return tapped;
    }

    public boolean cancelDelete() {
        boolean tapped = tapText("Cancel");
        if (tapped) pause(1500);
        return tapped;
    }

    // ───────────────────────────── helpers ─────────────────────────────

    private String childText(WebElement row, String id) {
        try {
            List<WebElement> els = row.findElements(byId(id));
            if (els.isEmpty()) return null;
            String t = els.get(0).getText();
            return t == null || t.isBlank() ? null : t.trim();
        } catch (Exception e) { return null; }
    }

    private String text(String id) {
        try {
            List<WebElement> els = driver.findElements(byId(id));
            return els.isEmpty() ? null : els.get(0).getText();
        } catch (Exception e) { return null; }
    }

    private boolean displayed(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    private boolean clickIfPresent(String id) {
        try {
            for (WebElement e : driver.findElements(byId(id))) {
                if (e.isDisplayed()) { e.click(); pause(1500); return true; }
            }
        } catch (Exception ignored) { }
        return false;
    }

    boolean tapText(String label) {
        try {
            for (WebElement e : driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + label + "\")"))) {
                if (e.isDisplayed()) { e.click(); pause(900); return true; }
            }
        } catch (Exception ignored) { }
        return false;
    }

    void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
