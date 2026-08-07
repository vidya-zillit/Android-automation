package com.zillit.automation.core;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public final class DriverFactory {

    private DriverFactory() {
    }

    /** Seconds a session may sit idle before Appium reaps it. */
    private static long newCommandTimeoutSeconds() {
        String v = Config.get("zillit.newCommandTimeout");
        if (v != null && !v.isBlank()) {
            try { return Long.parseLong(v.trim()); } catch (NumberFormatException ignored) { }
        }
        return 900;   // 15 min — covers a long stretch of work on the OTHER device
    }

    public static AndroidDriver create() {
        return create(Config.has("zillit.udid") ? Config.get("zillit.udid") : null, 0);
    }

    /**
     * Creates a session bound to a specific device. Pass a distinct
     * {@code systemPort} per concurrent session on the same Appium server — the
     * UiAutomator2 driver opens a per-session bridge on that port and two
     * sessions sharing 8200 (the default) collide. Used by the two-device
     * (sender + receiver) suites. {@code systemPort <= 0} means "let Appium
     * pick" (fine for a single session).
     */
    public static AndroidDriver create(String udidOverride, int systemPort) {
        // noReset (default true): reuse the installed app instead of reinstalling
        // the ~362 MB APK on every session — the main cause of repeated relaunches,
        // especially painful on a physical device over USB.
        boolean noReset = !"false".equalsIgnoreCase(configOrDefault("zillit.noReset", "true"));

        UiAutomator2Options options = new UiAutomator2Options()
                .setDeviceName(Config.get("zillit.deviceName"))
                // Appium kills a session that receives no commands for this long. In a
                // TWO-DEVICE suite one driver is always idle while the test drives the
                // other, and those stretches are long (an editor load alone allows 150s)
                // — at 120s session A died mid-run and every later case failed with
                // NoSuchSessionException. Generous by default; override with
                // zillit.newCommandTimeout / ZILLIT_NEW_COMMAND_TIMEOUT.
                .setNewCommandTimeout(Duration.ofSeconds(newCommandTimeoutSeconds()))
                // 120s, not 60: on the Pixel over USB an adb call late in a long
                // two-device run has timed out and taken the UiAutomator2 instrumentation
                // with it — "instrumentation process is not running (probably crashed)",
                // after which every later command on that device fails.
                .setAdbExecTimeout(Duration.ofSeconds(120))
                // Same story for the server's own launch/read: the defaults are tuned for
                // a fresh, idle device, and this suite asks for the opposite.
                .setUiautomator2ServerLaunchTimeout(Duration.ofSeconds(120))
                .setUiautomator2ServerInstallTimeout(Duration.ofSeconds(120))
                .setUiautomator2ServerReadTimeout(Duration.ofSeconds(300))
                // Animations add nothing here and cost stability: a gesture that lands
                // mid-transition is the single most common "the tap was swallowed".
                .setDisableWindowAnimation(true)
                .setAutoGrantPermissions(true)
                .setNoReset(noReset);

        if (systemPort > 0) {
            options.setSystemPort(systemPort);
            // Keep the mjpeg screen-stream port unique too, or the second session
            // fails to bind it.
            options.setMjpegServerPort(systemPort + 100);
        }

        String udid = udidOverride != null && !udidOverride.isBlank()
                ? udidOverride
                : (Config.has("zillit.udid") ? Config.get("zillit.udid") : null);
        if (udid != null) {
            options.setUdid(udid);
        }

        // Prefer launching an already-installed package (no reinstall) when one is
        // named; fall back to installing the APK.
        if (Config.has("zillit.appPackage")) {
            options.setAppPackage(Config.get("zillit.appPackage"))
                    .setAppActivity(Config.get("zillit.appActivity"))
                    .setAppWaitActivity("*");
        } else if (Config.has("zillit.apkPath")) {
            Path apk = Paths.get(Config.get("zillit.apkPath")).toAbsolutePath().normalize();
            if (!Files.exists(apk)) {
                throw new IllegalStateException(
                        "APK not found: " + apk + ". Build it first (./gradlew assembleDevelopDebug in Zillit-Android) "
                                + "or point zillit.apkPath / zillit.appPackage at what you want to test.");
            }
            options.setApp(apk.toString());
        } else {
            throw new IllegalStateException("Set either zillit.apkPath or zillit.appPackage in config.properties");
        }

        try {
            return new AndroidDriver(new URL(Config.get("zillit.serverUrl")), options);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Bad Appium server URL: " + Config.get("zillit.serverUrl"), e);
        }
    }

    private static String configOrDefault(String key, String fallback) {
        String v = Config.get(key);
        return v.isEmpty() ? fallback : v;
    }
}
