package com.warehouse.service;

import com.warehouse.algorithm.PathFinder;
import com.warehouse.model.Order;
import com.warehouse.model.OrderStatus;
import com.warehouse.model.Product;
import com.warehouse.model.Robot;
import com.warehouse.model.RobotStatus;
import com.warehouse.model.RobotTask;
import com.warehouse.model.Warehouse;
import com.warehouse.util.Position;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service orchestrating the intelligent assignment of warehouse orders to autonomous robots.
 * Selects optimal robots based on proximity, battery health, and path availability.
 */
public class RobotAssignmentService {

    private static final Logger LOGGER = Logger.getLogger(RobotAssignmentService.class.getName());
    private static final double MIN_BATTERY_FOR_ASSIGNMENT = 10.0;

    /**
     * Finds the best idle robot to fulfill a given order.
     *
     * @param warehouse the warehouse environment
     * @param order the order to assign
     * @param pathFinder the pathfinding algorithm
     * @return Optional containing the best robot, or empty if no eligible robot is found
     */
    public Optional<Robot> findBestRobotForOrder(Warehouse warehouse, Order order, PathFinder pathFinder) {
        if (warehouse == null || order == null || pathFinder == null) {
            return Optional.empty();
        }

        Position pickupTarget = resolvePickupTarget(warehouse, order);
        if (pickupTarget == null) {
            return Optional.empty();
        }

        Robot bestRobot = null;
        int shortestDistance = Integer.MAX_VALUE;
        double bestBattery = -1.0;

        for (Robot robot : warehouse.getRobots()) {
            if (robot.getStatus() != RobotStatus.IDLE || robot.getBatteryLevel() < MIN_BATTERY_FOR_ASSIGNMENT) {
                continue;
            }

            List<Position> path = pathFinder.findPath(warehouse, robot.getPosition(), pickupTarget);
            if (path == null || path.isEmpty()) {
                if (!robot.getPosition().equals(pickupTarget)) {
                    continue;
                }
            }

            int distance = (path != null && !path.isEmpty()) ? path.size() : 0;
            double battery = robot.getBatteryLevel();

            if (distance < shortestDistance || (distance == shortestDistance && battery > bestBattery)) {
                shortestDistance = distance;
                bestBattery = battery;
                bestRobot = robot;
            }
        }

        return Optional.ofNullable(bestRobot);
    }

    /**
     * Assigns a specific order to a specific robot.
     *
     * @param warehouse the warehouse environment
     * @param order the order to assign
     * @param robot the robot receiving the assignment
     * @param pathFinder the pathfinder to calculate routes
     * @return true if successfully assigned, false otherwise
     */
    public synchronized boolean assignOrder(Warehouse warehouse, Order order, Robot robot, PathFinder pathFinder) {
        if (warehouse == null || order == null || robot == null || pathFinder == null) {
            return false;
        }

        if (robot.getStatus() != RobotStatus.IDLE || robot.getBatteryLevel() < MIN_BATTERY_FOR_ASSIGNMENT) {
            return false;
        }

        Position pickupTarget = resolvePickupTarget(warehouse, order);
        if (pickupTarget == null) {
            return false;
        }

        Product primaryItem = order.getItems().isEmpty() ? null : order.getItems().get(0);
        Position deliveryPos = order.getDeliveryPosition() != null ? order.getDeliveryPosition() : new Position(0, 0);

        RobotTask task;
        if (pickupTarget.equals(robot.getPosition()) && !pickupTarget.equals(deliveryPos)) {
            // Already at pickup location, initiate delivery
            order.startProcessing();
            task = new RobotTask("TASK-DELIVER-" + order.getId(), RobotTask.TaskType.DELIVER, deliveryPos, order, primaryItem);
        } else {
            task = new RobotTask("TASK-PICKUP-" + order.getId(), RobotTask.TaskType.PICKUP, pickupTarget, order, primaryItem);
        }

        // Calculate and set initial path
        List<Position> path = pathFinder.findPath(warehouse, robot.getPosition(), task.getTargetPosition());
        if (path != null && !path.isEmpty()) {
            if (path.get(0).equals(robot.getPosition()) && path.size() > 1) {
                robot.setPath(path.subList(1, path.size()));
            } else {
                robot.setPath(path);
            }
        }

        robot.assignTask(task);
        order.assignToRobot(robot.getId());
        LOGGER.info("Assigned order [" + order.getId() + "] to robot [" + robot.getId() + "] heading to " + task.getTargetPosition());
        return true;
    }

    /**
     * Batch processes and assigns pending orders to available idle robots.
     *
     * @param warehouse warehouse environment
     * @param orderService order management service
     * @param pathFinder pathfinding algorithm
     * @return number of newly assigned orders
     */
    public synchronized int assignPendingOrders(Warehouse warehouse, OrderService orderService, PathFinder pathFinder) {
        if (warehouse == null || orderService == null || pathFinder == null) {
            return 0;
        }

        List<Order> pending = orderService.getPendingOrders();
        int assignedCount = 0;

        for (Order order : pending) {
            Optional<Robot> bestRobot = findBestRobotForOrder(warehouse, order, pathFinder);
            if (bestRobot.isPresent()) {
                boolean success = assignOrder(warehouse, order, bestRobot.get(), pathFinder);
                if (success) {
                    assignedCount++;
                }
            }
        }

        return assignedCount;
    }

