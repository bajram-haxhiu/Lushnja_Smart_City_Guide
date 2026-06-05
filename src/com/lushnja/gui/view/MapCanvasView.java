package com.lushnja.gui.view;

import com.lushnja.core.graph.CityGraph;
import com.lushnja.core.model.Location;
import com.lushnja.core.model.Road;
import com.lushnja.core.model.Route;
import com.lushnja.models.Place;
import com.lushnja.models.Place.PlaceCategory;
import com.lushnja.services.PlaceService;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import com.lushnja.utils.LanguageManager;
import java.util.*;
import java.util.function.Consumer;

/**
 * MapCanvasView — Renders the city graph with places, routes, and labels.
 * Supports zoom, pan, category-filtered display, and route highlighting.
 */
public class MapCanvasView {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final double MARGIN   = 60.0;
    private static final double ZOOM_MIN = 0.3;
    private static final double ZOOM_MAX = 12.0;
    private static final double ZOOM_STEP= 0.12;

    // ── Colors (warm parchment palette) ──────────────────────────────────────
    private static final Color BG_WARM      = Color.web("#e8e0d0");
    private static final Color BG_INNER     = Color.web("#ede6d8");
    private static final Color GRID_COL     = Color.web("#d8cfbf");
    private static final Color ROAD_NORMAL  = Color.web("#b8a898");
    private static final Color ROAD_HEAVY   = Color.web("#e07030");
    private static final Color ROAD_BLOCKED = Color.web("#cc3333");
    private static final Color ROAD_MOD     = Color.web("#d4a020");
    private static final Color ROUTE_TEAL   = Color.web("#2a9d8f");
    private static final Color TEXT_DARK    = Color.web("#2c3e50");

    // Dark mode colors
    private static final Color DARK_BG      = Color.web("#1a1a2e");
    private static final Color DARK_INNER   = Color.web("#16213e");
    private static final Color DARK_GRID    = Color.web("#2a2a4a");
    private static final Color DARK_ROAD    = Color.web("#4a5568");
    private static final Color DARK_TEXT    = Color.web("#e2e8f0");

    // ── Fields ────────────────────────────────────────────────────────────────
    private final CityGraph     graph;
    private final PlaceService  placeService;
    private final Canvas        canvas;
    private final StackPane     container;

    private Route       currentRoute;
    private Set<String> highlightedIds     = new HashSet<>();
    private Set<String> traversalOrder     = new LinkedHashSet<>(); // BFS/DFS
    private PlaceCategory filterCategory   = null;
    private boolean       darkMode         = false;
    private Consumer<Place> onPlaceClick   = null;

    private double minLat, maxLat, minLon, maxLon;
    private double zoomFactor = 1.0;
    private double panX = 0.0, panY = 0.0;
    private double dragStartX, dragStartY;

    // ── Constructor ───────────────────────────────────────────────────────────
    public MapCanvasView(CityGraph graph, PlaceService placeService) {
        this.graph        = graph;
        this.placeService = placeService;
        this.canvas       = new Canvas(900, 680);
        this.container    = new StackPane(canvas);
        container.setStyle("-fx-background-color: #e8e0d0;");

        canvas.widthProperty().bind(container.widthProperty());
        canvas.heightProperty().bind(container.heightProperty());
        canvas.widthProperty().addListener(e  -> renderGraph());
        canvas.heightProperty().addListener(e -> renderGraph());

        registerZoom();
        registerPan();
        registerClick();

        // Re-render map when language changes (legend text updates)
        LanguageManager.getInstance().addListener(() ->
            javafx.application.Platform.runLater(this::renderGraph));
    }

    public Pane getView() { return container; }

    // ── Input Handlers ────────────────────────────────────────────────────────

    private void registerZoom() {
        canvas.setOnScroll((ScrollEvent e) -> {
            double old = zoomFactor;
            zoomFactor = e.getDeltaY() > 0
                ? Math.min(ZOOM_MAX, zoomFactor + ZOOM_STEP)
                : Math.max(ZOOM_MIN, zoomFactor - ZOOM_STEP);
            double r = zoomFactor / old;
            panX = e.getX() - r * (e.getX() - panX);
            panY = e.getY() - r * (e.getY() - panY);
            renderGraph();
            e.consume();
        });
    }

