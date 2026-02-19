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

        // 1. Intentar cargar desde archivo externo (junto al JAR)
        java.io.File externalConfig = new java.io.File("config.properties");
        if (externalConfig.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(externalConfig)) {
                properties.load(fis);
                logger.info("✓ Configuración cargada desde archivo externo: {}", externalConfig.getAbsolutePath());
                logger.debug("DB URL: {}", properties.getProperty("db.url"));
                return;
            } catch (Exception e) {
                logger.warn("Error cargando config externo, intentando interno...", e);
            }
        }

        // 2. Si no existe externo, cargar desde recursos internos (dentro del JAR)
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.error("FATAL: config.properties no encontrado");
                throw new RuntimeException("Falta config.properties");
            }
            properties.load(input);
            logger.info("✓ Configuración cargada desde recursos internos");
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

    /**
     * Elimina todos los datos de la base de datos (usuarios, mensajes, grupos, contactos, miembros).
     * Las tablas se mantienen pero quedan vacías.
     * @return true si se eliminaron los datos correctamente, false en caso de error
     */
    public boolean clearAllData() {
        logger.warn("⚠️ INICIANDO BORRADO DE TODOS LOS DATOS DE LA BASE DE DATOS");

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Desactivar las restricciones de clave foránea temporalmente
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            // Eliminar datos de todas las tablas en el orden correcto
            stmt.execute("DELETE FROM miembros_grupo");
            logger.info("✓ Tabla miembros_grupo vaciada");

            stmt.execute("DELETE FROM mensajes");
            logger.info("✓ Tabla mensajes vaciada");

            stmt.execute("DELETE FROM contactos");
            logger.info("✓ Tabla contactos vaciada");

            stmt.execute("DELETE FROM grupos");
            logger.info("✓ Tabla grupos vaciada");

            stmt.execute("DELETE FROM usuarios");
            logger.info("✓ Tabla usuarios vaciada");

            // Reiniciar los AUTO_INCREMENT
            stmt.execute("ALTER TABLE usuarios AUTO_INCREMENT = 1");
            stmt.execute("ALTER TABLE mensajes AUTO_INCREMENT = 1");
            stmt.execute("ALTER TABLE grupos AUTO_INCREMENT = 1");

            // Reactivar las restricciones de clave foránea
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            logger.warn("✅ TODOS LOS DATOS HAN SIDO ELIMINADOS DE LA BASE DE DATOS");
            return true;

        } catch (SQLException e) {
            logger.error("❌ Error eliminando datos de la base de datos", e);
            return false;
        }
    }
}