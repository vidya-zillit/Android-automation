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
}
