package com.warehouse.model;

/**
 * Represents the lifecycle status of a warehouse order.
 */
public enum OrderStatus {
    PENDING,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
