package com.lushnja.services;

import java.util.List;
import java.util.Optional;

import com.lushnja.models.ParkingSpot;
import com.lushnja.models.Place;
import com.lushnja.models.Place.PlaceCategory;
import com.lushnja.models.WeatherInfo;


/**
 * Rule-based chatbot assistant for Lushnja Smart City Guide.
 * Answers common questions in Albanian using local data.
 *
 * Ready for OpenAI/Claude API integration — see method callExternalAI().
 */
public class ChatbotService {

    private final PlaceService   placeService;
    private final ParkingService parkingService;
    private final WeatherService weatherService;

    // Lushnja center coords
    private static final double CENTER_LAT = 40.9426;
    private static final double CENTER_LON = 19.7062;

    public ChatbotService(PlaceService placeService, ParkingService parkingService,
                          WeatherService weatherService) {
        this.placeService   = placeService;
        this.parkingService = parkingService;
        this.weatherService = weatherService;
    }

    /**
     * Process user message and return Albanian response.
     */
    public String respond(String message) {
        if (message == null || message.isBlank()) return greeting();
        String msg = message.toLowerCase().trim();

        // ── Greetings ─────────────────────────────────────────────────────────
        if (matches(msg, "persh", "cka ka", "hello", "hi", "allo", "miredita", "mirembrema")) {
            return greeting();
        }

        // ── Eating / Restaurants ──────────────────────────────────────────────
        if (matches(msg, "ha ", "restorant", "manse", "ushqim", "drekë", "darkë", "kuzhin")) {
            return answerRestaurants();
        }

        // ── Cafe / Coffee ─────────────────────────────────────────────────────
        if (matches(msg, "kafe", "kafene", "ekspres", "cappucc")) {
            return answerCafe();
        }

        // ── Hotels ───────────────────────────────────────────────────────────
        if (matches(msg, "hotel", "qëndroj", "fle", "dhomë", "akomodim")) {
            return answerHotels();
        }

        // ── Pharmacy ─────────────────────────────────────────────────────────
        if (matches(msg, "farmaci", "barna", "ilaç", "medikam")) {
            return answerPharmacy();
        }

        // ── Hospital ─────────────────────────────────────────────────────────
        if (matches(msg, "spital", "mjek", "doktor", "ambulanc", "emergj")) {
            return answerHospital();
        }

        // ── Police ────────────────────────────────────────────────────────────
        if (matches(msg, "polici", "komisariat")) {
            return answerPolice();
        }

        // ── Parking ───────────────────────────────────────────────────────────
        if (matches(msg, "parkim", "makinë", "automobil", "veturë")) {
            return answerParking();
        }

        // ── Weather ───────────────────────────────────────────────────────────
        if (matches(msg, "mot", "temperatur", "shi", "diell", "erë", "reshje")) {
            return answerWeather();
        }

        // ── Tourist spots ─────────────────────────────────────────────────────
        if (matches(msg, "vizitoj", "turist", "pamje", "vend interes", "shëtitj")) {
            return answerTourism();
        }

        // ── Banks / ATM ───────────────────────────────────────────────────────
        if (matches(msg, "bank", "atm", "lekë", "para", "arke")) {
            return answerBanks();
        }

        // ── Bus station / Transport ───────────────────────────────────────────
        if (matches(msg, "autobus", "stacion", "transport", "bilete", "shkoj në")) {
            return answerTransport();
        }

        // ── Parks ─────────────────────────────────────────────────────────────
        if (matches(msg, "park", "lulisht", "shëtitj")) {
            return answerParks();
        }

        // ── DSA explanation ───────────────────────────────────────────────────
        if (matches(msg, "dijkstra", "algoritëm", "bfs", "dfs", "grafik")) {
            return answerDSA();
        }

        // ── About the city ────────────────────────────────────────────────────
        if (matches(msg, "lushnje", "qyteti", "histori", "tregoj")) {
            return answerAboutCity();
        }

        // ── Help ──────────────────────────────────────────────────────────────
        if (matches(msg, "ndihmë", "help", "çfarë di", "çfarë mund")) {
            return answerHelp();
        }

        return defaultResponse(message);
    }

