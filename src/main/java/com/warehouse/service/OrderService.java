package com.warehouse.service;

import com.warehouse.database.OrderRepository;
import com.warehouse.model.Order;
import com.warehouse.model.OrderStatus;
import com.warehouse.model.Product;
import com.warehouse.util.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service managing customer orders, order processing, validation, and priority scheduling.
 * Prioritizes orders using priority descending and FIFO creation-time tie-breaking.
 */
public class OrderService {

    private static final Logger LOGGER = Logger.getLogger(OrderService.class.getName());

    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final OrderRepository orderRepository;

    /**
     * Comparator for order prioritization:
     * 1. Higher priority number first
     * 2. Earlier creation timestamp first (FIFO)
     */
    public static final Comparator<Order> ORDER_PRIORITY_COMPARATOR = Comparator
            .comparingInt(Order::getPriority).reversed()
            .thenComparing(Order::getCreatedAt);

    public OrderService() {
        this(null);
    }

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Adds an order to the service registry after validating inputs.
     *
     * @param order the order to add
     * @throws IllegalArgumentException if order is null, ID is blank, or duplicate ID exists
     */
    public synchronized void addOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        if (order.getId() == null || order.getId().isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or blank");
        }
        if (orders.containsKey(order.getId())) {
            throw new IllegalArgumentException("Order with ID [" + order.getId() + "] already exists");
        }

        orders.put(order.getId(), order);
        persistOrder(order);
    }

    /**
     * Creates and registers a new order.
     *
     * @param id order ID
     * @param items product items to pick
     * @param priority non-negative priority level
     * @param deliveryPosition target dispatch position
     * @return the created Order
     */
    public synchronized Order createOrder(String id, List<Product> items, int priority, Position deliveryPosition) {
        Order order = new Order(id, items, priority, deliveryPosition);
        addOrder(order);
        return order;
    }

    /**
     * Finds an order by its ID.
     */
    public Optional<Order> getOrder(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(orders.get(id));
    }

    /**
     * Returns an unmodifiable list of all orders.
     */
    public List<Order> getAllOrders() {
        return Collections.unmodifiableList(new ArrayList<>(orders.values()));
    }

    /**
     * Returns all pending orders, sorted according to order priority.
     */
    public synchronized List<Order> getPendingOrders() {
        return orders.values().stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .sorted(ORDER_PRIORITY_COMPARATOR)
                .collect(Collectors.toList());
    }

    /**
     * Peeks at the highest priority pending order without changing its state.
     */
    public synchronized Optional<Order> peekNextPendingOrder() {
        return getPendingOrders().stream().findFirst();
    }

    /**
     * Retrieves and returns the highest priority pending order, or empty if none.
     */
    public synchronized Optional<Order> pollNextPendingOrder() {
        List<Order> pending = getPendingOrders();
        if (pending.isEmpty()) {
            return Optional.empty();
        }
        Order next = pending.get(0);
        return Optional.of(next);
    }

    /**
     * Updates an order's lifecycle status.
     *
     * @param id order ID
     * @param status new status
     * @return true if updated, false if order not found
     */
    public synchronized boolean updateOrderStatus(String id, OrderStatus status) {
        if (id == null || status == null) {
            return false;
        }
        Order order = orders.get(id);
        if (order != null) {
            order.setStatus(status);
            persistOrder(order);
            return true;
        }
        return false;
    }

    /**
     * Marks an order as completed.
     */
    public synchronized boolean completeOrder(String id) {
        return updateOrderStatus(id, OrderStatus.COMPLETED);
    }

    /**
     * Cancels an order.
     */
    public synchronized boolean cancelOrder(String id) {
        if (id == null) return false;
        Order order = orders.get(id);
        if (order != null) {
            order.cancel();
            persistOrder(order);
            return true;
        }
        return false;
    }

    public int getTotalOrderCount() {
        return orders.size();
    }

    public int getPendingOrderCount() {
        return (int) orders.values().stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
    }

    public int getCompletedOrderCount() {
        return (int) orders.values().stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count();
    }

    public int getInProgressOrderCount() {
        return (int) orders.values().stream().filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS || o.getStatus() == OrderStatus.ASSIGNED).count();
    }

    public void clear() {
        orders.clear();
    }

    private void persistOrder(Order order) {
        if (orderRepository != null) {
            try {
                orderRepository.save(order);
            } catch (Exception e) {
                LOGGER.fine("Could not persist order to repository: " + e.getMessage());
            }
        }
    }
}
