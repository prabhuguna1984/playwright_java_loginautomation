package com.automation.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigReader {
    private static final Properties PROPERTIES = new Properties();

    private ConfigReader() {
    }

    static {
        try (InputStream input = ConfigReader.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IllegalStateException("config.properties was not found on the classpath");
            }
            PROPERTIES.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read config.properties", exception);
        }
    }

    /**
     * Returns a required, non-blank configuration value.
     *
     * @param key the property name to read
     * @return the trimmed value
     * @throws IllegalStateException if the property is missing or blank
     */
    private static String getProperty(String key) {
        String value = System.getProperty(key, PROPERTIES.getProperty(key));
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required config.properties value: " + key);
        }
        return value.trim();
    }

    /** @return the configured application URL */
    public static String getApplicationUrl() {
        return getProperty("application.url");
    }

    /** @return the configured browser type */
    public static String getBrowserType() {
        return getProperty("browser.type");
    }

    /**
     * Reads the configured headless browser setting.
     *
     * @return {@code true} when the browser should run headlessly
     * @throws IllegalStateException if the setting is not true or false
     */
    public static boolean isHeadless() {
        String value = getProperty("browser.headless");
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalStateException("browser.headless must be true or false");
        }
        return Boolean.parseBoolean(value);
    }
}
