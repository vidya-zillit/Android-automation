package com.zillit.automation.pages.box;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Production Diary → drawer → History ({@code HistoryActivity}, {@code new_box_activity_history}).
 *
 * <p>Filter row: {@code spinnerFilter} (action, default ALL), {@code btnPickDate} /
 * {@code btnClearDate} (date, default none). The activity log is {@code historyList}; each
 * card ({@code new_box_item_history_card}) carries {@code tvActionLabel} ("ADDED", "CHANGED",
 * "REMOVED"…), {@code tvTargetType} ("Schedule", "Event", "Note"…), {@code tvHistoryTitle}
 * (the target's title in curly quotes, as the server sent it), {@code tvDetails} (server text,
 * optional), {@code tvDateTime} and {@code tvByName} ("by …"). Empty: {@code emptyState}.
 */
public class BoxHistoryPage extends BoxSchedulePage {

    public BoxHistoryPage(AndroidDriver driver) {
        super(driver);
    }

    /** One activity-log card, as rendered. */
    public static final class Card {
        public final String action, target, title, details, when, by;
        Card(String action, String target, String title, String details, String when, String by) {
            this.action = action; this.target = target; this.title = title;
            this.details = details; this.when = when; this.by = by;
        }
        public String key() { return action + "|" + target + "|" + title + "|" + when + "|" + by; }
        @Override public String toString() {
            return action + " · " + target + " · " + title + (details == null ? "" : " [" + details + "]")
                    + " · " + when + " · " + by;
        }
    }

    @Override
    public boolean isLoaded(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        do {
            // historyList is VISIBLE in the layout before the log arrives, so wait for a card
            // or the empty state — not for the list itself.
            if (!displayed("historyLoader") && (present("tvActionLabel") || displayed("emptyState"))) return true;
            pause(400);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    public boolean emptyShown() { return displayed("emptyState"); }

    /** What is on screen, for a failure message: the activity and the first visible texts. */
    public String describe() {
        StringBuilder b = new StringBuilder();
        try { b.append(driver.currentActivity()).append(" | "); } catch (Exception ignored) { }
        b.append("loader=").append(displayed("historyLoader")).append(" empty=").append(emptyShown())
                .append(" list=").append(displayed("historyList")).append(" | ");
        try {
            int n = 0;
            for (WebElement t : driver.findElements(AppiumBy.className("android.widget.TextView"))) {
                String s = safe(t);
                if (s == null) continue;
                b.append(s).append(" ¦ ");
                if (++n >= 30) break;
            }
        } catch (Exception ignored) { }
        return b.toString();
    }

    public boolean clearDate() { return clickIfPresent("btnClearDate"); }

    /**
     * Cards on screen, top to bottom.
     *
     * <p>Read field by field from the screen and paired by position — NOT by walking up from
     * each label with an {@code ancestor::} XPath: UiAutomator2 evaluates an element-scoped
     * XPath inside that element's own subtree, so the walk found nothing and every card was
     * lost. Each card has exactly one action label, target, title, time and "by"; the details
     * line is optional, so it is matched to the card whose title sits just above it.
     */
    public List<Card> visibleCards() {
        List<Card> out = new ArrayList<>();
        try {
            List<WebElement> labels = driver.findElements(byId("tvActionLabel"));
            List<WebElement> targets = driver.findElements(byId("tvTargetType"));
            List<WebElement> titles = driver.findElements(byId("tvHistoryTitle"));
            List<WebElement> whens = driver.findElements(byId("tvDateTime"));
            List<WebElement> bys = driver.findElements(byId("tvByName"));
            List<WebElement> details = driver.findElements(byId("tvDetails"));
            // A card cut by the screen edge can lose its top or bottom fields; keep only the
            // cards whose title is on screen and take the nearest field of each kind.
            for (WebElement title : titles) {
                int y = title.getRect().getY();
                out.add(new Card(nearestAbove(labels, y), nearestAbove(targets, y), safe(title),
                        nearestBelow(details, y, titles), nearestBelow(whens, y, titles), nearestBelow(bys, y, titles)));
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** Text of the element closest above {@code y} (the card's header row sits above its title). */
    private static String nearestAbove(List<WebElement> els, int y) {
        WebElement best = null;
        int bestY = Integer.MIN_VALUE;
        for (WebElement e : els) {
            int ey = e.getRect().getY();
            if (ey <= y && ey > bestY) { best = e; bestY = ey; }
        }
        return best == null ? null : safe(best);
    }

    /** Text of the element closest below {@code y} but above the next title, or null. */
    private static String nearestBelow(List<WebElement> els, int y, List<WebElement> titles) {
        int limit = Integer.MAX_VALUE;
        for (WebElement t : titles) {
            int ty = t.getRect().getY();
            if (ty > y && ty < limit) limit = ty;
        }
        WebElement best = null;
        int bestY = Integer.MAX_VALUE;
        for (WebElement e : els) {
            int ey = e.getRect().getY();
            if (ey > y && ey < limit && ey < bestY) { best = e; bestY = ey; }
        }
        return best == null ? null : safe(best);
    }

    /** Every card, scrolling {@code historyList} to its end (bounded). Newest first, as the app lists them. */
    public List<Card> allCards(int maxSwipes) {
        Map<String, Card> seen = new LinkedHashMap<>();
        int quiet = 0;
        for (int i = 0; i <= maxSwipes && quiet < 2; i++) {
            int before = seen.size();
            for (Card c : visibleCards()) seen.putIfAbsent(c.key(), c);
            quiet = seen.size() == before ? quiet + 1 : 0;
            if (!scrollList("historyList", true)) break;
        }
        return new ArrayList<>(seen.values());
    }

    private String child(WebElement root, String id) {
        try {
            List<WebElement> c = root.findElements(byId(id));
            return c.isEmpty() ? null : safe(c.get(0));
        } catch (Exception e) { return null; }
    }

    private static String safe(WebElement e) {
        try {
            String t = e.getText();
            return t == null || t.isBlank() ? null : t.trim();
        } catch (Exception ex) { return null; }
    }
}
