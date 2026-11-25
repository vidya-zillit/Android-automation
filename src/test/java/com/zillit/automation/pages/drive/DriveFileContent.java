package com.zillit.automation.pages.drive;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriverException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reads what a Drive file REALLY contains, from outside the editor.
 *
 * <p>The Zillit Sheets / Docs pages paint the document on a canvas, so nothing typed is in the
 * accessibility tree. The one honest read-back is the file itself: File details → Download puts
 * it in {@code /sdcard/Download/Drive/<name>} ({@code FileDetailViewModel}:
 * {@code setDestinationInExternalPublicDir(DIRECTORY_DOWNLOADS, "Drive/$fileName")}), and
 * Appium's {@code pullFolder} — which needs no {@code --relaxed-security} — brings that folder
 * back as a zip. Office files are zips of XML, so a marker typed into a cell or a paragraph shows
 * up as plain text in {@code xl/sharedStrings.xml} / {@code xl/worksheets/*.xml} /
 * {@code word/document.xml}.
 *
 * <p>DownloadManager never overwrites: a second download of the same name lands as
 * {@code name-1.xlsx}. So every copy whose name starts with the file's base name is searched, and
 * a unique marker per run means "any copy holds it" is exactly the question.
 */
public final class DriveFileContent {

    private static final List<String> FOLDERS = List.of("/sdcard/Download/Drive", "/sdcard/Download");

    private final AndroidDriver driver;

    public DriveFileContent(AndroidDriver driver) {
        this.driver = driver;
    }

    /**
     * Polls the download folders until a downloaded copy of {@code fileName} contains
     * {@code marker}, or the timeout passes. Returns what was found, for the failure message.
     */
    public Result waitForMarker(String fileName, String marker, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        Result last = new Result();
        while (System.currentTimeMillis() < deadline) {
            last = scan(fileName, marker);
            if (last.markerFound) return last;
            sleep(3000);
        }
        return last;
    }

    /** One pass over the download folders. */
    public Result scan(String fileName, String marker) {
        Result r = new Result();
        String base = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        for (String folder : FOLDERS) {
            byte[] zip;
            try { zip = driver.pullFolder(folder); }
            catch (WebDriverException e) { r.notes.add(folder + ": " + firstLine(e.getMessage())); continue; }
            try (ZipInputStream outer = new ZipInputStream(new ByteArrayInputStream(zip))) {
                ZipEntry e;
                while ((e = outer.getNextEntry()) != null) {
                    String name = e.getName().replaceFirst(".*/", "");
                    if (e.isDirectory() || !name.startsWith(base)) continue;
                    // pullFolder of /sdcard/Download also returns Drive/… — only take direct children
                    // there, the Drive/ pass already covered the subfolder.
                    if (folder.equals("/sdcard/Download") && e.getName().contains("Drive/")) continue;
                    byte[] file = outer.readAllBytes();
                    r.copies.add(name + " (" + file.length + " B)");
                    if (contains(file, marker)) { r.markerFound = true; r.foundIn = name; return r; }
                }
            } catch (IOException ex) {
                r.notes.add(folder + ": unreadable zip " + ex.getMessage());
            }
        }
        return r;
    }

    /** Names of everything under /sdcard/Download (Drive/ included, as "Drive/<name>"). */
    public List<String> downloadNames() {
        List<String> out = new ArrayList<>();
        for (String folder : List.of("/sdcard/Download/Drive", "/sdcard/Download")) {
            try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(driver.pullFolder(folder)))) {
                ZipEntry e;
                while ((e = z.getNextEntry()) != null) {
                    if (e.isDirectory()) continue;
                    String n = e.getName();
                    out.add(folder.endsWith("Drive") ? "Drive/" + n.replaceFirst(".*/", "") : n.replaceFirst("^[^/]*/", ""));
                }
            } catch (Exception ex) {
                System.out.println(">>> pullFolder " + folder + " failed: " + firstLine(ex.getMessage()));
            }
        }
        return out;
    }

    /** Polls until a downloaded file's name contains {@code part}. Returns the name or null. */
    public String waitForDownloadNamed(String part, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            for (String n : downloadNames()) if (n.contains(part)) return n;
            sleep(3000);
        }
        return null;
    }

    /**
     * Polls for a downloaded .zip whose entries include every one of {@code entries}
     * (bulk Download of two or more files is ONE server zip). Returns its name or null.
     */
    public String waitForZipContaining(List<String> entries, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try (ZipInputStream outer = new ZipInputStream(new ByteArrayInputStream(driver.pullFolder("/sdcard/Download")))) {
                ZipEntry e;
                while ((e = outer.getNextEntry()) != null) {
                    if (e.isDirectory() || !e.getName().toLowerCase().endsWith(".zip")) continue;
                    byte[] zip = outer.readAllBytes();
                    List<String> inner = new ArrayList<>();
                    try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(zip))) {
                        ZipEntry ie;
                        while ((ie = z.getNextEntry()) != null) inner.add(ie.getName().replaceFirst(".*/", ""));
                    }
                    if (inner.containsAll(entries)) return e.getName().replaceFirst(".*/", "");
                }
            } catch (Exception ignored) { }
            sleep(4000);
        }
        return null;
    }

    /** True when the marker is in the file — directly, or in any XML part of an office zip. */
    static boolean contains(byte[] file, String marker) {
        if (new String(file, StandardCharsets.ISO_8859_1).contains(marker)) return true;
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(file))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                z.transferTo(out);
                if (out.toString(StandardCharsets.UTF_8).contains(marker)) return true;
            }
        } catch (IOException ignored) { }
        return false;
    }

    public static final class Result {
        public boolean markerFound;
        public String foundIn;
        public final List<String> copies = new ArrayList<>();
        public final List<String> notes = new ArrayList<>();
        @Override public String toString() {
            return "found=" + markerFound + (foundIn == null ? "" : " in " + foundIn)
                    + " copies=" + copies + (notes.isEmpty() ? "" : " notes=" + notes);
        }
    }

    private static String firstLine(String s) {
        return s == null ? "" : s.split("\n")[0];
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
