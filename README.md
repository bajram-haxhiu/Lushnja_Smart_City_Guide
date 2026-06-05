# Lushnja Smart City Guide DSA

## Overview

**Lushnja Smart City Guide DSA** is a JavaFX desktop application developed as a project for the **Data Structures and Algorithms (DSA)** course.

The project represents a smart city guide for the city of **Lushnja, Albania**. It helps users explore important places, find routes, use graph algorithms, check parking information, switch between English and Albanian, and interact with an AI-powered assistant.

The main goal of this project is to demonstrate how **Data Structures and Algorithms** can be applied in a real-world navigation and city guide system.

---

## Main Purpose

This project focuses on applying DSA concepts in a practical application.

The city of Lushnja is represented as a graph where:

* Locations are represented as **nodes**
* Roads are represented as **edges**
* Distances and travel times are represented as **weights**
* Algorithms are used to explore and find routes between locations

---

## Features

### Smart City Guide

* Interactive city map visualization
* Important places in Lushnja
* Search functionality
* Category filtering
* Place details
* Tourist mode
* Emergency mode
* Parking assistant
* Day/Night mode

---

### Data Structures and Algorithms

The project includes and demonstrates:

* Graph representation
* Nodes and edges
* Weighted graph
* Dijkstra algorithm
* BFS traversal
* DFS traversal
* Searching
* Sorting
* Priority Queue usage

---

### Dijkstra Algorithm

The application allows the user to select a starting location and a destination.

The system then calculates the shortest route using **Dijkstra’s Algorithm**.

The output shows:

* Algorithm used
* Start node
* Destination node
* Shortest path
* Total distance
* Total estimated time
* Number of visited nodes

---

### BFS and DFS Traversal

The project also includes:

* Breadth-First Search
* Depth-First Search

The user can choose a starting node and visualize the traversal order.

The output shows:

* Algorithm used
* Starting node
* Traversal order
* Number of visited nodes
* Time complexity explanation

---

### Parking Assistant

The application includes a parking assistant that shows parking places in Lushnja.

Each parking location includes:

* Parking name
* Address
* Total spaces
* Free spaces
* Parking status
* Price

Parking status can be:

* Available
* Almost Full
* Full

---

### Language Support

The application supports two languages:

* English
* Albanian

The default language is English.

Users can switch the interface language using the **EN / SQ** button.

The language switch updates:

* Buttons
* Labels
* Menus
* Status messages
* Route results
* DSA results
* Chatbot interface

---

### Gemini AI Chatbot

The project includes a floating AI assistant powered by **Gemini API**.

The chatbot can answer questions about:

* Lushnja
* Places to visit
* Parking
* Routes
* Dijkstra algorithm
* BFS and DFS
* Data Structures and Algorithms
* How the project works

The chatbot appears as a floating button in the bottom-right corner of the application.

---

## Technologies Used

* Java
* JavaFX
* Eclipse IDE
* Gson
* SLF4J
* Gemini API
* Object-Oriented Programming
* Data Structures and Algorithms

---

## Project Structure

```text
Lushnja_Smart_City_Guide_DSA/
│
├── src/
│   ├── com.lushnja.app
│   │   └── MainApp.java
│   │
│   ├── com.lushnja.core.algorithms
│   │   ├── DijkstraAlgorithm.java
│   │   ├── BFSAlgorithm.java
│   │   └── DFSAlgorithm.java
│   │
│   ├── com.lushnja.core.graph
│   │   └── CityGraph.java
│   │
│   ├── com.lushnja.core.model
│   │   ├── Location.java
│   │   ├── Road.java
│   │   └── Route.java
│   │
│   ├── com.lushnja.gui.view
│   │   ├── MainView.java
│   │   └── MapCanvasView.java
│   │
│   ├── com.lushnja.models
│   │   ├── Place.java
│   │   └── ParkingSpot.java
│   │
│   ├── com.lushnja.services
│   │   ├── AIChatService.java
│   │   ├── ChatbotService.java
│   │   ├── DataLoaderService.java
│   │   ├── NavigationService.java
│   │   ├── ParkingService.java
│   │   └── PlaceService.java
│   │
│   ├── com.lushnja.utils
│   │   └── LanguageManager.java
│   │
│   └── resources/
│       ├── css/
│       ├── data/
│       └── i18n/
│
├── .settings/
├── .classpath
├── .project
├── .gitignore
└── README.md
```

---

## How to Run the Project in Eclipse

### 1. Download or Clone the Repository

```bash
git clone https://github.com/bajram-haxhiu/Lushnja_Smart_City_Guide_DSA.git
```

### 2. Open Eclipse

Open Eclipse IDE.

### 3. Import the Project

Go to:

```text
File → Import → General → Existing Projects into Workspace
```

Select the project folder and click **Finish**.

### 4. Add Required Libraries

Make sure the following libraries are added to the project build path:

* JavaFX SDK
* Gson
* SLF4J API
* SLF4J Simple

JavaFX should be added to the **Modulepath**.

Gson and SLF4J should be added to the **Classpath**.

---

## JavaFX VM Arguments

To run the application, JavaFX VM arguments must be added in Eclipse.

Go to:

```text
Run → Run Configurations → Arguments → VM arguments
```

Example:

```text
--module-path "C:\path\to\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.web,javafx.media,javafx.swing
```

Replace the path with the real JavaFX SDK path on your computer.

---

## Gemini API Setup

The chatbot uses Gemini API.

The API key must not be written directly inside the code.

Instead, add it as an environment variable in Eclipse.

Go to:

```text
Run → Run Configurations → Environment → Add
```

Add:

```text
Name: GEMINI_API_KEY
Value: your_gemini_api_key
```

The project reads the key using:

```java
System.getenv("GEMINI_API_KEY")
```

If the API key is missing, the application will use a fallback response and will not crash.

---

## Important Security Note

Do not upload API keys to GitHub.

Do not write the API key directly in Java files.

Do not upload screenshots that contain API keys.

Recommended `.gitignore` content:

```gitignore
bin/
*.class
.env
config.properties
*.log
target/
*.launch
.metadata/
```

---

## DSA Concepts Explained

### Graph

The city is modeled as a graph.

Each location is a node, and each road is an edge.

### Dijkstra Algorithm

Dijkstra is used to find the shortest path between two locations based on weighted distances.

### BFS

BFS is used to explore the graph level by level from a selected starting node.

### DFS

DFS is used to explore the graph deeply by visiting connected nodes recursively or using a stack.

### Priority Queue

A priority queue is used in Dijkstra’s algorithm to always process the nearest unvisited node first.

---

## Academic Purpose

This project was developed for the **Data Structures and Algorithms** course.

It demonstrates how theoretical DSA concepts can be used to solve real-world problems such as city navigation, route finding, and graph traversal.

---

## Future Improvements

Possible future improvements include:

* Real-time maps API integration
* Real-time traffic data
* Live parking availability
* More detailed tourist recommendations
* User accounts and saved favorite places
* Mobile version
* More cities in Albania

---

## Author

**Bajram Haxhiu**

---

## License

This project is created for educational and academic purposes.
