package com.warehouse.util;

import java.util.List;
import java.util.Objects;

/**
 * Represents an immutable 2D discrete grid position (x, y) in the warehouse.
 * Typically x maps to column (horizontal) and y maps to row (vertical).
 */
public final class Position {

    private final int x;
    private final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /**
     * Alias for X coordinate representing grid column.
     */
    public int getCol() {
        return x;
    }

    /**
     * Alias for Y coordinate representing grid row.
     */
    public int getRow() {
        return y;
    }

    /**
     * Calculates the Manhattan distance (L1 norm) between this position and another.
     *
     * @param other target position
     * @return non-negative integer distance
     */
    public int manhattanDistance(Position other) {
        if (other == null) {
            throw new IllegalArgumentException("Target position cannot be null");
        }
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }

    /**
     * Checks if another position is orthogonally adjacent (1 cell away in 4 directions).
     *
     * @param other target position
     * @return true if adjacent, false otherwise
     */
    public boolean isAdjacent(Position other) {
        return other != null && manhattanDistance(other) == 1;
    }

    public Position up() {
        return new Position(x, y - 1);
    }

    public Position down() {
        return new Position(x, y + 1);
    }

    public Position left() {
        return new Position(x - 1, y);
    }

    public Position right() {
        return new Position(x + 1, y);
    }

    /**
     * Returns 4-directional orthogonal neighbors in standard order: Up, Down, Left, Right.
     */
    public List<Position> getOrthogonalNeighbors() {
        return List.of(up(), down(), left(), right());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return x == position.x && y == position.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
