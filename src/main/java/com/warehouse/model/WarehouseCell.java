package com.warehouse.model;

import com.warehouse.util.Position;
import java.util.Objects;

/**
 * Represents a single discrete cell on the warehouse grid.
 * Tracks static traversability, obstacle flag, and optional product storage.
 */
public class WarehouseCell {

    private final Position position;
    private boolean obstacle;
    private boolean traversable;
    private Product product;

    public WarehouseCell(Position position) {
        this(position, false);
    }

    public WarehouseCell(int x, int y) {
        this(new Position(x, y), false);
    }

    public WarehouseCell(Position position, boolean obstacle) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        this.position = position;
        this.obstacle = obstacle;
        this.traversable = !obstacle;
        this.product = null;
    }

    public Position getPosition() {
        return position;
    }

    public int getX() {
        return position.getX();
    }

    public int getY() {
        return position.getY();
    }

    public boolean isObstacle() {
        return obstacle;
    }

    public void setObstacle(boolean obstacle) {
        this.obstacle = obstacle;
        this.traversable = !obstacle;
    }

    public boolean isTraversable() {
        return traversable && !obstacle;
    }

    public void setTraversable(boolean traversable) {
        if (this.obstacle && traversable) {
            throw new IllegalArgumentException("Cannot set an obstacle cell as traversable without clearing obstacle first");
        }
        this.traversable = traversable;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public boolean hasProduct() {
        return this.product != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WarehouseCell that = (WarehouseCell) o;
        return Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position);
    }

    @Override
    public String toString() {
        return "WarehouseCell{" +
                "position=" + position +
                ", obstacle=" + obstacle +
                ", traversable=" + isTraversable() +
                (product != null ? ", product=" + product.getId() : "") +
                '}';
    }
}
