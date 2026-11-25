package com.zillit.automation.core;

/**
 * The ONLY email addresses suites may send to: the project's own test accounts. Nothing is ever
 * addressed to an invented mailbox — a made-up address on a real domain (qa.recipient@zillit.com)
 * can reach a stranger or bounce into someone's inbox.
 *
 * <p>Defaults are the two ZL-Android-Automation users (read off the app's From line and the To
 * suggestions, 2026-11-23); override with {@code zillit.email.recipient} / {@code zillit.email.self}
 * (env {@code ZILLIT_EMAIL_RECIPIENT} / {@code ZILLIT_EMAIL_SELF}) on another project.
 */
public final class TestEmails {

    private TestEmails() { }

    /** The other test user (Vidya Pixel, the Pixel's account) — the default recipient. */
    public static String recipient() {
        return Config.has("zillit.email.recipient") ? Config.get("zillit.email.recipient")
                : "vidyapixel.zlandroidautomation@zillit.net";
    }

    /** The signed-in user on the emulators (Vidya Testing) — for a second, distinct address. */
    public static String self() {
        return Config.has("zillit.email.self") ? Config.get("zillit.email.self")
                : "vidyatesting.zlandroidautomation@zillit.net";
    }
}
