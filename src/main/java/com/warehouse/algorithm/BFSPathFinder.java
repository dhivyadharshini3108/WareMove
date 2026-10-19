package com.warehouse.algorithm;

import com.warehouse.model.Warehouse;
import com.warehouse.util.Position;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Breadth-First Search (BFS) pathfinder for unweighted 4-directional warehouse grids.
 * Guarantees finding the shortest path in terms of steps.
 */
public class BFSPathFinder implements PathFinder {

    @Override
    public List<Position> findPath(Warehouse warehouse, Position start, Position destination) {
        return findPath(warehouse, start, destination, false);
    }

    @Override
    public List<Position> findPath(Warehouse warehouse, Position start, Position destination, boolean avoidOccupiedCells) {
        if (warehouse == null || start == null || destination == null) {
            return Collections.emptyList();
        }

        // Validate boundary constraints
        if (!warehouse.isValidPosition(start) || !warehouse.isValidPosition(destination)) {
            return Collections.emptyList();
        }

        // Validate traversability of endpoints
        if (!warehouse.isTraversable(start) || !warehouse.isTraversable(destination)) {
            return Collections.emptyList();
        }

        // Handle identical start and destination
        if (start.equals(destination)) {
            return List.of(start);
        }

        Queue<Position> queue = new ArrayDeque<>();
        Set<Position> visited = new HashSet<>();
        Map<Position, Position> cameFrom = new HashMap<>();

        queue.add(start);
        visited.add(start);

        boolean found = false;

        while (!queue.isEmpty()) {
            Position current = queue.poll();

            if (current.equals(destination)) {
                found = true;
                break;
            }

            for (Position neighbor : current.getOrthogonalNeighbors()) {
                if (!warehouse.isValidPosition(neighbor) || !warehouse.isTraversable(neighbor)) {
                    continue;
                }

                if (avoidOccupiedCells && warehouse.isOccupied(neighbor) && !neighbor.equals(destination) && !neighbor.equals(start)) {
                    continue;
                }

                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    cameFrom.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        if (!found) {
            return Collections.emptyList();
        }

        // Reconstruct path from destination back to start
        LinkedList<Position> path = new LinkedList<>();
        Position step = destination;
        while (step != null) {
            path.addFirst(step);
            step = cameFrom.get(step);
        }

        return path;
    }
}
