package com.zillit.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings → "Zillit Help" (header "ZILLIT GUIDE"), its Contact Us page, and the
 * mail composer Email Us opens.
 *
 * <p>Ids confirmed on the device: the guide list is {@code rvHelper} with the
 * usual {@code nameTv} rows; Contact Us offers {@code cardCallUs} and
 * {@code cardEmail}; the composer has {@code et_cc}, {@code et_bcc},
 * {@code et_subject}, {@code btn_send}, and a body {@code editor}.
 *
 * <p><b>The recipient fields collapse into chips — but only once focus leaves
 * them.</b> While the field still has focus the address is plain text in
 * {@code et_bcc}; hiding the keyboard does NOT commit it. Tap another field and
 * {@code et_bcc} disappears, replaced by {@code tv_collapsed_name_bcc} holding
 * the display name alone ("Bhavik", not "Bhavik@zillit.com"). Reading either one
 * on its own reports a correctly filled BCC as blank at some point in that
 * sequence, so {@link #bccRecipient()} reads both.
 *
 * <p><b>The body is an EditText INSIDE a WebView</b> ({@code editor}), so it
 * takes ordinary key input despite the WebView wrapper. It arrives pre-filled
 * with a "Sent from Android" signature.
 *
 * <p><b>Sending is real.</b> "To" is pre-filled with Zillit's Support address by
 * the app, so a send reaches their support inbox — not only the BCC address.
 */
public class ZillitHelpPage extends BasePage {

    /** The Settings row that opens this screen. */
    public static final String SETTINGS_ROW = "Zillit Help";

    /** Entries the guide list offers. */
    public static final List<String> GUIDE_ENTRIES = List.of(
            "Contact us", "FAQ", "Privacy Policy", "Reviews", "Terms of Use", "Tutorial for Admin");

    /** The app pre-fills the recipient; nothing types it. */
    public static final String SUPPORT_RECIPIENT = "Support";

    /** Placeholder shown by an empty recipient field — not a value. */
    private static final String RECIPIENT_HINT = "Add recipient";

    public ZillitHelpPage(AndroidDriver driver) {
        super(driver);
    }

    // ---------------- navigation ----------------

    public boolean openSettingsTab() {
        try {
            WebElement navBar = driver.findElement(byId("bottomNavigation"));
            for (WebElement it : navBar.findElements(byId("menuText"))) {
                String t = it.getText();
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

    public boolean openZillitHelp() {
        return tapListRow(SETTINGS_ROW);
    }

    public boolean openContactUs() {
        return tapListRow("Contact us");
    }

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

    /** Gets to ZILLIT GUIDE from wherever the app is, retrying each hop. */
    public boolean ensureOnZillitHelp(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isLoaded(Duration.ofMillis(800))) {
                return true;
            }
            if (isOnCompose(Duration.ofMillis(300)) || isOnContactUs(Duration.ofMillis(300))) {
                tapBack();
                sleep(1500);
                continue;
            }
            if (isOnSettingsTab(Duration.ofMillis(500))) {
                openZillitHelp();
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

    public boolean isOnSettingsTab(Duration timeout) {
        return onSettingsTabScreen(timeout);
    }

    private boolean hasBottomNav() {
        try {
            return !driver.findElements(byId("bottomNavigation")).isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    // ---------------- ZILLIT GUIDE ----------------

    public boolean isLoaded(Duration timeout) {
        return headerIs("ZILLIT GUIDE", timeout);
    }

    public List<String> guideEntries() {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement e : driver.findElements(byId("nameTv"))) {
                String t = safeText(e);
                if (t != null && !t.isBlank()) {
                    out.add(t.trim());
                }
            }
        } catch (WebDriverException ignored) {
        }
        return out;
    }

    // ---------------- web pages (FAQ / Privacy Policy / Terms of Use) ----------------

    /** Opens any row on the guide by its label. */
    public boolean openGuideEntry(String label) {
        forgetSnapshot();
        return tapListRow(label);
    }

    /**
     * True once the named web page is up AND its content has rendered.
     *
     * <p>A loaded page is judged by how much the WebView actually exposes, not by
     * the header alone: the header appears immediately while the page is still
     * blank, so keying on it would call an empty screen "loaded". The three pages
     * measured 47, 25 and 33 nodes, so a handful is a safe floor.
     *
     * <p>The successful reading is kept, so {@link #webViewNodeCount()} and
     * {@link #webViewTexts()} can report on the same page without paying for it
     * again — see {@link #snapshot()}.
     */
    public boolean isWebPageLoaded(String header, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            if (header.equalsIgnoreCase(headerText())) {
                Snapshot snap = snapshot();
                if (snap.nodeCount > 3) {
                    last = snap;
                    return true;
                }
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(700);
        }
    }

    /** How many elements the WebView exposes — 0 means nothing has rendered. */
    public int webViewNodeCount() {
        return current().nodeCount;
    }

    /** Visible text inside the WebView, for asserting a page really is the right one. */
    public List<String> webViewTexts() {
        return current().texts;
    }

    public boolean webPageContains(String fragment) {
        String needle = fragment.toLowerCase();
        return current().texts.stream().anyMatch(t -> t.toLowerCase().contains(needle));
    }

    /**
     * One reading of the WebView's contents.
     *
     * <p><b>Why this is not an XPath query.</b> The obvious spelling —
     * {@code driver.findElements(By.xpath("//android.webkit.WebView//*"))} — makes
     * UiAutomator2 walk the whole document remotely and measured <b>11.5 seconds
     * per call</b> on a loaded emulator. Checking a page used to cost four such
     * calls (the load check, two for the log line, one for the content assertion),
     * so a single page took the best part of a minute and read as a hang.
     *
     * <p>{@code getPageSource()} costs the same once, but it returns the entire
     * tree in one go, so node count and text come out of a local parse for free.
     */
    private static final class Snapshot {
        static final Snapshot EMPTY = new Snapshot(0, List.of());
        final int nodeCount;
        final List<String> texts;

        Snapshot(int nodeCount, List<String> texts) {
            this.nodeCount = nodeCount;
            this.texts = texts;
        }
    }

    /** The reading {@link #isWebPageLoaded} settled on, so callers reuse it. */
    private Snapshot last;

    private Snapshot current() {
        return last != null ? last : snapshot();
    }

    private Snapshot snapshot() {
        String xml;
        try {
            xml = driver.getPageSource();
        } catch (WebDriverException e) {
            return Snapshot.EMPTY;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document doc = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
            List<String> texts = new ArrayList<>();
            int[] count = {0};
            collectWebViews(doc.getDocumentElement(), false, texts, count);
            return new Snapshot(count[0], texts);
        } catch (Exception malformed) {
            return Snapshot.EMPTY;
        }
    }

    /** Walks the tree, counting and reading everything BENEATH a WebView node. */
    private void collectWebViews(Node node, boolean insideWebView, List<String> texts, int[] count) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Node classAttr = child.getAttributes().getNamedItem("class");
            boolean isWebView = classAttr != null && classAttr.getNodeValue().contains("WebView");
            if (insideWebView) {
                count[0]++;
                Node textAttr = child.getAttributes().getNamedItem("text");
                if (textAttr != null && !textAttr.getNodeValue().isBlank()) {
                    texts.add(textAttr.getNodeValue().trim());
                }
            }
            collectWebViews(child, insideWebView || isWebView, texts, count);
        }
    }

    /** Drops the kept reading — call when leaving a page. */
    private void forgetSnapshot() {
        last = null;
    }

    // ---------------- CONTACT US ----------------

    public boolean isOnContactUs(Duration timeout) {
        return headerIs("CONTACT US", timeout);
    }

    public boolean hasCallUsCard() {
        return present("cardCallUs");
    }

    public boolean hasEmailUsCard() {
        return present("cardEmail");
    }

    public boolean tapEmailUs() {
        return tapById("cardEmail");
    }

    // ---------------- mail composer ----------------

    /** Keyed on the subject field — it exists only on the composer. */
    public boolean isOnCompose(Duration timeout) {
        return presentWithin("et_subject", timeout);
    }

    /** The pre-filled recipient chip, e.g. "Support". */
    public String toRecipient() {
        return textOf("tv_collapsed_name_to");
    }

    /**
     * The BCC value — from the CHIP when it has formed, else from the input.
     *
     * <p>Once committed the address collapses into {@code tv_collapsed_name_bcc}
     * (as the display name alone, e.g. "Bhavik") and {@code et_bcc} disappears, so
     * reading only the input would report a filled BCC as empty. Before that it is
     * still raw text in {@code et_bcc}, so reading only the chip has the same
     * problem in reverse — hence both, with the placeholder filtered out.
     */
    public String bccRecipient() {
        String chip = textOf("tv_collapsed_name_bcc");
        if (!chip.isBlank()) {
            return chip;
        }
        String raw = textOf("et_bcc");
        return RECIPIENT_HINT.equalsIgnoreCase(raw) ? "" : raw;
    }

    /**
     * Types the BCC address and commits it.
     *
     * <p><b>The address is not committed until focus LEAVES the field</b> — hiding
     * the keyboard is not enough; it stays raw text in {@code et_bcc} and no chip
     * exists yet. Tapping the subject (what a user does next anyway) is what
     * collapses it, so that tap is part of typing the address rather than
     * something the caller has to remember.
     */
    public boolean typeBcc(String address) {
        if (!typeIntoField("et_bcc", address)) {
            return false;
        }
        try {
            driver.findElement(byId("et_subject")).click();
            sleep(1200);
        } catch (WebDriverException noSubjectYet) {
            // Leave it uncommitted — bccRecipient() still reads the raw input.
        }
        return true;
    }

    public String subject() {
        return textOf("et_subject");
    }

    public boolean typeSubject(String text) {
        return typeIntoField("et_subject", text);
    }

    /** Body text, including the signature the composer starts with. */
    public String body() {
        try {
            for (WebElement e : driver.findElements(AppiumBy.xpath(
                    "//*[contains(@resource-id,'editor')]"))) {
                String t = safeText(e);
                if (t != null) {
                    return t.trim();
                }
            }
        } catch (WebDriverException ignored) {
        }
        return "";
    }

    /** Types into the body — an EditText nested inside the WebView. */
    public boolean typeBody(String text) {
        try {
            WebElement e = driver.findElement(AppiumBy.xpath("//*[contains(@resource-id,'editor')]"));
            e.click();
            sleep(1000);
            e.sendKeys(text);
            hideKeyboard();
            sleep(1200);
            return true;
        } catch (WebDriverException ex) {
            return false;
        }
    }

    /** Sends the mail. Real — it reaches Zillit Support, not just the BCC address. */
    public boolean tapSend() {
        hideKeyboard();
        sleep(500);
        return tapById("btn_send");
    }

    // ---------------- shared ----------------

    public boolean tapBack() {
        forgetSnapshot();
        return tapById("backBtn");
    }

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

    /** Named to avoid clashing with BasePage's protected typeInto(String,String). */
    private boolean typeIntoField(String id, String text) {
        try {
            WebElement f = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(byId(id)));
            f.click();
            sleep(600);
            f.sendKeys(text);
            hideKeyboard();
            sleep(1200);
            return true;
        } catch (WebDriverException e) {
            return false;
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
