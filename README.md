# WareMove - Autonomous Warehouse Robotics Simulator

WareMove is a modular, high-performance JavaFX desktop simulation platform that models automated warehouse logistics with autonomous mobile robots. The system orchestrates order intake, dynamic prioritization, intelligent fleet task assignment, collision-free A* pathfinding around shelving aisles, multi-phase product pickup and delivery, and optional MySQL persistence via JDBC.

---

## Architecture and Package Structure

```
com.warehouse
├── algorithm
│   ├── PathFinder.java               # Pathfinding abstraction interface
│   ├── AStarPathFinder.java          # A* algorithm with Manhattan distance heuristic
│   └── BFSPathFinder.java            # Breadth-first search pathfinding
├── app
│   ├── Main.java                     # JavaFX application entry point & dashboard launcher
│   └── SimulationDemo.java           # Interactive CLI simulation demo
├── database
│   ├── DatabaseManager.java          # JDBC connection manager with external config loading
│   ├── RobotRepository.java          # DAO for robot state persistence & retrieval
│   └── OrderRepository.java          # DAO for order lifecycle persistence & retrieval
├── model
│   ├── Order.java                    # Customer order with items, priority, and destination
│   ├── OrderStatus.java              # Order lifecycle states (PENDING, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED)
│   ├── Product.java                  # Warehouse inventory item with rack storage position
│   ├── Robot.java                    # Autonomous robot entity with battery, position, status, and path
│   ├── RobotStatus.java              # Robot operational states (IDLE, MOVING, WORKING, CHARGING, ERROR)
│   ├── RobotTask.java                # Discrete robot mission (PICKUP, DELIVER, CHARGE, MOVE)
│   ├── Warehouse.java                # 2D grid environment tracking static obstacles and dynamic occupancy
│   └── WarehouseCell.java            # Individual cell model tracking traversability and product storage
├── service
│   ├── OrderService.java             # Order intake, validation, prioritization, and lifecycle tracking
│   ├── RobotAssignmentService.java   # Intelligent robot selection, route generation, and task transitions
│   └── WarehouseService.java         # Central warehouse layout, fleet operations, and simulation orchestration
├── simulation
│   ├── CollisionManager.java         # Vertex and edge (head-on swap) collision prevention
│   ├── SimulationEngine.java         # Scheduled / step-based simulation worker loop
│   └── SimulationState.java          # Tick counting, pause/resume/running lifecycle flags
├── ui
│   ├── DashboardController.java      # JavaFX main controller wiring simulation loop & controls
│   ├── WarehouseView.java            # Custom Canvas 2D grid renderer (shelves, robots, paths, stations)
│   ├── RobotDetailsView.java         # Real-time robot cards, status badges, and battery progress bars
│   └── OrderPanel.java               # Interactive order placement form and priority queue monitor
└── util
    └── Position.java                 # Immutable 2D grid coordinate (x, y) with Manhattan distance
```

---

## Key Features

1. **Intelligent Fleet Assignment & Prioritization**:
   - Customer orders are prioritized dynamically using priority values (descending) with FIFO timestamp tie-breaking.
   - Idle robots with sufficient battery are matched based on shortest path distance to the pickup shelf.
2. **Two-Phase Autonomous Fulfillment**:
   - `PICKUP`: The assigned robot computes an optimal route around shelving obstacles to the product shelf.
   - `DELIVER`: Upon reaching the shelf, the robot transitions to transporting items to the packing/dispatch zone.
   - `COMPLETED`: When reaching the delivery area, the order is marked completed and the robot returns to `IDLE` status.
3. **Safety & Collision Avoidance**:
   - Prevents vertex collisions (multiple robots targeting the same cell) using deterministic ID resolution.
   - Prevents edge collisions (head-on cell swaps).
   - Dynamically avoids shelving units, walls, and stationary robots.
4. **Rich JavaFX Dashboard**:
   - Modern dark slate visual theme with color-coded status badges, battery meters, and trajectory dots.
   - Simulation playback controls: **Start**, **Pause**, **Resume**, **Step**, and **Reset**.
   - Adjustable simulation tick speed slider (100ms - 1000ms).
   - Real-time order creation form and sample order injectors.
5. **Database Connectivity with Graceful Fallback**:
   - MySQL persistence using JDBC prepared statements and resource management.
   - External configuration loaded from `db.properties` or environment variables.
   - **Graceful degradation**: If MySQL is offline or unconfigured, the application runs completely in-memory with zero crashes.

---

## Database Configuration and Setup

### 1. Configuration File (`src/main/resources/db.properties` or `./db.properties`)

Configuration is loaded externally without hardcoding credentials:
```properties
db.url=jdbc:mysql://localhost:3306/waremove?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true
db.user=root
db.password=
db.driver=com.mysql.cj.jdbc.Driver
```

You can also override credentials via environment variables:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

Or JVM system properties (`-Ddb.url=...`, `-Ddb.user=...`, `-Ddb.password=...`).

### 2. Schema Setup (`src/main/resources/schema.sql`)

When a valid MySQL connection is detected at startup, `DatabaseManager` automatically creates the required tables. You can also run the schema manually:

```sql
CREATE DATABASE IF NOT EXISTS waremove;
USE waremove;

CREATE TABLE IF NOT EXISTS robots (
    id VARCHAR(64) PRIMARY KEY,
    pos_x INT NOT NULL,
    pos_y INT NOT NULL,
    battery_level DOUBLE NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_task_id VARCHAR(64),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(64) PRIMARY KEY,
    priority INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    delivery_x INT,
    delivery_y INT,
    assigned_robot_id VARCHAR(64),
    items_summary TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## Instructions for Running the Application

### Prerequisites
- **Java**: JDK 21 or newer (compatible with Java 21-27)
- **Maven**: Version 3.8+
- **MySQL** (optional): 8.0+ if persistent database storage is enabled

### 1. Compile the Project
```bash
mvn clean compile
```

### 2. Run All Tests
```bash
mvn test
```

### 3. Launch JavaFX Graphical Dashboard
To run the full visual warehouse dashboard:
```bash
mvn javafx:run
```
Or run the Java class directly:
```bash
java --module-path <path-to-javafx-sdk>/lib --add-modules javafx.controls -cp target/classes com.warehouse.app.Main
```

### 4. Run Interactive CLI Demonstration
To test the simulation, routing, and collision avoidance in the console:
```bash
mvn test-compile exec:java -Dexec.mainClass="com.warehouse.app.SimulationDemo"
```

---

## Build and Test Verification

- **Compiler**: Maven Compiler Plugin with Java 21 target
- **Test Framework**: JUnit Jupiter 5.10.2
- **Results**:
  - `PathFindingTest`: 6 / 6 passed
  - `ModelTest`: 8 / 8 passed
  - `SimulationTest`: 8 / 8 passed
  - `OrderServiceTest`: 4 / 4 passed
  - `RobotAssignmentServiceTest`: 5 / 5 passed
  - `WarehouseServiceTest`: 4 / 4 passed
  - `DatabaseTest`: 4 / 4 passed
  - `SimulationControlTest`: 2 / 2 passed
  - `WarehouseIntegrationTest`: 2 / 2 passed
  - **Total**: **43 / 43 tests passing** (0 Failures, 0 Errors, 0 Skipped).
