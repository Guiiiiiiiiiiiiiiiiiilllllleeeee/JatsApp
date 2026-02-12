package com.jatsapp.server.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {

    private static DatabaseManager instance;
    private Connection connection;
    private Properties properties;

    // Constructor privado (Patrón Singleton)
    private DatabaseManager() {
        loadProperties();
    }

    // Método para obtener la instancia única
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void loadProperties() {
        properties = new Properties();
        // Carga el archivo desde src/main/resources
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("FATAL: config.properties no encontrado en src/main/resources/");
            }
            properties.load(input);

            // Validar propiedades críticas
            validateProperty("db.url");
            validateProperty("db.user");
            validateProperty("db.driver");

            System.out.println("✓ config.properties cargado correctamente");
        } catch (IOException ex) {
            throw new RuntimeException("Error cargando config.properties", ex);
        }
    }

    private void validateProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Propiedad requerida faltante o vacía en config.properties: " + key);
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Cargar driver (opcional en versiones nuevas, pero recomendado)
                Class.forName(properties.getProperty("db.driver"));

                connection = DriverManager.getConnection(
                        properties.getProperty("db.url"),
                        properties.getProperty("db.user"),
                        properties.getProperty("db.password")
                );
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver de base de datos no encontrado", e);
            }
        }
        return connection;
    }
}