    // ── Answers ───────────────────────────────────────────────────────────────

    private String greeting() {
        return "Mirë se erdhe! 👋\n\nUnë jam asistenti virtual i Lushnja Smart City Guide.\n\n" +
               "Mund të të ndihmoj me:\n" +
               "• Gjetjen e restoranteve dhe kafeneve\n" +
               "• Farmacitë dhe spitalet\n" +
               "• Parkimet e lira\n" +
               "• Vendet turistike\n" +
               "• Informacione rreth motit\n\n" +
               "Çfarë dëshiron të dish? 😊";
    }

    private String answerRestaurants() {
        List<Place> rests = placeService.filterByCategory(PlaceCategory.RESTORANTE);
        StringBuilder sb = new StringBuilder("🍽️ **Restorante në Lushnje:**\n\n");
        for (Place r : rests) {
            sb.append("📍 ").append(r.getName()).append("\n");
            sb.append("   📌 ").append(r.getAddress()).append("\n");
            sb.append("   ⭐ Vlerësimi: ").append(r.getRating()).append("/5\n");
            sb.append("   🕐 Orari: ").append(r.getOpeningHours()).append("\n\n");
        }
        sb.append("Shko te seksioni 'Restorante' për të parë vendndodhjen në hartë.");
        return sb.toString();
    }

    private String answerCafe() {
        List<Place> cafes = placeService.filterByCategory(PlaceCategory.KAFENE);
        StringBuilder sb = new StringBuilder("☕ **Kafene në Lushnje:**\n\n");
        for (Place c : cafes) {
            sb.append("📍 ").append(c.getName()).append("\n");
            sb.append("   📌 ").append(c.getAddress()).append("\n");
            sb.append("   ⭐ ").append(c.getRating()).append("/5\n\n");
        }
        return sb.toString();
    }

    private String answerHotels() {
        List<Place> hotels = placeService.filterByCategory(PlaceCategory.HOTELE);
        StringBuilder sb = new StringBuilder("🏨 **Hotele në Lushnje:**\n\n");
        for (Place h : hotels) {
            sb.append("📍 ").append(h.getName()).append("\n");
            sb.append("   📌 ").append(h.getAddress()).append("\n");
            sb.append("   ⭐ ").append(h.getRating()).append("/5\n");
            sb.append("   🕐 ").append(h.getOpeningHours()).append("\n\n");
        }
        return sb.toString();
    }

    private String answerPharmacy() {
        Optional<Place> nearest = placeService.findNearest(CENTER_LAT, CENTER_LON, PlaceCategory.FARMACI);
        StringBuilder sb = new StringBuilder("💊 **Farmacitë më të afërta:**\n\n");
        List<Place> all = placeService.filterByCategory(PlaceCategory.FARMACI);
        for (Place p : all) {
            sb.append("📍 ").append(p.getName()).append("\n");
            sb.append("   📌 ").append(p.getAddress()).append("\n");
            sb.append("   🕐 Orari: ").append(p.getOpeningHours()).append("\n\n");
        }
        nearest.ifPresent(p ->
            sb.append("✅ Farmacia më e afërt me qendrën: **").append(p.getName()).append("**"));
        return sb.toString();
    }

    private String answerHospital() {
        Optional<Place> hosp = placeService.findNearest(CENTER_LAT, CENTER_LON, PlaceCategory.SPITALE);
        return "🏥 **Shërbim mjekësor:**\n\n" +
               hosp.map(h ->
                   "📍 **" + h.getName() + "**\n" +
                   "📌 " + h.getAddress() + "\n" +
                   "🕐 Orari: " + h.getOpeningHours() + "\n\n" +
                   "📞 Numri emergjent: **127**\n" +
                   "🚑 Ambulanca: **127**\n\n" +
                   "Për rrugën, shko te 'Rruga Më e Shkurtër' dhe zgjidh 'Spitali i Lushnjes' si destinacion."
               ).orElse("Nuk u gjet spital në databazë.");
    }

