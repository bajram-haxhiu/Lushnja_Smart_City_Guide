package com.lushnja.core.model;

import java.util.Objects;

/**
 * Represents a geographic location (node) in the city graph.
 * Each location has a unique ID, display name, and geographic coordinates.
 */
public class Location {

    private final String id;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final LocationType type;

    // Canvas rendering coordinates (set at runtime by the GUI layer)
    private double canvasX;
    private double canvasY;

    public enum LocationType {
        INTERSECTION,
        LANDMARK,
        HOSPITAL,
        SCHOOL,
        MARKET,
        PARK,
        GOVERNMENT,
        TRANSPORT_HUB,
        RESIDENTIAL
    }

    public Location(String id, String name, double latitude, double longitude, LocationType type) {
        if (id == null || id.isBlank())    throw new IllegalArgumentException("Location ID cannot be null or blank");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Location name cannot be null or blank");
        this.id        = id;
        this.name      = name;
        this.latitude  = latitude;
        this.longitude = longitude;
        this.type      = type;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getId()          { return id; }
    public String getName()        { return name; }
    public double getLatitude()    { return latitude; }
    public double getLongitude()   { return longitude; }
    public LocationType getType()  { return type; }
    public double getCanvasX()     { return canvasX; }
    public double getCanvasY()     { return canvasY; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setCanvasX(double canvasX) { this.canvasX = canvasX; }
    public void setCanvasY(double canvasY) { this.canvasY = canvasY; }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /**
     * Calculates the Haversine distance (in km) between this location and another.
     */
    public double distanceTo(Location other) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(this.latitude))
                 * Math.cos(Math.toRadians(other.latitude))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location)) return false;
        return Objects.equals(id, ((Location) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Location{id='%s', name='%s', type=%s, lat=%.4f, lon=%.4f}",
                id, name, type, latitude, longitude);
    }
}
