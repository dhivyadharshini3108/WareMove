package com.warehouse.simulation;

import com.warehouse.model.Warehouse;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Encapsulates the execution state of the warehouse simulation.
 * Manages simulation ticks and execution status while delegating warehouse/robot
 * state to the underlying Warehouse instance to prevent inconsistency.
 */
public class SimulationState {

    private final Warehouse warehouse;
    private final AtomicLong currentTick;
    private volatile boolean running;
    private volatile boolean paused;

    public SimulationState(Warehouse warehouse) {
        if (warehouse == null) {
            throw new IllegalArgumentException("Warehouse cannot be null");
        }
        this.warehouse = warehouse;
        this.currentTick = new AtomicLong(0);
        this.running = false;
        this.paused = false;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public long getCurrentTick() {
        return currentTick.get();
    }

    public long incrementTick() {
        return currentTick.incrementAndGet();
    }

    public void resetTick() {
        currentTick.set(0);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void reset() {
        this.running = false;
        this.paused = false;
        this.currentTick.set(0);
    }

    @Override
    public String toString() {
        return "SimulationState{" +
                "currentTick=" + currentTick.get() +
                ", running=" + running +
                ", paused=" + paused +
                ", robotsCount=" + warehouse.getRobots().size() +
                '}';
    }
}
