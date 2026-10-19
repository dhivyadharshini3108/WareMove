package com.warehouse.ui;

import com.warehouse.model.Order;
import com.warehouse.service.WarehouseService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controller and primary container for the WareMove JavaFX dashboard.
 * Coordinates UI components, controls simulation execution lifecycle,
 * and maintains continuous graphical synchronization with warehouse state.
 */
public class DashboardController extends BorderPane {

    private final WarehouseService warehouseService;

    private final WarehouseView warehouseView;
    private final RobotDetailsView robotDetailsView;
    private final OrderPanel orderPanel;

    private Timeline simulationTimeline;
    private long tickIntervalMillis = 400;

    private final Button btnStart;
    private final Button btnPause;
    private final Button btnResume;
    private final Button btnStep;
    private final Button btnReset;

    private final Label lblStatus;
    private final Label lblTick;

    public DashboardController(WarehouseService warehouseService) {
        if (warehouseService == null) {
            throw new IllegalArgumentException("WarehouseService cannot be null");
        }
        this.warehouseService = warehouseService;

        setStyle("-fx-background-color: #0b0f19;");

        // 1. Instantiate Child UI Views
        this.warehouseView = new WarehouseView(warehouseService.getWarehouse());
        this.warehouseView.setStations(warehouseService.getDeliveryStation(), warehouseService.getChargingStation());

        this.robotDetailsView = new RobotDetailsView();
        this.orderPanel = new OrderPanel(
                warehouseService.getOrderService(),
                this::handleOrderCreated,
                warehouseService::createSampleOrders
        );

        // 2. Control Buttons & Status
        btnStart = createStyledButton("Start", "#10b981", "#059669");
        btnPause = createStyledButton("Pause", "#f59e0b", "#d97706");
        btnResume = createStyledButton("Resume", "#0ea5e9", "#0284c7");
        btnStep = createStyledButton("Step", "#6366f1", "#4f46e5");
        btnReset = createStyledButton("Reset", "#f43f5e", "#e11d48");

        btnPause.setDisable(true);
        btnResume.setDisable(true);

        lblStatus = new Label("STOPPED");
        lblStatus.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 12px; -fx-font-size: 11px;");

        lblTick = new Label("Tick: 0");
        lblTick.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 13px;");

        // 3. Setup Layout
        setTop(buildTopBar());

        ScrollPane centerScrollPane = new ScrollPane(warehouseView);
        centerScrollPane.setFitToWidth(true);
        centerScrollPane.setFitToHeight(true);
        centerScrollPane.setStyle("-fx-background: #0b0f19; -fx-background-color: #0b0f19; -fx-border-color: transparent;");
        setCenter(centerScrollPane);

        setRight(buildSideBar());

        // 4. Setup Simulation Timeline Loop
        initTimeline();

        // 5. Wire Control Actions
        btnStart.setOnAction(e -> handleStart());
        btnPause.setOnAction(e -> handlePause());
        btnResume.setOnAction(e -> handleResume());
        btnStep.setOnAction(e -> handleStep());
        btnReset.setOnAction(e -> handleReset());

        // Initial sync
        refreshAll();
    }

