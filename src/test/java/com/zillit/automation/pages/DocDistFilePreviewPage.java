package com.zillit.automation.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;

/**
 * DOCUMENT DISTRIBUTION — the file preview, which is a separate ACTIVITY
 * ({@code FilePreviewActivity}), not a destination in {@code dd_nav_graph}.
 *
 * <p>That distinction matters twice (trap 3.8): "the preview opened" is asserted on the
 * ACTIVITY, not on some fragment id — and BACK finishes the activity, landing on the
 * library exactly where the row was tapped, not on any nav-graph destination.
 *
 * <p>A PDF renders as a list of {@code item_dd_pdf_page} rows whose page image carries
 * {@code dd_page} — so "the PDF actually rendered" is "at least one dd_page exists",
 * which is a far stronger check than the activity merely being resumed.
 */
public class DocDistFilePreviewPage extends BasePage {

    public DocDistFilePreviewPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isOpen(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isPreviewActivity()) return true;
            sleep(400);
        }
        return false;
    }

    public boolean isPreviewActivity() {
        try {
            String a = driver.currentActivity();
            return a != null && a.contains("FilePreviewActivity");
        } catch (WebDriverException e) {
            return false;
        }
    }

    /** At least one rendered PDF page ({@code item_dd_pdf_page} → {@code dd_page}). */
    public boolean hasPdfPage(Duration timeout) {
        return isVisible("dd_page", timeout);
    }

    public int pdfPageCount() {
        try { return driver.findElements(byId("dd_page")).size(); }
        catch (WebDriverException e) { return 0; }
    }

    /** BACK out of the preview — the activity finishes, the library is underneath. */
    public boolean close() {
        for (int i = 0; i < 3 && isPreviewActivity(); i++) {
            try { driver.navigate().back(); } catch (WebDriverException ignored) { }
            sleep(400);
        }
        return !isPreviewActivity();
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
