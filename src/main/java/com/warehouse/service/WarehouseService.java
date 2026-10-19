package com.warehouse.service;

import com.warehouse.algorithm.AStarPathFinder;
import com.warehouse.algorithm.PathFinder;
import com.warehouse.database.OrderRepository;
import com.warehouse.database.RobotRepository;
import com.warehouse.model.Order;
import com.warehouse.model.Product;
import com.warehouse.model.Robot;
import com.warehouse.model.RobotStatus;
import com.warehouse.model.Warehouse;
import com.warehouse.simulation.CollisionManager;
import com.warehouse.simulation.SimulationEngine;
import com.warehouse.util.Position;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Central service managing warehouse operations, physical grid configuration,
 * product inventory, robot fleet lifecycle, and simulation execution.
 */
public class WarehouseService {

    private static final Logger LOGGER = Logger.getLogger(WarehouseService.class.getName());

    private final Warehouse warehouse;
    private final PathFinder pathFinder;
    private final CollisionManager collisionManager;
    private final SimulationEngine simulationEngine;
    private final OrderService orderService;
    private final RobotAssignmentService assignmentService;
    private final RobotRepository robotRepository;
    private final OrderRepository orderRepository;

    private Position deliveryStation = new Position(0, 0);
    private Position chargingStation = new Position(0, 5);

    public WarehouseService() {
        this(12, 12);
    }

    public WarehouseService(int width, int height) {
        this(new Warehouse(width, height), new AStarPathFinder(), new CollisionManager(),
                new OrderService(), new RobotAssignmentService(), null, null);
    }

    public WarehouseService(Warehouse warehouse, PathFinder pathFinder, CollisionManager collisionManager,
                            OrderService orderService, RobotAssignmentService assignmentService,
                            RobotRepository robotRepository, OrderRepository orderRepository) {
        if (warehouse == null) {
            throw new IllegalArgumentException("Warehouse cannot be null");
        }
        this.warehouse = warehouse;
        this.pathFinder = (pathFinder != null) ? pathFinder : new AStarPathFinder();
        this.collisionManager = (collisionManager != null) ? collisionManager : new CollisionManager();
        this.orderService = (orderService != null) ? orderService : new OrderService(orderRepository);
        this.assignmentService = (assignmentService != null) ? assignmentService : new RobotAssignmentService();
        this.robotRepository = robotRepository;
        this.orderRepository = orderRepository;

        this.simulationEngine = new SimulationEngine(this.warehouse, this.pathFinder, this.collisionManager);
    }

    /**
     * Initializes a standard warehouse layout with shelving racks, products, and robots.
     */
    public void setupDefaultWarehouse() {
        int width = warehouse.getWidth();
        int height = warehouse.getHeight();

        // 1. Establish delivery station and charging station
        this.deliveryStation = new Position(1, height - 2);
        this.chargingStation = new Position(1, 1);

        // 2. Erect shelving blocks (obstacles) leaving aisles
        for (int y = 2; y <= height - 3; y++) {
            if (width >= 10) {
                warehouse.setObstacle(3, y, true);
                warehouse.setObstacle(4, y, true);
                warehouse.setObstacle(7, y, true);
                warehouse.setObstacle(8, y, true);
            } else if (width >= 6) {
                warehouse.setObstacle(2, y, true);
                warehouse.setObstacle(3, y, true);
            } else if (width >= 4) {
                warehouse.setObstacle(2, y, true);
            }
        }

        // 3. Populate products on shelves and inventory positions
        if (width >= 10 && height >= 5) {
            addProduct(new Product("P-A1", "Electronics Pack", 20, new Position(3, 2)), new Position(3, 2));
            addProduct(new Product("P-A2", "Mechanical Tools", 15, new Position(4, 3)), new Position(4, 3));
            addProduct(new Product("P-B1", "Medical Supplies", 30, new Position(7, 2)), new Position(7, 2));
            addProduct(new Product("P-B2", "Automotive Parts", 10, new Position(8, 4)), new Position(8, 4));
        } else if (width >= 6 && height >= 5) {
            addProduct(new Product("P-A1", "Electronics Pack", 20, new Position(2, 2)), new Position(2, 2));
            addProduct(new Product("P-A2", "Mechanical Tools", 15, new Position(3, 3)), new Position(3, 3));
        }

        // 4. Deploy initial robot fleet at idle charging/staging coordinates
        addRobot(new Robot("ROBOT-1", new Position(1, 1), 100.0));
        addRobot(new Robot("ROBOT-2", new Position(1, 2), 95.0));
        if (width >= 8) {
            addRobot(new Robot("ROBOT-3", new Position(width - 2, 1), 90.0));
        }

        // 5. Add initial sample orders to verify system workflow
        createSampleOrders();

        LOGGER.info("Default warehouse setup completed successfully: " + width + "x" + height);
    }

