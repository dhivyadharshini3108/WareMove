package com.warehouse.database;

import com.warehouse.model.Robot;
import com.warehouse.model.RobotStatus;
import com.warehouse.util.Position;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for persisting and querying Robot entities in MySQL.
 * Uses prepared statements and guarantees proper resource management.
 * Handles database connectivity issues gracefully without throwing unhandled runtime exceptions.
 */
public class RobotRepository {

    private static final Logger LOGGER = Logger.getLogger(RobotRepository.class.getName());
    private final DatabaseManager databaseManager;

    public RobotRepository() {
        this(DatabaseManager.getInstance());
    }

    public RobotRepository(DatabaseManager databaseManager) {
        this.databaseManager = (databaseManager != null) ? databaseManager : DatabaseManager.getInstance();
    }

    /**
     * Saves or updates a robot in the database.
     *
     * @param robot the robot to persist
     * @return true if successfully saved, false if failed or database unavailable
     */
    public boolean save(Robot robot) {
        if (robot == null) {
            return false;
        }

        String sql = "INSERT INTO robots (id, pos_x, pos_y, battery_level, status, current_task_id) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "pos_x = VALUES(pos_x), " +
                "pos_y = VALUES(pos_y), " +
                "battery_level = VALUES(battery_level), " +
                "status = VALUES(status), " +
                "current_task_id = VALUES(current_task_id)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, robot.getId());
            stmt.setInt(2, robot.getPosition().getX());
            stmt.setInt(3, robot.getPosition().getY());
            stmt.setDouble(4, robot.getBatteryLevel());
            stmt.setString(5, robot.getStatus().name());
            stmt.setString(6, robot.getCurrentTask() != null ? robot.getCurrentTask().getTaskId() : null);

            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to save robot [" + robot.getId() + "]: " + e.getMessage());
            return false;
        }
    }

    /**
     * Finds a robot by its unique identifier.
     *
     * @param id robot ID
     * @return Optional containing the Robot if found, or empty Optional
     */
    public Optional<Robot> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        String sql = "SELECT id, pos_x, pos_y, battery_level, status FROM robots WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Robot robot = mapResultSetToRobot(rs);
                    return Optional.of(robot);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to find robot [" + id + "]: " + e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Retrieves all saved robots from the database.
     *
     * @return List of robots, or empty list if none found or database offline
     */
    public List<Robot> findAll() {
        List<Robot> robots = new ArrayList<>();
        String sql = "SELECT id, pos_x, pos_y, battery_level, status FROM robots ORDER BY id ASC";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                robots.add(mapResultSetToRobot(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to retrieve robots: " + e.getMessage());
        }

        return robots;
    }

    /**
     * Deletes a robot record by ID.
     *
     * @param id robot ID
     * @return true if deleted, false otherwise
     */
    public boolean delete(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }

        String sql = "DELETE FROM robots WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to delete robot [" + id + "]: " + e.getMessage());
            return false;
        }
    }

    private Robot mapResultSetToRobot(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        int x = rs.getInt("pos_x");
        int y = rs.getInt("pos_y");
        double battery = rs.getDouble("battery_level");
        String statusStr = rs.getString("status");

        Robot robot = new Robot(id, new Position(x, y), battery);
        try {
            if (statusStr != null) {
                robot.setStatus(RobotStatus.valueOf(statusStr));
            }
        } catch (IllegalArgumentException e) {
            robot.setStatus(RobotStatus.IDLE);
        }
        return robot;
    }
}
