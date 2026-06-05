package com.lushnja.models;

/**
 * Represents a parking spot in Lushnja.
 */
public class ParkingSpot {

    public enum Status {
        I_LIRE("I lirë ✓"),
        POTHUAJSE_PLOT("Pothuajse plot ⚠"),
        PLOT("Plot ✗");

        private final String label;
        Status(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final String id;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final String address;
    private final int totalSpaces;
    private int freeSpaces;
    private final String price;

    public ParkingSpot(String id, String name, double latitude, double longitude,
                       String address, int totalSpaces, int freeSpaces, String price) {
        this.id          = id;
        this.name        = name;
        this.latitude    = latitude;
        this.longitude   = longitude;
        this.address     = address;
        this.totalSpaces = totalSpaces;
        this.freeSpaces  = Math.max(0, Math.min(freeSpaces, totalSpaces));
        this.price       = price;
    }

    public Status getStatus() {
        double ratio = (double) freeSpaces / totalSpaces;
        if (freeSpaces == 0) return Status.PLOT;
        if (ratio < 0.25)    return Status.POTHUAJSE_PLOT;
        return Status.I_LIRE;
    }

    /** Simulate real-time update (random ±3 spaces). */
    public void simulateUpdate() {
        int change = (int)(Math.random() * 7) - 3;
        freeSpaces = Math.max(0, Math.min(totalSpaces, freeSpaces + change));
    }

    public double distanceTo(double lat, double lon) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat - this.latitude);
        double dLon = Math.toRadians(lon - this.longitude);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(this.latitude))
                 * Math.cos(Math.toRadians(lat))
                 * Math.sin(dLon/2)*Math.sin(dLon/2);
        return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public String getId()        { return id; }
    public String getName()      { return name; }
    public double getLatitude()  { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAddress()   { return address; }
    public int getTotalSpaces()  { return totalSpaces; }
    public int getFreeSpaces()   { return freeSpaces; }
    public void setFreeSpaces(int n){ freeSpaces = Math.max(0, Math.min(totalSpaces, n)); }
    public String getPrice()     { return price; }

    @Override public String toString() {
        return name + " [" + freeSpaces + "/" + totalSpaces + " vendet e lira]";
    }
}
