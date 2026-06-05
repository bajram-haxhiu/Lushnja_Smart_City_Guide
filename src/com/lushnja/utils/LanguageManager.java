package com.lushnja.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Language Manager — handles EN/SQ switching.
 * Loads strings from resources/i18n/messages_en.properties
 * and resources/i18n/messages_sq.properties.
 *
 * Usage:
 *   LanguageManager.getInstance().setLanguage("en");
 *   String label = LanguageManager.getInstance().get("search.section");
 *   LanguageManager.getInstance().addListener(lm -> rebuildUI());
 */
public class LanguageManager {

    private static final LanguageManager INSTANCE = new LanguageManager();

    private String currentLang = "en"; // default English
    private Properties props = new Properties();
    private final List<Runnable> listeners = new ArrayList<>();

    private LanguageManager() {
        loadProperties("en");
    }

    public static LanguageManager getInstance() { return INSTANCE; }

    // ── Language Switch ───────────────────────────────────────────────────────

    public void setLanguage(String lang) {
        String normalized = lang.toLowerCase().trim();
        if (normalized.equals(currentLang)) return;
        currentLang = normalized;
        loadProperties(normalized);
        notifyListeners();
    }

    public void toggleLanguage() {
        setLanguage(currentLang.equals("en") ? "sq" : "en");
    }

    public String getCurrentLanguage() { return currentLang; }

    public boolean isEnglish() { return "en".equals(currentLang); }

    // ── Get String ────────────────────────────────────────────────────────────

    /**
     * Get localized string. Falls back to the key if not found.
     */
    public String get(String key) {
        return props.getProperty(key, key);
    }

    /**
     * Get localized string with MessageFormat substitutions.
     * e.g. get("search.found", 5, "hospital")
     */
    public String get(String key, Object... args) {
        String pattern = props.getProperty(key, key);
        try {
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            return pattern;
        }
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    public void addListener(Runnable listener) { listeners.add(listener); }
    public void removeListener(Runnable listener) { listeners.remove(listener); }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }

    // ── Load Properties ───────────────────────────────────────────────────────

    private void loadProperties(String lang) {
        String path = "/i18n/messages_" + lang + ".properties";
        Properties loaded = new Properties();
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                loaded.load(new InputStreamReader(is, StandardCharsets.UTF_8));
                props = loaded;
            } else {
                System.err.println("Language file not found: " + path);
            }
        } catch (IOException e) {
            System.err.println("Error loading language file: " + path);
        }
    }
}
