-- WareMove Database Schema

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
