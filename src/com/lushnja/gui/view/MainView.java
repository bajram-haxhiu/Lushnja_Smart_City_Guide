package com.lushnja.gui.view;

import com.lushnja.core.graph.CityGraph;
import com.lushnja.core.model.Location;
import com.lushnja.core.model.Route;
import com.lushnja.models.Place;
import com.lushnja.models.Place.PlaceCategory;
import com.lushnja.models.ParkingSpot;
import com.lushnja.services.*;
import com.lushnja.utils.LanguageManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

/**
 * MainView — Lushnja Smart City Guide (v3)
 *
 * Layout:
 * ┌──────────────────────────────────────────────────────────┐
 * │  HEADER: Title | Tourist | Emergency | Night | EN/SQ    │
 * ├────────────┬────────────────────────────────────────────┤
 * │  LEFT      │        MAP CANVAS (center)                 │
 * │  SIDEBAR   │                                            │
 * │  Search    │                                            │
 * │  Categories│                                            │
 * │  Route     │                                            │
 * │  BFS/DFS   │                                            │
 * ├────────────┴────────────────────────────────────────────┤
 * │  STATUS BAR                                             │
 * └──────────────────────────────────────────────────────────┘
 * RIGHT-SIDE TABS (Details | Parking | Favorites | DSA Results)
 * FLOATING CHATBOT BUTTON (bottom-right corner)
 */
public class MainView {

    private static final double W = 1280.0, H = 820.0;

    private final Stage           primaryStage;
    private final CityGraph       graph;
    private final PlaceService    placeService;
    private final NavigationService navService;
    private final DataLoaderService dataLoader;
    private final ParkingService  parkingService;
    private final ChatbotService  chatbotService;
    private final AIChatService   aiChatService;
    private final LanguageManager lm = LanguageManager.getInstance();

    // ── UI State ──────────────────────────────────────────────────────────────
    private boolean   darkMode        = false;
    private boolean   chatOpen        = false;

    // ── UI Components ─────────────────────────────────────────────────────────
    private MapCanvasView   mapCanvas;
    private TextField       searchField;
    private Label           statusLabel;
    private TextArea        detailsArea;
    private TextArea        dsaResultArea;
    private VBox            parkingListBox;
    private ComboBox<String> routeSrcCombo, routeDstCombo, routeAlgoCombo;
    private Label           routeResultLabel;
    private ComboBox<String> dsaStartCombo;

    // Rebuildable top-level containers
    private Label    headerTitle;
    private Label    headerSubtitle;
    private Button   darkBtn;
    private Button   langBtn;
    private Button   emergBtn;
    private Button   touristBtn;
    private Label    searchSectionLabel;
    private Label    catSectionLabel;
    private Label    routeSectionLabel;
    private Label    dsaSectionLabel;
    private Label    routeFromLabel;
    private Label    routeToLabel;
    private Label    routeAlgoLabel;
    private Button   routeFindBtn;
    private Button   routeClearBtn;
    private Button   bfsBtn;
    private Button   dfsBtn;
    private Button   dsaClearBtn;
    private FlowPane catFlowPane;

    // Floating chat
    private VBox     chatPanel;
    private Button   chatToggleBtn;
    private TextArea chatLog;
    private TextField chatInput;
    private Button   chatSendBtn;
    private Label    chatTitleLabel;
    private Label    chatInputPrompt;

    // Right tab references
    private TabPane  rightTabPane;
    private Tab      detailsTab, parkingTab, favoritesTab, dsaTab;
    private Label    detailsPromptLabel; // used as placeholder

    // ── Constructor ───────────────────────────────────────────────────────────

    public MainView(Stage stage) {
        this.primaryStage  = stage;
        this.graph         = new CityGraph();
        this.placeService  = new PlaceService();
        this.dataLoader    = new DataLoaderService(graph);
        this.parkingService= new ParkingService();
        this.aiChatService = new AIChatService();

        dataLoader.loadDefaultDataset();
        placeService.loadPlaces("/data/locations.csv");
        parkingService.loadParking("/data/parking.csv");

        this.navService    = new NavigationService(graph, placeService);
        this.chatbotService= new ChatbotService(placeService, parkingService, null);

        // Register language change listener — rebuild dynamic labels
        lm.addListener(this::onLanguageChanged);
    }

    // ── Show ──────────────────────────────────────────────────────────────────

    public void show() {
        Scene scene = buildScene();
        primaryStage.setTitle("Lushnja Smart City Guide");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.show();
        mapCanvas.renderGraph();
    }

    // ── Scene ─────────────────────────────────────────────────────────────────

    private Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setBottom(buildStatusBar());

        mapCanvas = new MapCanvasView(graph, placeService);
        mapCanvas.setOnPlaceClick(this::showPlaceDetails);

        root.setLeft(buildLeftPanel());
        root.setCenter(mapCanvas.getView());
        root.setRight(buildRightPanel());

        // Floating chat overlay on top of everything
        StackPane overlay = new StackPane(root, buildFloatingChat());
        overlay.setPickOnBounds(false); // allows clicks to pass through empty overlay areas

        Scene scene = new Scene(overlay, W, H);
        String css = getClass().getResource("/css/style.css") != null
                ? getClass().getResource("/css/style.css").toExternalForm() : null;
        if (css != null) scene.getStylesheets().add(css);

        return scene;
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        headerTitle    = new Label("🗺  Lushnja Smart City Guide");
        headerTitle.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#ffffff;");

        headerSubtitle = new Label(lm.get("app.subtitle"));
        headerSubtitle.setStyle("-fx-font-size:11px; -fx-text-fill:#a8c7e8;");

        VBox titles = new VBox(2, headerTitle, headerSubtitle);
        titles.setAlignment(Pos.CENTER_LEFT);

