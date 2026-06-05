package com.lushnja.models;

/**
 * Holds current weather information for Lushnja.
 * Data can come from Open-Meteo API or simulated fallback.
 */
public class WeatherInfo {

    private final double temperature;
    private final String condition;
    private final double windSpeed;
    private final double precipitationMm;
    private final int    weatherCode;
    private final String lastUpdated;

    public WeatherInfo(double temperature, String condition, double windSpeed,
                       double precipitationMm, int weatherCode, String lastUpdated) {
        this.temperature     = temperature;
        this.condition       = condition;
        this.windSpeed       = windSpeed;
        this.precipitationMm = precipitationMm;
        this.weatherCode     = weatherCode;
        this.lastUpdated     = lastUpdated;
    }

    public String getEmoji() {
        // Based on WMO weather interpretation codes
        if (weatherCode == 0)               return "☀️";
        if (weatherCode <= 2)               return "⛅";
        if (weatherCode == 3)               return "☁️";
        if (weatherCode >= 45 && weatherCode <= 48) return "🌫️";
        if (weatherCode >= 51 && weatherCode <= 67) return "🌧️";
        if (weatherCode >= 71 && weatherCode <= 77) return "🌨️";
        if (weatherCode >= 80 && weatherCode <= 82) return "🌦️";
        if (weatherCode >= 95)               return "⛈️";
        return "🌡️";
    }

    public double getTemperature()      { return temperature; }
    public String getCondition()        { return condition; }
    public double getWindSpeed()        { return windSpeed; }
    public double getPrecipitationMm()  { return precipitationMm; }
    public int    getWeatherCode()      { return weatherCode; }
    public String getLastUpdated()      { return lastUpdated; }

    @Override public String toString() {
        return String.format("WeatherInfo{temp=%.1f°C, %s, wind=%.1f km/h}", 
                             temperature, condition, windSpeed);
    }
}
