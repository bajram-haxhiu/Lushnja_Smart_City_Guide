package com.lushnja.core.model;

import java.util.Objects;

/**
 * Represents a road segment (edge) between two locations in the city graph.
 * Roads are directional by default; bidirectional roads are stored as two edges.
 *
 * Weight for pathfinding = baseDistance * trafficMultiplier
 */
public class Road {

    private final String id;
    private final Location source;
    private final Location destination;

    /** Physical distance in kilometres */
    private final double baseDistance;

    /** Road name (e.g. "Bulevardi Skënderbeu") */
    private final String roadName;

    /** Road surface / classification */
    private final RoadType roadType;

    /** Current traffic state; affects effective travel weight */
    private TrafficLevel trafficLevel;

    /** True if the road is closed (blocked) */
    private boolean blocked;

    /** Speed limit in km/h */
    private final double speedLimitKmh;

    // ── Enumerations ──────────────────────────────────────────────────────────

    public enum RoadType {
        HIGHWAY(1.0),
        MAIN_ROAD(1.1),
        SECONDARY_ROAD(1.2),
        RESIDENTIAL_STREET(1.4),
        ALLEY(1.8);

        private final double baseMultiplier;
        RoadType(double baseMultiplier) { this.baseMultiplier = baseMultiplier; }
        public double getBaseMultiplier() { return baseMultiplier; }
    }

    public enum TrafficLevel {
        FREE_FLOW(1.0),
        LIGHT(1.3),
        MODERATE(1.7),
        HEAVY(2.5),
        GRIDLOCK(4.0);

        private final double multiplier;
        TrafficLevel(double multiplier) { this.multiplier = multiplier; }
        public double getMultiplier() { return multiplier; }
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    public Road(String id, Location source, Location destination,
                double baseDistance, String roadName,
                RoadType roadType, double speedLimitKmh) {
        this.id           = Objects.requireNonNull(id, "Road ID cannot be null");
        this.source       = Objects.requireNonNull(source, "Source cannot be null");
        this.destination  = Objects.requireNonNull(destination, "Destination cannot be null");
        this.baseDistance = baseDistance;
        this.roadName     = roadName;
        this.roadType     = roadType;
        this.speedLimitKmh = speedLimitKmh;
        this.trafficLevel = TrafficLevel.FREE_FLOW;
        this.blocked      = false;
    }

    // ── Weight Calculation ────────────────────────────────────────────────────

    /**
     * Returns the effective weight used by Dijkstra.
     * Weight is infinity if the road is blocked.
     */
    public double getEffectiveWeight() {
        if (blocked) return Double.MAX_VALUE;
        return baseDistance
             * roadType.getBaseMultiplier()
             * trafficLevel.getMultiplier();
    }

    /**
     * Estimated travel time in minutes.
     */
    public double getEstimatedTimeMinutes() {
        if (blocked) return Double.MAX_VALUE;
        double effectiveSpeed = speedLimitKmh / trafficLevel.getMultiplier();
        return (baseDistance / effectiveSpeed) * 60.0;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()              { return id; }
    public Location getSource()        { return source; }
    public Location getDestination()   { return destination; }
    public double getBaseDistance()    { return baseDistance; }
    public String getRoadName()        { return roadName; }
    public RoadType getRoadType()      { return roadType; }
    public TrafficLevel getTrafficLevel() { return trafficLevel; }
    public boolean isBlocked()         { return blocked; }
    public double getSpeedLimitKmh()   { return speedLimitKmh; }

    public void setTrafficLevel(TrafficLevel trafficLevel) {
        this.trafficLevel = Objects.requireNonNull(trafficLevel, "TrafficLevel cannot be null");
    }

    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Road)) return false;
        return Objects.equals(id, ((Road) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Road{id='%s', '%s' -> '%s', dist=%.2f km, traffic=%s, blocked=%s}",
                id, source.getName(), destination.getName(),
                baseDistance, trafficLevel, blocked);
    }
}
