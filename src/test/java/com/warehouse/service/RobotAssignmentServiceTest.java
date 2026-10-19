package com.warehouse.service;

import com.warehouse.algorithm.AStarPathFinder;
import com.warehouse.algorithm.PathFinder;
import com.warehouse.model.Order;
import com.warehouse.model.OrderStatus;
import com.warehouse.model.Product;
import com.warehouse.model.Robot;
import com.warehouse.model.RobotStatus;
import com.warehouse.model.RobotTask;
import com.warehouse.model.Warehouse;
import com.warehouse.util.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RobotAssignmentServiceTest {

    private Warehouse warehouse;
    private PathFinder pathFinder;
    private RobotAssignmentService assignmentService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse(8, 8);
        pathFinder = new AStarPathFinder();
        assignmentService = new RobotAssignmentService();
        orderService = new OrderService();
    }

    @Test
    @DisplayName("Selects closest eligible robot to order pickup location")
    void testFindBestRobotForOrder() {
        Robot rNear = new Robot("R-NEAR", new Position(1, 1), 100.0);
        Robot rFar = new Robot("R-FAR", new Position(7, 7), 100.0);
        warehouse.addRobot(rNear);
        warehouse.addRobot(rFar);

        Product item = new Product("P1", "Item", 1, new Position(2, 2));
        Order order = new Order("ORD-1", List.of(item), 2, new Position(0, 0));

        Optional<Robot> chosen = assignmentService.findBestRobotForOrder(warehouse, order, pathFinder);
        assertTrue(chosen.isPresent());
        assertEquals("R-NEAR", chosen.get().getId(), "Should select the closest robot to the pickup position");
    }

    @Test
    @DisplayName("Ignores busy or depleted-battery robots")
    void testIgnoreIneligibleRobots() {
        Robot rBusy = new Robot("R-BUSY", new Position(1, 1), 100.0);
        rBusy.setStatus(RobotStatus.MOVING);

        Robot rLowBattery = new Robot("R-LOW", new Position(1, 2), 5.0); // < 10% min battery
        Robot rAvailable = new Robot("R-OK", new Position(5, 5), 80.0);

        warehouse.addRobot(rBusy);
        warehouse.addRobot(rLowBattery);
        warehouse.addRobot(rAvailable);

        Order order = new Order("ORD-1", 1);
        Optional<Robot> chosen = assignmentService.findBestRobotForOrder(warehouse, order, pathFinder);

        assertTrue(chosen.isPresent());
        assertEquals("R-OK", chosen.get().getId());
    }

    @Test
    @DisplayName("Assigns order and sets initial route")
    void testAssignOrder() {
        Robot robot = new Robot("R1", new Position(0, 0), 100.0);
        warehouse.addRobot(robot);

        Product item = new Product("P1", "Item", 1, new Position(2, 0));
        Order order = new Order("ORD-1", List.of(item), 2, new Position(0, 5));

        boolean assigned = assignmentService.assignOrder(warehouse, order, robot, pathFinder);
        assertTrue(assigned);

        assertEquals("R1", order.getAssignedRobotId());
        assertTrue(robot.hasTask());
        assertEquals(RobotTask.TaskType.PICKUP, robot.getCurrentTask().getTaskType());
        assertEquals(new Position(2, 0), robot.getCurrentTask().getTargetPosition());
        assertTrue(robot.hasPath());
    }

    @Test
    @DisplayName("Batch assigns pending orders to fleet")
    void testBatchAssignPendingOrders() {
        Robot r1 = new Robot("R1", new Position(0, 0), 100.0);
        Robot r2 = new Robot("R2", new Position(0, 1), 100.0);
        warehouse.addRobot(r1);
        warehouse.addRobot(r2);

        Order o1 = orderService.createOrder("O1", List.of(), 5, new Position(3, 3));
        Order o2 = orderService.createOrder("O2", List.of(), 3, new Position(4, 4));
        Order o3 = orderService.createOrder("O3", List.of(), 1, new Position(5, 5));

        int assigned = assignmentService.assignPendingOrders(warehouse, orderService, pathFinder);
        assertEquals(2, assigned, "Only 2 idle robots available to take 2 orders");

        assertEquals(1, orderService.getPendingOrderCount(), "1 lower priority order should remain pending");
        assertEquals(OrderStatus.ASSIGNED, o1.getStatus());
        assertEquals(OrderStatus.ASSIGNED, o2.getStatus());
        assertEquals(OrderStatus.PENDING, o3.getStatus());
    }

    @Test
    @DisplayName("Process task transition from PICKUP to DELIVER to COMPLETED")
    void testProcessTaskTransition() {
        Robot robot = new Robot("R1", new Position(2, 0), 100.0);
        warehouse.addRobot(robot);

        Product item = new Product("P1", "Item", 1, new Position(2, 0));
        Order order = new Order("ORD-1", List.of(item), 1, new Position(0, 0));

        // Assign task to pick up at (2, 0)
        RobotTask pickupTask = new RobotTask("T-PICKUP", RobotTask.TaskType.PICKUP, new Position(2, 0), order, item);
        robot.assignTask(pickupTask);
        order.assignToRobot(robot.getId());

        // Robot is already at (2, 0): transitioning should convert to DELIVER at (0, 0)
        assignmentService.processTaskTransition(warehouse, robot, pathFinder);

        assertEquals(RobotStatus.MOVING, robot.getStatus());
        assertNotNull(robot.getCurrentTask());
        assertEquals(RobotTask.TaskType.DELIVER, robot.getCurrentTask().getTaskType());
        assertEquals(new Position(0, 0), robot.getCurrentTask().getTargetPosition());
        assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());

        // Move robot to delivery location (0, 0)
        warehouse.moveRobot(robot, new Position(0, 0));

        // Transition should complete order
        assignmentService.processTaskTransition(warehouse, robot, pathFinder);

        assertNull(robot.getCurrentTask());
        assertEquals(RobotStatus.IDLE, robot.getStatus());
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }
}