    private String answerPolice() {
        Optional<Place> pol = placeService.findNearest(CENTER_LAT, CENTER_LON, PlaceCategory.POLICIA);
        return "👮 **Policia:**\n\n" +
               pol.map(p ->
                   "📍 **" + p.getName() + "**\n" +
                   "📌 " + p.getAddress() + "\n" +
                   "🕐 " + p.getOpeningHours() + "\n\n" +
                   "📞 Numri i urgjencës policore: **129**"
               ).orElse("Komisariati ndodhet në qendër të qytetit.\n📞 Policia: 129");
    }

    private String answerParking() {
        List<ParkingSpot> available = parkingService.getAvailableSpots();
        StringBuilder sb = new StringBuilder("🅿️ **Parkime të lira:**\n\n");
        if (available.isEmpty()) {
            sb.append("⚠️ Momentalisht nuk ka parkim të lirë.\nProvo pas pak minutash.");
        } else {
            for (ParkingSpot p : available) {
                sb.append("📍 ").append(p.getName()).append("\n");
                sb.append("   📌 ").append(p.getAddress()).append("\n");
                sb.append("   🚗 Vende të lira: **").append(p.getFreeSpaces())
                  .append("/").append(p.getTotalSpaces()).append("**\n");
                sb.append("   💰 ").append(p.getPrice()).append("\n\n");
            }
        }
        sb.append("Shko te seksioni 'Parkime' për detaje të plota.");
        return sb.toString();
    }

    private String answerWeather() {
        if (weatherService == null) return "Weather information is not available."; WeatherInfo w = weatherService.getMockWeather();
        return String.format(
            "%s **Moti në Lushnje:**\n\n" +
            "🌡️ Temperatura: **%.1f°C**\n" +
            "🌤️ Kushtet: %s\n" +
            "💨 Era: %.1f km/h\n" +
            "⏱️ Përditësuar: %s\n\n" +
            "_Dil te seksioni 'Moti' për parashikimin e plotë._",
            w.getEmoji(), w.getTemperature(), w.getCondition(),
            w.getWindSpeed(), w.getLastUpdated()
        );
    }

    private String answerTourism() {
        List<Place> recs = placeService.getTouristRecommendations();
        StringBuilder sb = new StringBuilder("🗺️ **Vendet e Rekomanduar për Vizitë:**\n\n");
        int i = 1;
        for (Place p : recs) {
            sb.append(i++).append(". **").append(p.getName()).append("**\n");
            sb.append("   📌 ").append(p.getAddress()).append("\n");
            sb.append("   ⭐ ").append(p.getRating()).append("/5 — ")
              .append(p.getDescription(), 0, Math.min(60, p.getDescription().length())).append("...\n\n");
        }
        sb.append("Shko te seksioni 'Mënyrë Turisti' për itinerarin e plotë 1-ditor.");
        return sb.toString();
    }

    private String answerBanks() {
        List<Place> banks = placeService.filterByCategory(PlaceCategory.BANKA);
        StringBuilder sb = new StringBuilder("🏦 **Banka & ATM në Lushnje:**\n\n");
        for (Place b : banks) {
            sb.append("📍 ").append(b.getName()).append("\n");
            sb.append("   📌 ").append(b.getAddress()).append("\n");
            sb.append("   🕐 ").append(b.getOpeningHours()).append("\n\n");
        }
        return sb.toString();
    }

    private String answerTransport() {
        Optional<Place> station = placeService.findNearest(CENTER_LAT, CENTER_LON, PlaceCategory.STACIONE_AUTOBUSI);
        return "🚌 **Transport publik:**\n\n" +
               station.map(s ->
                   "📍 **" + s.getName() + "**\n" +
                   "📌 " + s.getAddress() + "\n" +
                   "🕐 " + s.getOpeningHours() + "\n\n" +
                   "Nga Lushnja ka lidhje me:\n" +
                   "• Tiranë (~1h 30min)\n" +
                   "• Fier (~25min)\n" +
                   "• Berat (~45min)\n" +
                   "• Durrës (~1h 45min)"
               ).orElse("Stacioni i autobuseve ndodhet në qendër të qytetit.");
    }