    public synchronized void processTaskTransition(Warehouse warehouse, Robot robot, PathFinder pathFinder) {
        processTaskTransition(warehouse, robot, null, pathFinder);
    }

    /**
     * Handles progression when a robot completes its current step or task.
     * Seamlessly transitions PICKUP tasks into DELIVER tasks and marks completed orders.
     */
    public synchronized void processTaskTransition(Warehouse warehouse, Robot robot, OrderService orderService, PathFinder pathFinder) {
        if (warehouse == null || robot == null || pathFinder == null) {
            return;
        }

        RobotTask currentTask = robot.getCurrentTask();

        // 1. If robot still holds active task and has arrived at task destination
        if (currentTask != null && robot.getPosition().equals(currentTask.getTargetPosition())) {
            Order order = currentTask.getOrder();
            robot.completeTask();

            if (currentTask.getTaskType() == RobotTask.TaskType.PICKUP && order != null) {
                dispatchDeliveryTask(warehouse, robot, order, currentTask.getProduct(), pathFinder);
            } else if (currentTask.getTaskType() == RobotTask.TaskType.DELIVER && order != null) {
                order.complete();
                LOGGER.info("Robot [" + robot.getId() + "] successfully fulfilled and delivered order [" + order.getId() + "]");
            }
            return;
        }

        // 2. If simulation engine already cleared completed task, check associated order state
        if (currentTask == null && orderService != null) {
            for (Order order : orderService.getAllOrders()) {
                if (robot.getId().equals(order.getAssignedRobotId())) {
                    if (order.getStatus() == OrderStatus.ASSIGNED) {
                        // Robot finished pickup, dispatch delivery
                        Product product = order.getItems().isEmpty() ? null : order.getItems().get(0);
                        dispatchDeliveryTask(warehouse, robot, order, product, pathFinder);
                        break;
                    } else if (order.getStatus() == OrderStatus.IN_PROGRESS) {
                        // Robot finished delivery
                        order.complete();
                        LOGGER.info("Robot [" + robot.getId() + "] successfully delivered order [" + order.getId() + "]");
                        break;
                    }
                }
            }
        }
    }

    private void dispatchDeliveryTask(Warehouse warehouse, Robot robot, Order order, Product product, PathFinder pathFinder) {
        order.startProcessing();

        Position deliveryTarget = order.getDeliveryPosition() != null ? order.getDeliveryPosition() : new Position(0, 0);
        Position safeDelivery = resolveTraversableTarget(warehouse, deliveryTarget);

        RobotTask deliverTask = new RobotTask("TASK-DELIVER-" + order.getId(),
                RobotTask.TaskType.DELIVER, safeDelivery, order, product);

        List<Position> path = pathFinder.findPath(warehouse, robot.getPosition(), safeDelivery);
        if (path != null && !path.isEmpty()) {
            if (path.get(0).equals(robot.getPosition()) && path.size() > 1) {
                robot.setPath(path.subList(1, path.size()));
            } else {
                robot.setPath(path);
            }
        }

        robot.assignTask(deliverTask);
        LOGGER.info("Robot [" + robot.getId() + "] picked up items for order [" + order.getId() + "]. Moving to delivery " + safeDelivery);
    }

    /**
     * Resolves the coordinate where a robot must navigate to pick up items for an order.
     */
    public Position resolvePickupTarget(Warehouse warehouse, Order order) {
        if (order == null) return null;

        if (!order.getItems().isEmpty()) {
            for (Product item : order.getItems()) {
                if (item.getStoragePosition() != null) {
                    Position safe = resolveTraversableTarget(warehouse, item.getStoragePosition());
                    if (safe != null) {
                        return safe;
                    }
                }
            }
        }

        if (order.getDeliveryPosition() != null) {
            return resolveTraversableTarget(warehouse, order.getDeliveryPosition());
        }

        return resolveTraversableTarget(warehouse, new Position(warehouse.getWidth() / 2, warehouse.getHeight() / 2));
    }

    /**
     * Ensures target position is traversable; if not, selects the closest orthogonally adjacent traversable cell.
     */
    public Position resolveTraversableTarget(Warehouse warehouse, Position target) {
        if (warehouse == null || target == null) return null;

        if (warehouse.isValidPosition(target) && warehouse.isTraversable(target)) {
            return target;
        }

        for (Position neighbor : target.getOrthogonalNeighbors()) {
            if (warehouse.isValidPosition(neighbor) && warehouse.isTraversable(neighbor)) {
                return neighbor;
            }
        }

        return null;
    }
}
