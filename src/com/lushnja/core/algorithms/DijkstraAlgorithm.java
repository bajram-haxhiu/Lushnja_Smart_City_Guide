package com.lushnja.core.algorithms;

import com.lushnja.core.graph.CityGraph;
import com.lushnja.core.model.Location;
import com.lushnja.core.model.Road;
import com.lushnja.core.model.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Dijkstra's shortest-path algorithm implementation.
 *
 * <p>Finds the minimum-weight path between two locations in a weighted directed graph.
 * The weight of each edge is the road's {@link Road#getEffectiveWeight()}, which
 * incorporates physical distance, road type, and current traffic conditions.
 *
 * <p>Time complexity: O((V + E) log V) using a priority queue.
 */
public class DijkstraAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(DijkstraAlgorithm.class);

    private final CityGraph graph;

    public DijkstraAlgorithm(CityGraph graph) {
        this.graph = Objects.requireNonNull(graph, "Graph cannot be null");
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Finds the shortest-distance route between source and destination.
     *
     * @param sourceId      ID of the starting location
     * @param destinationId ID of the target location
     * @return an Optional containing the Route, or empty if no path exists
     */
    public Optional<Route> findShortestPath(String sourceId, String destinationId) {
        logger.info("Dijkstra: {} → {}", sourceId, destinationId);

        validateInput(sourceId, destinationId);

        if (sourceId.equals(destinationId)) {
            return buildSingleNodeRoute(sourceId);
        }

        // dist[v] = best known distance from source to v
        Map<String, Double> dist      = new HashMap<>();
        // prev[v] = road used to reach v on the shortest path
        Map<String, Road>   prevRoad  = new HashMap<>();
        // Visited set
        Set<String>         visited   = new HashSet<>();

        // Priority queue ordered by distance (smallest first)
        PriorityQueue<NodeEntry> pq = new PriorityQueue<>(Comparator.comparingDouble(e -> e.distance));

        // Initialise all distances to infinity
        for (Location loc : graph.getAllLocations()) {
            dist.put(loc.getId(), Double.MAX_VALUE);
        }
        dist.put(sourceId, 0.0);
        pq.offer(new NodeEntry(sourceId, 0.0));

        // ── Main loop ─────────────────────────────────────────────────────────
        while (!pq.isEmpty()) {
            NodeEntry current = pq.poll();
            String currentId  = current.locationId;

            if (visited.contains(currentId)) continue;
            visited.add(currentId);

            // Early exit if we reached the destination
            if (currentId.equals(destinationId)) break;

            // Relax all outgoing edges
            for (Road road : graph.getAdjacentRoads(currentId)) {
                if (road.isBlocked()) continue;

                String nextId = road.getDestination().getId();
                if (visited.contains(nextId)) continue;

                double newDist = dist.get(currentId) + road.getEffectiveWeight();
                if (newDist < dist.get(nextId)) {
                    dist.put(nextId, newDist);
                    prevRoad.put(nextId, road);
                    pq.offer(new NodeEntry(nextId, newDist));
                }
            }
        }

        // Check if destination was reached
        if (dist.get(destinationId) == Double.MAX_VALUE) {
            logger.warn("No path found from {} to {}", sourceId, destinationId);
            return Optional.empty();
        }

        return Optional.of(reconstructRoute(sourceId, destinationId, dist, prevRoad,
                Route.RouteType.SHORTEST_DISTANCE));
    }

    /**
     * Finds the fastest-time route (uses time-based weight).
     * Internally re-runs Dijkstra with time as the edge weight.
     */
    public Optional<Route> findFastestPath(String sourceId, String destinationId) {
        logger.info("Dijkstra (time): {} → {}", sourceId, destinationId);
        validateInput(sourceId, destinationId);

        Map<String, Double> dist     = new HashMap<>();
        Map<String, Road>   prevRoad = new HashMap<>();
        Set<String>         visited  = new HashSet<>();
        PriorityQueue<NodeEntry> pq  = new PriorityQueue<>(Comparator.comparingDouble(e -> e.distance));

        for (Location loc : graph.getAllLocations()) dist.put(loc.getId(), Double.MAX_VALUE);
        dist.put(sourceId, 0.0);
        pq.offer(new NodeEntry(sourceId, 0.0));

        while (!pq.isEmpty()) {
            NodeEntry current = pq.poll();
            String currentId  = current.locationId;
            if (visited.contains(currentId)) continue;
            visited.add(currentId);
            if (currentId.equals(destinationId)) break;

            for (Road road : graph.getAdjacentRoads(currentId)) {
                if (road.isBlocked()) continue;
                String nextId = road.getDestination().getId();
                if (visited.contains(nextId)) continue;

                double newDist = dist.get(currentId) + road.getEstimatedTimeMinutes();
                if (newDist < dist.get(nextId)) {
                    dist.put(nextId, newDist);
                    prevRoad.put(nextId, road);
                    pq.offer(new NodeEntry(nextId, newDist));
                }
            }
        }

        if (dist.get(destinationId) == Double.MAX_VALUE) {
            logger.warn("No time-optimal path found from {} to {}", sourceId, destinationId);
            return Optional.empty();
        }
        return Optional.of(reconstructRoute(sourceId, destinationId, dist, prevRoad,
                Route.RouteType.FASTEST_TIME));
    }

    // ── Route Reconstruction ──────────────────────────────────────────────────

    private Route reconstructRoute(String sourceId, String destinationId,
                                   Map<String, Double> dist,
                                   Map<String, Road> prevRoad,
                                   Route.RouteType routeType) {
        List<Road>     roadPath     = new LinkedList<>();
        List<Location> locationPath = new LinkedList<>();

        String current = destinationId;
        while (prevRoad.containsKey(current)) {
            Road road = prevRoad.get(current);
            ((LinkedList<Road>) roadPath).addFirst(road);
            ((LinkedList<Location>) locationPath).addFirst(road.getDestination());
            current = road.getSource().getId();
        }
        // Add the source location
        graph.getLocation(sourceId).ifPresent(loc ->
                ((LinkedList<Location>) locationPath).addFirst(loc));

        double totalDist = roadPath.stream().mapToDouble(Road::getBaseDistance).sum();
        double totalTime = roadPath.stream().mapToDouble(Road::getEstimatedTimeMinutes).sum();

        return new Route(locationPath, roadPath, totalDist, totalTime,
                routeType, "Dijkstra");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateInput(String sourceId, String destinationId) {
        if (!graph.containsLocation(sourceId))
            throw new IllegalArgumentException("Source location not found: " + sourceId);
        if (!graph.containsLocation(destinationId))
            throw new IllegalArgumentException("Destination location not found: " + destinationId);
    }

    private Optional<Route> buildSingleNodeRoute(String locationId) {
        return graph.getLocation(locationId).map(loc ->
                new Route(List.of(loc), List.of(), 0.0, 0.0,
                        Route.RouteType.SHORTEST_DISTANCE, "Dijkstra"));
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /** Immutable priority queue entry. */
    private record NodeEntry(String locationId, double distance) {}
}
