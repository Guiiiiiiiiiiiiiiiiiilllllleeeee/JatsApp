package com.jatsapp.server.dao;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.stream.Collectors;

public class DatabaseManager {

    private static DatabaseManager instance;
    private Connection connection;
    private Properties properties;

    private DatabaseManager() {
        loadProperties();
        // Al instanciar, intentamos inicializar las tablas
        initTables();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void loadProperties() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) throw new RuntimeException("Falta config.properties");
            properties.load(input);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName(properties.getProperty("db.driver"));
                connection = DriverManager.getConnection(
                        properties.getProperty("db.url"),
                        properties.getProperty("db.user"),
                        properties.getProperty("db.password")
                );
            } catch (ClassNotFoundException e) {
                throw new SQLException("Falta el Driver MySQL", e);
            }
        }
        return connection;
    }

    // --- NUEVO MÉTODO PARA CREAR TABLAS AUTOMÁTICAMENTE ---
    private void initTables() {
        System.out.println("🛠️ Verificando estructura de base de datos...");

        try (Connection conn = getConnection();
             InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {

            if (is == null) {
                System.err.println("⚠️ No se encontró schema.sql. Saltando creación de tablas.");
                return;
            }

            // Leer el archivo completo
            String script = new BufferedReader(new InputStreamReader(is))
                    .lines().collect(Collectors.joining("\n"));

            // Separar por punto y coma (;) para ejecutar comando a comando
            String[] commands = script.split(";");

            try (Statement stmt = conn.createStatement()) {
                for (String sql : commands) {
                    if (!sql.trim().isEmpty()) {
                        stmt.execute(sql.trim());
                    }
                }
            }
            System.out.println("✅ Tablas verificadas/creadas correctamente.");

        } catch (SQLException e) {
            System.err.println("❌ Error SQL al iniciar tablas: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error leyendo schema.sql: " + e.getMessage());
        }
    }
}