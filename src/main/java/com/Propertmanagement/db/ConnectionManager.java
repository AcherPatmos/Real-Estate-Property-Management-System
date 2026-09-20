package com.Propertmanagement.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

// Hands out JDBC connections to the configured database.
public class ConnectionManager {

    private ConnectionManager() {
    }

//   A connection to the application database named in db.url.
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                DbConfig.getUrl(),
                DbConfig.getUser(),
                DbConfig.getPassword());
    }

//    Creates the database itself if it is not already present, by connecting
//    to the server without selecting a schema
    public static void ensureDatabaseExists() throws SQLException {
        String fullUrl = DbConfig.getUrl();
        String serverUrl = serverUrlOf(fullUrl);
        String databaseName = databaseNameOf(fullUrl);

        try (Connection serverConn = DriverManager.getConnection(
                serverUrl, DbConfig.getUser(), DbConfig.getPassword());
             Statement stmt = serverConn.createStatement()) {

            // The database name comes from our own configuration file, not from
            // user input, and CREATE DATABASE does not accept a bind parameter.
            // It is validated by databaseNameOf() before reaching this line.
            stmt.execute("CREATE DATABASE IF NOT EXISTS `" + databaseName + "`");
        }
    }

    // URL parsing

//  Strips the database name out of a JDBC URL, keeping the host, the port
//  and any query parameters.
//  jdbc:mysql://localhost:3306/propertymanagement?useSSL=false becomes
//  jdbc:mysql://localhost:3306/?useSSL=false
    static String serverUrlOf(String jdbcUrl) {
        String withoutQuery = jdbcUrl;
        String query = "";

        int questionMark = jdbcUrl.indexOf('?');
        if (questionMark >= 0) {
            withoutQuery = jdbcUrl.substring(0, questionMark);
            query = jdbcUrl.substring(questionMark);
        }

        int lastSlash = withoutQuery.lastIndexOf('/');
        if (lastSlash < 0) {
            throw new IllegalStateException(
                    "db.url is not a valid JDBC URL - no '/' before the database name: " + jdbcUrl);
        }

        return withoutQuery.substring(0, lastSlash + 1) + query;
    }

//   The database name at the end of a JDBC URL, before any query parameters.
    static String databaseNameOf(String jdbcUrl) {
        String withoutQuery = jdbcUrl;

        int questionMark = jdbcUrl.indexOf('?');
        if (questionMark >= 0) {
            withoutQuery = jdbcUrl.substring(0, questionMark);
        }

        int lastSlash = withoutQuery.lastIndexOf('/');
        String name = lastSlash < 0 ? "" : withoutQuery.substring(lastSlash + 1);

        if (name.isEmpty()) {
            throw new IllegalStateException(
                    "db.url does not name a database. Expected something like "
                            + "jdbc:mysql://localhost:3306/propertymanagement - got: " + jdbcUrl);
        }

        // Guard the identifier before it is concatenated into CREATE DATABASE.
        if (!name.matches("[A-Za-z0-9_]+")) {
            throw new IllegalStateException(
                    "The database name in db.url must contain only letters, digits "
                            + "and underscores - got: " + name);
        }

        return name;
    }
}