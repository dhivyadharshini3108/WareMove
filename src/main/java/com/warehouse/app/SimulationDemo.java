package com.warehouse.app;

import com.warehouse.algorithm.AStarPathFinder;
import com.warehouse.algorithm.PathFinder;
import com.warehouse.model.Robot;
import com.warehouse.model.RobotTask;
import com.warehouse.model.Warehouse;
import com.warehouse.simulation.CollisionManager;
import com.warehouse.simulation.SimulationEngine;
import com.warehouse.util.Position;

import java.util.List;

/**
 * Interactive console demonstration of the WareMove simulation,
 * pathfinding algorithms, and collision management.
 */
public class SimulationDemo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   WareMove - Warehouse Simulation Live Demo     ");
        System.out.println("==================================================");

        // 1. Initialize a 7x7 warehouse grid
        Warehouse warehouse = new Warehouse(7, 7);

        // 2. Add obstacles to simulate warehouse shelving rows
        System.out.println("\n[1] Setting up warehouse shelves (obstacles)...");
        for (int y = 1; y <= 4; y++) {
            warehouse.setObstacle(2, y, true); // Wall at column 2
            warehouse.setObstacle(4, y, true); // Wall at column 4
        }
        System.out.println("Shelves erected at columns 2 and 4 (rows 1-4).");

        // 3. Create two autonomous robots
        System.out.println("\n[2] Deploying robots into warehouse...");
        Robot robot1 = new Robot("Robot-ALPHA", new Position(0, 2), 100.0);
        Robot robot2 = new Robot("Robot-BETA", new Position(6, 2), 100.0);
        warehouse.addRobot(robot1);
        warehouse.addRobot(robot2);

        // 4. Assign tasks that require routing around the shelves
        Position target1 = new Position(3, 2); // Center between shelves
        Position target2 = new Position(0, 0);

        robot1.assignTask(new RobotTask("TASK-PICKUP", RobotTask.TaskType.PICKUP, target1));
        robot2.assignTask(new RobotTask("TASK-DELIVER", RobotTask.TaskType.DELIVER, target2));

        // 5. Inspect A* calculated routes
        PathFinder pathFinder = new AStarPathFinder();
        List<Position> path1 = pathFinder.findPath(warehouse, robot1.getPosition(), target1);
        System.out.println("\n[3] Calculated A* route for Robot-ALPHA: " + path1);

        // 6. Instantiate simulation engine and advance step by step
        SimulationEngine engine = new SimulationEngine(warehouse, pathFinder, new CollisionManager());

        System.out.println("\n[4] Running simulation ticks:");
        printGrid(warehouse);

        for (int tick = 1; tick <= 8; tick++) {
            engine.step();
            System.out.printf("--- Tick %d --- | ALPHA: %s (Battery: %.0f%%) | BETA: %s (Battery: %.0f%%)%n",
                    engine.getState().getCurrentTick(),
                    robot1.getPosition(), robot1.getBatteryLevel(),
                    robot2.getPosition(), robot2.getBatteryLevel());

            if (!robot1.hasTask() && !robot2.hasTask()) {
                System.out.println("All robot tasks fulfilled!");
                break;
            }
        }

        System.out.println("\nFinal Warehouse State:");
        printGrid(warehouse);
        System.out.println("==================================================");
        System.out.println("Demo completed successfully!");
    }

    private static void printGrid(Warehouse warehouse) {
        System.out.println("  0 1 2 3 4 5 6  (X)");
        for (int y = 0; y < warehouse.getHeight(); y++) {
            StringBuilder sb = new StringBuilder();
            sb.append(y).append(" ");
            for (int x = 0; x < warehouse.getWidth(); x++) {
                Position p = new Position(x, y);
                if (warehouse.isOccupied(p)) {
                    Robot r = warehouse.getRobotAt(p);
                    sb.append(r.getId().contains("ALPHA") ? "A " : "B ");
                } else if (warehouse.getCell(p).isObstacle()) {
                    sb.append("# ");
                } else {
                    sb.append(". ");
                }
            }
            System.out.println(sb);
        }
        System.out.println();
    }
}
