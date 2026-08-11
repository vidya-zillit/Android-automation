package com.zillit.automation.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;

/**
 * Layered configuration: system property > environment variable > config/config.properties.
 * Key "zillit.apkPath" maps to env var "ZILLIT_APK_PATH".
 */
public final class Config {

    private static final Properties FILE_PROPS = load();

    private Config() {
    }

    private static Properties load() {
        Properties props = new Properties();
        Path path = Paths.get("config", "config.properties");
        try (InputStream in = new FileInputStream(path.toFile())) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path.toAbsolutePath(), e);
        }
        return props;
    }

    public static String get(String key) {
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        String envKey = key.replace('.', '_')
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase(Locale.ROOT);
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String fromFile = FILE_PROPS.getProperty(key, "");
        return fromFile.trim();
    }

    public static boolean has(String key) {
        return !get(key).isEmpty();
    }

    /**
     * The package resource-ids belong to, from configuration alone — "" if nothing
     * is set.
     *
     * <p><b>Why this is not read from the foreground app.</b> Asking the device
     * which package is in front only gives the right answer when the app under
     * test happens to BE in front. It is not, for instance, right after a suite
     * that opened the camera, the gallery or a QR scanner, or when a previous run
     * left the launcher up. Resolving ids from whatever was on screen at that
     * moment silently builds every locator against the wrong package — nothing
     * matches, and every wait burns its full timeout instead of failing fast.
     *
     * <p>{@code zillit.resourcePackage} wins so a flavour whose resource ids differ
     * from its application id can still be pointed somewhere else;
     * {@code zillit.appPackage} is the ordinary answer and is already set.
     */
    public static String configuredAppPackage() {
        if (has("zillit.resourcePackage")) {
            return get("zillit.resourcePackage");
        }
        return has("zillit.appPackage") ? get("zillit.appPackage") : "";
    }
}
