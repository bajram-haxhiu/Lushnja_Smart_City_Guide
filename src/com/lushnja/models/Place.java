package com.lushnja.models;

import java.util.Objects;

/**
 * Represents a place of interest in Lushnja city.
 * Extended from Location with category, rating, description fields.
 */
public class Place {

    public enum PlaceCategory {
        VENDE_TURISTIKE("Vende Turistike"),
        RESTORANTE("Restorante"),
        KAFENE("Kafene"),
        HOTELE("Hotele"),
        SPITALE("Spitale"),
        FARMACI("Farmaci"),
        BANKA("Banka"),
        SHKOLLA("Shkolla"),
        PARKE("Parke"),
        STACIONE_AUTOBUSI("Stacione Autobusi"),
        PIKA_TAKSIE("Pika Taksie"),
        POLICIA("Policia"),
        PARKIME("Parkime"),
        TREGU("Tregu"),
        INSTITUCIONE("Institucione"),
        INDUSTRIAL("Industrial"),
        LAGJE("Lagje"),
        RRUGE("Rrugë");

        private final String albanianName;
        PlaceCategory(String albanianName) { this.albanianName = albanianName; }
        public String getAlbanianName() { return albanianName; }
    }

    private final String id;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final PlaceCategory category;
    private final String address;
    private final double rating;
    private final String openingHours;
    private final String description;
    private boolean isFavorite;

    // Canvas rendering coords (set at runtime)
    private double canvasX;
    private double canvasY;

    public Place(String id, String name, double latitude, double longitude,
                 PlaceCategory category, String address, double rating,
                 String openingHours, String description) {
        this.id           = Objects.requireNonNull(id);
        this.name         = Objects.requireNonNull(name);
        this.latitude     = latitude;
        this.longitude    = longitude;
        this.category     = category != null ? category : PlaceCategory.VENDE_TURISTIKE;
        this.address      = address != null ? address : "";
        this.rating       = rating;
        this.openingHours = openingHours != null ? openingHours : "";
        this.description  = description != null ? description : "";
        this.isFavorite   = false;
    }

    // ── Haversine distance ────────────────────────────────────────────────────
    public double distanceTo(Place other) {
        final double R = 6371.0;
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(this.latitude))
                 * Math.cos(Math.toRadians(other.latitude))
                 * Math.sin(dLon/2) * Math.sin(dLon/2);
        return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    public double distanceTo(double lat, double lon) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat - this.latitude);
        double dLon = Math.toRadians(lon - this.longitude);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(this.latitude))
                 * Math.cos(Math.toRadians(lat))
                 * Math.sin(dLon/2) * Math.sin(dLon/2);
        return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    // ── Getters / setters ─────────────────────────────────────────────────────
    public String getId()           { return id; }
    public String getName()         { return name; }
    public double getLatitude()     { return latitude; }
    public double getLongitude()    { return longitude; }
    public PlaceCategory getCategory()  { return category; }
    public String getAddress()      { return address; }
    public double getRating()       { return rating; }
    public String getOpeningHours() { return openingHours; }
    public String getDescription()  { return description; }
    public boolean isFavorite()     { return isFavorite; }
    public void setFavorite(boolean f) { this.isFavorite = f; }
    public double getCanvasX()      { return canvasX; }
    public double getCanvasY()      { return canvasY; }
    public void setCanvasX(double x){ this.canvasX = x; }
    public void setCanvasY(double y){ this.canvasY = y; }

    /** Checks if this place matches a search term (name or category). */
    public boolean matchesSearch(String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.toLowerCase().trim();
        return name.toLowerCase().contains(q)
            || category.getAlbanianName().toLowerCase().contains(q)
            || address.toLowerCase().contains(q)
            || description.toLowerCase().contains(q);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Place)) return false;
        return Objects.equals(id, ((Place)o).id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() {
        return String.format("Place{id='%s', name='%s', category=%s}", id, name, category);
    }
}
