package com.lushnja.services;

import com.lushnja.models.Place;
import com.lushnja.models.Place.PlaceCategory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing, searching, and filtering places in Lushnja.
 * Demonstrates searching and sorting DSA concepts.
 */
public class PlaceService {

    private final List<Place> allPlaces = new ArrayList<>();
    private final Set<String> favoriteIds = new LinkedHashSet<>();

    // ── Data Loading ──────────────────────────────────────────────────────────

    public void loadPlaces(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) { System.err.println("Places resource not found: " + resourcePath); return; }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    // CSV: id,name,lat,lon,type,category,address,rating,openingHours,description
                    // Split with limit -1 to preserve empty fields; handle comma in description
                    String[] parts = splitCsv(line);
                    if (parts.length < 10) continue;
                    String id           = parts[0].trim();
                    String name         = parts[1].trim();
                    double lat          = Double.parseDouble(parts[2].trim());
                    double lon          = Double.parseDouble(parts[3].trim());
                    PlaceCategory cat   = parseCategory(parts[5].trim());
                    String address      = parts[6].trim();
                    double rating       = Double.parseDouble(parts[7].trim());
                    String hours        = parts[8].trim();
                    String desc         = parts[9].trim().replace("\"", "");
                    allPlaces.add(new Place(id, name, lat, lon, cat, address, rating, hours, desc));
                } catch (Exception e) {
                    System.err.println("Skipping line: " + line + " — " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading places: " + e.getMessage());
        }
    }

    private String[] splitCsv(String line) {
        List<String> result = new ArrayList<>();
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

    private PlaceCategory parseCategory(String cat) {
        for (PlaceCategory pc : PlaceCategory.values()) {
            if (pc.getAlbanianName().equalsIgnoreCase(cat) || pc.name().equalsIgnoreCase(cat.replace(" ", "_"))) {
                return pc;
            }
        }
        // Fuzzy matching
        String lower = cat.toLowerCase();
        if (lower.contains("spital")) return PlaceCategory.SPITALE;
        if (lower.contains("farmaci")) return PlaceCategory.FARMACI;
        if (lower.contains("bank")) return PlaceCategory.BANKA;
        if (lower.contains("park")) return PlaceCategory.PARKE;
        if (lower.contains("hotel")) return PlaceCategory.HOTELE;
        if (lower.contains("restorant")) return PlaceCategory.RESTORANTE;
        if (lower.contains("kafene") || lower.contains("kafe")) return PlaceCategory.KAFENE;
        if (lower.contains("shkol")) return PlaceCategory.SHKOLLA;
        if (lower.contains("polici")) return PlaceCategory.POLICIA;
        if (lower.contains("stacion")) return PlaceCategory.STACIONE_AUTOBUSI;
        if (lower.contains("taksi")) return PlaceCategory.PIKA_TAKSIE;
        if (lower.contains("parkim")) return PlaceCategory.PARKIME;
        if (lower.contains("treg")) return PlaceCategory.TREGU;
        if (lower.contains("institucion") || lower.contains("bashki") || lower.contains("qeveri")) return PlaceCategory.INSTITUCIONE;
        return PlaceCategory.VENDE_TURISTIKE;
    }

    // ── Search ────────────────────────────────────────────────────────────────

    /**
     * Linear search by name or category (O(n)).
     * Returns list of matching places sorted by relevance.
     */
    public List<Place> search(String query) {
        if (query == null || query.isBlank()) return new ArrayList<>(allPlaces);
        String q = query.toLowerCase().trim();
        return allPlaces.stream()
                .filter(p -> p.matchesSearch(q))
                .sorted(Comparator.comparing(p -> {
                    // Exact name match = highest priority
                    if (p.getName().toLowerCase().equals(q)) return 0;
                    if (p.getName().toLowerCase().startsWith(q)) return 1;
                    return 2;
                }))
                .collect(Collectors.toList());
    }

    /**
     * Filter places by exact category.
     */
    public List<Place> filterByCategory(PlaceCategory category) {
        if (category == null) return new ArrayList<>(allPlaces);
        return allPlaces.stream()
                .filter(p -> p.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * Sort places by distance from a reference point.
     * Demonstrates sorting algorithm concept.
     */
    public List<Place> sortByDistance(double refLat, double refLon) {
        return allPlaces.stream()
                .sorted(Comparator.comparingDouble(p -> p.distanceTo(refLat, refLon)))
                .collect(Collectors.toList());
    }

    /**
     * Sort by rating (descending).
     */
    public List<Place> sortByRating() {
        return allPlaces.stream()
                .sorted(Comparator.comparingDouble(Place::getRating).reversed())
                .collect(Collectors.toList());
    }

    // ── Nearest Place Finder ──────────────────────────────────────────────────

    public Optional<Place> findNearest(double lat, double lon, PlaceCategory category) {
        return allPlaces.stream()
                .filter(p -> category == null || p.getCategory() == category)
                .min(Comparator.comparingDouble(p -> p.distanceTo(lat, lon)));
    }

    public List<Place> findNearest(double lat, double lon, PlaceCategory category, int count) {
        return allPlaces.stream()
                .filter(p -> category == null || p.getCategory() == category)
                .sorted(Comparator.comparingDouble(p -> p.distanceTo(lat, lon)))
                .limit(count)
                .collect(Collectors.toList());
    }

    // ── Favorites ─────────────────────────────────────────────────────────────

    public void addFavorite(String placeId) {
        favoriteIds.add(placeId);
        allPlaces.stream().filter(p -> p.getId().equals(placeId)).forEach(p -> p.setFavorite(true));
    }

    public void removeFavorite(String placeId) {
        favoriteIds.remove(placeId);
        allPlaces.stream().filter(p -> p.getId().equals(placeId)).forEach(p -> p.setFavorite(false));
    }

    public boolean isFavorite(String placeId) { return favoriteIds.contains(placeId); }

    public List<Place> getFavorites() {
        return allPlaces.stream().filter(Place::isFavorite).collect(Collectors.toList());
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public List<Place> getAllPlaces() { return Collections.unmodifiableList(allPlaces); }

    public Optional<Place> findById(String id) {
        return allPlaces.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    public List<String> getAllCategories() {
        return allPlaces.stream()
                .map(p -> p.getCategory().getAlbanianName())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /** Tourist recommendation list for a first-time visitor. */
    public List<Place> getTouristRecommendations() {
        List<Place> recommended = new ArrayList<>();
        // Curated picks for one-day tour of Lushnja
        String[] ids = {"LOC01", "LOC07", "LOC33", "LOC13", "LOC12", "LOC23", "LOC21", "LOC11"};
        for (String id : ids) findById(id).ifPresent(recommended::add);
        return recommended;
    }
}
