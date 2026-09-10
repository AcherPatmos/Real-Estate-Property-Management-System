package com.Propertmanagement.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaCreation {

    private static final String CREATE_PROPERTY_TABLE =
            "CREATE TABLE IF NOT EXISTS Property (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(255) NOT NULL," +
                    "address VARCHAR(255) NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";

    private static final String CREATE_BUILDING_TABLE =
            "CREATE TABLE IF NOT EXISTS Building (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "property_id INT NOT NULL," +
                    "name VARCHAR(255) NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (property_id) REFERENCES Property(id) ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_FLOOR_TABLE =
            "CREATE TABLE IF NOT EXISTS Floor (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "building_id INT NOT NULL," +
                    "floor_number INT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (building_id) REFERENCES Building(id) ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_UNIT_TABLE =
            "CREATE TABLE IF NOT EXISTS Unit (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "floor_id INT NOT NULL," +
                    "unit_number VARCHAR(50) NOT NULL," +
                    "status VARCHAR(50) NOT NULL DEFAULT 'VACANT'," +
                    "rent_amount DECIMAL(10,2) NOT NULL DEFAULT 0," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (floor_id) REFERENCES Floor(id) ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_TENANT_TABLE =
            "CREATE TABLE IF NOT EXISTS Tenant (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "first_name VARCHAR(100) NOT NULL," +
                    "last_name VARCHAR(100) NOT NULL," +
                    "email VARCHAR(255) NOT NULL," +
                    "phone VARCHAR(50)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
    private static final String CREATE_LEASE_TABLE =
            "CREATE TABLE IF NOT EXISTS Lease (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "tenant_id INT NOT NULL," +
                    "unit_id INT NOT NULL," +
                    "start_date DATE NOT NULL," +
                    "end_date DATE NOT NULL," +
                    "monthly_rent DECIMAL(10,2) NOT NULL," +
                    "status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (tenant_id) REFERENCES Tenant(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (unit_id) REFERENCES Unit(id) ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_PAYMENT_TABLE =
            "CREATE TABLE IF NOT EXISTS Payment (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "lease_id INT NOT NULL," +
                    "amount DECIMAL(10,2) NOT NULL," +
                    "payment_date DATE NOT NULL," +
                    "method VARCHAR(50)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (lease_id) REFERENCES Lease(id) ON DELETE CASCADE" +
                    ")";

    public static void initializeSchema() {
        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(CREATE_PROPERTY_TABLE);
            stmt.execute(CREATE_BUILDING_TABLE);
            stmt.execute(CREATE_FLOOR_TABLE);
            stmt.execute(CREATE_UNIT_TABLE);
            stmt.execute(CREATE_TENANT_TABLE);
            stmt.execute(CREATE_LEASE_TABLE);
            stmt.execute(CREATE_PAYMENT_TABLE);


            System.out.println("Schema verified/created successfully.");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema.", e);
        }
    }


}