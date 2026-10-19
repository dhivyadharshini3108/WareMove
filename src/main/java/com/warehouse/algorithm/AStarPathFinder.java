package com.warehouse.algorithm;

import com.warehouse.model.Warehouse;
import com.warehouse.util.Position;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A* PathFinder using Manhattan distance heuristic for 4-directional grid navigation.
 * Guarantees optimal (shortest) path on uniform-cost grids with admissible heuristic.
 */
public class AStarPathFinder implements PathFinder {

    private static class Node implements Comparable<Node> {
        final Position position;
        final int gCost; // Exact cost from start to this node
        final int fCost; // gCost + hCost (estimated total cost)

        Node(Position position, int gCost, int fCost) {
            this.position = position;
            this.gCost = gCost;
            this.fCost = fCost;
        }

        @Override
        public int compareTo(Node other) {
            int cmp = Integer.compare(this.fCost, other.fCost);
            if (cmp == 0) {
                // Secondary tie-breaker: prefer higher gCost (closer to destination)
                return Integer.compare(other.gCost, this.gCost);
            }
            return cmp;
        }
    }

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

        PriorityQueue<Node> openQueue = new PriorityQueue<>();
        Map<Position, Integer> gScore = new HashMap<>();
        Map<Position, Position> cameFrom = new HashMap<>();
        Set<Position> closedSet = new HashSet<>();

        gScore.put(start, 0);
        int initialH = start.manhattanDistance(destination);
        openQueue.add(new Node(start, 0, initialH));

        boolean found = false;

        while (!openQueue.isEmpty()) {
            Node current = openQueue.poll();

            if (current.position.equals(destination)) {
                found = true;
                break;
            }

            if (closedSet.contains(current.position)) {
                continue;
            }
            closedSet.add(current.position);

            for (Position neighbor : current.position.getOrthogonalNeighbors()) {
                if (!warehouse.isValidPosition(neighbor) || !warehouse.isTraversable(neighbor)) {
                    continue;
                }

                if (avoidOccupiedCells && warehouse.isOccupied(neighbor) && !neighbor.equals(destination) && !neighbor.equals(start)) {
                    continue;
                }

                if (closedSet.contains(neighbor)) {
                    continue;
                }

                int tentativeG = current.gCost + 1; // Movement cost is 1 per cell
                int currentBestG = gScore.getOrDefault(neighbor, Integer.MAX_VALUE);

                if (tentativeG < currentBestG) {
                    cameFrom.put(neighbor, current.position);
                    gScore.put(neighbor, tentativeG);
                    int fCost = tentativeG + neighbor.manhattanDistance(destination);
                    openQueue.add(new Node(neighbor, tentativeG, fCost));
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
