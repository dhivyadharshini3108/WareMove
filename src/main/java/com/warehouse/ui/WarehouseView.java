package com.warehouse.ui;

import com.warehouse.model.Product;
import com.warehouse.model.Robot;
import com.warehouse.model.RobotStatus;
import com.warehouse.model.RobotTask;
import com.warehouse.model.Warehouse;
import com.warehouse.model.WarehouseCell;
import com.warehouse.util.Position;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.Collection;
import java.util.List;

/**
 * Custom JavaFX canvas component rendering the warehouse grid,
 * obstacles/shelves, product inventory, stations, and robots in real-time.
 */
public class WarehouseView extends StackPane {

    private final Canvas canvas;
    private Warehouse warehouse;
    private Position deliveryStation;
    private Position chargingStation;

    // Palette colors
    private static final Color COLOR_BG = Color.web("#111827");
    private static final Color COLOR_CELL_FLOOR = Color.web("#1e293b");
    private static final Color COLOR_CELL_BORDER = Color.web("#334155");
    private static final Color COLOR_SHELF = Color.web("#475569");
    private static final Color COLOR_SHELF_BORDER = Color.web("#64748b");
    private static final Color COLOR_DELIVERY_ZONE = Color.web("#6366f1", 0.35);
    private static final Color COLOR_DELIVERY_BORDER = Color.web("#818cf8");
    private static final Color COLOR_CHARGING_ZONE = Color.web("#eab308", 0.35);
    private static final Color COLOR_CHARGING_BORDER = Color.web("#facc15");
    private static final Color COLOR_PRODUCT_BADGE = Color.web("#ec4899");
    private static final Color COLOR_PATH_DOT = Color.web("#38bdf8", 0.6);
    private double cellSize = 48.0;

    public WarehouseView(Warehouse warehouse) {
        this.warehouse = warehouse;
        int cols = (warehouse != null) ? warehouse.getWidth() : 12;
        int rows = (warehouse != null) ? warehouse.getHeight() : 12;

        double initialW = Math.max(520, cols * cellSize + 40);
        double initialH = Math.max(520, rows * cellSize + 40);
        this.canvas = new Canvas(initialW, initialH);

        setAlignment(Pos.CENTER);
        setPadding(new Insets(16));
        setStyle("-fx-background-color: #0b0f19;");
        getChildren().add(canvas);

        render();
    }

    public void setStations(Position delivery, Position charging) {
        this.deliveryStation = delivery;
        this.chargingStation = charging;
        render();
    }

    public void update(Warehouse warehouse) {
        this.warehouse = warehouse;
        if (warehouse != null) {
            int cols = warehouse.getWidth();
            int rows = warehouse.getHeight();
            double targetW = Math.max(520, cols * cellSize + 40);
            double targetH = Math.max(520, rows * cellSize + 40);
            if (canvas.getWidth() != targetW || canvas.getHeight() != targetH) {
                canvas.setWidth(targetW);
                canvas.setHeight(targetH);
            }
        }
        render();
    }

    public void setCellSize(double cellSize) {
        this.cellSize = Math.max(30.0, Math.min(80.0, cellSize));
        if (warehouse != null) {
            canvas.setWidth(Math.max(520, warehouse.getWidth() * this.cellSize + 40));
            canvas.setHeight(Math.max(520, warehouse.getHeight() * this.cellSize + 40));
        }
        render();
    }

