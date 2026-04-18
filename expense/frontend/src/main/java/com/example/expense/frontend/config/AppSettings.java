package com.example.expense.frontend.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppSettings {

    private static final String PROPERTIES_FILE = "/application.properties";

    private final Properties properties = new Properties();

    public AppSettings() {
        try (InputStream input = getClass().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
            // Falling back to defaults
        }
    }

    public String getBackendBaseUrl() {
        return properties.getProperty("backend.base-url", "http://localhost:8080");
    }
}

