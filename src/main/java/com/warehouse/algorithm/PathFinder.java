package com.warehouse.algorithm;

import com.warehouse.model.Warehouse;
import com.warehouse.util.Position;
import java.util.List;

/**
 * Common pathfinding abstraction for warehouse navigation.
 * All implementations adhere to 4-directional orthogonal movement (up, down, left, right)
 * with a uniform movement cost of 1 per step, no diagonal transitions,
 * and return paths including both start and destination.
 */
public interface PathFinder {

    /**
     * Finds the shortest path between start and destination on the warehouse grid.
     *
     * @param warehouse the warehouse model containing dimensions, cells, and obstacles
     * @param start the start position
     * @param destination the destination position
     * @return an ordered list of positions from start to destination inclusive.
     *         Returns an empty list if:
     *         - warehouse, start, or destination is null
     *         - start or destination is out of warehouse bounds
     *         - start or destination is an obstacle / untraversable
     *         - no traversable path connects start and destination
     *         Returns a single-element list containing start if start.equals(destination) and is traversable.
     */
    List<Position> findPath(Warehouse warehouse, Position start, Position destination);

    /**
     * Overloaded method allowing dynamic occupancy avoidance.
     *
     * @param warehouse the warehouse model
     * @param start the start position
     * @param destination the destination position
     * @param avoidOccupiedCells if true, cells occupied by other robots are treated as non-traversable
     * @return path from start to destination inclusive, or empty list
     */
    List<Position> findPath(Warehouse warehouse, Position start, Position destination, boolean avoidOccupiedCells);
}
