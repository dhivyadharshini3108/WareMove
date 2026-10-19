package com.warehouse.simulation;

import com.warehouse.algorithm.AStarPathFinder;
import com.warehouse.algorithm.PathFinder;
import com.warehouse.model.Robot;
import com.warehouse.model.RobotStatus;
import com.warehouse.model.RobotTask;
import com.warehouse.model.Warehouse;
import com.warehouse.util.Position;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Coordinates warehouse simulation cycles, integrating pathfinding, collision management,
 * and robot task progression.
 */
public class SimulationEngine {

    private final Warehouse warehouse;
    private final PathFinder pathFinder;
    private final CollisionManager collisionManager;
    private final SimulationState state;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;

    public SimulationEngine(Warehouse warehouse) {
        this(warehouse, new AStarPathFinder(), new CollisionManager());
    }

    public SimulationEngine(Warehouse warehouse, PathFinder pathFinder, CollisionManager collisionManager) {
        if (warehouse == null) {
            throw new IllegalArgumentException("Warehouse cannot be null");
        }
        if (pathFinder == null) {
            throw new IllegalArgumentException("PathFinder cannot be null");
        }
        if (collisionManager == null) {
            throw new IllegalArgumentException("CollisionManager cannot be null");
        }
        this.warehouse = warehouse;
        this.pathFinder = pathFinder;
        this.collisionManager = collisionManager;
        this.state = new SimulationState(warehouse);
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

    public SimulationState getState() {
        return state;
    }

    public boolean isRunning() {
        return state.isRunning();
    }

    public boolean isPaused() {
        return state.isPaused();
    }

    /**
     * Alias for step(), advancing the simulation by one logical tick.
     */
    public synchronized void tick() {
        step();
    }

    /**
     * Advances the simulation by one logical tick.
     * Evaluates tasks, requests routes, resolves collisions, and executes valid robot movements.
     */
    public synchronized void step() {
        Map<Robot, Position> proposedMoves = new HashMap<>();

        // 1. Evaluate robot states and propose next positions
        for (Robot robot : warehouse.getRobots()) {
            // Depleted battery or inactive states prevent movement
            if (!robot.hasBattery() || robot.getStatus() == RobotStatus.CHARGING || robot.getStatus() == RobotStatus.ERROR) {
                proposedMoves.put(robot, robot.getPosition());
                continue;
            }

            RobotTask task = robot.getCurrentTask();
            if (task == null) {
                if (robot.getStatus() != RobotStatus.CHARGING) {
                    robot.setStatus(RobotStatus.IDLE);
                }
                proposedMoves.put(robot, robot.getPosition());
                continue;
            }

            // Check if already at task target
            if (robot.getPosition().equals(task.getTargetPosition())) {
                robot.completeTask();
                proposedMoves.put(robot, robot.getPosition());
                continue;
            }

            // Plan path if not already planned
            if (!robot.hasPath()) {
                List<Position> path = pathFinder.findPath(warehouse, robot.getPosition(), task.getTargetPosition());
                if (path != null && !path.isEmpty()) {
                    // First element is current position; queue subsequent steps
                    if (path.get(0).equals(robot.getPosition()) && path.size() > 1) {
                        robot.setPath(path.subList(1, path.size()));
                    } else if (path.size() == 1 && path.get(0).equals(robot.getPosition())) {
                        robot.completeTask();
                    } else {
                        robot.setPath(path);
                    }
                }
            }

            // Propose next step if path is available
            if (robot.hasPath()) {
                Position nextStep = robot.peekNextStep();
                proposedMoves.put(robot, nextStep);
            } else {
                // Destination unreachable or blocked
                proposedMoves.put(robot, robot.getPosition());
            }
        }

        // 2. Resolve movements deterministically to avoid collisions
        Map<Robot, Position> approvedMoves = collisionManager.resolveMovements(warehouse, proposedMoves);

        // 3. Apply approved moves and update robot statuses
        for (Map.Entry<Robot, Position> entry : approvedMoves.entrySet()) {
            Robot robot = entry.getKey();
            Position approvedPos = entry.getValue();

            if (!approvedPos.equals(robot.getPosition())) {
                boolean moved = warehouse.moveRobot(robot, approvedPos);
                if (moved) {
                    robot.popNextStep();
                    robot.setStatus(RobotStatus.MOVING);
                    robot.consumeBattery(1.0);

                    // Check for task completion
                    RobotTask currentTask = robot.getCurrentTask();
                    if (currentTask != null && robot.getPosition().equals(currentTask.getTargetPosition())) {
                        robot.completeTask();
                    }
                }
            } else {
                // Movement blocked or stationary
                if (robot.getCurrentTask() != null && robot.hasPath()) {
                    // Robot waits safely for route to clear
                }
            }
        }

        // 4. Advance tick counter
        state.incrementTick();
    }

    /**
     * Starts background simulation ticking with default 500ms period.
     */
    public synchronized void start() {
        start(500);
    }

    /**
     * Starts background simulation ticking at the specified interval in milliseconds.
     */
    public synchronized void start(long intervalMillis) {
        if (state.isRunning()) {
            if (state.isPaused()) {
                resume();
            }
            return;
        }

        state.setRunning(true);
        state.setPaused(false);

        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "WareMove-SimulationWorker");
            thread.setDaemon(true);
            return thread;
        });

        scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (state.isRunning() && !state.isPaused()) {
                    step();
                }
            } catch (Throwable t) {
                // Protect worker thread from crashing
            }
        }, 0, intervalMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Pauses the running simulation without stopping the background scheduler.
     */
    public synchronized void pause() {
        state.setPaused(true);
    }

    /**
     * Resumes a paused simulation.
     */
    public synchronized void resume() {
        if (state.isRunning()) {
            state.setPaused(false);
        }
    }

    /**
     * Stops the simulation and cleanly shuts down background threads.
     */
    public synchronized void stop() {
        state.setRunning(false);
        state.setPaused(false);

        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(200, TimeUnit.MILLISECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }
    }

    /**
     * Resets simulation engine state, stopping any running threads.
     */
    public synchronized void reset() {
        stop();
        state.reset();
    }
}
