package com.Propertmanagement.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class DbConfig {

    static final String CONFIG_PATH_PROPERTY = "db.config";
    static final String FILE_NAME = "db.properties";

    private static Properties settings;
    private static String source;

    private DbConfig() {
    }

    public static String getUrl() {
        return required("db.url");
    }

    public static String getUser() {
        return required("db.user");
    }

    // Not "required": an empty password is legitimate on some local MySQL installs.
    public static String getPassword() {
        return settings().getProperty("db.password", "").trim();
    }

    private static String required(String key) {
        String value = settings().getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " is missing from " + source + ".");
        }
        return value.trim();
    }

    private static synchronized Properties settings() {
        if (settings == null) {
            settings = load();
        }
        return settings;
    }

    private static Properties load() {
//      signals an error if our exec. app fails to run on a new host machine
        String explicit = System.getProperty(CONFIG_PATH_PROPERTY);
        if (explicit != null && !explicit.isBlank()) {
            Path path = Path.of(explicit.trim());
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("The settings file " + path.toAbsolutePath()
                        + " does not exist. Copy db.properties.example there and fill it in.");
            }
            return fromFile(path);
        }

        // 2. Next to wherever the application was launched from.
        Path local = Path.of(FILE_NAME);
        if (Files.isRegularFile(local)) {
            return fromFile(local);
        }

        // 3. Bundled in the build; the development fallback.
        try (InputStream in = DbConfig.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
            if (in == null) {
                throw new IllegalStateException("No db.properties found. Copy db.properties.example "
                        + "to db.properties next to the application and fill in your MySQL url, "
                        + "user and password.");
            }
            Properties loaded = new Properties();
            loaded.load(in);
            source = "the db.properties bundled with the application";
            return loaded;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the bundled db.properties.", e);
        }
    }

    private static Properties fromFile(Path path) {
        Properties loaded = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            loaded.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path.toAbsolutePath() + ".", e);
        }
        source = path.toAbsolutePath().toString();
        return loaded;
    }
}