    private void registerPan() {
        canvas.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragStartX = e.getX() - panX;
                dragStartY = e.getY() - panY;
            }
        });
        canvas.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                panX = e.getX() - dragStartX;
                panY = e.getY() - dragStartY;
                renderGraph();
            }
        });
    }

    private void registerClick() {
        canvas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                zoomFactor = 1.0; panX = 0.0; panY = 0.0; renderGraph(); return;
            }
            if (e.getButton() == MouseButton.PRIMARY && onPlaceClick != null) {
                double mx = e.getX(), my = e.getY();
                for (Place p : getVisiblePlaces()) {
                    double cx = p.getCanvasX(), cy = p.getCanvasY();
                    if (Math.sqrt((mx-cx)*(mx-cx) + (my-cy)*(my-cy)) < 14 * Math.max(0.7, zoomFactor)) {
                        onPlaceClick.accept(p);
                        return;
                    }
                }
            }
        });
    }

    // ── Projection ────────────────────────────────────────────────────────────

    private void computeBounds() {
        Collection<Location> locs = graph.getAllLocations();
        if (locs.isEmpty()) return;
        minLat = locs.stream().mapToDouble(Location::getLatitude).min().orElse(40.92);
        maxLat = locs.stream().mapToDouble(Location::getLatitude).max().orElse(40.97);
        minLon = locs.stream().mapToDouble(Location::getLongitude).min().orElse(19.68);
        maxLon = locs.stream().mapToDouble(Location::getLongitude).max().orElse(19.73);
        double lp  = (maxLat - minLat) * 0.12;
        double lnp = (maxLon - minLon) * 0.12;
        minLat -= lp; maxLat += lp; minLon -= lnp; maxLon += lnp;
    }

    private double projX(double lon) {
        double base = MARGIN + (lon - minLon) / (maxLon - minLon) * (canvas.getWidth() - 2*MARGIN);
        return base * zoomFactor + panX;
    }

    private double projY(double lat) {
        double base = MARGIN + (1 - (lat - minLat)/(maxLat - minLat)) * (canvas.getHeight() - 2*MARGIN);
        return base * zoomFactor + panY;
    }

    // ── Main Render ───────────────────────────────────────────────────────────

    public void renderGraph() {
        computeBounds();
        // Update canvas coords for places
        for (Place p : placeService.getAllPlaces()) {
            p.setCanvasX(projX(p.getLongitude()));
            p.setCanvasY(projY(p.getLatitude()));
        }
        // Also update graph location coords
        for (Location l : graph.getAllLocations()) {
            l.setCanvasX(projX(l.getLongitude()));
            l.setCanvasY(projY(l.getLatitude()));
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();

        drawBackground(gc, w, h);
        drawGrid(gc, w, h);

        Set<String> routeRoads = new HashSet<>();
        if (currentRoute != null)
            currentRoute.getRoadPath().forEach(r -> { routeRoads.add(r.getId()); routeRoads.add(r.getId().replace("_rev","")); });

        drawRoadShadows(gc);
        drawRoads(gc, routeRoads);
        if (currentRoute != null && !currentRoute.isEmpty()) drawRoute(gc, currentRoute);

        drawPlaces(gc);
        drawCompass(gc, w);
        drawLegend(gc, w, h);
        drawZoomHint(gc, w, h);
    }

    // ── Draw Background ───────────────────────────────────────────────────────

    private void drawBackground(GraphicsContext gc, double w, double h) {
        Color bg1 = darkMode ? DARK_BG    : BG_WARM;
        Color bg2 = darkMode ? DARK_INNER : BG_INNER;
        gc.setFill(bg1); gc.fillRect(0,0,w,h);
        for (int i = 0; i < 30; i++) {
            double alpha = (30-i)/250.0;
            gc.setFill(Color.color(0.1,0.08,0.05,alpha));
            gc.fillRect(i,i,w-i*2,h-i*2);
        }
        gc.setFill(bg2);
        gc.fillRoundRect(25, 25, w-50, h-50, 10, 10);
    }

    private void drawGrid(GraphicsContext gc, double w, double h) {
        Color gc2 = darkMode ? DARK_GRID : GRID_COL;
        gc.setStroke(gc2); gc.setLineWidth(0.5); gc.setLineDashes(2,8);
        for (double x = MARGIN; x < w-MARGIN; x+=50) gc.strokeLine(x,MARGIN,x,h-MARGIN);
        for (double y = MARGIN; y < h-MARGIN; y+=50) gc.strokeLine(MARGIN,y,w-MARGIN,y);
        gc.setLineDashes(0);
    }

    private void drawRoadShadows(GraphicsContext gc) {
        gc.setStroke(Color.web("#00000015")); gc.setLineDashes(0);
        for (Road r : graph.getAllRoads()) {
            if (r.getId().endsWith("_rev") || r.isBlocked()) continue;
            gc.setLineWidth(Math.max(2, 2.8*zoomFactor)+1.5);
            gc.strokeLine(r.getSource().getCanvasX()+1, r.getSource().getCanvasY()+1,
                          r.getDestination().getCanvasX()+1, r.getDestination().getCanvasY()+1);
        }
    }

    private void drawRoads(GraphicsContext gc, Set<String> routeRoads) {
        for (Road r : graph.getAllRoads()) {
            if (r.getId().endsWith("_rev")) continue;
            double x1=r.getSource().getCanvasX(), y1=r.getSource().getCanvasY();
            double x2=r.getDestination().getCanvasX(), y2=r.getDestination().getCanvasY();
            gc.setLineDashes(0);
            if (r.isBlocked()) {
                gc.setStroke(ROAD_BLOCKED);
                gc.setLineWidth(Math.max(1.5, 2*zoomFactor));
                gc.setLineDashes(6, 4);
            } else {
                Color roadCol = darkMode ? DARK_ROAD : ROAD_NORMAL;
                switch (r.getTrafficLevel()) {
                    case HEAVY, GRIDLOCK -> gc.setStroke(ROAD_HEAVY);
                    case MODERATE        -> gc.setStroke(ROAD_MOD);
                    default              -> gc.setStroke(roadCol);
                }
                double lw = switch(r.getRoadType()) {
                    case HIGHWAY   -> Math.max(2.5, 4.0*zoomFactor);
                    case MAIN_ROAD -> Math.max(2.0, 3.0*zoomFactor);
                    default        -> Math.max(1.2, 2.0*zoomFactor);
                };
                gc.setLineWidth(lw);
            }
            gc.strokeLine(x1,y1,x2,y2);
            gc.setLineDashes(0);
            if (zoomFactor > 1.8) {
                double mx=(x1+x2)/2, my=(y1+y2)/2;
                double ang = Math.atan2(y2-y1,x2-x1);
                gc.save(); gc.translate(mx,my);
                if (ang > Math.PI/2||ang<-Math.PI/2) gc.rotate(Math.toDegrees(ang)+180);
                else gc.rotate(Math.toDegrees(ang));
                gc.setFill(darkMode ? Color.web("#8090a8") : Color.web("#8a7a6a"));
                gc.setFont(Font.font("Georgia", FontPosture.ITALIC, 7));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(r.getRoadName(), 0, -3);
                gc.restore();
                gc.setTextAlign(TextAlignment.LEFT);
            }
        }
    }

    private void drawRoute(GraphicsContext gc, Route route) {
        var rp = route.getRoadPath();
        if (rp.isEmpty()) return;
        gc.setStroke(Color.web("#2a9d8f44")); gc.setLineWidth(16*zoomFactor); gc.setLineDashes(0);
        for (Road r : rp) gc.strokeLine(r.getSource().getCanvasX(), r.getSource().getCanvasY(),
                                         r.getDestination().getCanvasX(), r.getDestination().getCanvasY());
        gc.setStroke(Color.web("#ffffff88")); gc.setLineWidth(5.5*zoomFactor);
        for (Road r : rp) gc.strokeLine(r.getSource().getCanvasX(), r.getSource().getCanvasY(),
                                         r.getDestination().getCanvasX(), r.getDestination().getCanvasY());
        gc.setStroke(ROUTE_TEAL); gc.setLineWidth(3.5*zoomFactor);
        for (Road r : rp) gc.strokeLine(r.getSource().getCanvasX(), r.getSource().getCanvasY(),
                                         r.getDestination().getCanvasX(), r.getDestination().getCanvasY());
        // Arrows
        gc.setStroke(Color.WHITE); gc.setLineWidth(1.5);
        for (Road r : rp) drawArrow(gc, r.getSource().getCanvasX(), r.getSource().getCanvasY(),
                                        r.getDestination().getCanvasX(), r.getDestination().getCanvasY());
    }

    private void drawArrow(GraphicsContext gc, double x1,double y1,double x2,double y2) {
        double mx=(x1+x2)/2, my=(y1+y2)/2;
        double ang=Math.atan2(y2-y1,x2-x1), l=8*zoomFactor, a=Math.PI/6;
        gc.strokeLine(mx,my,mx-l*Math.cos(ang-a),my-l*Math.sin(ang-a));
        gc.strokeLine(mx,my,mx-l*Math.cos(ang+a),my-l*Math.sin(ang+a));
    }

    private void drawPlaces(GraphicsContext gc) {
        List<Place> visible = getVisiblePlaces();
        // Draw traversal highlights first (lower z-order)
        for (Place p : visible) {
            if (traversalOrder.contains(p.getId())) {
                double cx=p.getCanvasX(), cy=p.getCanvasY();
                gc.setFill(Color.web("#ffd70050"));
                gc.fillOval(cx-18,cy-18,36,36);
            }
        }
        for (Place p : visible) {
            drawPin(gc, p, highlightedIds.contains(p.getId()));
        }
        // Number traversal order
        if (!traversalOrder.isEmpty()) {
            int num = 1;
            for (String id : traversalOrder) {
                Place p = placeService.findById(id).orElse(null);
                if (p == null || (filterCategory != null && p.getCategory() != filterCategory)) continue;
                gc.setFill(Color.web("#e9c46a"));
                gc.setFont(Font.font("Georgia", FontWeight.BOLD, 9));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(String.valueOf(num++), p.getCanvasX(), p.getCanvasY() - 22*Math.max(0.7, Math.min(zoomFactor, 1.5)));
                gc.setTextAlign(TextAlignment.LEFT);
            }
        }
    }

    private void drawPin(GraphicsContext gc, Place p, boolean highlighted) {
        double cx=p.getCanvasX(), cy=p.getCanvasY();
        double scale = Math.max(0.65, Math.min(zoomFactor, 2.0));
        double headR = 7.0*scale, pinH=13.0*scale, pinW=10.0*scale;

        Color fill = highlighted ? Color.web("#2a9d8f") : getPinColor(p.getCategory());
        Color dark = highlighted ? Color.web("#1a6d65") : getDarkColor(p.getCategory());

        // Shadow
        gc.setFill(Color.web("#00000030"));
        gc.fillOval(cx-headR*0.7+1.5, cy+1.5, headR*1.4, headR*0.45);

        // Head
        gc.setFill(fill);
        gc.fillOval(cx-headR, cy-pinH, headR*2, headR*2);
        // Tail
        double[] px={cx-pinW*0.28, cx+pinW*0.28, cx};
        double[] py={cy-pinH+headR*1.15, cy-pinH+headR*1.15, cy};
        gc.fillPolygon(px, py, 3);
        // Border
        gc.setStroke(dark); gc.setLineWidth(highlighted?1.8:0.9);
        gc.strokeOval(cx-headR, cy-pinH, headR*2, headR*2);
        // Shine
        double dr=headR*0.28;
        gc.setFill(Color.web("#ffffffcc"));
        gc.fillOval(cx-dr-headR*0.15, cy-pinH+headR*0.28, dr*2, dr*2);

        // Label
        if (zoomFactor >= 0.5) {
            String lbl = p.getName();
            if (lbl.length() > 20) lbl = lbl.substring(0,18) + "…";
            double fs = Math.max(6.5, Math.min(8.0*scale, 10.0));
            gc.setFont(Font.font("Georgia", highlighted?FontWeight.BOLD:FontWeight.NORMAL, fs));
            gc.setTextAlign(TextAlignment.CENTER);
            double approxW = lbl.length()*fs*0.48;
            double pillH = fs+3.5, labelY = cy+6*scale;
            gc.setFill(darkMode ? Color.web("#2a3550dd") : Color.web("#ffffffdd"));
            gc.fillRoundRect(cx-approxW/2-3, labelY-pillH+2, approxW+6, pillH, 4, 4);
            gc.setStroke(Color.web("#c8bfb0")); gc.setLineWidth(0.5);
            gc.strokeRoundRect(cx-approxW/2-3, labelY-pillH+2, approxW+6, pillH, 4, 4);
            gc.setFill(darkMode ? DARK_TEXT : TEXT_DARK);
            gc.fillText(lbl, cx, labelY);
            gc.setTextAlign(TextAlignment.LEFT);
        }
    }

    private void drawCompass(GraphicsContext gc, double w) {
        double cx=w-55, cy=55, r=20;
        gc.setFill(darkMode ? Color.web("#2a3550cc") : Color.web("#ffffffcc"));
        gc.fillOval(cx-r, cy-r, r*2, r*2);
        gc.setStroke(Color.web("#c8bfb0")); gc.setLineWidth(1.5);
        gc.strokeOval(cx-r, cy-r, r*2, r*2);
        gc.setFill(Color.web("#e76f51"));
        double[] nx={cx, cx-4, cx+4}; double[] ny={cy-r+4, cy+2, cy+2};
        gc.fillPolygon(nx, ny, 3);
        gc.setFill(Color.web("#b8a898"));
        double[] sx={cx, cx-4, cx+4}; double[] sy={cy+r-4, cy-2, cy-2};
        gc.fillPolygon(sx, sy, 3);
        gc.setFill(darkMode ? DARK_TEXT : TEXT_DARK);
        gc.setFont(Font.font("Georgia", FontWeight.BOLD, 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("V", cx, cy-r-3);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawLegend(GraphicsContext gc, double w, double h) {
        boolean en = LanguageManager.getInstance().isEnglish();
        double lx=18, ly=h-215, lw=160, lh=198;
        gc.setFill(darkMode ? Color.web("#1e2a3aee") : Color.web("#ffffffee"));
        gc.fillRoundRect(lx, ly, lw, lh, 10, 10);
        gc.setStroke(Color.web("#c8bfb0")); gc.setLineWidth(1.2);
        gc.strokeRoundRect(lx,ly,lw,lh,10,10);
        gc.setFill(Color.web("#264653"));
        gc.fillRoundRect(lx, ly, lw, 22, 10, 10);
        gc.fillRect(lx, ly+12, lw, 10);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Georgia", FontWeight.BOLD, 10));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(en ? "LEGEND" : "LEGENDA", lx+lw/2, ly+15);
        gc.setTextAlign(TextAlignment.LEFT);

        String[][] items = {
            {en ? "Restaurant/Cafe" : "Restorant/Kafene",   "#e76f51"},
            {en ? "Hospital"        : "Spital",              "#e63946"},
            {en ? "Pharmacy"        : "Farmaci",             "#e63946"},
            {en ? "Hotel"           : "Hotel",               "#6a4c93"},
            {en ? "Park"            : "Park",                "#57cc04"},
            {en ? "Bank/ATM"        : "Bankë/ATM",           "#2a9d8f"},
            {en ? "School"          : "Shkollë",             "#f4a261"},
            {en ? "Transport/Taxi"  : "Transport/Taksi",     "#e9c46a"},
            {en ? "Police"          : "Policia",             "#264653"},
            {en ? "Parking"         : "Parkim",              "#8ecae6"},
            {en ? "Active Route"    : "Rruga aktive",        "#2a9d8f"},
        };
        gc.setFont(Font.font("Georgia", 9));
        for (int i=0; i<items.length; i++) {
            double iy = ly+30+i*16;
            Color c = Color.web(items[i][1]);
            if (i < 10) {
                gc.setFill(c); gc.fillOval(lx+10, iy-7, 8, 8);
                gc.setFill(Color.web("#ffffffcc")); gc.fillOval(lx+12, iy-5, 3, 3);
            } else {
                gc.setStroke(c); gc.setLineWidth(2.5); gc.setLineDashes(0);
                gc.strokeLine(lx+8, iy-3, lx+20, iy-3);
            }
            gc.setFill(darkMode ? DARK_TEXT : TEXT_DARK);
            gc.fillText(items[i][0], lx+26, iy);
        }
    }

    private void drawZoomHint(GraphicsContext gc, double w, double h) {
        boolean en = LanguageManager.getInstance().isEnglish();
        double bx=w/2-170, by=h-28, bw=340, bh=18;
        gc.setFill(darkMode ? Color.web("#1e2a3acc") : Color.web("#ffffffcc"));
        gc.fillRoundRect(bx, by, bw, bh, 9, 9);
        gc.setFill(Color.web("#5a6a7a"));
        gc.setFont(Font.font("Georgia", FontPosture.ITALIC, 9));
        gc.setTextAlign(TextAlignment.CENTER);
        String hint = en
            ? String.format("Zoom: %.0f%%  |  Scroll = zoom  |  Drag = pan  |  Double-click = reset  |  Click = details", zoomFactor*100)
            : String.format("Zoom: %.0f%%  |  Scroll=zoom  |  Drag=pan  |  2xKlik=reset  |  Klik=detaje", zoomFactor*100);
        gc.fillText(hint, w/2, by+12);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    // ── Color Helpers ─────────────────────────────────────────────────────────

    private Color getPinColor(PlaceCategory cat) {
        if (cat == null) return Color.web("#e63946");
        return switch (cat) {
            case SPITALE           -> Color.web("#e63946");
            case FARMACI           -> Color.web("#e63946");
            case HOTELE            -> Color.web("#6a4c93");
            case PARKE             -> Color.web("#57cc04");
            case BANKA             -> Color.web("#2a9d8f");
            case SHKOLLA           -> Color.web("#f4a261");
            case STACIONE_AUTOBUSI -> Color.web("#e9c46a");
            case PIKA_TAKSIE       -> Color.web("#f4a261");
            case RESTORANTE        -> Color.web("#e76f51");
            case KAFENE            -> Color.web("#e76f51");
            case POLICIA           -> Color.web("#264653");
            case PARKIME           -> Color.web("#8ecae6");
            case VENDE_TURISTIKE   -> Color.web("#e9c46a");
            case INSTITUCIONE      -> Color.web("#6a4c93");
            default                -> Color.web("#adb5bd");
        };
    }

    private Color getDarkColor(PlaceCategory cat) {
        if (cat == null) return Color.web("#991020");
        return switch (cat) {
            case SPITALE, FARMACI  -> Color.web("#991020");
            case HOTELE            -> Color.web("#3d2060");
            case PARKE             -> Color.web("#338802");
            case BANKA             -> Color.web("#1a6d65");
            case SHKOLLA           -> Color.web("#c47830");
            case STACIONE_AUTOBUSI -> Color.web("#b89030");
            case PIKA_TAKSIE       -> Color.web("#b89030");
            case RESTORANTE, KAFENE-> Color.web("#aa4422");
            case POLICIA           -> Color.web("#142b35");
            case PARKIME           -> Color.web("#5a9ab8");
            default                -> Color.web("#888888");
        };
    }

    private List<Place> getVisiblePlaces() {
        if (filterCategory == null) return placeService.getAllPlaces();
        return placeService.filterByCategory(filterCategory);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void showRoute(Route route) {
        this.currentRoute = route;
        this.highlightedIds.clear();
        if (route != null) route.getLocationPath().forEach(l -> highlightedIds.add(l.getId()));
        renderGraph();
    }

    public void clearRoute() { this.currentRoute = null; this.highlightedIds.clear(); renderGraph(); }

    public void setFilterCategory(PlaceCategory cat) { this.filterCategory = cat; renderGraph(); }
    public void clearFilter() { this.filterCategory = null; renderGraph(); }

    public void showTraversal(List<String> orderedIds) {
        this.traversalOrder.clear();
        this.traversalOrder.addAll(orderedIds);
        renderGraph();
    }

    public void clearTraversal() { this.traversalOrder.clear(); renderGraph(); }

    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
        container.setStyle(dark ? "-fx-background-color: #1a1a2e;" : "-fx-background-color: #e8e0d0;");
        renderGraph();
    }

    public void setOnPlaceClick(Consumer<Place> handler) { this.onPlaceClick = handler; }

    public void highlightPlaces(List<String> ids) {
        this.highlightedIds.clear();
        this.highlightedIds.addAll(ids);
        renderGraph();
    }
}
