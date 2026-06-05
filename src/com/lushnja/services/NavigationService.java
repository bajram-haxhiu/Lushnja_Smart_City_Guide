package com.lushnja.services;

import com.lushnja.core.algorithms.BFSAlgorithm;
import com.lushnja.core.algorithms.DFSAlgorithm;
import com.lushnja.core.algorithms.DijkstraAlgorithm;
import com.lushnja.core.graph.CityGraph;
import com.lushnja.core.model.Location;
import com.lushnja.core.model.Road;
import com.lushnja.core.model.Route;
import com.lushnja.models.Place;
import com.lushnja.models.Place.PlaceCategory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central navigation service orchestrating Dijkstra, BFS, DFS,
 * and emergency/nearest-place logic.
 */
public class NavigationService {

    private final CityGraph          graph;
    private final DijkstraAlgorithm  dijkstra;
    private final BFSAlgorithm       bfs;
    private final DFSAlgorithm       dfs;
    private final PlaceService       placeService;

    public NavigationService(CityGraph graph, PlaceService placeService) {
        this.graph        = Objects.requireNonNull(graph);
        this.placeService = placeService;
        this.dijkstra     = new DijkstraAlgorithm(graph);
        this.bfs          = new BFSAlgorithm(graph);
        this.dfs          = new DFSAlgorithm(graph);
    }

    // ── Route Finding ─────────────────────────────────────────────────────────

    public Optional<Route> findShortestRoute(String srcId, String dstId) {
        return dijkstra.findShortestPath(srcId, dstId);
    }

    public Optional<Route> findFastestRoute(String srcId, String dstId) {
        return dijkstra.findFastestPath(srcId, dstId);
    }

    public Optional<Route> findBFSRoute(String srcId, String dstId) {
        return bfs.findPath(srcId, dstId);
    }

    public Optional<Route> findDFSRoute(String srcId, String dstId) {
        return dfs.findPath(srcId, dstId);
    }

    // ── DSA Traversal ─────────────────────────────────────────────────────────

    public List<Location> bfsTraversal(String startId) { return bfs.traverse(startId); }
    public List<Location> dfsTraversal(String startId) { return dfs.traverse(startId); }

    // ── Emergency Nearest ─────────────────────────────────────────────────────

    /**
     * Find nearest hospital, pharmacy, police, and taxi from a given graph location.
     * Returns a map of category name → Optional<Place>.
     */
    public Map<String, Optional<Place>> findEmergencyPlaces(String locationId) {
        Optional<Location> loc = graph.getLocation(locationId);
        double lat = loc.map(Location::getLatitude).orElse(40.9426);
        double lon = loc.map(Location::getLongitude).orElse(19.7062);

        Map<String, Optional<Place>> result = new LinkedHashMap<>();
        result.put("Spitali",   placeService.findNearest(lat, lon, PlaceCategory.SPITALE));
        result.put("Farmacia",  placeService.findNearest(lat, lon, PlaceCategory.FARMACI));
        result.put("Policia",   placeService.findNearest(lat, lon, PlaceCategory.POLICIA));
        result.put("Taksi",     placeService.findNearest(lat, lon, PlaceCategory.PIKA_TAKSIE));
        return result;
    }

    // ── Traffic ───────────────────────────────────────────────────────────────

    public void simulateRandomTraffic() {
        Random rnd = new Random();
        Road.TrafficLevel[] levels = Road.TrafficLevel.values();
        for (Road r : graph.getAllRoads()) {
            if (!r.getId().endsWith("_rev"))
                graph.setTrafficLevel(r.getId(), levels[rnd.nextInt(levels.length)]);
        }
    }

    public void clearAllTraffic() {
        graph.getAllRoads().forEach(r -> r.setTrafficLevel(Road.TrafficLevel.FREE_FLOW));
    }

    public void setTrafficLevel(String roadId, Road.TrafficLevel level) {
        graph.setTrafficLevel(roadId, level);
    }

    public void setRoadBlocked(String roadId, boolean blocked) {
        graph.setRoadBlocked(roadId, blocked);
    }

    public void clearAllBlocks() {
        graph.getAllRoads().forEach(r -> r.setBlocked(false));
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Collection<Location> getAllLocations()  { return graph.getAllLocations(); }
    public Collection<Road>     getAllRoads()      { return graph.getAllRoads(); }
    public boolean isGraphConnected()              { return graph.isConnected(); }
    public int getTotalLocations()                 { return graph.locationCount(); }
    public int getTotalRoads()                     { return graph.roadCount() / 2; }
    public CityGraph getGraph()                    { return graph; }
}
