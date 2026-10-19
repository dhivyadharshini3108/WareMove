package com.warehouse.model;

import com.warehouse.util.Position;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Represents an autonomous warehouse mobile robot.
 */
public class Robot {

    private final String id;
    private Position position;
    private RobotStatus status;
    private RobotTask currentTask;
    private final Deque<Position> plannedPath = new ArrayDeque<>();
    private double batteryLevel;

    public Robot(String id, Position initialPosition) {
        this(id, initialPosition, 100.0);
    }

    public Robot(String id, Position initialPosition, double batteryLevel) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Robot ID cannot be null or blank");
        }
        if (initialPosition == null) {
            throw new IllegalArgumentException("Initial position cannot be null");
        }
        validateBattery(batteryLevel);

        this.id = id;
        this.position = initialPosition;
        this.batteryLevel = batteryLevel;
        this.status = RobotStatus.IDLE;
        this.currentTask = null;
    }

    public String getId() {
        return id;
    }

    public synchronized Position getPosition() {
        return position;
    }

    public synchronized void setPosition(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        this.position = position;
    }

    public synchronized RobotStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(RobotStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Robot status cannot be null");
        }
        this.status = status;
    }

    public synchronized RobotTask getCurrentTask() {
        return currentTask;
    }

    public synchronized boolean hasTask() {
        return currentTask != null && !currentTask.isCompleted();
    }

    public synchronized void assignTask(RobotTask task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        this.currentTask = task;
        this.status = RobotStatus.MOVING;
    }

    public synchronized RobotTask completeTask() {
        if (this.currentTask != null) {
            this.currentTask.markCompleted();
            RobotTask completed = this.currentTask;
            this.currentTask = null;
            this.status = RobotStatus.IDLE;
            clearPath();
            return completed;
        }
        return null;
    }

    public synchronized void cancelTask() {
        this.currentTask = null;
        this.status = RobotStatus.IDLE;
        clearPath();
    }

    public synchronized double getBatteryLevel() {
        return batteryLevel;
    }

    public synchronized void setBatteryLevel(double batteryLevel) {
        validateBattery(batteryLevel);
        this.batteryLevel = batteryLevel;
    }

    public synchronized void consumeBattery(double amount) {
        if (amount < 0.0) {
            throw new IllegalArgumentException("Battery consumption amount cannot be negative: " + amount);
        }
        this.batteryLevel = Math.max(0.0, this.batteryLevel - amount);
        if (this.batteryLevel == 0.0 && this.status != RobotStatus.CHARGING) {
            this.status = RobotStatus.ERROR;
        }
    }

    public synchronized void chargeBattery(double amount) {
        if (amount < 0.0) {
            throw new IllegalArgumentException("Battery charge amount cannot be negative: " + amount);
        }
        this.batteryLevel = Math.min(100.0, this.batteryLevel + amount);
    }

    public synchronized boolean hasBattery() {
        return this.batteryLevel > 0.0;
    }

    private void validateBattery(double level) {
        if (Double.isNaN(level) || level < 0.0 || level > 100.0) {
            throw new IllegalArgumentException("Battery level must be between 0.0 and 100.0: " + level);
        }
    }

    public synchronized void setPath(List<Position> path) {
        this.plannedPath.clear();
        if (path != null) {
            for (Position p : path) {
                if (p != null) {
                    this.plannedPath.add(p);
                }
            }
        }
    }

    public synchronized boolean hasPath() {
        return !plannedPath.isEmpty();
    }

    public synchronized Position peekNextStep() {
        return plannedPath.peekFirst();
    }

    public synchronized Position popNextStep() {
        return plannedPath.pollFirst();
    }

    public synchronized List<Position> getPlannedPath() {
        return new ArrayList<>(plannedPath);
    }

    public synchronized void clearPath() {
        this.plannedPath.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Robot robot = (Robot) o;
        return Objects.equals(id, robot.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Robot{" +
                "id='" + id + '\'' +
                ", position=" + position +
                ", status=" + status +
                ", battery=" + batteryLevel + "%" +
                (currentTask != null ? ", task=" + currentTask.getTaskId() : "") +
                '}';
    }
}
