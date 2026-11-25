package com.zillit.automation.pages.esignature;

import com.zillit.automation.pages.common.VisiblePage;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.List;

/**
 * {@code DocuSignCreateSignatureDialog} — an AlertDialog with a two-step flow:
 * step 1 "How would you like to make your signature?" (cards {@code cardType} "Type your …"
 * and {@code cardDraw} "Draw your …"; {@code cardUpload} is compiled out by
 * {@code SHOW_UPLOAD_METHOD = false}), step 2 the chosen pane plus the footer button
 * {@code btnUse} "Use this signature" / "Use these initials".
 *
 * <p>{@code btnUse} is never disabled — an empty pane only dims it (alpha 0.45) and a tap
 * raises a toast: "Please type a name first" / "Please draw a signature first".
 */
public class ESignCreateSignatureDialog extends VisiblePage {

    public static final String TITLE = "Manage Your Saved Signatures";
    public static final String WARN_TYPE = "Please type a name first";
    public static final String WARN_DRAW = "Please draw a signature first";

    public ESignCreateSignatureDialog(AndroidDriver driver) {
        super(driver);
    }

    public boolean isShown(Duration timeout) {
        return isVisible("cardType", timeout) || isVisible("btnUse", Duration.ofSeconds(1));
    }

    public String title() { return firstText("tvDialogTitle"); }
    public String stepEyebrow() { return firstText("tvStepEyebrow"); }
    public String stepTitle() { return firstText("tvStepTitle"); }
    public String typeCardTitle() { return firstText("tvCardTypeTitle"); }
    public String drawCardTitle() { return firstText("tvCardDrawTitle"); }
    public boolean uploadCardShown() { return !onScreen("cardUpload").isEmpty(); }

    public boolean chooseType() { return tapFirst("cardType"); }
    public boolean chooseDraw() { return tapFirst("cardDraw"); }
    public boolean tapBackStep() { return tapFirst("btnBack"); }
    public boolean close() { return tapFirst("btnClose"); }
    public String useLabel() { return firstText("btnUse"); }

    public boolean typePaneShown() { return !onScreen("etTyped").isEmpty(); }
    public boolean drawPaneShown() { return !onScreen("pad").isEmpty(); }

    public void typeName(String name) {
        List<WebElement> f = onScreen("etTyped");
        if (f.isEmpty()) return;
        try { f.get(0).click(); f.get(0).clear(); f.get(0).sendKeys(name); } catch (WebDriverException ignored) { }
        pause(600);
        hideKeyboardIfShown();
    }

    public String typedText() { return firstText("etTyped"); }
    public String typedPreview() { return firstText("tvTypedPreview"); }

    /** Scribbles a Z across the SignaturePad. */
    public boolean drawOnPad() {
        List<WebElement> p = onScreen("pad");
        if (p.isEmpty()) return false;
        try {
            var r = p.get(0).getRect();
            int x0 = r.getX() + r.getWidth() / 5, x1 = r.getX() + r.getWidth() * 4 / 5;
            int y0 = r.getY() + r.getHeight() / 3, y1 = r.getY() + r.getHeight() * 2 / 3;
            PointerInput f = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence s = new Sequence(f, 1);
            s.addAction(f.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x0, y0));
            s.addAction(f.createPointerDown(0));
            s.addAction(f.createPointerMove(Duration.ofMillis(250), PointerInput.Origin.viewport(), x1, y0));
            s.addAction(f.createPointerMove(Duration.ofMillis(250), PointerInput.Origin.viewport(), x0, y1));
            s.addAction(f.createPointerMove(Duration.ofMillis(250), PointerInput.Origin.viewport(), x1, y1));
            s.addAction(f.createPointerUp(0));
            driver.perform(List.of(s));
            pause(600);
            return true;
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean clearPad() { return tapFirst("btnClearPad"); }

    /** Taps the footer button; scrolls nothing — the footer sits under the ScrollView. */
    public boolean tapUse() {
        List<WebElement> b = onScreen("btnUse");
        if (b.isEmpty()) return false;
        try { b.get(0).click(); } catch (WebDriverException e) { return false; }
        return true;
    }

    public boolean isGone(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (onScreen("btnUse").isEmpty() && onScreen("cardType").isEmpty()) return true;
            pause(300);
        }
        return false;
    }
}