    /**
     * Renders the complete warehouse state onto the Canvas.
     */
    public void render() {
        if (warehouse == null) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double canvasW = canvas.getWidth();
        double canvasH = canvas.getHeight();

        gc.clearRect(0, 0, canvasW, canvasH);
        gc.setFill(COLOR_BG);
        gc.fillRect(0, 0, canvasW, canvasH);

        int cols = warehouse.getWidth();
        int rows = warehouse.getHeight();

        final double cellSz = this.cellSize;
        double gridW = cols * cellSz;
        double gridH = rows * cellSz;
        double offsetX = (canvasW - gridW) / 2.0;
        double offsetY = (canvasH - gridH) / 2.0;

        // 1. Draw floor grid and cells
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                double cx = offsetX + (x * cellSz);
                double cy = offsetY + (y * cellSz);
                Position pos = new Position(x, y);
                WarehouseCell cell = warehouse.getCell(x, y);

                if (cell.isObstacle()) {
                    // Shelf / Obstacle
                    gc.setFill(COLOR_SHELF);
                    gc.fillRoundRect(cx + 2, cy + 2, cellSz - 4, cellSz - 4, 6, 6);
                    gc.setStroke(COLOR_SHELF_BORDER);
                    gc.setLineWidth(1.5);
                    gc.strokeRoundRect(cx + 2, cy + 2, cellSz - 4, cellSz - 4, 6, 6);

                    // Shelf crosshatch rack lines
                    gc.setStroke(Color.web("#334155"));
                    gc.setLineWidth(1.0);
                    gc.strokeLine(cx + 4, cy + (cellSz / 2), cx + cellSz - 4, cy + (cellSz / 2));
                } else {
                    // Traversable floor tile
                    gc.setFill(COLOR_CELL_FLOOR);
                    gc.fillRoundRect(cx + 1, cy + 1, cellSz - 2, cellSz - 2, 4, 4);
                    gc.setStroke(COLOR_CELL_BORDER);
                    gc.setLineWidth(0.8);
                    gc.strokeRoundRect(cx + 1, cy + 1, cellSz - 2, cellSz - 2, 4, 4);
                }

                // Station highlights
                if (pos.equals(deliveryStation)) {
                    gc.setFill(COLOR_DELIVERY_ZONE);
                    gc.fillRoundRect(cx + 2, cy + 2, cellSz - 4, cellSz - 4, 6, 6);
                    gc.setStroke(COLOR_DELIVERY_BORDER);
                    gc.setLineWidth(2.0);
                    gc.strokeRoundRect(cx + 2, cy + 2, cellSz - 4, cellSz - 4, 6, 6);

                    gc.setFill(COLOR_DELIVERY_BORDER);
                    gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, Math.max(9, cellSz * 0.22)));
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText("PACK", cx + (cellSz / 2), cy + (cellSz / 2) + 4);
                } else if (pos.equals(chargingStation)) {
                    gc.setFill(COLOR_CHARGING_ZONE);
                    gc.fillRoundRect(cx + 2, cy + 2, cellSz - 4, cellSz - 4, 6, 6);
                    gc.setStroke(COLOR_CHARGING_BORDER);
                    gc.setLineWidth(2.0);
                    gc.strokeRoundRect(cx + 2, cy + 2, cellSz - 4, cellSz - 4, 6, 6);

                    gc.setFill(COLOR_CHARGING_BORDER);
                    gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, Math.max(9, cellSz * 0.22)));
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText("DOCK", cx + (cellSz / 2), cy + (cellSz / 2) + 4);
                }

                // Product stored in cell
                if (cell.hasProduct()) {
                    Product prod = cell.getProduct();
                    double badgeSize = Math.max(12, cellSz * 0.35);
                    double bx = cx + cellSz - badgeSize - 4;
                    double by = cy + 4;

                    gc.setFill(COLOR_PRODUCT_BADGE);
                    gc.fillOval(bx, by, badgeSize, badgeSize);
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(1.0);
                    gc.strokeOval(bx, by, badgeSize, badgeSize);

                    gc.setFill(Color.WHITE);
                    gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, Math.max(8, badgeSize * 0.6)));
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText("P", bx + (badgeSize / 2), by + (badgeSize / 2) + 3);
                }
            }
        }

        // 2. Draw planned robot path trajectories
        for (Robot robot : warehouse.getRobots()) {
            List<Position> path = robot.getPlannedPath();
            if (path != null && !path.isEmpty()) {
                gc.setFill(COLOR_PATH_DOT);
                double dotRadius = Math.max(3, cellSz * 0.12);
                for (Position p : path) {
                    if (warehouse.isValidPosition(p)) {
                        double px = offsetX + (p.getX() * cellSz) + (cellSz / 2);
                        double py = offsetY + (p.getY() * cellSz) + (cellSz / 2);
                        gc.fillOval(px - dotRadius, py - dotRadius, dotRadius * 2, dotRadius * 2);
                    }
                }
            }
        }

        // 3. Draw Robots
        Collection<Robot> robots = warehouse.getRobots();
        for (Robot robot : robots) {
            Position pos = robot.getPosition();
            double rx = offsetX + (pos.getX() * cellSz);
            double ry = offsetY + (pos.getY() * cellSz);

            drawRobot(gc, robot, rx, ry, cellSz);
        }
    }

    private void drawRobot(GraphicsContext gc, Robot robot, double rx, double ry, double cellSize) {
        double botMargin = cellSize * 0.15;
        double botSize = cellSize - (botMargin * 2);
        double cx = rx + botMargin;
        double cy = ry + botMargin;

        Color statusColor;
        switch (robot.getStatus()) {
            case MOVING -> statusColor = Color.web("#06b6d4"); // Cyan
            case WORKING -> statusColor = Color.web("#f59e0b"); // Amber
            case CHARGING -> statusColor = Color.web("#eab308"); // Yellow
            case ERROR -> statusColor = Color.web("#ef4444"); // Red
            default -> statusColor = Color.web("#10b981"); // Green (IDLE)
        }

        // Robot circular body
        gc.setFill(Color.web("#0f172a"));
        gc.fillOval(cx, cy, botSize, botSize);

        gc.setStroke(statusColor);
        gc.setLineWidth(3.0);
        gc.strokeOval(cx, cy, botSize, botSize);

        // Robot ID abbreviation label
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, Math.max(9, botSize * 0.35)));
        gc.setTextAlign(TextAlignment.CENTER);

        String displayId = robot.getId();
        if (displayId.length() > 3) {
            // e.g. "ROBOT-1" -> "R1"
            displayId = displayId.replaceAll("(?i)robot[-_]?", "R");
            if (displayId.length() > 4) {
                displayId = displayId.substring(0, 4);
            }
        }
        gc.fillText(displayId, cx + (botSize / 2), cy + (botSize / 2) + (botSize * 0.12));

        // Mini battery bar under the robot
        double barW = botSize * 0.8;
        double barH = Math.max(3, botSize * 0.1);
        double barX = cx + (botSize - barW) / 2.0;
        double barY = cy + botSize + 2;

        gc.setFill(Color.web("#334155"));
        gc.fillRect(barX, barY, barW, barH);

        double pct = Math.max(0.0, Math.min(1.0, robot.getBatteryLevel() / 100.0));
        Color batteryColor = pct > 0.5 ? Color.web("#10b981") : (pct > 0.2 ? Color.web("#f59e0b") : Color.web("#ef4444"));
        gc.setFill(batteryColor);
        gc.fillRect(barX, barY, barW * pct, barH);
    }
}
