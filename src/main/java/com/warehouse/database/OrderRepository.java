package com.warehouse.database;

import com.warehouse.model.Order;
import com.warehouse.model.OrderStatus;
import com.warehouse.model.Product;
import com.warehouse.util.Position;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Data Access Object for persisting and querying Order entities in MySQL.
 * Uses prepared statements and guarantees proper resource management.
 * Gracefully handles database exceptions and connection unavailability.
 */
public class OrderRepository {

    private static final Logger LOGGER = Logger.getLogger(OrderRepository.class.getName());
    private final DatabaseManager databaseManager;

    public OrderRepository() {
        this(DatabaseManager.getInstance());
    }

    public OrderRepository(DatabaseManager databaseManager) {
        this.databaseManager = (databaseManager != null) ? databaseManager : DatabaseManager.getInstance();
    }

    /**
     * Saves or updates an order in the database.
     *
     * @param order the order to persist
     * @return true if successfully saved, false if failed or database unavailable
     */
    public boolean save(Order order) {
        if (order == null) {
            return false;
        }

        String sql = "INSERT INTO orders (id, priority, status, delivery_x, delivery_y, assigned_robot_id, items_summary) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "priority = VALUES(priority), " +
                "status = VALUES(status), " +
                "delivery_x = VALUES(delivery_x), " +
                "delivery_y = VALUES(delivery_y), " +
                "assigned_robot_id = VALUES(assigned_robot_id), " +
                "items_summary = VALUES(items_summary)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, order.getId());
            stmt.setInt(2, order.getPriority());
            stmt.setString(3, order.getStatus().name());

            if (order.getDeliveryPosition() != null) {
                stmt.setInt(4, order.getDeliveryPosition().getX());
                stmt.setInt(5, order.getDeliveryPosition().getY());
            } else {
                stmt.setNull(4, Types.INTEGER);
                stmt.setNull(5, Types.INTEGER);
            }

            stmt.setString(6, order.getAssignedRobotId());

            String itemsSummary = order.getItems().stream()
                    .map(Product::getId)
                    .collect(Collectors.joining(","));
            stmt.setString(7, itemsSummary);

            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to save order [" + order.getId() + "]: " + e.getMessage());
            return false;
        }
    }

    /**
     * Finds an order by its unique identifier.
     *
     * @param id order ID
     * @return Optional containing the Order if found, or empty Optional
     */
    public Optional<Order> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        String sql = "SELECT id, priority, status, delivery_x, delivery_y, assigned_robot_id, items_summary FROM orders WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Order order = mapResultSetToOrder(rs);
                    return Optional.of(order);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to find order [" + id + "]: " + e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Retrieves all saved orders from the database.
     *
     * @return List of orders, or empty list if none found or database offline
     */
    public List<Order> findAll() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT id, priority, status, delivery_x, delivery_y, assigned_robot_id, items_summary FROM orders ORDER BY priority DESC, created_at ASC";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                orders.add(mapResultSetToOrder(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to retrieve orders: " + e.getMessage());
        }

        return orders;
    }

    /**
     * Retrieves orders filtered by status.
     *
     * @param status target OrderStatus
     * @return List of orders matching the status
     */
    public List<Order> findByStatus(OrderStatus status) {
        List<Order> orders = new ArrayList<>();
        if (status == null) {
            return orders;
        }

        String sql = "SELECT id, priority, status, delivery_x, delivery_y, assigned_robot_id, items_summary FROM orders WHERE status = ? ORDER BY priority DESC";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapResultSetToOrder(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to retrieve orders with status [" + status + "]: " + e.getMessage());
        }

        return orders;
    }

    /**
     * Deletes an order record by ID.
     *
     * @param id order ID
     * @return true if deleted, false otherwise
     */
    public boolean delete(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }

        String sql = "DELETE FROM orders WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to delete order [" + id + "]: " + e.getMessage());
            return false;
        }
    }

    private Order mapResultSetToOrder(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        int priority = rs.getInt("priority");
        String statusStr = rs.getString("status");
        int delX = rs.getInt("delivery_x");
        boolean delXNull = rs.wasNull();
        int delY = rs.getInt("delivery_y");
        boolean delYNull = rs.wasNull();
        String assignedRobot = rs.getString("assigned_robot_id");
        String itemsSummary = rs.getString("items_summary");

        Position deliveryPos = (!delXNull && !delYNull) ? new Position(delX, delY) : null;
        List<Product> products = new ArrayList<>();
        if (itemsSummary != null && !itemsSummary.isBlank()) {
            for (String pid : itemsSummary.split(",")) {
                String clean = pid.trim();
                if (!clean.isEmpty()) {
                    products.add(new Product(clean, "Product-" + clean));
                }
            }
        }

        Order order = new Order(id, products, priority, deliveryPos);
        try {
            if (statusStr != null) {
                order.setStatus(OrderStatus.valueOf(statusStr));
            }
        } catch (IllegalArgumentException e) {
            order.setStatus(OrderStatus.PENDING);
        }

        if (assignedRobot != null && !assignedRobot.isBlank()) {
            order.assignToRobot(assignedRobot);
        }

        return order;
    }
}
