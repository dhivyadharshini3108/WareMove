package com.warehouse.app;

import com.warehouse.database.DatabaseManager;
import com.warehouse.database.OrderRepository;
import com.warehouse.database.RobotRepository;
import com.warehouse.service.OrderService;
import com.warehouse.service.RobotAssignmentService;
import com.warehouse.service.WarehouseService;
import com.warehouse.ui.DashboardController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Logger;

/**
 * Main application entry point for the WareMove warehouse robotics simulator.
 * Initializes services, database connectivity (with graceful fallback), and launches
 * the JavaFX dashboard.
 */
public class Main extends Application {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private DashboardController dashboardController;

    @Override
    public void start(Stage stage) {
        LOGGER.info("Starting WareMove Warehouse Simulator...");

        // 1. Initialize Database Repositories (with graceful fallback if DB offline)
        DatabaseManager dbManager = DatabaseManager.getInstance();
        boolean dbConnected = dbManager.testConnection();
        if (dbConnected) {
            LOGGER.info("Connected to MySQL database successfully. Verifying tables...");
            dbManager.initializeTables();
        } else {
            LOGGER.info("MySQL database offline or unconfigured. Proceeding with in-memory execution.");
        }

        RobotRepository robotRepository = new RobotRepository(dbManager);
        OrderRepository orderRepository = new OrderRepository(dbManager);

        // 2. Initialize Core Services and Default Warehouse
        OrderService orderService = new OrderService(orderRepository);
        RobotAssignmentService assignmentService = new RobotAssignmentService();
        WarehouseService warehouseService = new WarehouseService(
                new com.warehouse.model.Warehouse(12, 12),
                new com.warehouse.algorithm.AStarPathFinder(),
                new com.warehouse.simulation.CollisionManager(),
                orderService,
                assignmentService,
                robotRepository,
                orderRepository
        );
        warehouseService.setupDefaultWarehouse();

        // 3. Instantiate Dashboard Controller
        dashboardController = new DashboardController(warehouseService);

        // 4. Configure Primary Scene and Stage
        Scene scene = new Scene(dashboardController, 1200, 800);

        stage.setTitle("WareMove - Autonomous Warehouse Robotics Simulator");
        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(680);

        // 5. Clean Application Shutdown on Close
        stage.setOnCloseRequest(e -> {
            LOGGER.info("Shutting down WareMove simulation...");
            if (dashboardController != null) {
                dashboardController.shutdown();
            }
        });

        stage.show();
        LOGGER.info("WareMove Dashboard displayed successfully.");
    }

    @Override
    public void stop() {
        if (dashboardController != null) {
            dashboardController.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
