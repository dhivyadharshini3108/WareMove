package com.warehouse.model;

import com.warehouse.util.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse(5, 5);
    }

    @Test
    @DisplayName("Robot initialization and valid state updates")
    void testRobotInitializationAndState() {
        Position startPos = new Position(1, 1);
        Robot robot = new Robot("R1", startPos, 90.0);

        assertEquals("R1", robot.getId());
        assertEquals(startPos, robot.getPosition());
        assertEquals(RobotStatus.IDLE, robot.getStatus());
        assertEquals(90.0, robot.getBatteryLevel(), 0.001);
        assertFalse(robot.hasTask());
        assertFalse(robot.hasPath());

        // State update
        robot.setStatus(RobotStatus.MOVING);
        assertEquals(RobotStatus.MOVING, robot.getStatus());

        // Task assignment and completion
        RobotTask task = new RobotTask("T1", RobotTask.TaskType.PICKUP, new Position(3, 3));
        robot.assignTask(task);
        assertTrue(robot.hasTask());
        assertEquals("T1", robot.getCurrentTask().getTaskId());

        RobotTask completed = robot.completeTask();
        assertNotNull(completed);
        assertTrue(completed.isCompleted());
        assertFalse(robot.hasTask());
        assertEquals(RobotStatus.IDLE, robot.getStatus());
    }

    @Test
    @DisplayName("Invalid robot data and battery values validation")
    void testInvalidRobotDataAndBattery() {
        Position pos = new Position(0, 0);

        // Null / blank ID
        assertThrows(IllegalArgumentException.class, () -> new Robot(null, pos));
        assertThrows(IllegalArgumentException.class, () -> new Robot("  ", pos));

        // Null position
        assertThrows(IllegalArgumentException.class, () -> new Robot("R1", null));

        // Invalid battery levels
        assertThrows(IllegalArgumentException.class, () -> new Robot("R1", pos, -5.0));
        assertThrows(IllegalArgumentException.class, () -> new Robot("R1", pos, 105.0));
        assertThrows(IllegalArgumentException.class, () -> new Robot("R1", pos, Double.NaN));

        Robot robot = new Robot("R1", pos, 50.0);
        assertThrows(IllegalArgumentException.class, () -> robot.setBatteryLevel(-1.0));
        assertThrows(IllegalArgumentException.class, () -> robot.setBatteryLevel(100.1));

        // Battery consumption and charging
        robot.consumeBattery(20.0);
        assertEquals(30.0, robot.getBatteryLevel(), 0.001);

        robot.chargeBattery(50.0);
        assertEquals(80.0, robot.getBatteryLevel(), 0.001);

        // Charge clamps at 100
        robot.chargeBattery(50.0);
        assertEquals(100.0, robot.getBatteryLevel(), 0.001);

        // Consume to zero
        robot.consumeBattery(120.0);
        assertEquals(0.0, robot.getBatteryLevel(), 0.001);
        assertFalse(robot.hasBattery());
        assertEquals(RobotStatus.ERROR, robot.getStatus());
    }

    @Test
    @DisplayName("Warehouse dimension validation")
    void testWarehouseDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new Warehouse(0, 5));
        assertThrows(IllegalArgumentException.class, () -> new Warehouse(5, 0));
        assertThrows(IllegalArgumentException.class, () -> new Warehouse(-1, -1));

        Warehouse wh = new Warehouse(10, 8);
        assertEquals(10, wh.getWidth());
        assertEquals(8, wh.getHeight());
        assertEquals(10, wh.getCols());
        assertEquals(8, wh.getRows());
    }

    @Test
    @DisplayName("Out-of-bounds position checks")
    void testOutOfBoundsChecks() {
        assertTrue(warehouse.isValidPosition(new Position(0, 0)));
        assertTrue(warehouse.isValidPosition(new Position(4, 4)));

        assertFalse(warehouse.isValidPosition(new Position(-1, 0)));
        assertFalse(warehouse.isValidPosition(new Position(0, -1)));
        assertFalse(warehouse.isValidPosition(new Position(5, 0)));
        assertFalse(warehouse.isValidPosition(new Position(0, 5)));
        assertFalse(warehouse.isValidPosition(null));

        assertThrows(IllegalArgumentException.class, () -> warehouse.getCell(new Position(5, 5)));
        assertThrows(IllegalArgumentException.class, () -> warehouse.getCell(-1, 2));
    }

    @Test
    @DisplayName("Obstacle and traversability behavior")
    void testObstacleAndTraversability() {
        Position pos = new Position(2, 2);
        assertTrue(warehouse.isTraversable(pos));
        assertFalse(warehouse.getCell(pos).isObstacle());

        // Set obstacle
        warehouse.setObstacle(pos, true);
        assertTrue(warehouse.getCell(pos).isObstacle());
        assertFalse(warehouse.isTraversable(pos));

        // Cannot place robot on obstacle
        Robot robot = new Robot("R1", pos);
        assertThrows(IllegalArgumentException.class, () -> warehouse.addRobot(robot));

        // Clear obstacle
        warehouse.setObstacle(pos, false);
        assertFalse(warehouse.getCell(pos).isObstacle());
        assertTrue(warehouse.isTraversable(pos));
    }

    @Test
    @DisplayName("Robot occupancy and cell conflict prevention")
    void testWarehouseOccupancy() {
        Robot r1 = new Robot("R1", new Position(1, 1));
        Robot r2 = new Robot("R2", new Position(1, 1));

        assertTrue(warehouse.addRobot(r1));
        assertTrue(warehouse.isOccupied(new Position(1, 1)));
        assertEquals(r1, warehouse.getRobotAt(new Position(1, 1)));

        // Cannot add second robot to same cell
        assertFalse(warehouse.addRobot(r2));

        // Move robot
        Position target = new Position(1, 2);
        assertTrue(warehouse.moveRobot(r1, target));
        assertEquals(target, r1.getPosition());
        assertTrue(warehouse.isOccupied(target));
        assertFalse(warehouse.isOccupied(new Position(1, 1)));

        // Remove robot
        assertTrue(warehouse.removeRobot("R1"));
        assertFalse(warehouse.isOccupied(target));
        assertNull(warehouse.getRobot("R1"));
    }

    @Test
    @DisplayName("Product validation and attributes")
    void testProduct() {
        assertThrows(IllegalArgumentException.class, () -> new Product(null, "Item"));
        assertThrows(IllegalArgumentException.class, () -> new Product("P1", ""));
        assertThrows(IllegalArgumentException.class, () -> new Product("P1", "Item", -1, null));

        Product product = new Product("P1", "Widget", 10, new Position(2, 3));
        assertEquals("P1", product.getId());
        assertEquals("Widget", product.getName());
        assertEquals(10, product.getQuantity());
        assertEquals(new Position(2, 3), product.getStoragePosition());

        product.setQuantity(15);
        assertEquals(15, product.getQuantity());
    }

    @Test
    @DisplayName("Order status lifecycle updates")
    void testOrderStatusLifecycle() {
        assertThrows(IllegalArgumentException.class, () -> new Order(null));
        assertThrows(IllegalArgumentException.class, () -> new Order("O1", -1));

        Order order = new Order("O1", 5);
        assertEquals("O1", order.getId());
        assertEquals(5, order.getPriority());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertNull(order.getAssignedRobotId());

        order.addItem(new Product("P1", "Box"));
        assertEquals(1, order.getItems().size());

        order.assignToRobot("R1");
        assertEquals(OrderStatus.ASSIGNED, order.getStatus());
        assertEquals("R1", order.getAssignedRobotId());

        order.startProcessing();
        assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());

        order.complete();
        assertEquals(OrderStatus.COMPLETED, order.getStatus());

        Order order2 = new Order("O2");
        order2.assignToRobot("R2");
        order2.cancel();
        assertEquals(OrderStatus.CANCELLED, order2.getStatus());
        assertNull(order2.getAssignedRobotId());
    }
}
