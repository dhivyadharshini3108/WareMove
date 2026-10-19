package com.warehouse.simulation;

import com.warehouse.model.Order;
import com.warehouse.model.OrderStatus;
import com.warehouse.model.Robot;
import com.warehouse.model.RobotStatus;
import com.warehouse.model.Warehouse;
import com.warehouse.service.OrderService;
import com.warehouse.service.RobotAssignmentService;
import com.warehouse.service.WarehouseService;
import com.warehouse.util.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimulationControlTest {

    private WarehouseService warehouseService;
    private OrderService orderService;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse(8, 8);
        orderService = new OrderService();
        warehouseService = new WarehouseService(
                warehouse,
                new com.warehouse.algorithm.AStarPathFinder(),
                new CollisionManager(),
                orderService,
                new RobotAssignmentService(),
                null,
                null
        );
    }

    @Test
    @DisplayName("Verify simulation start, pause, resume, reset lifecycle controls")
    void testSimulationLifecycleControls() throws InterruptedException {
        Robot r = new Robot("R1", new Position(0, 0), 100.0);
        warehouseService.addRobot(r);

        SimulationEngine engine = warehouseService.getSimulationEngine();
        assertFalse(engine.isRunning());
        assertFalse(engine.isPaused());
        assertEquals(0, engine.getState().getCurrentTick());

        // 1. Start simulation with fast tick
        warehouseService.startSimulation(40);
        assertTrue(engine.isRunning());
        assertFalse(engine.isPaused());

        Thread.sleep(100);
        long tickRunning = engine.getState().getCurrentTick();
        assertTrue(tickRunning > 0, "Simulation tick should advance while running");

        // 2. Pause simulation
        warehouseService.pauseSimulation();
        assertTrue(engine.isPaused());

        Thread.sleep(100);
        long tickPaused = engine.getState().getCurrentTick();
        assertEquals(tickRunning, tickPaused, "Simulation tick must remain unchanged while paused");

        // 3. Resume simulation
        warehouseService.resumeSimulation();
        assertFalse(engine.isPaused());
        assertTrue(engine.isRunning());

        Thread.sleep(100);
        long tickResumed = engine.getState().getCurrentTick();
        assertTrue(tickResumed > tickPaused, "Simulation tick should advance after resuming");

        // 4. Reset simulation
        warehouseService.resetSimulation();
        assertFalse(engine.isRunning());
        assertFalse(engine.isPaused());
        assertEquals(0, engine.getState().getCurrentTick(), "Simulation tick must reset to 0");
        assertEquals(RobotStatus.IDLE, r.getStatus());
        assertEquals(100.0, r.getBatteryLevel(), 0.001);
    }

    @Test
    @DisplayName("Verify dashboard metrics and order updates on simulation steps")
    void testDashboardUpdatesAndMetrics() {
        warehouseService.setupDefaultWarehouse();

        assertEquals(0, warehouseService.getSimulationEngine().getState().getCurrentTick());
        assertTrue(orderService.getPendingOrderCount() > 0);

        int initialPending = orderService.getPendingOrderCount();

        // Single step advancement
        warehouseService.stepSimulation();

        assertEquals(1, warehouseService.getSimulationEngine().getState().getCurrentTick());
        // Assignment must have decreased pending count or assigned robots
        assertTrue(orderService.getInProgressOrderCount() > 0);
        assertTrue(orderService.getPendingOrderCount() < initialPending);

        // Advance several steps to observe delivery
        for (int i = 0; i < 20; i++) {
            warehouseService.stepSimulation();
        }

        assertTrue(warehouseService.getSimulationEngine().getState().getCurrentTick() > 10);
        assertTrue(orderService.getCompletedOrderCount() > 0, "At least one order should be completed after 20 steps");
    }
}
