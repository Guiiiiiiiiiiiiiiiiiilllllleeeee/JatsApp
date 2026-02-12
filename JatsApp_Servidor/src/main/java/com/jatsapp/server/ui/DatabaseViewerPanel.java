package com.jatsapp.server.ui;

import com.jatsapp.server.dao.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseViewerPanel {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseViewerPanel.class);

    public void refreshAllTables() {
        logger.info("Refrescando todas las tablas de la base de datos");
    }

    public static void refreshTableData(JTable table) {
        String tableName = table.getName();

        if (tableName == null) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0); // Limpiar tabla

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            String query = "";

            switch (tableName) {
                case "usersTable":
                    query = "SELECT id_usuario, nombre_usuario, email, actividad, ultimo_acceso FROM usuarios ORDER BY id_usuario DESC LIMIT 100";
                    break;

                case "messagesTable":
                    query = "SELECT m.id_mensaje, " +
                           "(SELECT nombre_usuario FROM usuarios WHERE id_usuario = m.id_emisor) as emisor, " +
                           "m.id_destinatario, m.tipo_destinatario, " +
                           "COALESCE(m.contenido, m.nombre_fichero) as contenido, " +
                           "m.fecha_envio " +
                           "FROM mensajes m ORDER BY m.fecha_envio DESC LIMIT 100";
                    break;

                case "groupsTable":
                    query = "SELECT id_grupo, nombre_grupo, id_admin, fecha_creacion FROM grupos ORDER BY id_grupo DESC LIMIT 100";
                    break;

                case "contactsTable":
                    query = "SELECT c.id_propietario, c.id_contacto, c.fecha_agregado " +
                           "FROM contactos c ORDER BY c.fecha_agregado DESC LIMIT 100";
                    break;

                default:
                    logger.warn("Tabla desconocida: {}", tableName);
                    return;
            }

            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                Object[] row = new Object[model.getColumnCount()];
                for (int i = 0; i < row.length; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                model.addRow(row);
            }

            logger.debug("Tabla {} actualizada con {} filas", tableName, model.getRowCount());

        } catch (SQLException e) {
            logger.error("Error actualizando tabla {}", tableName, e);
            JOptionPane.showMessageDialog(null,
                "Error al cargar datos de " + tableName + ": " + e.getMessage(),
                "Error de Base de Datos",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // Método para obtener estadísticas
    public static String getDatabaseStats() {
        StringBuilder stats = new StringBuilder();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // Contar usuarios
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuarios");
            if (rs.next()) {
                stats.append("👤 Usuarios: ").append(rs.getInt(1)).append("\n");
            }

            // Contar mensajes
            rs = stmt.executeQuery("SELECT COUNT(*) FROM mensajes");
            if (rs.next()) {
                stats.append("💬 Mensajes: ").append(rs.getInt(1)).append("\n");
            }

            // Contar grupos
            rs = stmt.executeQuery("SELECT COUNT(*) FROM grupos");
            if (rs.next()) {
                stats.append("👥 Grupos: ").append(rs.getInt(1)).append("\n");
            }

            // Usuarios activos
            rs = stmt.executeQuery("SELECT COUNT(*) FROM usuarios WHERE actividad = 'activo'");
            if (rs.next()) {
                stats.append("🟢 Usuarios Activos: ").append(rs.getInt(1)).append("\n");
            }

        } catch (SQLException e) {
            logger.error("Error obteniendo estadísticas de BD", e);
            stats.append("Error al obtener estadísticas");
        }

        return stats.toString();
    }

    // Método para ejecutar consultas SQL personalizadas
    public static ResultSet executeCustomQuery(String query) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(query);
    }
}

