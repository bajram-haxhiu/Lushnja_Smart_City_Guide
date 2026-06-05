# 🗺 Lushnja Smart City Guide

> **University Course Project — Data Structures and Algorithms (DSA)**
>
> A smart city guide application for Lushnja, Albania with interactive map, graph algorithms, AI chatbot, and parking assistant.

---

## 📌 Overview

**Lushnja Smart City Guide** is a Java/JavaFX desktop application that serves as a smart city assistant for Lushnja, Albania. Built for the DSA course, it demonstrates core computer science concepts through a real-world city navigation system.

**Default language: English** (switch to Albanian with the EN/SQ button)

---

## ✨ Features

| Feature | Description |
|---|---|
| 🗺 **Interactive Map** | City graph visualization with zoom, pan, and click-to-select |
| 🔍 **Search & Filter** | Find places by name or category |
| 🧭 **Shortest Path** | Dijkstra, BFS, DFS route finding with detailed DSA output |
| 🔬 **BFS/DFS Traversal** | Visual graph traversal with numbered visit order |
| 🚨 **Emergency Mode** | Instantly find nearest hospital, pharmacy, police, taxi |
| 🗺 **Tourist Mode** | Recommended 1-day itinerary for first-time visitors |
| 🅿 **Parking Assistant** | Real-time free spaces simulation with status indicator |
| 🤖 **AI Chatbot** | Floating Gemini-powered assistant (EN/SQ) |
| ⭐ **Favorites** | Save and manage favorite places |
| 🌙 **Day/Night Mode** | Light and dark map themes |
| 🇬🇧🇦🇱 **EN / SQ Switch** | Full English and Albanian UI with one click |

---

## 🔬 DSA Concepts Demonstrated

### Graph Data Structure
- **Nodes (V)**: 35 real places in Lushnja (restaurants, hospitals, parks, etc.)
- **Edges (E)**: 73 road segments with weights (distance in km)
- Implementation: **Adjacency List** in `CityGraph.java`

### Dijkstra's Algorithm
- Finds the **shortest distance** or **fastest time** path
- Uses a **Priority Queue (Min-Heap)** for efficient node selection
- Time complexity: **O((V+E) log V)**
- File: `DijkstraAlgorithm.java`

### BFS — Breadth-First Search
- Explores the graph **level by level**
- Finds the path with the **minimum number of hops**
- Time complexity: **O(V + E)**
- File: `BFSAlgorithm.java`

### DFS — Depth-First Search
- Explores the graph **depth by depth** with backtracking
- Finds **alternative paths** through the city
- Time complexity: **O(V + E)**
- File: `DFSAlgorithm.java`

### Search & Sort Algorithms
- **Linear search** by name/category: O(n) — `PlaceService.java`
- **Sorting by distance** using Haversine formula: O(n log n)
- **Priority Queue** usage visible in Dijkstra implementation

---

## 🏗 Project Structure

```
LUSHNJA_MAPS/
├── src/
│   ├── com/lushnja/
│   │   ├── app/
│   │   │   ├── MainApp.java               ← Entry point
│   │   │   └── GeminiTest.java            ← API test utility
│   │   ├── core/
│   │   │   ├── graph/
│   │   │   │   └── CityGraph.java         ← Adjacency list graph
│   │   │   ├── model/
│   │   │   │   ├── Location.java          ← Graph node
│   │   │   │   ├── Road.java              ← Weighted edge
│   │   │   │   └── Route.java             ← Route result
│   │   │   └── algorithms/
│   │   │       ├── DijkstraAlgorithm.java ← Shortest path
│   │   │       ├── BFSAlgorithm.java      ← BFS traversal
│   │   │       └── DFSAlgorithm.java      ← DFS traversal
│   │   ├── models/
│   │   │   ├── Place.java                 ← City place with category
│   │   │   ├── ParkingSpot.java           ← Parking model
│   │   │   └── WeatherInfo.java           ← (unused in UI)
│   │   ├── services/
│   │   │   ├── AIChatService.java         ← Gemini API integration
│   │   │   ├── ApiConfig.java             ← API key reader
│   │   │   ├── ChatbotService.java        ← Rule-based fallback
│   │   │   ├── DataLoaderService.java     ← CSV data loader
│   │   │   ├── NavigationService.java     ← Algorithm orchestrator
│   │   │   ├── ParkingService.java        ← Parking management
│   │   │   └── PlaceService.java          ← Search, filter, sort
│   │   ├── gui/
│   │   │   └── view/
│   │   │       ├── MainView.java          ← Main window + all panels
│   │   │       └── MapCanvasView.java     ← Canvas map renderer
│   │   └── utils/
│   │       ├── GeoUtils.java             ← Haversine, formatting
│   │       └── LanguageManager.java      ← EN/SQ i18n manager
│   └── resources/
│       ├── data/
│       │   ├── locations.csv             ← 35 Lushnja places
│       │   ├── roads.csv                 ← 73 road connections
│       │   └── parking.csv              ← 8 parking spots
│       ├── i18n/
│       │   ├── messages_en.properties    ← English UI strings
│       │   └── messages_sq.properties   ← Albanian UI strings
│       └── css/
│           └── style.css                ← Application styles
├── .classpath                           ← Eclipse build config
├── .project                             ← Eclipse project config
└── README.md                            ← This file
```