    private String answerParks() {
        List<Place> parks = placeService.filterByCategory(PlaceCategory.PARKE);
        StringBuilder sb = new StringBuilder("🌳 **Parqe dhe zona të gjelbra:**\n\n");
        for (Place p : parks) {
            sb.append("📍 ").append(p.getName()).append("\n");
            sb.append("   📌 ").append(p.getAddress()).append("\n");
            sb.append("   🕐 ").append(p.getOpeningHours()).append("\n\n");
        }
        return sb.toString();
    }

    private String answerDSA() {
        return "🖥️ **Algoritmët e përdorur në këtë aplikacion:**\n\n" +
               "**📊 Dijkstra Algorithm**\n" +
               "Gjen rrugën më të shkurtër midis dy pikave.\n" +
               "Kompleksiteti: O((V+E) log V)\n\n" +
               "**🔵 BFS (Breadth-First Search)**\n" +
               "Eksploron grafikun nivel pas niveli.\n" +
               "Kompleksiteti: O(V+E)\n\n" +
               "**🟢 DFS (Depth-First Search)**\n" +
               "Eksploron grafikun thellësi pas thellësie.\n" +
               "Kompleksiteti: O(V+E)\n\n" +
               "**Graf:** " + "Lushnja ka vendndodhje (nyje) dhe rrugë (brinja).\n" +
               "Shko te seksioni 'BFS/DFS' për ta parë vizualisht!";
    }

    private String answerAboutCity() {
        return "🏙️ **Lushnja — Qyteti i Zemrës së Shqipërisë:**\n\n" +
               "📍 Lushnja është qytet i rëndësishëm i Shqipërisë qendrore.\n\n" +
               "🌿 Njihet si 'Kryeqyteti i Bujqësisë' — zona e saj është ndër\n" +
               "     më pjellore të Shqipërisë.\n\n" +
               "📜 Është seli e Kongresit historik të 1920-s, ku u vendos kufiri\n" +
               "     dhe qeveria e parë shqiptare.\n\n" +
               "👥 Popullsia: ~32.000 banorë (qyteti)\n" +
               "🗓️ Themeluar: Shek. XVII\n" +
               "🌡️ Klima: Mesdhetare — verë e nxehtë, dimër i butë\n\n" +
               "Vizito Muzeun e Kongresit dhe Sheshin 'Toka Jonë'! 🗺️";
    }

    private String answerHelp() {
        return "ℹ️ **Çfarë mund të bëj:**\n\n" +
               "• 🍽️ «Ku mund të ha?» → restorante\n" +
               "• ☕ «Ku ka kafene?» → kafene\n" +
               "• 🏨 «Ku fle?» → hotele\n" +
               "• 💊 «Ku ka farmaci?» → farmacitë\n" +
               "• 🏥 «Ku është spitali?» → shëndetësia\n" +
               "• 🅿️ «Ku ka parkim?» → parkimet\n" +
               "• 🌤️ «Si është moti?» → moti i ditës\n" +
               "• 🗺️ «Çfarë të vizitoj?» → turizëm\n" +
               "• 🏦 «Ku ka ATM?» → bankat\n" +
               "• 🚌 «Ku është stacioni?» → transport\n\n" +
               "Shkruaj pyetjen tënde dhe unë do të ndihmoj! 😊";
    }

    private String defaultResponse(String message) {
        return "🤔 Nuk e kuptova saktë pyetjen tuaj.\n\n" +
               "Mund të provoni:\n" +
               "• \"Ku mund të ha në Lushnje?\"\n" +
               "• \"Ku ka farmaci?\"\n" +
               "• \"Si është moti?\"\n" +
               "• \"Ku ka parkim të lirë?\"\n" +
               "• \"Çfarë të vizitoj?\"\n\n" +
               "Shkruaj **ndihmë** për listën e plotë.";
    }

    private boolean matches(String message, String... keywords) {
        for (String kw : keywords) {
            if (message.contains(kw)) return true;
        }
        return false;
    }

    // Optional: plug in OpenAI/Claude API here
    // private String callExternalAI(String message) {
    //     // TODO: configure API key via environment variable
    //     // String apiKey = System.getenv("OPENAI_API_KEY");
    //     // ... HTTP call to OpenAI completions endpoint
    //     return null;
    // }
}
