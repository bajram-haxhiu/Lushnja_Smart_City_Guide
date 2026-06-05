package com.lushnja.services;

public class ApiConfig {

    public static String getGeminiApiKey() {
        return System.getenv("GEMINI_API_KEY");
    }

    public static boolean hasGeminiApiKey() {
        String key = getGeminiApiKey();
        return key != null && !key.isBlank();
    }
}