---

## 🚀 How to Run in Eclipse

### Requirements
- **Java 21+** (JDK 21 or newer)
- **JavaFX SDK 21** — Download from [gluonhq.com/products/javafx](https://gluonhq.com/products/javafx/)
- **Gson JAR** — Download from [mvnrepository.com/artifact/com.google.code.gson/gson](https://mvnrepository.com/artifact/com.google.code.gson/gson)
- **Eclipse IDE 2023+**

### Steps

**1. Import the project:**
```
File → Import → Existing Projects into Workspace → select LUSHNJA_MAPS folder
```

**2. Add JavaFX JARs to Build Path:**
```
Right-click project → Build Path → Configure Build Path
→ Libraries → Add External JARs
→ Select all JARs from javafx-sdk-21/lib/
```

**3. Add Gson JAR to Build Path:**
```
Build Path → Add External JARs → select gson-2.x.x.jar
```

**4. Configure VM Arguments:**
```
Run → Run Configurations → Arguments → VM Arguments
```
Add:
```
--module-path "C:\path\to\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.web,javafx.media
```

**5. Set Gemini API Key (optional but recommended):**
```
Run → Run Configurations → Environment → New
Name:  GEMINI_API_KEY
Value: your_gemini_api_key
```
> Without the key, the chatbot shows a helpful fallback message.  
> Get a free key at [aistudio.google.com](https://aistudio.google.com/)

**6. Run:**
```
Right-click MainApp.java → Run As → Java Application
```

---

## 🌐 Language Switch

Click the **EN / SQ** button in the top-right header to instantly switch between:
- 🇬🇧 **English** (default)
- 🇦🇱 **Albanian (Shqip)**

All UI labels, buttons, categories, status messages, and DSA output update immediately.  
The AI chatbot also answers in the selected language.

---

## 🤖 Gemini AI Chatbot

The floating chat button (💬 bottom-right) opens the AI assistant powered by **Gemini 2.5 Flash**.

**It can answer questions about:**
- Places in Lushnja (restaurants, cafes, hotels, pharmacies...)
- How to get somewhere (routes, Dijkstra)
- DSA algorithm explanations (Dijkstra, BFS, DFS, graphs)
- Parking availability
- Tourism in Lushnja
- Albanian history and culture

**API key configuration:**
- Set environment variable `GEMINI_API_KEY` in Eclipse Run Configurations
- Never commit the API key to source control
- If missing, a clear fallback message is shown

---

## 🅿 Parking Assistant

The **Parking** tab shows all 8 parking locations in Lushnja with:
- Real-time free space count (simulated)
- Color-coded status: 🟢 Available / 🟡 Almost Full / 🔴 Full
- Price (free or paid)
- Click "Simulate Update" to refresh parking availability

---

## 📊 Data Files

| File | Contents |
|---|---|
| `locations.csv` | 35 places: name, coordinates, category, address, rating, hours, description |
| `roads.csv` | 73 road connections: source, destination, distance, type, speed limit |
| `parking.csv` | 8 parking spots: name, location, capacity, free spaces, price |

---

## 🔧 Technologies Used

- **Java 21** — main language (virtual threads, records, pattern matching)
- **JavaFX 21** — GUI and Canvas rendering
- **Java HTTP Client** — Gemini API calls (no extra HTTP library needed)
- **Gson** — JSON parsing for Gemini API responses
- **Java Collections** — PriorityQueue, LinkedHashMap, LinkedHashSet

---

## 🔮 Possible Improvements

- [ ] Live Gemini streaming responses
- [ ] OpenStreetMap integration for real map tiles
- [ ] Route animation on the map
- [ ] Export itinerary as PDF
- [ ] Real parking API integration
- [ ] More DSA visualizations (priority queue animation)

---

*Lushnje, Albania — 40.94°N, 19.71°E*
