package com.lushnja.core.algorithms;

import com.lushnja.core.graph.CityGraph;
import com.lushnja.core.model.Location;
import com.lushnja.core.model.Road;
import com.lushnja.core.model.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Breadth-First Search (BFS) implementation for the city graph.
 *
 * <p>BFS explores nodes level by level (by hop count), guaranteeing the
 * fewest-hops path between two locations — but NOT the shortest distance.
 * It is useful for:
 * <ul>
 *   <li>Finding paths when all edge weights are equal</li>
 *   <li>Graph connectivity checks</li>
 *   <li>Level-order traversal / neighbour discovery</li>
 * </ul>
 *
 * <p>Time complexity: O(V + E)
 */
public class BFSAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(BFSAlgorithm.class);

    private final CityGraph graph;

    public BFSAlgorithm(CityGraph graph) {
        this.graph = Objects.requireNonNull(graph, "Graph cannot be null");
    }

    // ── Traversal ─────────────────────────────────────────────────────────────

    /**
     * Performs a BFS traversal starting from the given location.
     *
     * @param startId ID of the starting location
     * @return ordered list of visited locations (BFS order)
     */
    public List<Location> traverse(String startId) {
        validateLocation(startId);
        List<Location> result  = new ArrayList<>();
        Set<String>    visited = new LinkedHashSet<>();
        Queue<String>  queue   = new LinkedList<>();

        queue.add(startId);
        visited.add(startId);

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            graph.getLocation(currentId).ifPresent(result::add);

            for (Road road : graph.getAdjacentRoads(currentId)) {
                if (road.isBlocked()) continue;
                String nextId = road.getDestination().getId();
                if (!visited.contains(nextId)) {
                    visited.add(nextId);
                    queue.add(nextId);
                }
            }
        }
        logger.debug("BFS traversal from '{}': visited {} nodes", startId, result.size());
        return result;
    }

    // ── Pathfinding ───────────────────────────────────────────────────────────

    /**
     * Finds the fewest-hops path from source to destination using BFS.
     *
     * @return Optional Route (fewest stops), or empty if no path exists
     */
    public Optional<Route> findPath(String sourceId, String destinationId) {
        logger.info("BFS: {} → {}", sourceId, destinationId);
        validateLocation(sourceId);
        validateLocation(destinationId);

        if (sourceId.equals(destinationId)) {
            return graph.getLocation(sourceId).map(loc ->
                    new Route(List.of(loc), List.of(), 0.0, 0.0,
                            Route.RouteType.ALTERNATIVE, "BFS"));
        }

        Map<String, Road> prevRoad = new HashMap<>();
        Set<String>       visited  = new LinkedHashSet<>();
        Queue<String>     queue    = new LinkedList<>();

        queue.add(sourceId);
        visited.add(sourceId);

        boolean found = false;
        outer:
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            for (Road road : graph.getAdjacentRoads(currentId)) {
                if (road.isBlocked()) continue;
                String nextId = road.getDestination().getId();
                if (!visited.contains(nextId)) {
                    visited.add(nextId);
                    prevRoad.put(nextId, road);
                    queue.add(nextId);
                    if (nextId.equals(destinationId)) {
                        found = true;
                        break outer;
                    }
                }
            }
        }

        if (!found) {
            logger.warn("BFS: no path from {} to {}", sourceId, destinationId);
            return Optional.empty();
        }

        // Reconstruct path
        List<Road>     roadPath     = new LinkedList<>();
        List<Location> locationPath = new LinkedList<>();
        String current = destinationId;
        while (prevRoad.containsKey(current)) {
            Road road = prevRoad.get(current);
            ((LinkedList<Road>) roadPath).addFirst(road);
            ((LinkedList<Location>) locationPath).addFirst(road.getDestination());
            current = road.getSource().getId();
        }
        graph.getLocation(sourceId).ifPresent(loc ->
                ((LinkedList<Location>) locationPath).addFirst(loc));

        double totalDist = roadPath.stream().mapToDouble(Road::getBaseDistance).sum();
        double totalTime = roadPath.stream().mapToDouble(Road::getEstimatedTimeMinutes).sum();

        return Optional.of(new Route(locationPath, roadPath, totalDist, totalTime,
                Route.RouteType.ALTERNATIVE, "BFS"));
    }

    // ── Connectivity ──────────────────────────────────────────────────────────

    /**
     * Returns all locations reachable from the given start location.
     */
    public Set<Location> findReachableLocations(String startId) {
        validateLocation(startId);
        Set<Location> reachable = new LinkedHashSet<>();
        Set<String>   visited   = new HashSet<>();
        Queue<String> queue     = new LinkedList<>();

        queue.add(startId);
        visited.add(startId);

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            graph.getLocation(currentId).ifPresent(reachable::add);
            for (Road road : graph.getAdjacentRoads(currentId)) {
                if (road.isBlocked()) continue;
                String nextId = road.getDestination().getId();
                if (!visited.contains(nextId)) {
                    visited.add(nextId);
                    queue.add(nextId);
                }
            }
        }
        return reachable;
    }

    /**
     * Returns the BFS-level (hop distance) of each location from the start.
     */
    public Map<String, Integer> computeLevels(String startId) {
        validateLocation(startId);
        Map<String, Integer> levels = new HashMap<>();
        Queue<String>        queue  = new LinkedList<>();

        queue.add(startId);
        levels.put(startId, 0);

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            int    nextLevel  = levels.get(currentId) + 1;
            for (Road road : graph.getAdjacentRoads(currentId)) {
                if (road.isBlocked()) continue;
                String nextId = road.getDestination().getId();
                if (!levels.containsKey(nextId)) {
                    levels.put(nextId, nextLevel);
                    queue.add(nextId);
                }
            }
        }
        return levels;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateLocation(String id) {
        if (!graph.containsLocation(id))
            throw new IllegalArgumentException("Location not found: " + id);
    }
}
