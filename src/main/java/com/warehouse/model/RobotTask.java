package com.warehouse.model;

import com.warehouse.util.Position;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents a discrete task assigned to a warehouse robot.
 */
public class RobotTask {

    public enum TaskType {
        PICKUP,
        DELIVER,
        CHARGE,
        MOVE
    }

    private final String taskId;
    private final TaskType taskType;
    private final Position targetPosition;
    private Order order;
    private Product product;
    private boolean completed;
    private final Instant createdAt;

    public RobotTask(String taskId, TaskType taskType, Position targetPosition) {
        this(taskId, taskType, targetPosition, null, null);
    }

    public RobotTask(String taskId, TaskType taskType, Position targetPosition, Order order, Product product) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("Task ID cannot be null or blank");
        }
        if (taskType == null) {
            throw new IllegalArgumentException("Task type cannot be null");
        }
        if (targetPosition == null) {
            throw new IllegalArgumentException("Target position cannot be null");
        }
        this.taskId = taskId;
        this.taskType = taskType;
        this.targetPosition = targetPosition;
        this.order = order;
        this.product = product;
        this.completed = false;
        this.createdAt = Instant.now();
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public Position getTargetPosition() {
        return targetPosition;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        this.completed = true;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RobotTask robotTask = (RobotTask) o;
        return Objects.equals(taskId, robotTask.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    @Override
    public String toString() {
        return "RobotTask{" +
                "taskId='" + taskId + '\'' +
                ", taskType=" + taskType +
                ", targetPosition=" + targetPosition +
                ", completed=" + completed +
                (order != null ? ", orderId=" + order.getId() : "") +
                '}';
    }
}
