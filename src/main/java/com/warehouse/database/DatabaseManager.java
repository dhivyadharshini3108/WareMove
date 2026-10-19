package com.warehouse.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages JDBC connectivity to MySQL database.
 * Loads configuration externally from db.properties, system properties, or environment variables.
 * Gracefully handles connection failures without terminating application flow.
 */
public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static volatile DatabaseManager instance;

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/waremove?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";

    private final String url;
    private final String user;
    private final String password;
    private final String driver;

    public DatabaseManager() {
        Properties props = loadConfiguration();
        this.url = System.getProperty("db.url", System.getenv().getOrDefault("DB_URL", props.getProperty("db.url", DEFAULT_URL)));
        this.user = System.getProperty("db.user", System.getenv().getOrDefault("DB_USER", props.getProperty("db.user", DEFAULT_USER)));
        this.password = System.getProperty("db.password", System.getenv().getOrDefault("DB_PASSWORD", props.getProperty("db.password", DEFAULT_PASSWORD)));
        this.driver = props.getProperty("db.driver", DEFAULT_DRIVER);
        initDriver();
    }

    public DatabaseManager(String url, String user, String password) {
        this.url = (url != null && !url.isBlank()) ? url : DEFAULT_URL;
        this.user = (user != null) ? user : DEFAULT_USER;
        this.password = (password != null) ? password : DEFAULT_PASSWORD;
        this.driver = DEFAULT_DRIVER;
        initDriver();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    private void initDriver() {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "MySQL JDBC Driver class not found: " + driver, e);
        }
    }

    private Properties loadConfiguration() {
        Properties properties = new Properties();

        // 1. Try loading from working directory db.properties
        File file = new File("db.properties");
        if (file.exists() && file.isFile()) {
            try (InputStream in = new FileInputStream(file)) {
                properties.load(in);
                return properties;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load db.properties from current directory", e);
            }
        }

        // 2. Try loading from classpath
        try (InputStream in = getClass().getResourceAsStream("/db.properties")) {
            if (in != null) {
                properties.load(in);
                return properties;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load /db.properties from classpath", e);
        }

        return properties;
    }

    /**
     * Obtains a new database connection.
     *
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Tests if the database is currently accessible.
     *
     * @return true if connection succeeds, false otherwise
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Database connection test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Initializes default database tables if connected.
     * Gracefully logs if database is unreachable.
     */
    public boolean initializeTables() {
        if (!testConnection()) {
            LOGGER.info("Database unavailable. Skipping table initialization.");
            return false;
        }

        String createRobots = "CREATE TABLE IF NOT EXISTS robots (" +
                "id VARCHAR(64) PRIMARY KEY, " +
                "pos_x INT NOT NULL, " +
                "pos_y INT NOT NULL, " +
                "battery_level DOUBLE NOT NULL, " +
                "status VARCHAR(32) NOT NULL, " +
                "current_task_id VARCHAR(64), " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";

        String createOrders = "CREATE TABLE IF NOT EXISTS orders (" +
                "id VARCHAR(64) PRIMARY KEY, " +
                "priority INT NOT NULL, " +
                "status VARCHAR(32) NOT NULL, " +
                "delivery_x INT, " +
                "delivery_y INT, " +
                "assigned_robot_id VARCHAR(64), " +
                "items_summary TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createRobots);
            stmt.executeUpdate(createOrders);
            LOGGER.info("Database tables verified/initialized successfully.");
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error initializing database tables: " + e.getMessage(), e);
            return false;
        }
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }
}