        touristBtn = headerBtn("🗺 " + lm.get("header.tourist"), "#2e7d32", "white");
        emergBtn   = headerBtn("🚨 " + lm.get("header.emergency"), "#c0392b", "white");
        emergBtn.setStyle(emergBtn.getStyle() + " -fx-font-weight:bold;");
        darkBtn    = headerBtn("🌙 " + lm.get("header.darkmode"), "#334e68", "white");
        langBtn    = headerBtn(lm.get("app.lang.button"), "#e67e22", "white");
        langBtn.setStyle("-fx-background-color:#e67e22; -fx-text-fill:white; -fx-cursor:hand;" +
                         "-fx-font-weight:bold; -fx-background-radius:8; -fx-padding:6 14;");

        touristBtn.setOnAction(e -> showTouristMode());
        emergBtn.setOnAction(e -> showEmergencyMode());
        darkBtn.setOnAction(e -> toggleDarkMode());
        langBtn.setOnAction(e -> { lm.toggleLanguage(); langBtn.setText(lm.get("app.lang.button")); });

        HBox spacer = new HBox(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(16, titles, spacer, touristBtn, emergBtn, darkBtn, langBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 20, 12, 20));
        header.setStyle("-fx-background-color: linear-gradient(to right, #1a3a4a, #2a9d8f);");
        return header;
    }

    private Button headerBtn(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg +
                   "; -fx-cursor:hand; -fx-background-radius:8; -fx-padding:6 14;");
        return b;
    }

    // ── Left Panel ────────────────────────────────────────────────────────────

    private VBox buildLeftPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(12));
        panel.setPrefWidth(260);
        panel.setMaxWidth(260);
        panel.setStyle("-fx-background-color:#f8f4ef; -fx-border-color:#ddd; -fx-border-width:0 1 0 0;");

        // ── Search ────────────────────────────────────────────────────────────
        searchSectionLabel = sectionLabel("🔍 " + lm.get("search.section"));
        panel.getChildren().add(searchSectionLabel);

        searchField = new TextField();
        searchField.setPromptText(lm.get("search.prompt"));
        searchField.setStyle("-fx-background-radius:20; -fx-padding:6 12; -fx-font-size:12px;");
        searchField.textProperty().addListener((obs, o, n) -> onSearch(n));

        Button clearSearch = new Button("✕");
        clearSearch.setStyle("-fx-background-color:transparent; -fx-cursor:hand; -fx-text-fill:#888;");
        clearSearch.setOnAction(e -> { searchField.clear(); mapCanvas.clearFilter(); });

        HBox searchRow = new HBox(5, searchField, clearSearch);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        panel.getChildren().add(searchRow);

        // ── Category Filters ──────────────────────────────────────────────────
        catSectionLabel = sectionLabel("📂 " + lm.get("categories.section"));
        panel.getChildren().add(catSectionLabel);
        catFlowPane = buildCategoryFilters();
        panel.getChildren().add(catFlowPane);

        // ── Route Finder ──────────────────────────────────────────────────────
        panel.getChildren().add(new Separator());
        routeSectionLabel = sectionLabel("🧭 " + lm.get("route.section"));
        panel.getChildren().add(routeSectionLabel);
        panel.getChildren().addAll(buildRoutePanel());

        // ── DSA Panel ─────────────────────────────────────────────────────────
        panel.getChildren().add(new Separator());
        dsaSectionLabel = sectionLabel("🔬 " + lm.get("dsa.section"));
        panel.getChildren().add(dsaSectionLabel);
        panel.getChildren().addAll(buildDSAPanel());

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");
        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        wrapper.setStyle("-fx-background-color:#f8f4ef;");
        wrapper.setPrefWidth(260);
        wrapper.setMaxWidth(260);
        return wrapper;
    }

    private FlowPane buildCategoryFilters() {
        FlowPane flow = new FlowPane(5, 5);
        String[][] cats = {
            {"Restaurants","Restorante","🍽"},
            {"Cafes","Kafene","☕"},
            {"Hotels","Hotele","🏨"},
            {"Hospitals","Spitale","🏥"},
            {"Pharmacies","Farmaci","💊"},
            {"Banks","Banka","🏦"},
            {"Schools","Shkolla","🏫"},
            {"Parks","Parke","🌳"},
            {"Bus Stops","Stacione Autobusi","🚌"},
            {"Taxi","Pika Taksie","🚕"},
            {"Police","Policia","👮"},
            {"Parking","Parkime","🅿"},
        };
        Button allBtn = catBtn(lm.get("categories.all"));
        allBtn.setOnAction(e -> { mapCanvas.clearFilter(); setStatus(lm.get("status.filterall")); });
        flow.getChildren().add(allBtn);

        for (String[] cat : cats) {
            // cat[0]=EN name, cat[1]=SQ/internal name, cat[2]=emoji
            Button btn = catBtn(cat[2] + " " + (lm.isEnglish() ? cat[0] : cat[1]));
            final String sqName = cat[1];
            btn.setOnAction(e -> {
                PlaceCategory pc = findCategory(sqName);
                mapCanvas.setFilterCategory(pc);
                setStatus(lm.get("status.filter", lm.isEnglish() ? cat[0] : sqName));
            });
            flow.getChildren().add(btn);
        }
        return flow;
    }

    private List<javafx.scene.Node> buildRoutePanel() {
        List<javafx.scene.Node> nodes = new ArrayList<>();
        List<String> names = placeService.getAllPlaces().stream()
                .sorted(Comparator.comparing(Place::getName))
                .map(p -> p.getId() + " — " + p.getName())
                .toList();

        routeSrcCombo = new ComboBox<>(FXCollections.observableArrayList(names));
        routeSrcCombo.setPromptText(lm.get("route.from.prompt"));
        routeSrcCombo.setMaxWidth(Double.MAX_VALUE);

        routeDstCombo = new ComboBox<>(FXCollections.observableArrayList(names));
        routeDstCombo.setPromptText(lm.get("route.to.prompt"));
        routeDstCombo.setMaxWidth(Double.MAX_VALUE);

        routeAlgoCombo = new ComboBox<>(FXCollections.observableArrayList(
            lm.get("algo.dijkstra.dist"),
            lm.get("algo.dijkstra.time"),
            lm.get("algo.bfs"),
            lm.get("algo.dfs")
        ));
        routeAlgoCombo.getSelectionModel().selectFirst();
        routeAlgoCombo.setMaxWidth(Double.MAX_VALUE);

        routeFindBtn = new Button("📍 " + lm.get("route.find"));
        routeFindBtn.setStyle("-fx-background-color:#2a9d8f; -fx-text-fill:white; -fx-font-weight:bold;" +
                              "-fx-cursor:hand; -fx-background-radius:8; -fx-font-size:12px;");
        routeFindBtn.setMaxWidth(Double.MAX_VALUE);
        routeFindBtn.setOnAction(e -> onFindRoute());

        routeClearBtn = new Button("✖ " + lm.get("route.clear"));
        routeClearBtn.setStyle("-fx-background-color:#e0e0e0; -fx-cursor:hand; -fx-background-radius:8;");
        routeClearBtn.setMaxWidth(Double.MAX_VALUE);
        routeClearBtn.setOnAction(e -> { mapCanvas.clearRoute(); routeResultLabel.setText(""); dsaResultArea.setText(""); });

        routeResultLabel = new Label("");
        routeResultLabel.setWrapText(true);
        routeResultLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#264653;");

        routeFromLabel  = new Label(lm.get("route.from"));
        routeToLabel    = new Label(lm.get("route.to"));
        routeAlgoLabel  = new Label(lm.get("route.algorithm"));

        HBox btnRow = new HBox(6, routeFindBtn, routeClearBtn);
        HBox.setHgrow(routeFindBtn, Priority.ALWAYS);

        nodes.addAll(List.of(routeFromLabel, routeSrcCombo,
                             routeToLabel,   routeDstCombo,
                             routeAlgoLabel, routeAlgoCombo,
                             btnRow, routeResultLabel));
        return nodes;
    }

    private List<javafx.scene.Node> buildDSAPanel() {
        List<javafx.scene.Node> nodes = new ArrayList<>();
        List<String> names = placeService.getAllPlaces().stream()
                .sorted(Comparator.comparing(Place::getName))
                .map(p -> p.getId() + " — " + p.getName())
                .toList();

        dsaStartCombo = new ComboBox<>(FXCollections.observableArrayList(names));
        dsaStartCombo.setPromptText(lm.get("dsa.start.prompt"));
        dsaStartCombo.setMaxWidth(Double.MAX_VALUE);

        bfsBtn = new Button("🔵 " + lm.get("dsa.bfs"));
        bfsBtn.setStyle("-fx-background-color:#2980b9; -fx-text-fill:white; -fx-cursor:hand;" +
                        "-fx-background-radius:8; -fx-font-size:12px;");
        bfsBtn.setMaxWidth(Double.MAX_VALUE);
        bfsBtn.setOnAction(e -> onTraversal(dsaStartCombo.getValue(), "BFS"));

        dfsBtn = new Button("🟢 " + lm.get("dsa.dfs"));
        dfsBtn.setStyle("-fx-background-color:#27ae60; -fx-text-fill:white; -fx-cursor:hand;" +
                        "-fx-background-radius:8; -fx-font-size:12px;");
        dfsBtn.setMaxWidth(Double.MAX_VALUE);
        dfsBtn.setOnAction(e -> onTraversal(dsaStartCombo.getValue(), "DFS"));

        dsaClearBtn = new Button("✖ " + lm.get("dsa.clear"));
        dsaClearBtn.setStyle("-fx-background-color:#e0e0e0; -fx-cursor:hand; -fx-background-radius:8;");
        dsaClearBtn.setMaxWidth(Double.MAX_VALUE);
        dsaClearBtn.setOnAction(e -> { mapCanvas.clearTraversal(); dsaResultArea.setText(""); detailsArea.setText(""); });

        nodes.addAll(List.of(dsaStartCombo, bfsBtn, dfsBtn, dsaClearBtn));
        return nodes;
    }

    // ── Right Panel ───────────────────────────────────────────────────────────

    private TabPane buildRightPanel() {
        rightTabPane = new TabPane();
        rightTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        rightTabPane.setPrefWidth(315);
        rightTabPane.setMaxWidth(315);

        detailsTab   = buildDetailsTab();
        parkingTab   = buildParkingTab();
        favoritesTab = buildFavoritesTab();
        dsaTab       = buildDSAResultTab();

        rightTabPane.getTabs().addAll(detailsTab, parkingTab, favoritesTab, dsaTab);
        return rightTabPane;
    }

    private Tab buildDetailsTab() {
        detailsArea = new TextArea();
        detailsArea.setEditable(false);
        detailsArea.setWrapText(true);
        detailsArea.setStyle("-fx-font-size:12px;");
        detailsArea.setPromptText(lm.get("details.prompt"));
        VBox box = new VBox(detailsArea);
        VBox.setVgrow(detailsArea, Priority.ALWAYS);
        box.setPadding(new Insets(8));
        Tab t = new Tab("📍 " + lm.get("tab.details"), box);
        t.setClosable(false);
        return t;
    }

    private Tab buildParkingTab() {
        parkingListBox = new VBox(7);
        parkingListBox.setPadding(new Insets(5));
        refreshParkingList();

        Button simBtn = new Button("🔄 " + lm.get("parking.simulate"));
        simBtn.setStyle("-fx-background-color:#e67e22; -fx-text-fill:white; -fx-cursor:hand;" +
                        "-fx-background-radius:8; -fx-font-size:12px;");
        simBtn.setMaxWidth(Double.MAX_VALUE);
        simBtn.setOnAction(e -> {
            parkingService.simulateUpdate();
            refreshParkingList();
            setStatus(lm.get("parking.simulated"));
        });

        ScrollPane scroll = new ScrollPane(parkingListBox);
        scroll.setFitToWidth(true);
        VBox box = new VBox(8, simBtn, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        box.setPadding(new Insets(8));
        Tab t = new Tab("🅿 " + lm.get("tab.parking"), box);
        t.setClosable(false);
        return t;
    }

    private Tab buildFavoritesTab() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));

        Label hint = new Label(lm.get("favorites.hint"));
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill:#777; -fx-font-size:11px;");

        TextArea favList = new TextArea();
        favList.setEditable(false);
        favList.setPromptText(lm.get("favorites.empty"));
        VBox.setVgrow(favList, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 " + lm.get("favorites.refresh"));
        refreshBtn.setStyle("-fx-cursor:hand; -fx-background-radius:8;");
        refreshBtn.setOnAction(e -> {
            List<Place> favs = placeService.getFavorites();
            if (favs.isEmpty()) { favList.setText(lm.get("favorites.empty")); return; }
            StringBuilder sb = new StringBuilder("⭐ " + lm.get("favorites.title") + "\n\n");
            for (Place p : favs) {
                sb.append("• ").append(p.getName()).append("\n");
                sb.append("  ").append(p.getAddress()).append("\n\n");
            }
            favList.setText(sb.toString());
        });

        box.getChildren().addAll(hint, refreshBtn, favList);
        Tab t = new Tab("⭐ " + lm.get("tab.favorites"), box);
        t.setClosable(false);
        return t;
    }

    private Tab buildDSAResultTab() {
        dsaResultArea = new TextArea();
        dsaResultArea.setEditable(false);
        dsaResultArea.setWrapText(true);
        dsaResultArea.setStyle("-fx-font-size:12px; -fx-font-family: 'Courier New', monospace;");
        dsaResultArea.setPromptText(lm.get("dsa.result.title") + "...");

        VBox box = new VBox(dsaResultArea);
        VBox.setVgrow(dsaResultArea, Priority.ALWAYS);
        box.setPadding(new Insets(8));
        Tab t = new Tab("🔬 " + lm.get("tab.dsa"), box);
        t.setClosable(false);
        return t;
    }

    // ── Status Bar ────────────────────────────────────────────────────────────

    private HBox buildStatusBar() {
        statusLabel = new Label(buildStatusText(""));
        statusLabel.setStyle("-fx-text-fill:#555; -fx-font-size:11px;");
        HBox bar = new HBox(statusLabel);
        bar.setPadding(new Insets(5, 14, 5, 14));
        bar.setStyle("-fx-background-color:#f0ebe3; -fx-border-color:#ddd; -fx-border-width:1 0 0 0;");
        return bar;
    }

    // ── Floating Chatbot ──────────────────────────────────────────────────────

    private StackPane buildFloatingChat() {
        // ── Chat Panel ────────────────────────────────────────────────────────
        chatLog = new TextArea();
        chatLog.setEditable(false);
        chatLog.setWrapText(true);
        chatLog.setStyle("-fx-font-size:12px;");
        chatLog.setPrefRowCount(12);
        updateChatWelcome();

        chatInput = new TextField();
        chatInput.setStyle("-fx-background-radius:15; -fx-padding:6 12; -fx-font-size:12px;");
        chatInput.setOnAction(e -> sendChatMessage());

        chatSendBtn = new Button("➤");
        chatSendBtn.setStyle("-fx-background-color:#2a9d8f; -fx-text-fill:white; -fx-cursor:hand;" +
                             "-fx-background-radius:15; -fx-font-size:14px;");
        chatSendBtn.setOnAction(e -> sendChatMessage());

        HBox inputRow = new HBox(6, chatInput, chatSendBtn);
        HBox.setHgrow(chatInput, Priority.ALWAYS);
        inputRow.setPadding(new Insets(4, 4, 4, 4));

        chatTitleLabel = new Label("💬 " + lm.get("chatbot.title"));
        chatTitleLabel.setStyle("-fx-font-weight:bold; -fx-font-size:13px; -fx-text-fill:#fff;");

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#fff; -fx-cursor:hand; -fx-font-size:14px;");
        closeBtn.setOnAction(e -> toggleChat());

        HBox chatHeader = new HBox(chatTitleLabel, new Pane() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, closeBtn);
        chatHeader.setAlignment(Pos.CENTER_LEFT);
        chatHeader.setPadding(new Insets(8, 10, 8, 12));
        chatHeader.setStyle("-fx-background-color:#264653; -fx-background-radius:10 10 0 0;");

        chatPanel = new VBox(0, chatHeader, chatLog, inputRow);
        chatPanel.setPrefWidth(360);
        chatPanel.setPrefHeight(400);
        chatPanel.setMaxWidth(360);
        chatPanel.setMaxHeight(400);
        chatPanel.setStyle("-fx-background-color:white; -fx-background-radius:10;" +
                           "-fx-border-color:#ccc; -fx-border-radius:10;" +
                           "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 10, 0, 0, 4);");
        VBox.setVgrow(chatLog, Priority.ALWAYS);
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);

        // ── Toggle Button ─────────────────────────────────────────────────────
        chatToggleBtn = new Button("💬  " + lm.get("chatbot.title"));
        chatToggleBtn.setStyle(
            "-fx-background-color:#264653; -fx-text-fill:white; -fx-cursor:hand;" +
            "-fx-background-radius:25; -fx-padding:10 20; -fx-font-size:13px;" +
            "-fx-font-weight:bold;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 3);");
        chatToggleBtn.setOnAction(e -> toggleChat());

        // ── Overlay Container ─────────────────────────────────────────────────
        VBox chatStack = new VBox(8, chatPanel, chatToggleBtn);
        chatStack.setAlignment(Pos.BOTTOM_RIGHT);
        chatStack.setPickOnBounds(false);

        StackPane overlay = new StackPane(chatStack);
        overlay.setAlignment(Pos.BOTTOM_RIGHT);
        overlay.setPadding(new Insets(0, 20, 20, 0));
        overlay.setPickOnBounds(false); // allow clicks to pass through transparent areas

        return overlay;
    }

    private void toggleChat() {
        chatOpen = !chatOpen;
        chatPanel.setVisible(chatOpen);
        chatPanel.setManaged(chatOpen);
        if (chatOpen) {
            chatInput.requestFocus();
            updateChatWelcome();
        }
    }

    private void updateChatWelcome() {
        if (chatLog != null && chatLog.getText().isBlank()) {
            chatLog.setText("── " + lm.get("chatbot.title") + " ──\n\n" +
                            lm.get("chatbot.welcome") + "\n\n");
        }
    }

    // ── Event Handlers ────────────────────────────────────────────────────────

    private void onSearch(String query) {
        if (query == null || query.isBlank()) { mapCanvas.clearFilter(); return; }
        List<Place> results = placeService.search(query);
        if (results.isEmpty()) {
            setStatus(lm.get("search.notfound", query));
            mapCanvas.highlightPlaces(List.of());
        } else {
            mapCanvas.highlightPlaces(results.stream().map(Place::getId).toList());
            setStatus(lm.get("search.found", results.size(), query));
        }
    }

    private void onFindRoute() {
        String src = routeSrcCombo.getValue();
        String dst = routeDstCombo.getValue();
        if (src == null || dst == null) {
            routeResultLabel.setText("⚠ " + lm.get("route.warn.select")); return;
        }
        String srcId = src.split(" — ")[0].trim();
        String dstId = dst.split(" — ")[0].trim();
        if (srcId.equals(dstId)) { routeResultLabel.setText("⚠ " + lm.get("route.warn.same")); return; }

        String algo = routeAlgoCombo.getValue();
        setStatus(lm.get("route.calculating"));
        routeResultLabel.setText("⏳ " + lm.get("route.calculating"));

        // Determine algorithm type from current combo label
        String algoKey = resolveAlgoKey(algo);

        Thread.ofVirtual().start(() -> {
            Optional<Route> result;
            try {
                result = switch (algoKey) {
                    case "DIJKSTRA_TIME" -> navService.findFastestRoute(srcId, dstId);
                    case "BFS"           -> navService.findBFSRoute(srcId, dstId);
                    case "DFS"           -> navService.findDFSRoute(srcId, dstId);
                    default              -> navService.findShortestRoute(srcId, dstId);
                };
            } catch (Exception ex) { result = Optional.empty(); }

            String srcName = src.contains(" — ") ? src.split(" — ")[1].trim() : src;
            String dstName = dst.contains(" — ") ? dst.split(" — ")[1].trim() : dst;
            Optional<Route> finalResult = result;

            Platform.runLater(() -> {
                if (finalResult.isEmpty()) {
                    routeResultLabel.setText("❌ " + lm.get("route.notfound"));
                    setStatus(lm.get("route.notfound"));
                } else {
                    Route r = finalResult.get();
                    mapCanvas.showRoute(r);
                    routeResultLabel.setText(String.format(
                        "✅ %s\n📏 %s %.2f %s\n⏱ %s %.0f %s\n🔗 %s %d",
                        lm.get("route.result.found"),
                        lm.get("route.result.distance"), r.getTotalDistanceKm(), lm.get("directions.km"),
                        lm.get("route.result.time"), r.getTotalTimeMinutes(), lm.get("directions.min"),
                        lm.get("route.result.stops"), r.getLocationPath().size()
                    ));
                    setStatus(lm.get("route.success"));

                    // Rich DSA output in DSA tab
                    String dsaOut = buildDSARouteOutput(algoKey, srcName, dstName, r);
                    dsaResultArea.setText(dsaOut);
                    rightTabPane.getSelectionModel().select(dsaTab);

                    // Also show directions in details tab
                    detailsArea.setText(buildDirections(r));
                }
            });
        });
    }

    private void onTraversal(String startVal, String type) {
        if (startVal == null) { setStatus(lm.get("dsa.start.prompt")); return; }
        String startId   = startVal.split(" — ")[0].trim();
        String startName = startVal.contains(" — ") ? startVal.split(" — ")[1].trim() : startVal;

        if (!graph.containsLocation(startId)) { setStatus("Invalid node."); return; }

        List<Location> order = type.equals("BFS")
            ? navService.bfsTraversal(startId)
            : navService.dfsTraversal(startId);

        mapCanvas.showTraversal(order.stream().map(Location::getId).toList());

        // Rich DSA output
        String dsaOut = buildDSATraversalOutput(type, startName, order);
        dsaResultArea.setText(dsaOut);
        rightTabPane.getSelectionModel().select(dsaTab);
        setStatus(type + " traversal: " + order.size() + " " + lm.get("dsa.traversal.visited") + ".");
    }

    private void showPlaceDetails(Place p) {
        boolean isFav = placeService.isFavorite(p.getId());
        StringBuilder sb = new StringBuilder();
        sb.append("📍 ").append(p.getName()).append("\n");
        sb.append("─".repeat(32)).append("\n\n");
        sb.append(lm.get("details.category")).append(" ").append(p.getCategory().getAlbanianName()).append("\n");
        sb.append(lm.get("details.address")).append("  ").append(p.getAddress()).append("\n");
        sb.append(lm.get("details.rating")).append("  ").append(p.getRating()).append("/5  ");
        sb.append("★".repeat((int)p.getRating())).append("\n");
        sb.append(lm.get("details.hours")).append("   ").append(p.getOpeningHours()).append("\n");
        sb.append(lm.get("details.coords")).append(" ").append(
            String.format("%.4f°N, %.4f°E", p.getLatitude(), p.getLongitude())).append("\n\n");
        sb.append(lm.get("details.description")).append("\n").append(p.getDescription()).append("\n\n");
        sb.append(isFav
            ? "★ " + lm.get("details.favorite.added") + " — click to remove"
            : "☆ " + lm.get("details.favorite.add"));
        detailsArea.setText(sb.toString());
        detailsArea.setOnMouseClicked(e -> {
            if (detailsArea.getText().contains("☆")) {
                placeService.addFavorite(p.getId());
                setStatus(lm.get("details.favorite.added") + ": " + p.getName());
                showPlaceDetails(p);
            }
        });
        rightTabPane.getSelectionModel().select(detailsTab);
        setStatus("Showing: " + p.getName());
    }

    private void sendChatMessage() {
        String msg = chatInput.getText().trim();
        if (msg.isBlank()) return;
        chatInput.clear();
        chatInput.setDisable(true);
        chatSendBtn.setDisable(true);

        chatLog.appendText("\n" + lm.get("chatbot.user") + " " + msg + "\n");
        chatLog.appendText(lm.get("chatbot.thinking") + "\n");
        chatLog.setScrollTop(Double.MAX_VALUE);

        String lang = lm.getCurrentLanguage();
        Thread.ofVirtual().start(() -> {
            String reply = aiChatService.askGemini(msg, lang);
            Platform.runLater(() -> {
                // Remove "thinking" line
                String current = chatLog.getText();
                int thinkIdx = current.lastIndexOf(lm.get("chatbot.thinking"));
                if (thinkIdx >= 0) chatLog.setText(current.substring(0, thinkIdx));

                chatLog.appendText(lm.get("chatbot.bot") + "\n" + reply + "\n");
                chatLog.appendText("─".repeat(36) + "\n");
                chatLog.setScrollTop(Double.MAX_VALUE);
                chatInput.setDisable(false);
                chatSendBtn.setDisable(false);
                chatInput.requestFocus();
            });
        });
    }

    private void refreshParkingList() {
        parkingListBox.getChildren().clear();
        for (ParkingSpot spot : parkingService.getAllSpots()) {
            String bgColor = switch (spot.getStatus()) {
                case I_LIRE         -> "#d4f4e0";
                case POTHUAJSE_PLOT -> "#fff3cd";
                case PLOT           -> "#fde8e8";
            };
            String statusLabel = switch (spot.getStatus()) {
                case I_LIRE         -> lm.get("parking.status.free");
                case POTHUAJSE_PLOT -> lm.get("parking.status.almost");
                case PLOT           -> lm.get("parking.status.full");
            };
            String dot = switch (spot.getStatus()) {
                case I_LIRE         -> "🟢";
                case POTHUAJSE_PLOT -> "🟡";
                case PLOT           -> "🔴";
            };

            VBox card = new VBox(3);
            card.setPadding(new Insets(7, 10, 7, 10));
            card.setStyle("-fx-background-color:" + bgColor +
                          "; -fx-background-radius:8; -fx-border-color:#ccc; -fx-border-radius:8;");

            Label name = new Label("🅿  " + spot.getName());
            name.setStyle("-fx-font-weight:bold; -fx-font-size:12px;");
            Label addr = new Label(spot.getAddress());
            addr.setStyle("-fx-font-size:11px; -fx-text-fill:#666;");
            Label status = new Label(dot + " " + statusLabel + " — " +
                spot.getFreeSpaces() + "/" + spot.getTotalSpaces() + " " + lm.get("parking.spaces"));
            status.setStyle("-fx-font-size:11px;");
            Label price = new Label("💰 " + spot.getPrice());
            price.setStyle("-fx-font-size:11px; -fx-text-fill:#555;");

            card.getChildren().addAll(name, addr, status, price);
            parkingListBox.getChildren().add(card);
        }
    }

    private void showEmergencyMode() {
        Map<String, Optional<Place>> emergency = navService.findEmergencyPlaces("LOC01");
        StringBuilder sb = new StringBuilder();
        sb.append("🚨 " + lm.get("emergency.title") + "\n");
        sb.append("═".repeat(34) + "\n\n");
        sb.append("📞 " + lm.get("emergency.numbers") + "\n");
        sb.append("  🚑 " + lm.get("emergency.ambulance") + "\n");
        sb.append("  👮 " + lm.get("emergency.police") + "\n");
        sb.append("  🔥 " + lm.get("emergency.fire") + "\n\n");
        sb.append("📍 " + lm.get("emergency.nearest") + "\n\n");

        String[] keys = {"Spitali", "Farmacia", "Policia", "Taksi"};
        String[] icons = {"🏥", "💊", "👮", "🚕"};
        int i = 0;
        for (Map.Entry<String, Optional<Place>> e : emergency.entrySet()) {
            String icon = i < icons.length ? icons[i++] : "📍";
            sb.append(icon + " " + e.getKey() + ":\n");
            e.getValue().ifPresentOrElse(
                p -> sb.append("  → " + p.getName() + "\n     " + p.getAddress() + "\n\n"),
                () -> sb.append("  → " + lm.get("emergency.notfound") + "\n\n")
            );
        }
        List<String> ids = new ArrayList<>();
        for (Optional<Place> op : emergency.values()) op.ifPresent(p -> ids.add(p.getId()));
        mapCanvas.highlightPlaces(ids);
        detailsArea.setText(sb.toString());
        rightTabPane.getSelectionModel().select(detailsTab);
        setStatus(lm.get("status.emergency"));
    }

    private void showTouristMode() {
        List<Place> recs = placeService.getTouristRecommendations();
        StringBuilder sb = new StringBuilder();
        sb.append("🗺  " + lm.get("tourist.title") + "\n");
        sb.append("═".repeat(34) + "\n\n");
        sb.append(lm.get("tourist.welcome") + "\n\n");

        String[] times  = {"08:00","09:30","11:00","12:30","14:00","15:30","17:00","19:30"};
        String[] ticons = {"🌅","🏛","📸","☕","🍽","🌳","🕌","🏨"};
        for (int i = 0; i < recs.size(); i++) {
            Place p = recs.get(i);
            String t = i < times.length ? times[i] : "";
            String ic= i < ticons.length ? ticons[i] : "📍";
            sb.append(ic + " " + t + "  " + p.getName() + "\n");
            sb.append("     " + p.getAddress() + "\n");
            int len = Math.min(65, p.getDescription().length());
            if (len > 0) sb.append("     " + p.getDescription().substring(0, len) + "…\n");
            sb.append("\n");
        }
        sb.append("💡 " + lm.get("tourist.tip"));

        detailsArea.setText(sb.toString());
        mapCanvas.highlightPlaces(recs.stream().map(Place::getId).toList());
        rightTabPane.getSelectionModel().select(detailsTab);
        setStatus(lm.get("status.tourist", recs.size()));
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        mapCanvas.setDarkMode(darkMode);
        if (darkMode) {
            darkBtn.setText("☀ " + lm.get("header.lightmode"));
            darkBtn.setStyle("-fx-background-color:#f39c12; -fx-text-fill:black; -fx-cursor:hand; -fx-background-radius:8; -fx-padding:6 14;");
        } else {
            darkBtn.setText("🌙 " + lm.get("header.darkmode"));
            darkBtn.setStyle("-fx-background-color:#334e68; -fx-text-fill:white; -fx-cursor:hand; -fx-background-radius:8; -fx-padding:6 14;");
        }
        setStatus(darkMode ? lm.get("status.dark") : lm.get("status.light"));
    }

    // ── DSA Output Builders ───────────────────────────────────────────────────

    private String buildDSARouteOutput(String algoKey, String srcName, String dstName, Route r) {
        boolean isBFS = algoKey.equals("BFS");
        boolean isDFS = algoKey.equals("DFS");
        StringBuilder sb = new StringBuilder();

        if (isBFS) {
            sb.append("══════════════════════════════════\n");
            sb.append(" " + lm.get("algo.output.bfs") + "\n");
            sb.append("══════════════════════════════════\n\n");
        } else if (isDFS) {
            sb.append("══════════════════════════════════\n");
            sb.append(" " + lm.get("algo.output.dfs") + "\n");
            sb.append("══════════════════════════════════\n\n");
        } else {
            sb.append("══════════════════════════════════\n");
            sb.append(" " + lm.get("algo.output.dijkstra") + "\n");
            sb.append("══════════════════════════════════\n\n");
        }

        sb.append(lm.get("algo.output.start") + " " + srcName + "\n");
        sb.append(lm.get("algo.output.destination") + " " + dstName + "\n\n");

        // Path
        sb.append(lm.get("algo.output.path") + "\n");
        var locs = r.getLocationPath();
        for (int i = 0; i < locs.size(); i++) {
            if (i == 0)               sb.append("  ▶ " + locs.get(i).getName() + "\n");
            else if (i == locs.size()-1) sb.append("  ◉ " + locs.get(i).getName() + "\n");
            else                      sb.append("  → " + locs.get(i).getName() + "\n");
        }

        sb.append("\n");
        sb.append(lm.get("algo.output.total.dist") + " " + String.format("%.2f km\n", r.getTotalDistanceKm()));
        sb.append(lm.get("algo.output.total.time") + " " + String.format("%.0f min\n", r.getTotalTimeMinutes()));
        sb.append(lm.get("algo.output.visited") + " " + locs.size() + "\n\n");

        sb.append("──────────────────────────────────\n");
        sb.append(lm.get("algo.output.complexity") + "\n");
        if (isBFS || isDFS) {
            sb.append("  " + lm.get("algo.output.complexity.bfsdfs") + "\n");
        } else {
            sb.append("  " + lm.get("algo.output.complexity.dijkstra") + "\n");
        }

        return sb.toString();
    }

    private String buildDSATraversalOutput(String type, String startName, List<Location> order) {
        StringBuilder sb = new StringBuilder();
        boolean isBFS = type.equals("BFS");

        sb.append("══════════════════════════════════\n");
        sb.append(" " + (isBFS ? lm.get("algo.output.bfs") : lm.get("algo.output.dfs")) + "\n");
        sb.append("══════════════════════════════════\n\n");
        sb.append(lm.get("algo.output.start") + " " + startName + "\n");
        sb.append(lm.get("algo.output.visited") + " " + order.size() + "\n\n");
        sb.append(lm.get("algo.output.traversal") + "\n");

        for (int i = 0; i < order.size(); i++) {
            sb.append(String.format("  %2d. %s\n", i + 1, order.get(i).getName()));
        }

        sb.append("\n──────────────────────────────────\n");
        sb.append(lm.get("algo.output.complexity") + " " + lm.get("algo.output.complexity.bfsdfs") + "\n");

        return sb.toString();
    }

    private String buildDirections(Route route) {
        if (route.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        var locs  = route.getLocationPath();
        var roads = route.getRoadPath();
        sb.append("═".repeat(32) + "\n");
        sb.append(" " + lm.get("directions.title") + "\n");
        sb.append("═".repeat(32) + "\n\n");
        sb.append(lm.get("directions.from") + " " + locs.get(0).getName() + "\n");
        sb.append(lm.get("directions.to") + " " + locs.get(locs.size()-1).getName() + "\n");
        sb.append(String.format(lm.get("directions.distance") + " %.2f %s\n",
            route.getTotalDistanceKm(), lm.get("directions.km")));
        sb.append(String.format(lm.get("directions.time") + " %.0f %s\n\n",
            route.getTotalTimeMinutes(), lm.get("directions.min")));
        for (int i = 0; i < roads.size(); i++) {
            var road = roads.get(i);
            sb.append(String.format("%d. %s %s\n   → %s (%.0f %s)\n",
                i+1, lm.get("directions.take"), road.getRoadName(),
                road.getDestination().getName(),
                road.getBaseDistance() * 1000, lm.get("directions.m")));
        }
        sb.append("\n✅ " + lm.get("directions.arrived") + "\n");
        return sb.toString();
    }

    // ── Language Change Callback ──────────────────────────────────────────────

    private void onLanguageChanged() {
        Platform.runLater(() -> {
            headerSubtitle.setText(lm.get("app.subtitle"));
            touristBtn.setText("🗺 " + lm.get("header.tourist"));
            emergBtn.setText("🚨 " + lm.get("header.emergency"));
            darkBtn.setText(darkMode ? "☀ " + lm.get("header.lightmode") : "🌙 " + lm.get("header.darkmode"));

            searchSectionLabel.setText("🔍 " + lm.get("search.section"));
            searchField.setPromptText(lm.get("search.prompt"));
            catSectionLabel.setText("📂 " + lm.get("categories.section"));
            routeSectionLabel.setText("🧭 " + lm.get("route.section"));
            dsaSectionLabel.setText("🔬 " + lm.get("dsa.section"));

            routeFromLabel.setText(lm.get("route.from"));
            routeToLabel.setText(lm.get("route.to"));
            routeAlgoLabel.setText(lm.get("route.algorithm"));
            routeFindBtn.setText("📍 " + lm.get("route.find"));
            routeClearBtn.setText("✖ " + lm.get("route.clear"));
            routeSrcCombo.setPromptText(lm.get("route.from.prompt"));
            routeDstCombo.setPromptText(lm.get("route.to.prompt"));

            // Rebuild algorithm combo
            int prevIdx = routeAlgoCombo.getSelectionModel().getSelectedIndex();
            routeAlgoCombo.getItems().setAll(
                lm.get("algo.dijkstra.dist"), lm.get("algo.dijkstra.time"),
                lm.get("algo.bfs"), lm.get("algo.dfs"));
            routeAlgoCombo.getSelectionModel().select(Math.max(0, prevIdx));

            bfsBtn.setText("🔵 " + lm.get("dsa.bfs"));
            dfsBtn.setText("🟢 " + lm.get("dsa.dfs"));
            dsaClearBtn.setText("✖ " + lm.get("dsa.clear"));
            dsaStartCombo.setPromptText(lm.get("dsa.start.prompt"));

            detailsTab.setText("📍 " + lm.get("tab.details"));
            parkingTab.setText("🅿 " + lm.get("tab.parking"));
            favoritesTab.setText("⭐ " + lm.get("tab.favorites"));
            dsaTab.setText("🔬 " + lm.get("tab.dsa"));

            detailsArea.setPromptText(lm.get("details.prompt"));
            dsaResultArea.setPromptText(lm.get("dsa.result.title") + "...");

            chatTitleLabel.setText("💬 " + lm.get("chatbot.title"));
            chatToggleBtn.setText("💬  " + lm.get("chatbot.title"));
            chatInput.setPromptText(lm.get("chatbot.input.prompt"));

            // Rebuild category buttons
            catFlowPane.getChildren().clear();
            catFlowPane.getChildren().addAll(buildCategoryFilters().getChildren());

            // Refresh parking labels
            refreshParkingList();

            updateStatusBar();
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight:bold; -fx-font-size:12px; -fx-text-fill:#1a3a4a; -fx-padding:4 0 2 0;");
        return l;
    }

    private Button catBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:#e8f4f8; -fx-text-fill:#264653; -fx-background-radius:12;" +
                   "-fx-padding:3 8; -fx-cursor:hand; -fx-font-size:10px;");
        b.setWrapText(true);
        b.setMaxWidth(120);
        return b;
    }

    private PlaceCategory findCategory(String sqName) {
        for (PlaceCategory pc : PlaceCategory.values()) {
            if (pc.getAlbanianName().equalsIgnoreCase(sqName)) return pc;
        }
        return null;
    }

    private String resolveAlgoKey(String comboValue) {
        if (comboValue == null) return "DIJKSTRA_DIST";
        // Match both EN and SQ labels
        String v = comboValue.toLowerCase();
        if (v.contains("kohë") || v.contains("fastest") || v.contains("time")) return "DIJKSTRA_TIME";
        if (v.contains("bfs") || v.contains("fewest") || v.contains("hop")) return "BFS";
        if (v.contains("dfs") || v.contains("altern")) return "DFS";
        return "DIJKSTRA_DIST";
    }

    private String buildStatusText(String extra) {
        String conn = graph.isConnected() ? lm.get("status.graph.connected") : lm.get("status.graph.disconnected");
        String base = String.format("%s: %d  |  %s: %d  |  %s: %s  |  %s",
            lm.get("status.locations").split(":")[0], graph.locationCount(),
            lm.get("status.roads").split(":")[0], graph.roadCount()/2,
            lm.get("status.graph").split(":")[0], conn,
            lm.get("status.location"));
        return extra.isBlank() ? base : extra + "  |  " + base;
    }

    private void setStatus(String msg) {
        Platform.runLater(() -> statusLabel.setText(buildStatusText(msg)));
    }

    private void updateStatusBar() {
        setStatus("");
    }
}
