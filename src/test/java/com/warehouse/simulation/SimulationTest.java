package com.warehouse.simulation;

import com.warehouse.model.Robot;
import com.warehouse.model.RobotStatus;
import com.warehouse.model.RobotTask;
import com.warehouse.model.Warehouse;
import com.warehouse.util.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SimulationTest {

    private Warehouse warehouse;
    private CollisionManager collisionManager;
    private SimulationEngine engine;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse(5, 5);
        collisionManager = new CollisionManager();
        engine = new SimulationEngine(warehouse);
    }

    @Test
    @DisplayName("Robot moves one step along a path per tick")
    void testRobotMovesOneStepAlongPath() {
        Robot robot = new Robot("R1", new Position(0, 0));
        warehouse.addRobot(robot);

        RobotTask task = new RobotTask("T1", RobotTask.TaskType.MOVE, new Position(0, 2));
        robot.assignTask(task);

        assertEquals(new Position(0, 0), robot.getPosition());
        assertEquals(0, engine.getState().getCurrentTick());

        // Advance 1 tick
        engine.step();

        assertEquals(1, engine.getState().getCurrentTick());
        assertEquals(new Position(0, 1), robot.getPosition());
        assertEquals(RobotStatus.MOVING, robot.getStatus());
        assertTrue(warehouse.isOccupied(new Position(0, 1)));
        assertFalse(warehouse.isOccupied(new Position(0, 0)));
        assertEquals(99.0, robot.getBatteryLevel(), 0.001);
    }

    @Test
    @DisplayName("Robot reaches destination and completes task")
    void testRobotReachesDestination() {
        Robot robot = new Robot("R1", new Position(0, 0));
        warehouse.addRobot(robot);

        Position destination = new Position(0, 2);
        RobotTask task = new RobotTask("T1", RobotTask.TaskType.DELIVER, destination);
        robot.assignTask(task);

        // Step 1: moves to (0, 1)
        engine.step();
        assertEquals(new Position(0, 1), robot.getPosition());
        assertTrue(robot.hasTask());

        // Step 2: moves to (0, 2) and completes task
        engine.step();
        assertEquals(destination, robot.getPosition());
        assertFalse(robot.hasTask(), "Robot should have completed its task upon reaching destination");
        assertEquals(RobotStatus.IDLE, robot.getStatus());
        assertTrue(task.isCompleted());
    }

    @Test
    @DisplayName("Robot cannot enter an occupied cell")
    void testRobotCannotEnterOccupiedCell() {
        Robot r1 = new Robot("R1", new Position(1, 1));
        Robot r2 = new Robot("R2", new Position(1, 2)); // directly below R1

        warehouse.addRobot(r1);
        warehouse.addRobot(r2);

        // R1 wants to move to (1, 2), but R2 is stationary there
        RobotTask task = new RobotTask("T1", RobotTask.TaskType.MOVE, new Position(1, 3));
        r1.assignTask(task);

        // Step
        engine.step();

        // R1 must have stayed at (1, 1) because (1, 2) is occupied by R2
        assertEquals(new Position(1, 1), r1.getPosition());
        assertEquals(new Position(1, 2), r2.getPosition());
        assertTrue(warehouse.isOccupied(new Position(1, 1)));
        assertTrue(warehouse.isOccupied(new Position(1, 2)));
    }

    @Test
    @DisplayName("Two robots requesting the same destination are handled safely and deterministically")
    void testDestinationConflictResolution() {
        // R1 at (0, 1), R2 at (2, 1). Both request (1, 1)
        Robot r1 = new Robot("R1", new Position(0, 1));
        Robot r2 = new Robot("R2", new Position(2, 1));

        warehouse.addRobot(r1);
        warehouse.addRobot(r2);

        Map<Robot, Position> requests = new HashMap<>();
        requests.put(r1, new Position(1, 1));
        requests.put(r2, new Position(1, 1));

        Map<Robot, Position> resolved = collisionManager.resolveMovements(warehouse, requests);

        // Deterministic resolution by robot ID: R1 gets priority, R2 holds position
        assertEquals(new Position(1, 1), resolved.get(r1));
        assertEquals(new Position(2, 1), resolved.get(r2));
        assertNotEquals(resolved.get(r1), resolved.get(r2), "Robots must never resolve to the same destination");
    }

    @Test
    @DisplayName("Head-on position swap (edge collision) is strictly prevented")
    void testHeadOnSwapPrevention() {
        // R1 at (1, 1), R2 at (1, 2)
        Robot r1 = new Robot("R1", new Position(1, 1));
        Robot r2 = new Robot("R2", new Position(1, 2));

        warehouse.addRobot(r1);
        warehouse.addRobot(r2);

        // Swap request: R1 wants (1, 2), R2 wants (1, 1)
        Map<Robot, Position> requests = new HashMap<>();
        requests.put(r1, new Position(1, 2));
        requests.put(r2, new Position(1, 1));

        Map<Robot, Position> resolved = collisionManager.resolveMovements(warehouse, requests);

        // Both robots must be prevented from swapping and remain at their starting positions
        assertEquals(new Position(1, 1), resolved.get(r1), "R1 must remain in place to prevent head-on swap");
        assertEquals(new Position(1, 2), resolved.get(r2), "R2 must remain in place to prevent head-on swap");
    }

    @Test
    @DisplayName("Invalid moves (out of bounds or into obstacles) are rejected")
    void testInvalidMovesRejected() {
        Robot r1 = new Robot("R1", new Position(0, 0));
        warehouse.addRobot(r1);

        // Move outside boundary: (-1, 0)
        assertFalse(collisionManager.isValidMove(warehouse, r1, new Position(-1, 0)));

        // Move into obstacle: (1, 0)
        warehouse.setObstacle(1, 0, true);
        assertFalse(collisionManager.isValidMove(warehouse, r1, new Position(1, 0)));

        // Teleport move (distance > 1)
        assertFalse(collisionManager.isValidMove(warehouse, r1, new Position(3, 3)));

        // Proposed moves containing invalid entries should resolve safely to current position
        Map<Robot, Position> requests = new HashMap<>();
        requests.put(r1, new Position(-1, 0));

        Map<Robot, Position> resolved = collisionManager.resolveMovements(warehouse, requests);
        assertEquals(new Position(0, 0), resolved.get(r1));
    }

    @Test
    @DisplayName("Paused simulation does not advance")
    void testPausedSimulationDoesNotAdvance() throws InterruptedException {
        Robot robot = new Robot("R1", new Position(0, 0));
        warehouse.addRobot(robot);
        robot.assignTask(new RobotTask("T1", RobotTask.TaskType.MOVE, new Position(0, 3)));

        engine.start(50);
        assertTrue(engine.isRunning());

        // Wait brief period for tick
        Thread.sleep(120);
        long tickBeforePause = engine.getState().getCurrentTick();
        assertTrue(tickBeforePause > 0);

        engine.pause();
        assertTrue(engine.isPaused());

        Thread.sleep(120);
        long tickDuringPause = engine.getState().getCurrentTick();
        assertEquals(tickBeforePause, tickDuringPause, "Simulation must not advance while paused");

        // Clean shutdown
        engine.stop();
        assertFalse(engine.isRunning());
    }

    @Test
    @DisplayName("Stopped simulation does not continue advancing")
    void testStoppedSimulationDoesNotContinueAdvancing() throws InterruptedException {
        Robot robot = new Robot("R1", new Position(0, 0));
        warehouse.addRobot(robot);

        engine.start(50);
        Thread.sleep(100);

        engine.stop();
        assertFalse(engine.isRunning());
        assertFalse(engine.isPaused());

        long tickAtStop = engine.getState().getCurrentTick();
        Thread.sleep(100);
        long tickAfterStop = engine.getState().getCurrentTick();

        assertEquals(tickAtStop, tickAfterStop, "Simulation must not advance after being stopped");
    }
}
