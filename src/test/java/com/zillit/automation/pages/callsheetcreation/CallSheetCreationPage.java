package com.zillit.automation.pages.callsheetcreation;

import com.zillit.automation.pages.workflow.DocWorkflowPage;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Tools → <b>Call Sheet Creation</b> ({@code ProjectDetailActivity}): what is particular to the
 * Call Sheet tool on top of the shared {@link DocWorkflowPage} — its tile, header, create
 * button and Permission tab. Cases: {@code CALL_SHEET_CREATION_TESTCASES.md}.
 */
public class CallSheetCreationPage extends DocWorkflowPage {

    /** Tools tile for a poster/admin; a view-only user sees {@link #TOOL_TILE_VIEW_ONLY}. */
    public static final String TOOL_TILE = "Call Sheet Creation";
    public static final String TOOL_TILE_VIEW_ONLY = "Drafts Call Sheet";
    public static final String HEADER = "CALL SHEET CREATION";
    public static final String ACTIVITY = "ProjectDetailActivity";
    public static final String EDITOR_ACTIVITY = "CallSheetEditorActivity";
    public static final String VIEWER_ACTIVITY = "CallSheetDetailActivity";
    public static final String FAB = "fab_new_callsheet";

    public CallSheetCreationPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean createShown() { return !onScreen(FAB).isEmpty(); }

    public String createDesc() {
        List<WebElement> f = onScreen(FAB);
        try { return f.isEmpty() ? null : f.get(0).getAttribute("content-desc"); }
        catch (WebDriverException e) { return null; }
    }

    public boolean tapCreate() { return tapFirst(FAB); }

    // ───────────────────────────── Permission tab (admin) ─────────────────────────────

    public String permissionDescription() { return firstText("tv_permission_description"); }

    /** One member row on the Permission tab. */
    public static final class Member {
        public final String name, designation;
        public final boolean on, enabled;
        Member(String name, String designation, boolean on, boolean enabled) {
            this.name = name; this.designation = designation; this.on = on; this.enabled = enabled;
        }
        @Override public String toString() { return name + (on ? " ON" : " off") + (enabled ? "" : " (locked)"); }
    }

    public List<Member> permissionMembers() {
        List<Member> out = new ArrayList<>();
        for (Band b : bandsOf("tv_user_name")) {
            WebElement sw = switchNear(elementIn(b, "tv_user_name"));
            boolean on = false, enabled = false;
            try {
                if (sw != null) { on = "true".equals(sw.getAttribute("checked")); enabled = sw.isEnabled(); }
            } catch (WebDriverException ignored) { }
            out.add(new Member(textIn(b, "tv_user_name"), textIn(b, "tv_user_designation"), on, enabled));
        }
        return out;
    }

    private WebElement switchNear(WebElement name) {
        if (name == null) return null;
        int y;
        try { y = name.getRect().getY() + name.getRect().getHeight() / 2; } catch (WebDriverException e) { return null; }
        for (WebElement s : elementsById("switch_permission")) {
            try {
                int sy = s.getRect().getY() + s.getRect().getHeight() / 2;
                if (Math.abs(sy - y) < 90) return s;
            } catch (WebDriverException ignored) { }
        }
        return null;
    }

    public boolean waitForList(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (!onScreen("recycler_drafts").isEmpty() || !onScreen("text_empty").isEmpty()) return true;
            pause(500);
        }
        return false;
    }
}
