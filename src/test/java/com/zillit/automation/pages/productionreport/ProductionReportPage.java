package com.zillit.automation.pages.productionreport;

import com.zillit.automation.pages.workflow.DocWorkflowPage;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * Tools → <b>Production Report Creation</b> ({@code ProductionReportDashboardActivity}) — the
 * twin of Call Sheet Creation on the same layouts; this adds its tile, header and create button.
 * The tile opens the dashboard only while remote config enables report templates for the
 * project, otherwise the legacy chat ({@link #CHAT_ACTIVITY}). Cases:
 * {@code PRODUCTION_REPORT_CREATION_TESTCASES.md}.
 */
public class ProductionReportPage extends DocWorkflowPage {

    public static final String TOOL_TILE = "Production Report Creation";
    public static final String HEADER = "PRODUCTION REPORT CREATION";
    public static final String ACTIVITY = "ProductionReportDashboardActivity";
    public static final String CHAT_ACTIVITY = "ProductionReportChatActivity";
    public static final String EDITOR_ACTIVITY = "ProductionReportEditorActivity";
    public static final String VIEWER_ACTIVITY = "ProductionReportDetailActivity";
    public static final String FAB = "fabCreate";

    public static final String SENT_OK = "Production report has been submitted for approval successfully.";
    public static final String COMMENTS_OK = "Production report has been sent for comments successfully.";

    public ProductionReportPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean createShown() { return !onScreen(FAB).isEmpty(); }

    public String createDesc() {
        List<WebElement> f = onScreen(FAB);
        try { return f.isEmpty() ? null : f.get(0).getAttribute("content-desc"); }
        catch (WebDriverException e) { return null; }
    }

    public boolean tapCreate() { return tapFirst(FAB); }

    public boolean waitForList(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (!onScreen("recycler_drafts").isEmpty() || !onScreen("text_empty").isEmpty()) return true;
            pause(500);
        }
        return false;
    }
}
