package com.warehouse.simulation;

import com.warehouse.model.Robot;
import com.warehouse.model.Warehouse;
import com.warehouse.util.Position;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manages spatial movement validation and collision avoidance for robots.
 * Handles single-move validation, deterministic multi-robot collision resolution,
 * prevention of vertex collisions (same destination), and prevention of edge collisions (head-on swaps).
 */
public class CollisionManager {

    /**
     * Checks if a single proposed move for a robot is statically and dynamically valid
     * against current warehouse state.
     *
     * @param warehouse warehouse environment
     * @param robot the robot requesting to move
     * @param targetPosition destination cell
     * @return true if the move is safe and valid, false otherwise
     */
    public boolean isValidMove(Warehouse warehouse, Robot robot, Position targetPosition) {
        if (warehouse == null || robot == null || targetPosition == null) {
            return false;
        }

        // Must be inside warehouse bounds
        if (!warehouse.isValidPosition(targetPosition)) {
            return false;
        }

        // Must be statically traversable (not an obstacle)
        if (!warehouse.isTraversable(targetPosition)) {
            return false;
        }

        Position current = robot.getPosition();

        // Target must be adjacent (4-directional, cost 1) or identical (staying still)
        if (!current.equals(targetPosition) && current.manhattanDistance(targetPosition) != 1) {
            return false; // Diagonal or multi-step movement prohibited
        }

        // Check occupancy: if another robot currently occupies the target position
        Robot occupant = warehouse.getRobotAt(targetPosition);
        if (occupant != null && !occupant.getId().equals(robot.getId())) {
            return false;
        }

        return true;
    }

    /**
     * Deterministically resolves simultaneous move requests for all robots in a simulation tick.
     *
     * @param warehouse current warehouse state
     * @param requestedMoves map of robot to proposed destination
     * @return map of robot to approved position (either the approved move or current position if rejected)
     */
    public Map<Robot, Position> resolveMovements(Warehouse warehouse, Map<Robot, Position> requestedMoves) {
        Map<Robot, Position> resolved = new HashMap<>();
        if (warehouse == null || requestedMoves == null || requestedMoves.isEmpty()) {
            return resolved;
        }

        // 1. Initial sanitization: filter out out-of-bounds, obstacle, or illegal distance moves
        for (Map.Entry<Robot, Position> entry : requestedMoves.entrySet()) {
            Robot robot = entry.getKey();
            Position target = entry.getValue();
            Position current = robot.getPosition();

            if (target == null
                    || !warehouse.isValidPosition(target)
                    || !warehouse.isTraversable(target)
                    || (!current.equals(target) && current.manhattanDistance(target) != 1)) {
                resolved.put(robot, current);
            } else {
                resolved.put(robot, target);
            }
        }

        // 2. Prevent head-on position swaps (edge collision):
        // If Robot A at P1 wants P2 and Robot B at P2 wants P1, both must hold their positions
        List<Robot> robotsList = new ArrayList<>(resolved.keySet());
        for (int i = 0; i < robotsList.size(); i++) {
            Robot robotA = robotsList.get(i);
            Position fromA = robotA.getPosition();
            Position toA = resolved.get(robotA);

            for (int j = i + 1; j < robotsList.size(); j++) {
                Robot robotB = robotsList.get(j);
                Position fromB = robotB.getPosition();
                Position toB = resolved.get(robotB);

                if (toA.equals(fromB) && toB.equals(fromA) && !fromA.equals(fromB)) {
                    // Head-on swap detected: both robots are halted for safety
                    resolved.put(robotA, fromA);
                    resolved.put(robotB, fromB);
                }
            }
        }

        // 3. Iterative conflict resolution until fixed-point convergence
        boolean changed = true;
        while (changed) {
            changed = false;

            // Group proposals by target cell to detect destination conflicts (vertex collision)
            Map<Position, List<Robot>> targetGroups = new HashMap<>();
            for (Map.Entry<Robot, Position> entry : resolved.entrySet()) {
                targetGroups.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
            }

            for (Map.Entry<Position, List<Robot>> entry : targetGroups.entrySet()) {
                List<Robot> contenders = entry.getValue();
                if (contenders.size() > 1) {
                    // Sort deterministically by robot ID
                    contenders.sort(Comparator.comparing(Robot::getId));

                    // First robot gets to keep the requested position
                    // All other robots must revert to their original positions
                    for (int k = 1; k < contenders.size(); k++) {
                        Robot rejectedRobot = contenders.get(k);
                        Position originalPos = rejectedRobot.getPosition();
                        if (!resolved.get(rejectedRobot).equals(originalPos)) {
                            resolved.put(rejectedRobot, originalPos);
                            changed = true;
                        }
                    }
                }
            }

            // Check if any robot is moving into a cell occupied by a stationary robot
            for (Robot robot : resolved.keySet()) {
                Position target = resolved.get(robot);
                Position current = robot.getPosition();

                if (!target.equals(current)) {
                    Robot currentOccupant = warehouse.getRobotAt(target);
                    if (currentOccupant != null && !currentOccupant.getId().equals(robot.getId())) {
                        Position occupantResolvedTarget = resolved.get(currentOccupant);
                        // If occupant is not moving or has reverted to staying at target
                        if (occupantResolvedTarget == null || occupantResolvedTarget.equals(target)) {
                            resolved.put(robot, current);
                            changed = true;
                        }
                    }
                }
            }
        }

        return resolved;
    }
}
