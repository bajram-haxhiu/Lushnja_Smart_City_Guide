package com.lushnja.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Gemini-powered AI chat service.
 * API key is read from the GEMINI_API_KEY environment variable — never hardcoded.
 * Falls back gracefully if the key is missing or the API is unreachable.
 */
public class AIChatService {

    private static final String GEMINI_MODEL = "gemini-2.5-flash";
    private static final String ENDPOINT_BASE =
        "https://generativelanguage.googleapis.com/v1beta/models/";

    private final HttpClient httpClient;

    public AIChatService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Send a message to Gemini and return the response.
     *
     * @param userMessage the user's question
     * @param language    "en" or "sq"
     * @return AI response string, or fallback text on failure
     */
    public String askGemini(String userMessage, String language) {
        String apiKey = ApiConfig.getGeminiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackAnswer(language);
        }

        try {
            String endpoint = ENDPOINT_BASE + GEMINI_MODEL + ":generateContent";
            String prompt   = buildPrompt(userMessage, language);

            // Build Gemini request JSON
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", prompt);
            JsonArray parts = new JsonArray();
            parts.add(textPart);
            JsonObject content = new JsonObject();
            content.add("parts", parts);
            JsonArray contents = new JsonArray();
            contents.add(content);
            JsonObject body = new JsonObject();
            body.add("contents", contents);

            // Safety settings — relax for city guide context
            JsonObject safetyThreshold = new JsonObject();
            safetyThreshold.addProperty("category", "HARM_CATEGORY_DANGEROUS_CONTENT");
            safetyThreshold.addProperty("threshold", "BLOCK_NONE");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Gemini API error " + response.statusCode() + ": " + response.body());
                return fallbackAnswer(language);
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            String text = root.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

            // Clean up response — remove excessive markdown symbols
            return cleanResponse(text);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallbackAnswer(language);
        } catch (Exception e) {
            System.err.println("Gemini request failed: " + e.getMessage());
            return fallbackAnswer(language);
        }
    }

    // ── Prompt Builder ────────────────────────────────────────────────────────

    private String buildPrompt(String userMessage, String language) {
        String langFull = "sq".equalsIgnoreCase(language) ? "Albanian" : "English";

        return """
                You are a smart city guide assistant embedded in a JavaFX application called "Lushnja Smart City Guide".
                This is also a university Data Structures and Algorithms (DSA) course project.

                Context about the application:
                - It shows an interactive city map of Lushnja, Albania with 35 real places
                - It uses a graph data structure (nodes = places, edges = roads)
                - Dijkstra's algorithm finds the shortest/fastest route between places
                - BFS (Breadth-First Search) explores the graph level by level
                - DFS (Depth-First Search) explores the graph depth by depth
                - A priority queue is used in Dijkstra for efficiency: O((V+E) log V)
                - BFS and DFS have complexity O(V + E)
                - The map supports category filtering, search, and route visualization
                - There is a parking assistant showing free spaces in real time
                - Places include: restaurants, cafes, hotels, hospitals, pharmacies, banks, schools, parks, bus stations, police

                About Lushnja:
                - City in central Albania, Fier County
                - Known as the "Agricultural Capital" of Albania
                - Site of the historic 1920 Congress of Lushnje (Kongresi i Lushnjes)
                - Population: ~32,000 in the city
                - Mediterranean climate: hot summers, mild winters
                - Good bus connections to Tirana (~1.5h), Fier (~25min), Berat (~45min)

                STRICT RULES:
                - Answer ONLY in %s. Do not mix languages.
                - Keep answers concise, clear, and helpful (3-6 sentences max unless explaining algorithms)
                - No excessive bullet points or markdown symbols
                - Professional, friendly tone suitable for tourists and students
                - For DSA questions, give a clear technical but understandable explanation

                User question: %s
                """.formatted(langFull, userMessage);
    }

    // ── Response Cleaner ──────────────────────────────────────────────────────

    private String cleanResponse(String text) {
        if (text == null) return "";
        // Remove excessive markdown that JavaFX TextArea won't render
        return text
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")  // bold
                .replaceAll("\\*(.+?)\\*", "$1")         // italic
                .replaceAll("#{1,6}\\s", "")              // headers
                .replaceAll("```[\\s\\S]*?```", "")       // code blocks
                .replaceAll("`(.+?)`", "$1")              // inline code
                .replaceAll("\\n{3,}", "\n\n")            // collapse extra newlines
                .trim();
    }

    // ── Fallback ──────────────────────────────────────────────────────────────

    private String fallbackAnswer(String language) {
        if ("sq".equalsIgnoreCase(language)) {
            return "Asistenti AI nuk është i lidhur.\n\n" +
                   "Sigurohu që GEMINI_API_KEY është vendosur si variabël mjedisore në Eclipse:\n" +
                   "Run Configurations → Environment → New\n" +
                   "Name: GEMINI_API_KEY\n" +
                   "Value: çelësi_yt_api";
        }
        return "The AI assistant is not connected.\n\n" +
               "Make sure GEMINI_API_KEY is set as an environment variable in Eclipse:\n" +
               "Run Configurations → Environment → New\n" +
               "Name: GEMINI_API_KEY\n" +
               "Value: your_api_key";
    }
}
