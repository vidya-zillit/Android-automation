package com.zillit.automation.pages.continuity;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@code ContinuitySceneListPage}: one scene number's records.
 *
 * <p>Two modes, decided by the tab it was opened from:
 * <ul>
 *   <li><b>My Department</b> — straight to a 2-column grid of cards, title "Scene No. - X".</li>
 *   <li><b>All Department</b> — first a department list ({@code recDepartmentScene}, title
 *       "Department List Continuity"), then a department's grid, title "Scene No - X".
 *       BACK from the grid returns to the list; BACK again leaves.</li>
 * </ul>
 *
 * <p>Reading a card. Every cell repeats the same ids ({@code tvScene}, {@code tvNotes},
 * {@code tvDetail}, {@code imgMore}, {@code layOverlay}), and Appium never looks UPWARD
 * from an element, so the card is read from its root DOWN: {@code layMainCard} is a
 * clickable MaterialCardView and survives in the tree. Where it does not, the fallback
 * slices by geometry — a row band from {@code bandsOf("tvScene")}, split into columns by
 * the anchor's x — because a grid band holds two cards.
 *
 * <p>The grid's shimmer ({@code loaderView}) is hidden only on a SUCCESSFUL response; after
 * an error it animates over the grid for good, and UiAutomator never reaches idle. So
 * {@link #waitLoaded} is an explicit wait with a deadline, and a timeout there is reported
 * as that, not as "no cards".
 */
public class ContinuityScenePage extends ContinuityPage {

    public static final String DEPT_LIST_TITLE = "Department List Continuity";
    public static final String DEPT_SEARCH_HINT = "Search By Department Name";
    public static final String GRID_SEARCH_HINT = "Search by Scene no./ Scene notes";
    public static final List<String> ADMIN_MENU_MY =
            List.of("Edit Details", "Forward", "Distribute", "Download", "Delete");

    public ContinuityScenePage(AndroidDriver driver) {
        super(driver);
    }

    /** One grid card as the user sees it. */
    public static final class Card {
        public final String scene;   // "Scene - X"
        public final String notes;   // "Scene Note - …" or null
        public final String detail;  // "Department - …" or null
        public final boolean video;
        public final boolean selected;
        public final Rectangle rect;
        Card(String scene, String notes, String detail, boolean video, boolean selected, Rectangle rect) {
            this.scene = scene; this.notes = notes; this.detail = detail;
            this.video = video; this.selected = selected; this.rect = rect;
        }
        /** Notes without the hard-coded "Scene Note - " prefix. */
        public String noteValue() {
            if (notes == null) return null;
            int i = notes.indexOf(" - ");
            return i < 0 ? notes : notes.substring(i + 3);
        }
        @Override public String toString() {
            return scene + (notes == null ? "" : " / " + notes) + (detail == null ? "" : " / " + detail)
                    + (selected ? " [selected]" : "");
        }
    }

    // ─────────────────────────── shell ───────────────────────────

    public boolean isLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!onScreen("recScene").isEmpty() || !onScreen("recDepartmentScene").isEmpty()) return true;
            pause(400);
        }
        return false;
    }

    /** Waits for the shimmer to go. False means it was still animating at the deadline. */
    public boolean waitLoaded(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (onScreen("loaderView").isEmpty() && onScreen("progress_circular").isEmpty()) return true;
            pause(500);
        }
        return false;
    }

    public boolean onDepartmentList() { return !onScreen("recDepartmentScene").isEmpty(); }
    public boolean onGrid() { return !onScreen("recScene").isEmpty(); }

    // ─────────────────────────── cards ───────────────────────────

    /** The cards on screen, top-left to bottom-right. */
    public List<Card> visibleCards() {
        List<Card> out = new ArrayList<>();
        List<WebElement> roots = onScreen("layMainCard");
        if (!roots.isEmpty()) {
            for (WebElement r : roots) {
                Card c = readCard(r);
                if (c != null) out.add(c);
            }
        } else {
            out.addAll(cardsByGeometry());
        }
        out.sort((a, b) -> {
            if (Math.abs(a.rect.getY() - b.rect.getY()) > 20) return Integer.compare(a.rect.getY(), b.rect.getY());
            return Integer.compare(a.rect.getX(), b.rect.getX());
        });
        return out;
    }

    private Card readCard(WebElement root) {
        try {
            String scene = childText(root, "tvScene");
            if (scene == null) return null;
            return new Card(scene, blankToNull(childText(root, "tvNotes")), blankToNull(childText(root, "tvDetail")),
                    !root.findElements(byId("playPauseImage")).isEmpty(),
                    !root.findElements(byId("layOverlay")).isEmpty(),
                    root.getRect());
        } catch (WebDriverException e) { return null; }
    }

    /** Fallback when card roots are pruned: row band by y, then column by the anchor's x. */
    private List<Card> cardsByGeometry() {
        List<Card> out = new ArrayList<>();
        List<WebElement> anchors = elementsById("tvScene");
        List<Band> bands = bandsOf("tvScene");
        for (int i = 0; i < anchors.size() && i < bands.size(); i++) {
            WebElement a = anchors.get(i);
            Rectangle ar = a.getRect();
            Band b = bands.get(i);
            String notes = null, detail = null;
            boolean video = false, selected = false;
            for (WebElement n : elementsIn(b, "tvNotes")) if (sameColumn(ar, n)) notes = text(n);
            for (WebElement d : elementsIn(b, "tvDetail")) if (sameColumn(ar, d)) detail = text(d);
            for (WebElement v : elementsIn(b, "playPauseImage")) if (sameColumn(ar, v)) video = true;
            for (WebElement s : elementsIn(b, "layOverlay")) if (sameColumn(ar, s)) selected = true;
            out.add(new Card(text(a), blankToNull(notes), blankToNull(detail), video, selected, ar));
        }
        return out;
    }

    private static boolean sameColumn(Rectangle anchor, WebElement e) {
        try {
            int cx = e.getRect().getX() + e.getRect().getWidth() / 2;
            return cx >= anchor.getX() - 40 && cx <= anchor.getX() + Math.max(anchor.getWidth(), 300);
        } catch (WebDriverException ex) { return false; }
    }

    /**
     * Every card, following the grid past the fold. Cards are keyed on scene + notes, so
     * records that must be told apart need distinct notes — the suite gives every record
     * its own.
     *
     * <p>Not {@link #readWholeList}: that decides "the list ends on screen" from the pitch
     * between the LAST TWO anchors, and in a two-column grid those two sit on the same row —
     * a pitch of zero, so it would always conclude there is nothing below. This one simply
     * scrolls until a window adds no new card, then scrolls back.
     */
    public List<Card> allCards() {
        java.util.Map<String, Card> seen = new java.util.LinkedHashMap<>();
        java.util.function.Consumer<List<Card>> add = cs -> {
            for (Card c : cs) seen.putIfAbsent(c.scene + "|" + c.notes + "|" + c.detail, c);
        };
        add.accept(visibleCards());
        int scrolls = 0;
        for (int pass = 0; pass < 14; pass++) {
            if (!scrollList("recScene", true)) break;
            scrolls++;
            int before = seen.size();
            add.accept(visibleCards());
            if (seen.size() == before) break;
        }
        for (int i = 0; i < scrolls; i++) scrollList("recScene", false);
        return new ArrayList<>(seen.values());
    }

    public Card cardWithNote(String noteValue) {
        for (Card c : allCards()) if (noteValue.equals(c.noteValue())) return c;
        return null;
    }

    /** The on-screen root of the card carrying this note, scrolling to it. */
    private WebElement cardRoot(String noteValue) {
        String want = "Scene Note - " + noteValue;
        scrollToRow("recScene", "tvNotes", want);
        for (WebElement r : onScreen("layMainCard")) {
            if (want.equals(childText(r, "tvNotes"))) return r;
        }
        return null;
    }

    /** The on-screen root of a card WITHOUT notes, scanning the visible window. */
    private WebElement cardRootWithoutNotes() {
        for (WebElement r : onScreen("layMainCard")) {
            if (blankToNull(childText(r, "tvNotes")) == null) return r;
        }
        return null;
    }

    public boolean tapCard(String noteValue) {
        WebElement r = noteValue == null ? cardRootWithoutNotes() : cardRoot(noteValue);
        if (r == null) return false;
        try {
            // Tap the image area, away from the ⋮ in the corner.
            Rectangle b = r.getRect();
            tapAt(b.getX() + b.getWidth() / 3, b.getY() + b.getHeight() / 3);
            pause(1500);
            return true;
        } catch (WebDriverException e) { return false; }
    }

    /** Opens the ⋮ menu of a card; returns its item labels in order, or null. */
    public List<String> openMenu(String noteValue) {
        WebElement r = noteValue == null ? cardRootWithoutNotes() : cardRoot(noteValue);
        if (r == null) return null;
        try {
            List<WebElement> more = r.findElements(byId("imgMore"));
            if (more.isEmpty()) return null;
            more.get(0).click();
            pause(1200);
        } catch (WebDriverException e) { return null; }
        return menuItems();
    }

    /** Average-colour fingerprint of a card's picture — tells a photo from the logo placeholder. */
    public int[] pictureColour(String noteValue) {
        WebElement r = cardRoot(noteValue);
        if (r == null) return null;
        try {
            List<WebElement> img = r.findElements(byId("imgScene"));
            WebElement target = img.isEmpty() ? r : img.get(0);
            byte[] png = target.getScreenshotAs(OutputType.BYTES);
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(png));
            if (bi == null) return null;
            long rs = 0, gs = 0, bs = 0, n = 0;
            for (int y = 0; y < bi.getHeight(); y += 4) {
                for (int x = 0; x < bi.getWidth(); x += 4) {
                    int p = bi.getRGB(x, y);
                    rs += (p >> 16) & 0xff; gs += (p >> 8) & 0xff; bs += p & 0xff; n++;
                }
            }
            return n == 0 ? null : new int[]{(int) (rs / n), (int) (gs / n), (int) (bs / n)};
        } catch (Exception e) { return null; }
    }

    // ─────────────────────────── selection mode ───────────────────────────

    public boolean selectionBarShown() { return !onScreen("layForward").isEmpty() || !onScreen("btnForward").isEmpty(); }
    public String selectionCount() { return firstText("tvCount"); }
    public String selectionAction() { return firstText("btnForward"); }
    public boolean tapSelectionAction() { return tapFirst("btnForward"); }
    public boolean cancelSelection() { return tapFirst("btnCancel"); }

    // ─────────────────────────── departments ───────────────────────────

    public List<String> departments() {
        List<String> out = new ArrayList<>();
        for (WebElement e : onScreen("textView")) {
            String s = text(e);
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    public boolean openDepartment(String name) {
        if (!scrollToRow("recDepartmentScene", "textView", name)) return false;
        for (WebElement e : onScreen("textView")) {
            if (name.equals(text(e))) {
                try { e.click(); pause(2000); return true; }
                catch (WebDriverException ex) { return false; }
            }
        }
        return false;
    }

    public List<String> longPressDepartment(String name) {
        for (WebElement e : onScreen("textView")) {
            if (name.equals(text(e))) {
                longPress(e);
                pause(1200);
                return popupMenuTitles();
            }
        }
        return null;
    }

    public boolean tapPopupMenu(String title) {
        try {
            for (WebElement e : driver.findElements(org.openqa.selenium.By.id("android:id/title"))) {
                if (title.equals(text(e))) { e.click(); pause(1500); return true; }
            }
        } catch (WebDriverException ignored) { }
        return false;
    }

    // ─────────────────────────── the viewer ───────────────────────────

    /** {@code CustomImageViewer} — the description a card opens with. */
    public String viewerDescription(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            String d = firstText("descriptionImage");
            if (d != null && !d.isEmpty()) return d;
            pause(500);
        }
        return null;
    }

    public boolean viewerShown(Duration timeout) {
        return isVisible("viewpager", timeout) || isVisible("descriptionImage", Duration.ofSeconds(2));
    }

    // ─────────────────────────── helpers ───────────────────────────

    private String childText(WebElement root, String id) {
        try {
            List<WebElement> c = root.findElements(byId(id));
            return c.isEmpty() ? null : text(c.get(0));
        } catch (WebDriverException e) { return null; }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private void tapAt(int x, int y) {
        org.openqa.selenium.interactions.PointerInput f = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence s = new org.openqa.selenium.interactions.Sequence(f, 1);
        s.addAction(f.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        s.addAction(f.createPointerDown(0));
        s.addAction(f.createPointerUp(0));
        driver.perform(List.of(s));
    }

    /** Distance between two colour fingerprints (0 = identical). */
    public static int colourDistance(int[] a, int[] b) {
        if (a == null || b == null) return Integer.MAX_VALUE;
        Objects.requireNonNull(a);
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]);
    }
}