    private HBox buildTopBar() {
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 16, 12, 16));
        topBar.setStyle("-fx-background-color: #111827; -fx-border-color: #1f2937; -fx-border-width: 0 0 1 0;");

        // Brand
        Label brand = new Label("WareMove");
        brand.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        Label subtitle = new Label("Autonomous Robotics Simulator");
        subtitle.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-text-fill: #64748b; -fx-padding: 4 0 0 0;");

        HBox brandBox = new HBox(6, brand, subtitle);
        brandBox.setAlignment(Pos.BASELINE_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Speed Slider
        Label speedLbl = new Label("Speed:");
        speedLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        Slider speedSlider = new Slider(100, 1000, tickIntervalMillis);
        speedSlider.setPrefWidth(120);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            tickIntervalMillis = newVal.longValue();
            if (simulationTimeline != null && simulationTimeline.getStatus() == Animation.Status.RUNNING) {
                simulationTimeline.stop();
                initTimeline();
                simulationTimeline.play();
            } else {
                initTimeline();
            }
        });

        HBox controlsBox = new HBox(8, btnStart, btnPause, btnResume, btnStep, btnReset);
        controlsBox.setAlignment(Pos.CENTER);

        HBox metricsBox = new HBox(10, lblStatus, lblTick);
        metricsBox.setAlignment(Pos.CENTER);

        topBar.getChildren().addAll(brandBox, spacer, speedLbl, speedSlider, controlsBox, metricsBox);
        return topBar;
    }

    private VBox buildSideBar() {
        VBox sideBar = new VBox(10);
        sideBar.setPadding(new Insets(10));
        sideBar.setPrefWidth(350);
        sideBar.setStyle("-fx-background-color: #0b0f19;");

        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: transparent;");

        Tab robotsTab = new Tab("Robots Fleet", robotDetailsView);
        robotsTab.setClosable(false);

        Tab ordersTab = new Tab("Orders", orderPanel);
        ordersTab.setClosable(false);

        tabPane.getTabs().addAll(robotsTab, ordersTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        sideBar.getChildren().add(tabPane);
        return sideBar;
    }

    private void initTimeline() {
        if (simulationTimeline != null) {
            simulationTimeline.stop();
        }

        simulationTimeline = new Timeline(new KeyFrame(Duration.millis(tickIntervalMillis), e -> {
            warehouseService.stepSimulation();
            refreshAll();
        }));
        simulationTimeline.setCycleCount(Animation.INDEFINITE);
    }

    private void handleStart() {
        simulationTimeline.play();
        warehouseService.getSimulationEngine().getState().setRunning(true);
        warehouseService.getSimulationEngine().getState().setPaused(false);

        btnStart.setDisable(true);
        btnPause.setDisable(false);
        btnResume.setDisable(true);

        lblStatus.setText("RUNNING");
        lblStatus.setStyle("-fx-background-color: #065f46; -fx-text-fill: #34d399; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 12px; -fx-font-size: 11px;");
    }

    private void handlePause() {
        simulationTimeline.pause();
        warehouseService.getSimulationEngine().getState().setPaused(true);

        btnPause.setDisable(true);
        btnResume.setDisable(false);

        lblStatus.setText("PAUSED");
        lblStatus.setStyle("-fx-background-color: #78350f; -fx-text-fill: #fbbf24; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 12px; -fx-font-size: 11px;");
    }

    private void handleResume() {
        simulationTimeline.play();
        warehouseService.getSimulationEngine().getState().setPaused(false);

        btnPause.setDisable(false);
        btnResume.setDisable(true);

        lblStatus.setText("RUNNING");
        lblStatus.setStyle("-fx-background-color: #065f46; -fx-text-fill: #34d399; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 12px; -fx-font-size: 11px;");
    }

    private void handleStep() {
        warehouseService.stepSimulation();
        refreshAll();
    }

    private void handleReset() {
        if (simulationTimeline != null) {
            simulationTimeline.stop();
        }
        warehouseService.resetSimulation();

        btnStart.setDisable(false);
        btnPause.setDisable(true);
        btnResume.setDisable(true);

        lblStatus.setText("STOPPED");
        lblStatus.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 12px; -fx-font-size: 11px;");

        refreshAll();
    }

    private void handleOrderCreated(Order order) {
        try {
            warehouseService.getOrderService().addOrder(order);
        } catch (Exception e) {
            // ignore duplicate
        }
        refreshAll();
    }

    /**
     * Synchronizes all visual components with warehouse state.
     */
    public void refreshAll() {
        long tick = warehouseService.getSimulationEngine().getState().getCurrentTick();
        lblTick.setText("Tick: " + tick);

        warehouseView.update(warehouseService.getWarehouse());
        robotDetailsView.refresh(warehouseService.getWarehouse().getRobots());
        orderPanel.refresh();
    }

    public void shutdown() {
        if (simulationTimeline != null) {
            simulationTimeline.stop();
        }
        warehouseService.stopSimulation();
    }

    private Button createStyledButton(String text, String bgColor, String hoverColor) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 5px; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisable()) {
                btn.setStyle("-fx-background-color: " + hoverColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 5px; -fx-cursor: hand;");
            }
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisable()) {
                btn.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 5px; -fx-cursor: hand;");
            }
        });
        return btn;
    }
}
