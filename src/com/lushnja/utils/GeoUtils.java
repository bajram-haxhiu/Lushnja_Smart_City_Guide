package com.lushnja.utils;

/**
 * Geographic and mathematical utility functions.
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {}

    /**
     * Calculates the Haversine distance between two geographic points.
     *
     * @return distance in kilometres
     */
    public static double haversineDistance(double lat1, double lon1,
                                           double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Converts kilometres to metres.
     */
    public static double kmToMetres(double km) { return km * 1000.0; }

    /**
     * Converts speed (km/h) and distance (km) to travel time in minutes.
     */
    public static double travelTimeMinutes(double distanceKm, double speedKmh) {
        if (speedKmh <= 0) return Double.MAX_VALUE;
        return (distanceKm / speedKmh) * 60.0;
    }

    /**
     * Linearly interpolates a geographic coordinate to a canvas pixel.
     *
     * @param value    the geo value (lat or lon)
     * @param minVal   minimum geo value in the bounding box
     * @param maxVal   maximum geo value in the bounding box
     * @param canvasSize canvas dimension (width or height)
     * @param margin   padding in pixels
     * @param invert   true when mapping latitude (Y axis is inverted)
     */
    public static double geoToCanvas(double value, double minVal, double maxVal,
                                     double canvasSize, double margin, boolean invert) {
        double range  = maxVal - minVal;
        if (range == 0) return canvasSize / 2;
        double norm = (value - minVal) / range;
        if (invert) norm = 1.0 - norm;
        return margin + norm * (canvasSize - 2 * margin);
    }

    /**
     * Formats a distance value as a human-readable string.
     */
    public static String formatDistance(double km) {
        if (km < 1.0) return String.format("%.0f m", km * 1000);
        return String.format("%.2f km", km);
    }

    /**
     * Formats a time value (minutes) as a human-readable string.
     */
    public static String formatTime(double minutes) {
        if (minutes < 1.0) return "< 1 min";
        if (minutes < 60)  return String.format("%.0f min", minutes);
        int h = (int) (minutes / 60);
        int m = (int) (minutes % 60);
        return String.format("%d h %02d min", h, m);
    }
}
