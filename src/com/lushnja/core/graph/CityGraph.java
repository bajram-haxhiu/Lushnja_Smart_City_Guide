package com.lushnja.core.graph;

import com.lushnja.core.model.Location;
import com.lushnja.core.model.Road;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Adjacency-list graph representing the city road network.
 *
 * <p>Nodes  = {@link Location} objects (intersections, landmarks, etc.)
 * <p>Edges  = {@link Road} objects (road segments with weight + traffic)
 *
 * <p>Supports both directed and undirected (bidirectional) roads.
 * Thread-safety is NOT guaranteed; synchronize externally if needed.
 */
public class CityGraph {

    private static final Logger logger = LoggerFactory.getLogger(CityGraph.class);

    /** Node store: locationId → Location */
    private final Map<String, Location> locations = new LinkedHashMap<>();

    /** Adjacency list: locationId → list of outgoing Roads */
    private final Map<String, List<Road>> adjacencyList = new HashMap<>();

    /** Edge store: roadId → Road */
    private final Map<String, Road> roads = new HashMap<>();

    // ── Node Operations ───────────────────────────────────────────────────────

    /**
     * Adds a location (node) to the graph.
     *
     * @throws IllegalArgumentException if a location with the same ID already exists
     */
    public void addLocation(Location location) {
        Objects.requireNonNull(location, "Location cannot be null");
        if (locations.containsKey(location.getId())) {
            throw new IllegalArgumentException("Location already exists: " + location.getId());
        }
        locations.put(location.getId(), location);
        adjacencyList.put(location.getId(), new ArrayList<>());
        logger.debug("Added location: {}", location.getName());
    }

    /**
     * Removes a location and all roads connected to it.
     */
    public void removeLocation(String locationId) {
        if (!locations.containsKey(locationId)) return;
        locations.remove(locationId);
        adjacencyList.remove(locationId);
        // Remove all roads that touch this location
        roads.values().removeIf(r ->
                r.getSource().getId().equals(locationId) ||
                r.getDestination().getId().equals(locationId));
        adjacencyList.values().forEach(list ->
                list.removeIf(r ->
                        r.getSource().getId().equals(locationId) ||
                        r.getDestination().getId().equals(locationId)));
        logger.debug("Removed location: {}", locationId);
    }

    // ── Edge Operations ───────────────────────────────────────────────────────

    /**
     * Adds a directed road from source to destination.
     */
    public void addRoad(Road road) {
        Objects.requireNonNull(road, "Road cannot be null");
        validateLocations(road.getSource().getId(), road.getDestination().getId());
        roads.put(road.getId(), road);
        adjacencyList.get(road.getSource().getId()).add(road);
        logger.debug("Added road: {}", road);
    }

    /**
     * Adds a bidirectional road (creates two directed edges).
     * The reverse road gets ID = originalId + "_rev".
     */
    public void addBidirectionalRoad(Road road) {
        addRoad(road);
        Road reverse = new Road(
                road.getId() + "_rev",
                road.getDestination(),
                road.getSource(),
                road.getBaseDistance(),
                road.getRoadName(),
                road.getRoadType(),
                road.getSpeedLimitKmh()
        );
        reverse.setTrafficLevel(road.getTrafficLevel());
        addRoad(reverse);
    }

    /**
     * Removes a road by ID.
     */
    public void removeRoad(String roadId) {
        Road road = roads.remove(roadId);
        if (road != null) {
            adjacencyList.getOrDefault(road.getSource().getId(), List.of()).remove(road);
            logger.debug("Removed road: {}", roadId);
        }
    }

    // ── Query Operations ──────────────────────────────────────────────────────

    public Optional<Location> getLocation(String id) {
        return Optional.ofNullable(locations.get(id));
    }

    public Optional<Road> getRoad(String id) {
        return Optional.ofNullable(roads.get(id));
    }

    /** Returns all outgoing roads from a given location. */
    public List<Road> getAdjacentRoads(String locationId) {
        return Collections.unmodifiableList(
                adjacencyList.getOrDefault(locationId, Collections.emptyList()));
    }

    /** Returns all neighbour locations reachable from the given location. */
    public List<Location> getNeighbours(String locationId) {
        return getAdjacentRoads(locationId).stream()
                .filter(r -> !r.isBlocked())
                .map(Road::getDestination)
                .toList();
    }

    public Collection<Location> getAllLocations() {
        return Collections.unmodifiableCollection(locations.values());
    }

    public Collection<Road> getAllRoads() {
        return Collections.unmodifiableCollection(roads.values());
    }

    public boolean containsLocation(String id) { return locations.containsKey(id); }
    public boolean containsRoad(String id)     { return roads.containsKey(id); }

    public int locationCount() { return locations.size(); }
    public int roadCount()     { return roads.size(); }

    // ── Traffic & Blocking ────────────────────────────────────────────────────

    /**
     * Blocks / unblocks a road by ID (both directions if bidirectional).
     */
    public void setRoadBlocked(String roadId, boolean blocked) {
        getRoad(roadId).ifPresent(r -> {
            r.setBlocked(blocked);
            logger.info("Road '{}' blocked={}", roadId, blocked);
        });
        // Also update the reverse direction if it exists
        getRoad(roadId + "_rev").ifPresent(r -> r.setBlocked(blocked));
    }

    /**
     * Updates the traffic level for a road (and its reverse if bidirectional).
     */
    public void setTrafficLevel(String roadId, Road.TrafficLevel level) {
        getRoad(roadId).ifPresent(r -> {
            r.setTrafficLevel(level);
            logger.info("Road '{}' traffic set to {}", roadId, level);
        });
        getRoad(roadId + "_rev").ifPresent(r -> r.setTrafficLevel(level));
    }

    // ── Graph Connectivity ────────────────────────────────────────────────────

    /**
     * Returns true if the graph is (weakly) connected —
     * every node can be reached from the first node (ignoring direction).
     */
    public boolean isConnected() {
        if (locations.isEmpty()) return true;
        Set<String> visited = new HashSet<>();
        String start = locations.keySet().iterator().next();
        bfsConnectivity(start, visited);
        return visited.size() == locations.size();
    }

    private void bfsConnectivity(String start, Set<String> visited) {
        Queue<String> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            adjacencyList.getOrDefault(current, List.of()).forEach(road -> {
                String next = road.getDestination().getId();
                if (!visited.contains(next)) {
                    visited.add(next);
                    queue.add(next);
                }
            });
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateLocations(String sourceId, String destId) {
        if (!locations.containsKey(sourceId))
            throw new IllegalArgumentException("Source location not found: " + sourceId);
        if (!locations.containsKey(destId))
            throw new IllegalArgumentException("Destination location not found: " + destId);
    }

    @Override
    public String toString() {
        return String.format("CityGraph{locations=%d, roads=%d, connected=%b}",
                locationCount(), roadCount(), isConnected());
    }
}
