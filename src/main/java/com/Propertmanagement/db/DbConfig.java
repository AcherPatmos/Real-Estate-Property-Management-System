package com.Propertmanagement.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DbConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream input = DbConfig.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new RuntimeException(
                        "db.properties not found on classpath. Copy db.properties.example to db.properties and fill in your credentials.");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load db.properties", e);
        }
    }

    public static String getUrl() {
        return props.getProperty("db.url");
    }

    public static String getUser() {
        return props.getProperty("db.user");
    }

    public static String getPassword() {
        return props.getProperty("db.password");
    }
}