package com.jatsapp.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

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
            if (input == null) {
                logger.error("FATAL: config.properties no encontrado");
                throw new RuntimeException("Falta config.properties");
            }
            properties.load(input);
            logger.info("✓ Configuración cargada correctamente");
            logger.debug("DB URL: {}", properties.getProperty("db.url"));
        } catch (Exception ex) {
            logger.error("Error cargando config.properties", ex);
            throw new RuntimeException("Error cargando config.properties", ex);
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
                logger.info("✓ Conexión a base de datos establecida");
            } catch (ClassNotFoundException e) {
                logger.error("Driver MySQL no encontrado", e);
                throw new SQLException("Falta el Driver MySQL", e);
            } catch (SQLException e) {
                logger.error("Error conectando a BD: {}", properties.getProperty("db.url"), e);
                throw e;
            }
        }
        return connection;
    }

    // --- NUEVO MÉTODO PARA CREAR TABLAS AUTOMÁTICAMENTE ---
    private void initTables() {
        logger.info("🛠️ Verificando estructura de base de datos...");

        try (Connection conn = getConnection();
             InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {

            if (is == null) {
                logger.warn("⚠️ No se encontró schema.sql. Saltando creación de tablas.");
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
                logger.info("✓ Tablas verificadas/creadas exitosamente");
            }
        } catch (SQLException e) {
            logger.error("Error inicializando tablas de BD", e);
        } catch (Exception e) {
            logger.error("Error leyendo schema.sql", e);
        }
    }
}