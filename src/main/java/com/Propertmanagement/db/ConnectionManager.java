package com.Propertmanagement.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionManager {

    private ConnectionManager() {
    }

    public static Connection getConnection() throws SQLException {
        String url = DbConfig.getUrl();
        String user = DbConfig.getUser();
        String psw = DbConfig.getPassword();

        // Step 1: connect to the server only (no DB selected yet), create the DB if missing
        try (Connection serverConn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", user, psw);
             Statement stmt = serverConn.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS propertymanagement");
        }

        // Step 2: connect into that database and return it to the caller
        return DriverManager.getConnection(url, user, psw);
    }
}