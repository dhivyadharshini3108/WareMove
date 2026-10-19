package com.warehouse.service;

import com.warehouse.model.Order;
import com.warehouse.model.OrderStatus;
import com.warehouse.model.Product;
import com.warehouse.util.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService();
    }

    @Test
    @DisplayName("Successfully create and register an order")
    void testCreateOrder() {
        Product p = new Product("P1", "Item 1", 10, new Position(2, 2));
        Order order = orderService.createOrder("ORD-001", List.of(p), 2, new Position(0, 0));

        assertNotNull(order);
        assertEquals("ORD-001", order.getId());
        assertEquals(2, order.getPriority());
        assertEquals(OrderStatus.PENDING, order.getStatus());

        Optional<Order> fetched = orderService.getOrder("ORD-001");
        assertTrue(fetched.isPresent());
        assertEquals("ORD-001", fetched.get().getId());
        assertEquals(1, orderService.getTotalOrderCount());
        assertEquals(1, orderService.getPendingOrderCount());
    }

    @Test
    @DisplayName("Reject duplicate and invalid orders")
    void testValidation() {
        assertThrows(IllegalArgumentException.class, () -> orderService.addOrder(null));

        Order valid = new Order("ORD-001", 1);
        orderService.addOrder(valid);

        Order duplicate = new Order("ORD-001", 2);
        assertThrows(IllegalArgumentException.class, () -> orderService.addOrder(duplicate));
    }

    @Test
    @DisplayName("Orders are prioritized by priority descending, then createdAt ascending")
    void testOrderPrioritization() throws InterruptedException {
        Order low = new Order("ORD-LOW", 1);
        Thread.sleep(10);
        Order high1 = new Order("ORD-HIGH-1", 5);
        Thread.sleep(10);
        Order high2 = new Order("ORD-HIGH-2", 5);
        Thread.sleep(10);
        Order med = new Order("ORD-MED", 3);

        // Add in random order
        orderService.addOrder(low);
        orderService.addOrder(high2);
        orderService.addOrder(high1);
        orderService.addOrder(med);

        List<Order> pending = orderService.getPendingOrders();
        assertEquals(4, pending.size());

        // First two should be high priority (5), with high1 before high2 (FIFO)
        assertEquals("ORD-HIGH-1", pending.get(0).getId());
        assertEquals("ORD-HIGH-2", pending.get(1).getId());
        assertEquals("ORD-MED", pending.get(2).getId());
        assertEquals("ORD-LOW", pending.get(3).getId());

        // Poll next pending order
        Optional<Order> next = orderService.pollNextPendingOrder();
        assertTrue(next.isPresent());
        assertEquals("ORD-HIGH-1", next.get().getId());
    }

    @Test
    @DisplayName("Lifecycle status updates: assign, complete, cancel")
    void testOrderStatusUpdates() {
        Order order = new Order("ORD-100", 2);
        orderService.addOrder(order);

        assertEquals(OrderStatus.PENDING, order.getStatus());

        orderService.updateOrderStatus("ORD-100", OrderStatus.ASSIGNED);
        assertEquals(OrderStatus.ASSIGNED, orderService.getOrder("ORD-100").get().getStatus());
        assertEquals(0, orderService.getPendingOrderCount());
        assertEquals(1, orderService.getInProgressOrderCount());

        orderService.completeOrder("ORD-100");
        assertEquals(OrderStatus.COMPLETED, orderService.getOrder("ORD-100").get().getStatus());
        assertEquals(1, orderService.getCompletedOrderCount());
        assertEquals(0, orderService.getInProgressOrderCount());

        Order order2 = new Order("ORD-200", 1);
        orderService.addOrder(order2);
        orderService.cancelOrder("ORD-200");
        assertEquals(OrderStatus.CANCELLED, orderService.getOrder("ORD-200").get().getStatus());
    }
}
