package com.warehouse.model;

import com.warehouse.util.Position;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a customer order to be fulfilled by warehouse robots.
 */
public class Order {

    private final String id;
    private final List<Product> items = new ArrayList<>();
    private OrderStatus status;
    private int priority;
    private final Instant createdAt;
    private String assignedRobotId;
    private Position deliveryPosition;

    public Order(String id) {
        this(id, 1);
    }

    public Order(String id, int priority) {
        this(id, Collections.emptyList(), priority, null);
    }

    public Order(String id, List<Product> items, int priority) {
        this(id, items, priority, null);
    }

    public Order(String id, List<Product> items, int priority, Position deliveryPosition) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or blank");
        }
        if (priority < 0) {
            throw new IllegalArgumentException("Order priority cannot be negative: " + priority);
        }
        this.id = id;
        if (items != null) {
            this.items.addAll(items);
        }
        this.priority = priority;
        this.deliveryPosition = deliveryPosition;
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
        this.assignedRobotId = null;
    }

    public String getId() {
        return id;
    }

    public synchronized List<Product> getItems() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    public synchronized void addItem(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        this.items.add(product);
    }

    public synchronized OrderStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(OrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("OrderStatus cannot be null");
        }
        this.status = status;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        if (priority < 0) {
            throw new IllegalArgumentException("Order priority cannot be negative: " + priority);
        }
        this.priority = priority;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public synchronized String getAssignedRobotId() {
        return assignedRobotId;
    }

    public synchronized void assignToRobot(String robotId) {
        if (robotId == null || robotId.isBlank()) {
            throw new IllegalArgumentException("Robot ID cannot be null or blank");
        }
        this.assignedRobotId = robotId;
        this.status = OrderStatus.ASSIGNED;
    }

    public synchronized void startProcessing() {
        this.status = OrderStatus.IN_PROGRESS;
    }

    public synchronized void complete() {
        this.status = OrderStatus.COMPLETED;
    }

    public synchronized void cancel() {
        this.status = OrderStatus.CANCELLED;
        this.assignedRobotId = null;
    }

    public Position getDeliveryPosition() {
        return deliveryPosition;
    }

    public void setDeliveryPosition(Position deliveryPosition) {
        this.deliveryPosition = deliveryPosition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", itemsCount=" + items.size() +
                ", status=" + status +
                ", priority=" + priority +
                (assignedRobotId != null ? ", assignedRobotId='" + assignedRobotId + '\'' : "") +
                '}';
    }
}
