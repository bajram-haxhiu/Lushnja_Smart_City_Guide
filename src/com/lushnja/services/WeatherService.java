package com.lushnja.services;

import com.lushnja.models.WeatherInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Weather service for Lushnja using Open-Meteo API (no API key required).
 * Falls back to mock/simulated data if the API is unreachable.
 *
 * Lushnja coordinates: lat=40.9419, lon=19.7053
 *
 * API endpoint example:
 * https://api.open-meteo.com/v1/forecast?latitude=40.9419&longitude=19.7053
 *   &current=temperature_2m,weathercode,windspeed_10m,precipitation
 *   &timezone=Europe/Tirane
 */
public class WeatherService {

    private static final String LUSHNJA_LAT = "40.9419";
    private static final String LUSHNJA_LON = "19.7053";

    private static final String API_URL =
        "https://api.open-meteo.com/v1/forecast?" +
        "latitude=" + LUSHNJA_LAT + "&longitude=" + LUSHNJA_LON +
        "&current=temperature_2m,weathercode,windspeed_10m,precipitation" +
        "&timezone=Europe%2FTirane";

    private final HttpClient httpClient;
    private WeatherInfo cachedWeather;
    private long cacheTime = 0;
    private static final long CACHE_DURATION_MS = 10 * 60 * 1000; // 10 minutes

    public WeatherService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    /**
     * Fetches weather asynchronously; calls onResult with a WeatherInfo object.
     * Falls back to mock data on failure.
     */
    public void fetchWeatherAsync(Consumer<WeatherInfo> onResult) {
        // Return cached if fresh
        if (cachedWeather != null && System.currentTimeMillis() - cacheTime < CACHE_DURATION_MS) {
            onResult.accept(cachedWeather);
            return;
        }
        Thread.ofVirtual().start(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    WeatherInfo info = parseOpenMeteoResponse(response.body());
                    cachedWeather = info;
                    cacheTime = System.currentTimeMillis();
                    onResult.accept(info);
                } else {
                    onResult.accept(getMockWeather());
                }
            } catch (Exception e) {
                onResult.accept(getMockWeather());
            }
        });
    }

    /**
     * Lightweight JSON parser for Open-Meteo response — no external lib needed.
     */
    private WeatherInfo parseOpenMeteoResponse(String json) {
        double temp     = extractDouble(json, "temperature_2m");
        int    code     = (int) extractDouble(json, "weathercode");
        double wind     = extractDouble(json, "windspeed_10m");
        double precip   = extractDouble(json, "precipitation");
        String cond     = codeToAlbanian(code);
        String updated  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        return new WeatherInfo(temp, cond, wind, precip, code, updated);
    }

    private double extractDouble(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return 0.0;
        int colon = json.indexOf(":", idx);
        if (colon < 0) return 0.0;
        int start = colon + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\n')) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        try { return Double.parseDouble(json.substring(start, end)); }
        catch (Exception e) { return 0.0; }
    }

    private String codeToAlbanian(int code) {
        if (code == 0)                      return "Diell i kthjellët";
        if (code <= 2)                      return "Pjesërisht me re";
        if (code == 3)                      return "Me re";
        if (code >= 45 && code <= 48)       return "Mjegull";
        if (code >= 51 && code <= 57)       return "Shiù i lehtë";
        if (code >= 61 && code <= 67)       return "Shi";
        if (code >= 71 && code <= 77)       return "Borë";
        if (code >= 80 && code <= 82)       return "Reshje shiu";
        if (code >= 95)                     return "Stuhi me vetëtima";
        return "Mot i ndryshueshëm";
    }

    /** Simulated weather for Lushnja (typical Mediterranean summer). */
    public WeatherInfo getMockWeather() {
        double[] temps  = {22.0, 24.5, 19.0, 27.0, 21.5, 16.0, 30.0};
        int[]    codes  = {0, 1, 2, 0, 3, 61, 0};
        double[] winds  = {8.0, 12.0, 5.5, 15.0, 9.0, 20.0, 6.5};
        int idx = (int)(System.currentTimeMillis() / 3600000) % temps.length;
        String updated  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        return new WeatherInfo(temps[idx], codeToAlbanian(codes[idx]),
                               winds[idx], 0.0, codes[idx], updated + " (simuluar)");
    }
}
