package com.warehouse.integration;

import com.warehouse.algorithm.AStarPathFinder;
import com.warehouse.model.Order;
import com.warehouse.model.OrderStatus;
import com.warehouse.model.Product;
import com.warehouse.model.Robot;
import com.warehouse.model.RobotStatus;
import com.warehouse.model.Warehouse;
import com.warehouse.service.OrderService;
import com.warehouse.service.RobotAssignmentService;
import com.warehouse.service.WarehouseService;
import com.warehouse.simulation.CollisionManager;
import com.warehouse.util.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WarehouseIntegrationTest {

    private WarehouseService warehouseService;
    private Warehouse warehouse;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse(8, 8);
        orderService = new OrderService();
        RobotAssignmentService assignmentService = new RobotAssignmentService();

        warehouseService = new WarehouseService(
                warehouse,
                new AStarPathFinder(),
                new CollisionManager(),
                orderService,
                assignmentService,
                null,
                null
        );
    }

    @Test
    @DisplayName("End-to-end autonomous fulfillment: Task Assignment -> Pathfinding -> Obstacle Avoidance -> Delivery -> Completion")
    void testEndToEndAutonomousOrderFulfillment() {
        // 1. Setup Shelves (obstacles) at col 3 (rows 1-4)
        for (int y = 1; y <= 4; y++) {
            warehouse.setObstacle(3, y, true);
        }

        // 2. Setup product on shelf cell (3, 2)
        Product electronicItem = new Product("PROD-E1", "Microchips", 10, new Position(3, 2));
        warehouseService.addProduct(electronicItem, new Position(3, 2));

        // 3. Deploy Robot-1 at (0, 2) and Robot-2 at (6, 2)
        Robot r1 = new Robot("R1", new Position(0, 2), 100.0);
        Robot r2 = new Robot("R2", new Position(6, 2), 100.0);
        warehouseService.addRobot(r1);
        warehouseService.addRobot(r2);

        // 4. Create high-priority Order targeting the product, delivery at (0, 0)
        Position deliveryTarget = new Position(0, 0);
        Order order = orderService.createOrder("ORD-HIGH", List.of(electronicItem), 5, deliveryTarget);

        assertEquals(OrderStatus.PENDING, order.getStatus());

        // 5. Step simulation across multiple ticks to watch autonomous execution
        boolean reachedCompletion = false;
        int maxTicks = 25;

        for (int tick = 1; tick <= maxTicks; tick++) {
            warehouseService.stepSimulation();

            // INVARIANT CHECK: Occupancy and robot positions must remain strictly consistent
            Set<Position> seenPositions = new HashSet<>();
            for (Robot robot : warehouse.getRobots()) {
                Position p = robot.getPosition();
                assertTrue(warehouse.isValidPosition(p), "Robot position must be within warehouse bounds: " + p);
                assertFalse(warehouse.getCell(p).isObstacle(), "Robot cannot be on obstacle cell: " + p);
                assertFalse(seenPositions.contains(p), "Two robots must never occupy the exact same position: " + p);
                seenPositions.add(p);
                assertEquals(robot, warehouse.getRobotAt(p), "Warehouse occupancy map must match robot position");
            }

            if (order.getStatus() == OrderStatus.COMPLETED) {
                reachedCompletion = true;
                break;
            }
        }

        assertTrue(reachedCompletion, "Order should reach COMPLETED status within " + maxTicks + " ticks");
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(0, orderService.getPendingOrderCount());
        assertEquals(1, orderService.getCompletedOrderCount());
    }

    @Test
    @DisplayName("Prioritized order dispatch: higher priority orders are fulfilled first")
    void testPriorityOrderFulfillmentOrder() {
        Robot r1 = new Robot("R1", new Position(0, 0), 100.0);
        warehouseService.addRobot(r1);

        // Add low priority order first
        Order lowOrder = orderService.createOrder("ORD-LOW", List.of(), 1, new Position(1, 1));
        // Add urgent high priority order second
        Order urgentOrder = orderService.createOrder("ORD-URGENT", List.of(), 10, new Position(2, 2));

        // Advance 1 tick
        warehouseService.stepSimulation();

        // Urgent order should have been picked and assigned to R1 first!
        assertEquals("R1", urgentOrder.getAssignedRobotId(), "Urgent order must be assigned before low priority order");
        assertEquals(OrderStatus.ASSIGNED, urgentOrder.getStatus());
        assertEquals(OrderStatus.PENDING, lowOrder.getStatus(), "Low priority order remains pending until robot frees up");
    }
}
