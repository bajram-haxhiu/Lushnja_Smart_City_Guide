package com.lushnja.services;

import com.lushnja.models.ParkingSpot;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing parking spots in Lushnja.
 * Simulates real-time availability; ready for live API integration.
 */
public class ParkingService {

    private final List<ParkingSpot> spots = new ArrayList<>();

    // ── Data Loading ──────────────────────────────────────────────────────────

    public void loadParking(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) { loadDefaults(); return; }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    String[] p = line.split(",", -1);
                    spots.add(new ParkingSpot(
                        p[0].trim(), p[1].trim(),
                        Double.parseDouble(p[2].trim()), Double.parseDouble(p[3].trim()),
                        p[4].trim(),
                        Integer.parseInt(p[5].trim()), Integer.parseInt(p[6].trim()),
                        p[8].trim()
                    ));
                } catch (Exception e) { /* skip */ }
            }
        } catch (IOException e) { loadDefaults(); }
    }

    private void loadDefaults() {
        spots.add(new ParkingSpot("P001", "Parkimi Sheshi Qendror", 40.94230, 19.70560,
                "Afër Sheshit Toka Jonë", 50, 23, "Falas"));
        spots.add(new ParkingSpot("P002", "Parkimi Spitalit", 40.93450, 19.71050,
                "Rruga Spitalit", 40, 38, "Falas"));
        spots.add(new ParkingSpot("P003", "Parkimi Bashkisë", 40.94300, 19.70580,
                "Afër Bashkisë", 30, 5, "Falas"));
        spots.add(new ParkingSpot("P004", "Parkimi Stacionit", 40.93970, 19.69900,
                "Afër Stacionit", 60, 41, "Falas"));
        spots.add(new ParkingSpot("P005", "Parkimi Tregut", 40.95090, 19.68960,
                "Rruga e Tregut", 35, 0, "Falas"));
        spots.add(new ParkingSpot("P006", "Parkimi Stadiumit", 40.93580, 19.70960,
                "Rruga Skënderbej", 45, 30, "Falas"));
        spots.add(new ParkingSpot("P007", "Parkimi Hotel Pilo", 40.94330, 19.70440,
                "Hotel Pilo Lala", 20, 8, "Me pagesë"));
        spots.add(new ParkingSpot("P008", "Parkimi Gjimnazit", 40.94140, 19.70490,
                "Rruga Teknike", 25, 15, "Falas"));
    }

    // ── Query Methods ─────────────────────────────────────────────────────────

    public List<ParkingSpot> getAllSpots() { return Collections.unmodifiableList(spots); }

    public List<ParkingSpot> getAvailableSpots() {
        return spots.stream()
                .filter(p -> p.getStatus() != ParkingSpot.Status.PLOT)
                .sorted(Comparator.comparingInt(ParkingSpot::getFreeSpaces).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Find nearest available parking to a destination (lat/lon).
     * Uses sorting by distance — O(n log n).
     */
    public Optional<ParkingSpot> findNearestAvailable(double lat, double lon) {
        return spots.stream()
                .filter(p -> p.getStatus() != ParkingSpot.Status.PLOT)
                .min(Comparator.comparingDouble(p -> p.distanceTo(lat, lon)));
    }

    public Optional<ParkingSpot> findNearest(double lat, double lon) {
        return spots.stream()
                .min(Comparator.comparingDouble(p -> p.distanceTo(lat, lon)));
    }

    public List<ParkingSpot> sortByDistance(double lat, double lon) {
        return spots.stream()
                .sorted(Comparator.comparingDouble(p -> p.distanceTo(lat, lon)))
                .collect(Collectors.toList());
    }

    /** Simulate real-time updates across all parking spots. */
    public void simulateUpdate() {
        spots.forEach(ParkingSpot::simulateUpdate);
    }

    /** Manually update a spot's free spaces (for demo/testing). */
    public void updateFreeSpaces(String id, int freeSpaces) {
        spots.stream().filter(p -> p.getId().equals(id))
             .findFirst().ifPresent(p -> p.setFreeSpaces(freeSpaces));
    }

    public int getTotalFreeSpaces() {
        return spots.stream().mapToInt(ParkingSpot::getFreeSpaces).sum();
    }
}
