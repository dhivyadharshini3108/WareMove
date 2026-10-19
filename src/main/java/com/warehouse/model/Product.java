package com.warehouse.model;

import com.warehouse.util.Position;
import java.util.Objects;

/**
 * Represents a product stored within the warehouse.
 */
public class Product {

    private final String id;
    private String name;
    private int quantity;
    private Position storagePosition;

    public Product(String id, String name) {
        this(id, name, 0, null);
    }

    public Product(String id, String name, int quantity, Position storagePosition) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or blank");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Product quantity cannot be negative: " + quantity);
        }
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.storagePosition = storagePosition;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or blank");
        }
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Product quantity cannot be negative: " + quantity);
        }
        this.quantity = quantity;
    }

    public Position getStoragePosition() {
        return storagePosition;
    }

    public void setStoragePosition(Position storagePosition) {
        this.storagePosition = storagePosition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Product{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", quantity=" + quantity +
                ", storagePosition=" + storagePosition +
                '}';
    }
}