    /**
     * Adds initial sample orders with varied priorities.
     */
    public void createSampleOrders() {
        int width = warehouse.getWidth();
        Position p1Pos = (width >= 10) ? new Position(3, 2) : new Position(2, 2);
        Position p2Pos = (width >= 10) ? new Position(4, 3) : new Position(3, 3);
        Position p3Pos = (width >= 10) ? new Position(7, 2) : p1Pos;

        Product p1 = warehouse.isValidPosition(p1Pos) ? warehouse.getCell(p1Pos).getProduct() : null;
        Product p2 = warehouse.isValidPosition(p2Pos) ? warehouse.getCell(p2Pos).getProduct() : null;
        Product p3 = warehouse.isValidPosition(p3Pos) ? warehouse.getCell(p3Pos).getProduct() : null;

        List<Product> items1 = (p1 != null) ? List.of(p1) : new ArrayList<>();
        List<Product> items2 = (p2 != null) ? List.of(p2) : new ArrayList<>();
        List<Product> items3 = (p3 != null) ? List.of(p3) : new ArrayList<>();

        orderService.createOrder("ORD-101", items1, 3, deliveryStation);
        orderService.createOrder("ORD-102", items2, 1, deliveryStation);
        orderService.createOrder("ORD-103", items3, 2, deliveryStation);
    }

    /**
     * Deploys a new robot into the warehouse.
     */
    public synchronized boolean addRobot(Robot robot) {
        if (robot == null) return false;
        boolean added = warehouse.addRobot(robot);
        if (added && robotRepository != null) {
            robotRepository.save(robot);
        }
        return added;
    }

    public synchronized boolean addRobot(String id, Position position, double battery) {
        return addRobot(new Robot(id, position, battery));
    }

    /**
     * Removes a robot from the warehouse.
     */
    public synchronized boolean removeRobot(String id) {
        boolean removed = warehouse.removeRobot(id);
        if (removed && robotRepository != null) {
            robotRepository.delete(id);
        }
        return removed;
    }

    /**
     * Configures obstacle at coordinate.
     */
    public void setObstacle(Position position, boolean obstacle) {
        if (warehouse.isValidPosition(position)) {
            warehouse.setObstacle(position, obstacle);
        }
    }

    public void setObstacle(int x, int y, boolean obstacle) {
        setObstacle(new Position(x, y), obstacle);
    }

    /**
     * Places a product at a warehouse cell.
     */
    public void addProduct(Product product, Position position) {
        if (warehouse.isValidPosition(position) && product != null) {
            warehouse.getCell(position).setProduct(product);
        }
    }

    public Product getProduct(Position position) {
        if (warehouse.isValidPosition(position)) {
            return warehouse.getCell(position).getProduct();
        }
        return null;
    }

    /**
     * Steps the entire warehouse simulation by one tick:
     * 1. Checks and handles robot task transitions (e.g. PICKUP -> DELIVER)
     * 2. Automatically assigns pending orders to available idle robots
     * 3. Advances simulation engine movements and collision handling
     * 4. Updates database state if repositories are attached
     */
    public synchronized void stepSimulation() {
        // Pre-step: transition completed tasks
        for (Robot robot : warehouse.getRobots()) {
            assignmentService.processTaskTransition(warehouse, robot, orderService, pathFinder);
        }

        // Assign any pending orders to newly available idle robots
        assignmentService.assignPendingOrders(warehouse, orderService, pathFinder);

        // Advance simulation tick
        simulationEngine.step();

        // Post-step: check if robot just completed a step or task
        for (Robot robot : warehouse.getRobots()) {
            assignmentService.processTaskTransition(warehouse, robot, orderService, pathFinder);
            if (robotRepository != null) {
                robotRepository.save(robot);
            }
        }
    }

    public void startSimulation() {
        startSimulation(500);
    }

    public void startSimulation(long intervalMillis) {
        simulationEngine.start(intervalMillis);
    }

    public void pauseSimulation() {
        simulationEngine.pause();
    }

    public void resumeSimulation() {
        simulationEngine.resume();
    }

    public void stopSimulation() {
        simulationEngine.stop();
    }

    public synchronized void resetSimulation() {
        simulationEngine.reset();
        for (Robot robot : warehouse.getRobots()) {
            robot.cancelTask();
            robot.setBatteryLevel(100.0);
            robot.setStatus(RobotStatus.IDLE);
        }
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public PathFinder getPathFinder() {
        return pathFinder;
    }

    public CollisionManager getCollisionManager() {
        return collisionManager;
    }

    public SimulationEngine getSimulationEngine() {
        return simulationEngine;
    }

    public OrderService getOrderService() {
        return orderService;
    }

    public RobotAssignmentService getAssignmentService() {
        return assignmentService;
    }

    public Position getDeliveryStation() {
        return deliveryStation;
    }

    public void setDeliveryStation(Position deliveryStation) {
        this.deliveryStation = deliveryStation;
    }

    public Position getChargingStation() {
        return chargingStation;
    }

    public void setChargingStation(Position chargingStation) {
        this.chargingStation = chargingStation;
    }
}
