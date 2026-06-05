package com.lushnja.app;

import com.lushnja.gui.view.MainView;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Lushnja Smart City Guide — Main Entry Point
 *
 * Course: Data Structures and Algorithms (DSA)
 *
 * Features:
 *  - Graph data structure (nodes = places, edges = roads)
 *  - Dijkstra algorithm for shortest path (distance and time)
 *  - BFS and DFS graph traversal with visual output
 *  - Search and category filtering
 *  - Gemini AI chatbot assistant (set GEMINI_API_KEY env variable)
 *  - Parking assistant with live status simulation
 *  - Emergency mode (nearest hospital, pharmacy, police, taxi)
 *  - Tourist mode (1-day itinerary)
 *  - Favorites feature
 *  - Day / Night mode
 *  - English / Albanian language switch (EN / SQ)
 *
 * How to set Gemini API key in Eclipse:
 *   Run → Run Configurations → Environment → New
 *   Name: GEMINI_API_KEY   Value: your_api_key_here
 */
public class MainApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        MainView view = new MainView(primaryStage);
        view.show();
    }
}
