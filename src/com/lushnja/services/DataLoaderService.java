package com.lushnja.services;

import com.lushnja.core.graph.CityGraph;
import com.lushnja.core.model.Location;
import com.lushnja.core.model.Road;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Loads city graph data (nodes + edges) from CSV resource files.
 *
 * CSV formats:
 *   locations.csv  →  id,name,lat,lon,type,category,address,rating,openingHours,description
 *   roads.csv      →  id,sourceId,destinationId,distance,roadName,roadType,speedLimit
 */
public class DataLoaderService {

    private final CityGraph graph;

    public DataLoaderService(CityGraph graph) {
        this.graph = graph;
    }

    public void loadDefaultDataset() {
        loadLocations("/data/locations.csv");
        loadRoads("/data/roads.csv");
        System.out.println("Graf i ngarkuar: " + graph.locationCount() +
                           " vendndodhje, " + graph.roadCount()/2 + " rrugë");
    }

    private void loadLocations(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) { System.err.println("Nuk u gjet: " + resourcePath); return; }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    String[] p = splitCsv(line);
                    String id  = p[0].trim();
                    String name= p[1].trim();
                    double lat = Double.parseDouble(p[2].trim());
                    double lon = Double.parseDouble(p[3].trim());
                    Location.LocationType type = parseLocationType(p[4].trim());
                    Location loc = new Location(id, name, lat, lon, type);
                    if (!graph.containsLocation(id)) graph.addLocation(loc);
                } catch (Exception e) {
                    // skip bad line silently
                }
            }
        } catch (IOException e) {
            System.err.println("Gabim duke lexuar vendndodhjet: " + e.getMessage());
        }
    }

    private void loadRoads(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) { System.err.println("Nuk u gjet: " + resourcePath); return; }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    String[] p     = line.split(",", -1);
                    String id      = p[0].trim();
                    String srcId   = p[1].trim();
                    String dstId   = p[2].trim();
                    double dist    = Double.parseDouble(p[3].trim());
                    String rname   = p[4].trim();
                    Road.RoadType rtype = Road.RoadType.valueOf(p[5].trim().toUpperCase());
                    double speed   = Double.parseDouble(p[6].trim());
                    Location src   = graph.getLocation(srcId).orElse(null);
                    Location dst   = graph.getLocation(dstId).orElse(null);
                    if (src != null && dst != null && !graph.containsRoad(id)) {
                        Road road = new Road(id, src, dst, dist, rname, rtype, speed);
                        graph.addBidirectionalRoad(road);
                    }
                } catch (Exception e) {
                    // skip bad line silently
                }
            }
        } catch (IOException e) {
            System.err.println("Gabim duke lexuar rrugët: " + e.getMessage());
        }
    }

    private Location.LocationType parseLocationType(String s) {
        try { return Location.LocationType.valueOf(s.toUpperCase()); }
        catch (Exception e) { return Location.LocationType.LANDMARK; }
    }

    private String[] splitCsv(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') { inQuotes = !inQuotes; }
            else if (c == ',' && !inQuotes) { result.add(sb.toString()); sb.setLength(0); }
            else { sb.append(c); }
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }
}
