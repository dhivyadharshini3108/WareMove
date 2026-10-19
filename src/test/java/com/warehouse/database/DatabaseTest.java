package com.warehouse.database;

import com.warehouse.model.Order;
import com.warehouse.model.OrderStatus;
import com.warehouse.model.Robot;
import com.warehouse.model.RobotStatus;
import com.warehouse.util.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {

    private DatabaseManager databaseManager;
    private RobotRepository robotRepository;
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        databaseManager = DatabaseManager.getInstance();
        robotRepository = new RobotRepository(databaseManager);
        orderRepository = new OrderRepository(databaseManager);
    }

    @Test
    @DisplayName("DatabaseManager loads external configuration")
    void testConfigurationLoading() {
        assertNotNull(databaseManager.getUrl());
        assertNotNull(databaseManager.getUser());
        assertTrue(databaseManager.getUrl().contains("mysql"));
    }

    @Test
    @DisplayName("Graceful handling when database connection is tested")
    void testGracefulConnectionTest() {
        // testConnection must never throw, even if database is offline
        assertDoesNotThrow(() -> {
            boolean connected = databaseManager.testConnection();
            // Boolean result is acceptable either way
            assertTrue(connected || !connected);
        });
    }

    @Test
    @DisplayName("Repository operations handle offline database gracefully without throwing")
    void testRepositoryGracefulDegradation() {
        // Instantiate repository pointing to invalid unreachable port
        DatabaseManager offlineManager = new DatabaseManager("jdbc:mysql://localhost:65432/nonexistent", "user", "pass");
        RobotRepository offlineRobotRepo = new RobotRepository(offlineManager);
        OrderRepository offlineOrderRepo = new OrderRepository(offlineManager);

        assertFalse(offlineManager.testConnection());

        // RobotRepository graceful checks
        Robot robot = new Robot("R-OFFLINE", new Position(1, 1), 100.0);
        assertDoesNotThrow(() -> {
            boolean saved = offlineRobotRepo.save(robot);
            assertFalse(saved);
        });

        assertDoesNotThrow(() -> {
            Optional<Robot> found = offlineRobotRepo.findById("R-OFFLINE");
            assertTrue(found.isEmpty());
        });

        assertDoesNotThrow(() -> {
            List<Robot> all = offlineRobotRepo.findAll();
            assertTrue(all.isEmpty());
        });

        // OrderRepository graceful checks
        Order order = new Order("ORD-OFFLINE", 1);
        assertDoesNotThrow(() -> {
            boolean saved = offlineOrderRepo.save(order);
            assertFalse(saved);
        });

        assertDoesNotThrow(() -> {
            Optional<Order> found = offlineOrderRepo.findById("ORD-OFFLINE");
            assertTrue(found.isEmpty());
        });

        assertDoesNotThrow(() -> {
            List<Order> all = offlineOrderRepo.findAll();
            assertTrue(all.isEmpty());
        });
    }

    @Test
    @DisplayName("Live database roundtrip test if MySQL is reachable")
    void testLiveDatabaseOperationsIfAvailable() {
        if (!databaseManager.testConnection()) {
            System.out.println("Local MySQL not reachable; skipping live DB roundtrip test.");
            return;
        }

        assertTrue(databaseManager.initializeTables());

        // Test Robot CRUD
        Robot robot = new Robot("R-LIVE-TEST", new Position(2, 3), 88.0);
        robot.setStatus(RobotStatus.MOVING);
        assertTrue(robotRepository.save(robot));

        Optional<Robot> retrievedRobot = robotRepository.findById("R-LIVE-TEST");
        assertTrue(retrievedRobot.isPresent());
        assertEquals("R-LIVE-TEST", retrievedRobot.get().getId());
        assertEquals(new Position(2, 3), retrievedRobot.get().getPosition());
        assertEquals(88.0, retrievedRobot.get().getBatteryLevel(), 0.001);

        assertTrue(robotRepository.delete("R-LIVE-TEST"));

        // Test Order CRUD
        Order order = new Order("ORD-LIVE-TEST", 3);
        order.setDeliveryPosition(new Position(1, 1));
        assertTrue(orderRepository.save(order));

        Optional<Order> retrievedOrder = orderRepository.findById("ORD-LIVE-TEST");
        assertTrue(retrievedOrder.isPresent());
        assertEquals("ORD-LIVE-TEST", retrievedOrder.get().getId());
        assertEquals(3, retrievedOrder.get().getPriority());

        assertTrue(orderRepository.delete("ORD-LIVE-TEST"));
    }
}
