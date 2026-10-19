package com.warehouse.service;

import com.warehouse.model.Product;
import com.warehouse.model.Robot;
import com.warehouse.model.RobotStatus;
import com.warehouse.model.Warehouse;
import com.warehouse.util.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WarehouseServiceTest {

    private WarehouseService warehouseService;

    @BeforeEach
    void setUp() {
        warehouseService = new WarehouseService(10, 10);
    }

    @Test
    @DisplayName("Setup default warehouse layout")
    void testSetupDefaultWarehouse() {
        warehouseService.setupDefaultWarehouse();
        Warehouse wh = warehouseService.getWarehouse();

        assertEquals(10, wh.getWidth());
        assertEquals(10, wh.getHeight());

        // Obstacles (shelves) must exist
        assertTrue(wh.getCell(3, 3).isObstacle());
        assertTrue(wh.getCell(4, 3).isObstacle());

        // Products populated
        assertNotNull(warehouseService.getProduct(new Position(3, 2)));

        // Robots populated
        assertFalse(wh.getRobots().isEmpty());
        assertTrue(wh.getRobots().size() >= 2);

        // Sample orders registered
        assertFalse(warehouseService.getOrderService().getAllOrders().isEmpty());
    }

    @Test
    @DisplayName("Robot and obstacle operations")
    void testRobotAndObstacleManagement() {
        Robot r = new Robot("R-CUSTOM", new Position(2, 2), 100.0);
        assertTrue(warehouseService.addRobot(r));
        assertEquals(r, warehouseService.getWarehouse().getRobot("R-CUSTOM"));

        assertTrue(warehouseService.removeRobot("R-CUSTOM"));
        assertNull(warehouseService.getWarehouse().getRobot("R-CUSTOM"));

        Position obs = new Position(5, 5);
        assertFalse(warehouseService.getWarehouse().getCell(obs).isObstacle());

        warehouseService.setObstacle(obs, true);
        assertTrue(warehouseService.getWarehouse().getCell(obs).isObstacle());

        warehouseService.setObstacle(obs, false);
        assertFalse(warehouseService.getWarehouse().getCell(obs).isObstacle());
    }

    @Test
    @DisplayName("Simulation controls lifecycle: start, pause, resume, reset")
    void testSimulationControls() {
        assertFalse(warehouseService.getSimulationEngine().isRunning());

        warehouseService.startSimulation(100);
        assertTrue(warehouseService.getSimulationEngine().isRunning());
        assertFalse(warehouseService.getSimulationEngine().isPaused());

        warehouseService.pauseSimulation();
        assertTrue(warehouseService.getSimulationEngine().isPaused());

        warehouseService.resumeSimulation();
        assertFalse(warehouseService.getSimulationEngine().isPaused());

        warehouseService.resetSimulation();
        assertFalse(warehouseService.getSimulationEngine().isRunning());
        assertEquals(0, warehouseService.getSimulationEngine().getState().getCurrentTick());
    }

    @Test
    @DisplayName("Step simulation automatically assigns orders and moves robots")
    void testStepSimulationAutonomousProgression() {
        warehouseService.setupDefaultWarehouse();
        long initialTick = warehouseService.getSimulationEngine().getState().getCurrentTick();

        warehouseService.stepSimulation();
        long afterTick = warehouseService.getSimulationEngine().getState().getCurrentTick();

        assertEquals(initialTick + 1, afterTick);
        // At least one robot should now be assigned and moving
        boolean anyActive = warehouseService.getWarehouse().getRobots().stream()
                .anyMatch(r -> r.getStatus() == RobotStatus.MOVING || r.hasTask());
        assertTrue(anyActive, "Robots should be assigned and actively processing orders");
    }
}
