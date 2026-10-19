package com.warehouse.model;

import com.warehouse.util.Position;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the 2D grid warehouse environment.
 * Maintains dimensions, static cells, obstacles, and dynamic robot occupancy.
 */
public class Warehouse {

    private final int width;   // columns (X)
    private final int height;  // rows (Y)
    private final WarehouseCell[][] grid;
    private final Map<String, Robot> robots = new ConcurrentHashMap<>();
    private final Map<Position, Robot> occupiedCells = new ConcurrentHashMap<>();

    public Warehouse(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Warehouse dimensions must be positive: width=" + width + ", height=" + height);
        }
        this.width = width;
        this.height = height;
        this.grid = new WarehouseCell[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                this.grid[x][y] = new WarehouseCell(new Position(x, y));
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getCols() {
        return width;
    }

    public int getRows() {
        return height;
    }

    /**
     * Validates whether a position is within the warehouse grid boundaries.
     */
    public boolean isValidPosition(Position pos) {
        return pos != null && isValidPosition(pos.getX(), pos.getY());
    }

    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Retrieves the warehouse cell at the given position.
     */
    public WarehouseCell getCell(Position pos) {
        if (!isValidPosition(pos)) {
            throw new IllegalArgumentException("Position out of bounds: " + pos);
        }
        return grid[pos.getX()][pos.getY()];
    }

    public WarehouseCell getCell(int x, int y) {
        if (!isValidPosition(x, y)) {
            throw new IllegalArgumentException("Coordinates out of bounds: (" + x + ", " + y + ")");
        }
        return grid[x][y];
    }

    /**
     * Checks if a position is valid and statically traversable (not an obstacle).
     */
    public boolean isTraversable(Position pos) {
        return isValidPosition(pos) && getCell(pos).isTraversable();
    }

    public boolean isTraversable(int x, int y) {
        return isValidPosition(x, y) && getCell(x, y).isTraversable();
    }

    /**
     * Configures obstacle status at a specific position.
     */
    public void setObstacle(Position pos, boolean obstacle) {
        getCell(pos).setObstacle(obstacle);
    }

    public void setObstacle(int x, int y, boolean obstacle) {
        getCell(x, y).setObstacle(obstacle);
    }

    /**
     * Adds a robot to the warehouse and updates cell occupancy.
     *
     * @param robot the robot to add
     * @return true if added, false if already present or cell is already occupied
     */
    public synchronized boolean addRobot(Robot robot) {
        if (robot == null) {
            throw new IllegalArgumentException("Robot cannot be null");
        }
        Position pos = robot.getPosition();
        if (!isValidPosition(pos)) {
            throw new IllegalArgumentException("Robot position is out of bounds: " + pos);
        }
        if (!isTraversable(pos)) {
            throw new IllegalArgumentException("Cannot place robot on obstacle/untraversable cell: " + pos);
        }
        if (occupiedCells.containsKey(pos)) {
            Robot occupant = occupiedCells.get(pos);
            if (!occupant.getId().equals(robot.getId())) {
                return false; // Cell already occupied by another robot
            }
        }
        robots.put(robot.getId(), robot);
        occupiedCells.put(pos, robot);
        return true;
    }

    /**
     * Removes a robot from the warehouse.
     */
    public synchronized boolean removeRobot(String robotId) {
        if (robotId == null) return false;
        Robot robot = robots.remove(robotId);
        if (robot != null) {
            occupiedCells.remove(robot.getPosition());
            return true;
        }
        return false;
    }

    public synchronized boolean removeRobot(Robot robot) {
        return robot != null && removeRobot(robot.getId());
    }

    /**
     * Atomically moves a robot to a new position, updating occupancy.
     *
     * @param robot the robot to move
     * @param newPos the destination position
     * @return true if moved successfully, false if move is invalid or cell is occupied
     */
    public synchronized boolean moveRobot(Robot robot, Position newPos) {
        if (robot == null || newPos == null) {
            return false;
        }
        if (!robots.containsKey(robot.getId())) {
            return false;
        }
        if (!isValidPosition(newPos) || !isTraversable(newPos)) {
            return false;
        }

        Position oldPos = robot.getPosition();
        if (oldPos.equals(newPos)) {
            return true;
        }

        Robot occupant = occupiedCells.get(newPos);
        if (occupant != null && !occupant.getId().equals(robot.getId())) {
            return false; // Target cell occupied by another robot
        }

        occupiedCells.remove(oldPos);
        robot.setPosition(newPos);
        occupiedCells.put(newPos, robot);
        return true;
    }

    /**
     * Checks if a cell is currently occupied by any robot.
     */
    public boolean isOccupied(Position pos) {
        return pos != null && occupiedCells.containsKey(pos);
    }

    /**
     * Retrieves the robot occupying a given position, or null if empty.
     */
    public Robot getRobotAt(Position pos) {
        return pos != null ? occupiedCells.get(pos) : null;
    }

    public Robot getRobot(String robotId) {
        return robotId != null ? robots.get(robotId) : null;
    }

    public Collection<Robot> getRobots() {
        return Collections.unmodifiableCollection(new ArrayList<>(robots.values()));
    }

    public Map<Position, Robot> getOccupiedCells() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(occupiedCells));
    }

    public List<WarehouseCell> getAllCells() {
        List<WarehouseCell> list = new ArrayList<>(width * height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                list.add(grid[x][y]);
            }
        }
        return list;
    }

    @Override
    public String toString() {
        return "Warehouse{" +
                "width=" + width +
                ", height=" + height +
                ", robotsCount=" + robots.size() +
                '}';
    }
}
