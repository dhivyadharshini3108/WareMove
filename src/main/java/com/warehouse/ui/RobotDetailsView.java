package com.warehouse.ui;

import com.warehouse.model.Robot;
import com.warehouse.model.RobotStatus;
import com.warehouse.model.RobotTask;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Collection;

/**
 * JavaFX sidebar view displaying real-time metrics and state cards for all warehouse robots.
 */
public class RobotDetailsView extends VBox {

    private final VBox cardsContainer;
    private final Label fleetCountBadge;

    public RobotDetailsView() {
        setSpacing(12);
        setPadding(new Insets(14));
        setPrefWidth(320);
        setMinWidth(280);
        setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8px; -fx-border-color: #334155; -fx-border-radius: 8px;");

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Robot Fleet");
        title.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        fleetCountBadge = new Label("0 Online");
        fleetCountBadge.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;");

        header.getChildren().addAll(title, spacer, fleetCountBadge);
        getChildren().add(header);

        // Scrollable cards list
        cardsContainer = new VBox(10);
        cardsContainer.setStyle("-fx-background-color: transparent;");

        ScrollPane scrollPane = new ScrollPane(cardsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().add(scrollPane);
    }

    /**
     * Refreshes the display with current robot statuses.
     */
    public void refresh(Collection<Robot> robots) {
        cardsContainer.getChildren().clear();

        if (robots == null || robots.isEmpty()) {
            Label empty = new Label("No robots deployed.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-style: italic;");
            cardsContainer.getChildren().add(empty);
            fleetCountBadge.setText("0 Online");
            return;
        }

        fleetCountBadge.setText(robots.size() + " Online");

        for (Robot robot : robots) {
            VBox card = createRobotCard(robot);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createRobotCard(Robot robot) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 6px; -fx-border-color: #334155; -fx-border-radius: 6px;");

        // Top line: Robot ID & Status Badge
        HBox topLine = new HBox(6);
        topLine.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label(robot.getId());
        idLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #f1f5f9;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(robot.getStatus().name());
        statusBadge.setStyle(getStatusBadgeStyle(robot.getStatus()));

        topLine.getChildren().addAll(idLabel, spacer, statusBadge);

        // Position & Coordinates
        Label posLabel = new Label(String.format("Location: (%d, %d)", robot.getPosition().getX(), robot.getPosition().getY()));
        posLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        // Battery Row
        HBox batteryRow = new HBox(6);
        batteryRow.setAlignment(Pos.CENTER_LEFT);

        ProgressBar batteryBar = new ProgressBar(Math.max(0.0, Math.min(1.0, robot.getBatteryLevel() / 100.0)));
        batteryBar.setPrefHeight(10);
        HBox.setHgrow(batteryBar, Priority.ALWAYS);
        batteryBar.setStyle(getBatteryBarStyle(robot.getBatteryLevel()));

        Label batteryLabel = new Label(String.format("%.0f%%", robot.getBatteryLevel()));
        batteryLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");

        batteryRow.getChildren().addAll(batteryBar, batteryLabel);

        // Task Row
        RobotTask task = robot.getCurrentTask();
        String taskText = (task != null)
                ? String.format("Task: %s -> (%d, %d)", task.getTaskType(), task.getTargetPosition().getX(), task.getTargetPosition().getY())
                : "Task: Idle (Awaiting orders)";
        Label taskLabel = new Label(taskText);
        taskLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (task != null ? "#38bdf8" : "#64748b") + ";");

        card.getChildren().addAll(topLine, posLabel, batteryRow, taskLabel);
        return card;
    }

    private String getStatusBadgeStyle(RobotStatus status) {
        String base = "-fx-padding: 2 6; -fx-background-radius: 4px; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white; ";
        return switch (status) {
            case MOVING -> base + "-fx-background-color: #0284c7;";
            case WORKING -> base + "-fx-background-color: #d97706;";
            case CHARGING -> base + "-fx-background-color: #ca8a04;";
            case ERROR -> base + "-fx-background-color: #dc2626;";
            default -> base + "-fx-background-color: #059669;"; // IDLE
        };
    }

    private String getBatteryBarStyle(double level) {
        String color = (level > 50) ? "#10b981" : (level > 20 ? "#f59e0b" : "#ef4444");
        return "-fx-accent: " + color + ";";
    }
}
