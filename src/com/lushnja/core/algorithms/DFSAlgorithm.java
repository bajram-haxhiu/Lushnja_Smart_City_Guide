package com.lushnja.core.algorithms;

import com.lushnja.core.graph.CityGraph;
import com.lushnja.core.model.Location;
import com.lushnja.core.model.Road;
import com.lushnja.core.model.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Depth-First Search (DFS) implementation for the city graph.
 *
 * <p>DFS explores as deep as possible along each branch before backtracking.
 * Useful for:
 * <ul>
 *   <li>Discovering all possible paths (with backtracking)</li>
 *   <li>Cycle detection</li>
 *   <li>Topological ordering</li>
 *   <li>Finding alternative/scenic routes</li>
 * </ul>
 *
 * <p>Time complexity: O(V + E)
 */
public class DFSAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(DFSAlgorithm.class);

    private final CityGraph graph;

    /** Maximum number of alternative paths to discover (prevents combinatorial explosion). */
    private static final int MAX_PATHS = 5;

    public DFSAlgorithm(CityGraph graph) {
        this.graph = Objects.requireNonNull(graph, "Graph cannot be null");
    }

    // ── Traversal ─────────────────────────────────────────────────────────────

    /**
     * Performs a full DFS traversal from the given start location.
     *
     * @return locations in DFS visit order
     */
    public List<Location> traverse(String startId) {
        validateLocation(startId);
        List<Location> result  = new ArrayList<>();
        Set<String>    visited = new LinkedHashSet<>();
        dfsVisit(startId, visited, result);
        logger.debug("DFS traversal from '{}': visited {} nodes", startId, result.size());
        return result;
    }

    private void dfsVisit(String currentId, Set<String> visited, List<Location> result) {
        visited.add(currentId);
        graph.getLocation(currentId).ifPresent(result::add);
        for (Road road : graph.getAdjacentRoads(currentId)) {
            if (road.isBlocked()) continue;
            String nextId = road.getDestination().getId();
            if (!visited.contains(nextId)) {
                dfsVisit(nextId, visited, result);
            }
        }
    }

    // ── Pathfinding ───────────────────────────────────────────────────────────

    /**
     * Finds ONE path from source to destination using DFS (first path found).
     * The path found by DFS is not guaranteed to be optimal.
     */
    public Optional<Route> findPath(String sourceId, String destinationId) {
        logger.info("DFS findPath: {} → {}", sourceId, destinationId);
        validateLocation(sourceId);
        validateLocation(destinationId);

        List<Road>     currentRoadPath  = new ArrayList<>();
        List<Location> currentLocPath   = new ArrayList<>();
        Set<String>    visited          = new LinkedHashSet<>();

        graph.getLocation(sourceId).ifPresent(currentLocPath::add);
        visited.add(sourceId);

        boolean found = dfsPath(sourceId, destinationId, visited, currentRoadPath, currentLocPath);

        if (!found) {
            logger.warn("DFS: no path from {} to {}", sourceId, destinationId);
            return Optional.empty();
        }

        double totalDist = currentRoadPath.stream().mapToDouble(Road::getBaseDistance).sum();
        double totalTime = currentRoadPath.stream().mapToDouble(Road::getEstimatedTimeMinutes).sum();

        return Optional.of(new Route(
                new ArrayList<>(currentLocPath),
                new ArrayList<>(currentRoadPath),
                totalDist, totalTime,
                Route.RouteType.ALTERNATIVE, "DFS"));
    }

    private boolean dfsPath(String currentId, String destinationId,
                             Set<String> visited,
                             List<Road> roadPath, List<Location> locPath) {
        if (currentId.equals(destinationId)) return true;

        for (Road road : graph.getAdjacentRoads(currentId)) {
            if (road.isBlocked()) continue;
            String nextId = road.getDestination().getId();
            if (!visited.contains(nextId)) {
                visited.add(nextId);
                roadPath.add(road);
                graph.getLocation(nextId).ifPresent(locPath::add);

                if (dfsPath(nextId, destinationId, visited, roadPath, locPath)) return true;

                // Backtrack
                roadPath.remove(roadPath.size() - 1);
                locPath.remove(locPath.size() - 1);
                visited.remove(nextId);
            }
        }
        return false;
    }

    // ── All Paths Discovery ───────────────────────────────────────────────────

    /**
     * Discovers up to {@value MAX_PATHS} distinct paths using DFS with backtracking.
     * Useful for presenting alternative route options.
     */
    public List<Route> findAllPaths(String sourceId, String destinationId) {
        logger.info("DFS findAllPaths: {} → {}", sourceId, destinationId);
        validateLocation(sourceId);
        validateLocation(destinationId);

        List<Route>    allRoutes       = new ArrayList<>();
        List<Road>     currentRoadPath = new ArrayList<>();
        List<Location> currentLocPath  = new ArrayList<>();
        Set<String>    visited         = new LinkedHashSet<>();

        graph.getLocation(sourceId).ifPresent(currentLocPath::add);
        visited.add(sourceId);

        dfsAllPaths(sourceId, destinationId, visited,
                currentRoadPath, currentLocPath, allRoutes);

        logger.info("DFS found {} path(s) from {} to {}", allRoutes.size(), sourceId, destinationId);
        return allRoutes;
    }

    private void dfsAllPaths(String currentId, String destinationId,
                              Set<String> visited,
                              List<Road> roadPath, List<Location> locPath,
                              List<Route> allRoutes) {
        if (allRoutes.size() >= MAX_PATHS) return;

        if (currentId.equals(destinationId)) {
            double totalDist = roadPath.stream().mapToDouble(Road::getBaseDistance).sum();
            double totalTime = roadPath.stream().mapToDouble(Road::getEstimatedTimeMinutes).sum();
            allRoutes.add(new Route(
                    new ArrayList<>(locPath),
                    new ArrayList<>(roadPath),
                    totalDist, totalTime,
                    Route.RouteType.ALTERNATIVE, "DFS"));
            return;
        }

        for (Road road : graph.getAdjacentRoads(currentId)) {
            if (road.isBlocked()) continue;
            String nextId = road.getDestination().getId();
            if (!visited.contains(nextId)) {
                visited.add(nextId);
                roadPath.add(road);
                graph.getLocation(nextId).ifPresent(locPath::add);

                dfsAllPaths(nextId, destinationId, visited, roadPath, locPath, allRoutes);

                // Backtrack
                roadPath.remove(roadPath.size() - 1);
                locPath.remove(locPath.size() - 1);
                visited.remove(nextId);
            }
        }
    }

    // ── Cycle Detection ───────────────────────────────────────────────────────

    /**
     * Returns true if the graph (starting from startId) contains a directed cycle.
     */
    public boolean hasCycle(String startId) {
        validateLocation(startId);
        Set<String> visited    = new HashSet<>();
        Set<String> recStack   = new HashSet<>();
        return dfsCycleDetect(startId, visited, recStack);
    }

    private boolean dfsCycleDetect(String id, Set<String> visited, Set<String> recStack) {
        visited.add(id);
        recStack.add(id);
        for (Road road : graph.getAdjacentRoads(id)) {
            if (road.isBlocked()) continue;
            String nextId = road.getDestination().getId();
            if (!visited.contains(nextId) && dfsCycleDetect(nextId, visited, recStack)) return true;
            if (recStack.contains(nextId)) return true;
        }
        recStack.remove(id);
        return false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateLocation(String id) {
        if (!graph.containsLocation(id))
            throw new IllegalArgumentException("Location not found: " + id);
    }
}
