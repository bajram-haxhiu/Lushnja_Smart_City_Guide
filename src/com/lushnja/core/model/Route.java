package com.lushnja.core.model;

import java.util.Collections;
import java.util.List;

/**
 * Represents a computed route between two locations.
 * Contains the ordered list of locations, the roads traversed,
 * total distance, and estimated travel time.
 */
public class Route {

    public enum RouteType {
        SHORTEST_DISTANCE,
        FASTEST_TIME,
        ALTERNATIVE
    }

    private final List<Location> locationPath;
    private final List<Road>     roadPath;
    private final double         totalDistanceKm;
    private final double         totalTimeMinutes;
    private final RouteType      routeType;
    private final String         algorithmUsed;

    public Route(List<Location> locationPath, List<Road> roadPath,
                 double totalDistanceKm, double totalTimeMinutes,
                 RouteType routeType, String algorithmUsed) {
        this.locationPath    = Collections.unmodifiableList(locationPath);
        this.roadPath        = Collections.unmodifiableList(roadPath);
        this.totalDistanceKm = totalDistanceKm;
        this.totalTimeMinutes = totalTimeMinutes;
        this.routeType       = routeType;
        this.algorithmUsed   = algorithmUsed;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public List<Location> getLocationPath()    { return locationPath; }
    public List<Road>     getRoadPath()        { return roadPath; }
    public double         getTotalDistanceKm() { return totalDistanceKm; }
    public double         getTotalTimeMinutes(){ return totalTimeMinutes; }
    public RouteType      getRouteType()       { return routeType; }
    public String         getAlgorithmUsed()   { return algorithmUsed; }

    public boolean isEmpty() { return locationPath.isEmpty(); }

    /** Returns human-readable turn-by-turn directions. */
    public String getDirections() {
        if (isEmpty()) return "No route found.";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Route: %s → %s%n",
                locationPath.get(0).getName(),
                locationPath.get(locationPath.size() - 1).getName()));
        sb.append(String.format("Total Distance: %.2f km%n", totalDistanceKm));
        sb.append(String.format("Estimated Time: %.0f min%n", totalTimeMinutes));
        sb.append(String.format("Algorithm: %s%n", algorithmUsed));
        sb.append("─────────────────────────%n".formatted());
        for (int i = 0; i < roadPath.size(); i++) {
            Road road = roadPath.get(i);
            sb.append(String.format("  %d. Take %s → %s (%.2f km)%n",
                    i + 1,
                    road.getRoadName(),
                    road.getDestination().getName(),
                    road.getBaseDistance()));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("Route{type=%s, distance=%.2f km, time=%.0f min, stops=%d}",
                routeType, totalDistanceKm, totalTimeMinutes, locationPath.size());
    }
}
