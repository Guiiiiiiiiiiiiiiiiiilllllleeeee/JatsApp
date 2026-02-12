package com.jatsapp.server.dao;

import com.jatsapp.common.User;
import com.jatsapp.server.service.SecurityService;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // 1. REGISTRAR USUARIO
    public boolean registerUser(User user) {
        // Hashear la contraseña antes de guardarla
        String hashedPassword = SecurityService.hashPassword(user.getPassword());
        String sql = "INSERT INTO usuarios (nombre_usuario, email, password_hash, actividad, email_verificado, fecha_registro) VALUES (?, ?, ?, 'desconectado', FALSE, NOW())";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, hashedPassword);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) user.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error registro DAO: " + e.getMessage());
        }
        return false;
    }

    // 2. LOGIN (Primer paso: Comprobar credenciales)
    public User login(String username, String password) {
        String sql = "SELECT * FROM usuarios WHERE nombre_usuario = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                String inputHash = SecurityService.hashPassword(password);

                // Comparar hashes
                if (storedHash.equals(inputHash)) {
                    User user = new User();
                    user.setId(rs.getInt("id_usuario"));
                    user.setUsername(rs.getString("nombre_usuario"));
                    user.setEmail(rs.getString("email"));
                    user.setActivityStatus(rs.getString("actividad"));
                    // NO devolvemos la password por seguridad
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 3. GUARDAR CÓDIGO 2FA
    public boolean set2FACode(int userId, String code, long expirationMillis) {
        String sql = "UPDATE usuarios SET codigo_2fa = ?, fecha_expiracion_codigo = ? WHERE id_usuario = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, code);
            pstmt.setLong(2, expirationMillis);
            pstmt.setInt(3, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 4. VERIFICAR CÓDIGO 2FA (¡Nuevo!)
    public boolean check2FA(int userId, String inputCode) {
        String sql = "SELECT codigo_2fa, fecha_expiracion_codigo FROM usuarios WHERE id_usuario = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String dbCode = rs.getString("codigo_2fa");
                long expiration = rs.getLong("fecha_expiracion_codigo");
                long now = System.currentTimeMillis();

                // Validamos: Código coincide Y no ha caducado
                if (dbCode != null && dbCode.equals(inputCode) && now < expiration) {
                    // Borrar código inmediatamente tras uso para evitar reuso
                    clear2FACode(userId);
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Método auxiliar para borrar código 2FA
    private void clear2FACode(int userId) {
        String sql = "UPDATE usuarios SET codigo_2fa = NULL, fecha_expiracion_codigo = NULL WHERE id_usuario = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 5. OBTENER CONTACTOS (mejorado: incluye contactos y chats con mensajes)
    public List<User> getContacts(int userId) {
        List<User> contacts = new ArrayList<>();
        String sql =
            "SELECT u.id_usuario, u.nombre_usuario, u.actividad " +
            "FROM usuarios u " +
            "WHERE u.id_usuario IN (" +
            "    SELECT c.id_contacto FROM contactos c WHERE c.id_propietario = ? " +
            ") AND u.id_usuario != ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId); // contactos explícitos
            pstmt.setInt(2, userId); // no incluirme a mí mismo
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                User contact = new User(
                    rs.getInt("id_usuario"),
                    rs.getString("nombre_usuario"),
                    rs.getString("actividad")
                );
                contacts.add(contact);
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo contactos: " + e.getMessage());
        }
        return contacts;
    }

    // 6. ACTUALIZAR ESTADO DE ACTIVIDAD
    public void updateActivityStatus(int userId, String status) {
        String sql = "UPDATE usuarios SET actividad = ?, ultimo_acceso = NOW() WHERE id_usuario = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error actualizando estado de usuario: " + e.getMessage());
        }
    }

    /**
     * Marca todos los usuarios como desconectados.
     * Útil al iniciar el servidor para limpiar estados inconsistentes.
     */
    public void setAllUsersOffline() {
        String sql = "UPDATE usuarios SET actividad = 'desconectado'";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int affected = pstmt.executeUpdate();
            System.out.println("✓ " + affected + " usuarios marcados como desconectados");
        } catch (SQLException e) {
            System.err.println("Error marcando usuarios como desconectados: " + e.getMessage());
        }
    }

    // En com.jatsapp.server.dao.UserDAO

    public boolean addContact(int ownerId, String contactUsername) {
        // 1. Buscamos el ID del usuario que queremos añadir
        String sqlFind = "SELECT id_usuario FROM usuarios WHERE nombre_usuario = ?";
        int contactId = -1;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlFind)) {

            pstmt.setString(1, contactUsername);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                contactId = rs.getInt("id_usuario");
            } else {
                return false; // El usuario no existe
            }

            if (contactId == ownerId) return false; // No puedes añadirte a ti mismo

            // 2. Insertamos la relación en la tabla contactos
            // Usamos IGNORE para que no de error si ya lo tienes añadido
            String sqlInsert = "INSERT IGNORE INTO contactos (id_propietario, id_contacto, fecha_agregado) VALUES (?, ?, NOW())";

            try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert)) {
                pstmtInsert.setInt(1, ownerId);
                pstmtInsert.setInt(2, contactId);
                return pstmtInsert.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    // En UserDAO.java
    public int getIdByUsername(Connection conn, String username) {
        String sql = "SELECT id_usuario FROM usuarios WHERE nombre_usuario = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id_usuario");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // No encontrado
    }

    // Verificar si el emisor es contacto del receptor
    public boolean isContact(int ownerId, int contactId) {
        String sql = "SELECT COUNT(*) FROM contactos WHERE id_propietario = ? AND id_contacto = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ownerId);
            pstmt.setInt(2, contactId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Verificar si ya hay historial de mensajes entre dos usuarios
    public boolean hasMessageHistory(int userId1, int userId2) {
        String sql = "SELECT COUNT(*) FROM mensajes WHERE " +
                     "(id_emisor = ? AND id_destinatario = ?) OR (id_emisor = ? AND id_destinatario = ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId1);
            pstmt.setInt(2, userId2);
            pstmt.setInt(3, userId2);
            pstmt.setInt(4, userId1);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Obtener usuario por ID
    public User getUserById(int userId) {
        String sql = "SELECT id_usuario, nombre_usuario, email, actividad FROM usuarios WHERE id_usuario = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id_usuario"));
                user.setUsername(rs.getString("nombre_usuario"));
                user.setEmail(rs.getString("email"));
                user.setActivityStatus(rs.getString("actividad"));
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Buscar usuarios por nombre (para que el usuario pueda enviar mensajes a cualquiera)
    public List<User> searchUsers(String searchTerm, int excludeUserId) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id_usuario, nombre_usuario, actividad FROM usuarios " +
                     "WHERE nombre_usuario LIKE ? AND id_usuario != ? LIMIT 20";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + searchTerm + "%");
            pstmt.setInt(2, excludeUserId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                User user = new User(
                    rs.getInt("id_usuario"),
                    rs.getString("nombre_usuario"),
                    rs.getString("actividad")
                );
                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // Añadir contacto por ID (más eficiente que por username)
    public boolean addContactById(int ownerId, int contactId) {
        if (contactId == ownerId || contactId <= 0) return false;

        String sql = "INSERT IGNORE INTO contactos (id_propietario, id_contacto, fecha_agregado) VALUES (?, ?, NOW())";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ownerId);
            pstmt.setInt(2, contactId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =================================================================
    // VERIFICACIÓN DE EMAIL
    // =================================================================

    /**
     * Verifica si el usuario ha confirmado su email
     */
    public boolean isEmailVerified(int userId) {
        String sql = "SELECT email_verificado FROM usuarios WHERE id_usuario = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("email_verificado");
            }
        } catch (SQLException e) {
            // Si la columna no existe, asumimos que está verificado (compatibilidad)
            System.err.println("Nota: columna email_verificado no existe, asumiendo true");
            return true;
        }
        return false;
    }

    /**
     * Marca el email del usuario como verificado
     */
    public boolean setEmailVerified(int userId, boolean verified) {
        String sql = "UPDATE usuarios SET email_verificado = ? WHERE id_usuario = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, verified);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error actualizando verificación de email: " + e.getMessage());
            return false;
        }
    }

    // Método para eliminar un contacto
    public boolean removeContact(int ownerId, String contactUsername) {
        String checkMessagesSql = "SELECT COUNT(*) FROM mensajes WHERE (id_emisor = ? AND id_destinatario = ?) OR (id_emisor = ? AND id_destinatario = ?)";
        String deleteContactSql = "DELETE FROM contactos WHERE id_propietario = ? AND id_contacto = (SELECT id_usuario FROM usuarios WHERE nombre_usuario = ?)";

        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement deleteContactStmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            if (conn == null || conn.isClosed()) {
                System.err.println("❌ No se pudo obtener una conexión válida a la base de datos.");
                return false;
            }

            // Obtener el ID del contacto usando la misma conexión
            int contactId = getIdByUsername(conn, contactUsername);
            if (contactId == -1) {
                System.out.println("El usuario no existe: " + contactUsername);
                return false;
            }

            // Verificar si hay mensajes entre los usuarios
            checkStmt = conn.prepareStatement(checkMessagesSql);
            checkStmt.setInt(1, ownerId);
            checkStmt.setInt(2, contactId);
            checkStmt.setInt(3, contactId);
            checkStmt.setInt(4, ownerId);
            rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) == 0) {
                // No hay mensajes, eliminar contacto
                deleteContactStmt = conn.prepareStatement(deleteContactSql);
                deleteContactStmt.setInt(1, ownerId);
                deleteContactStmt.setString(2, contactUsername);
                boolean result = deleteContactStmt.executeUpdate() > 0;
                System.out.println("✅ Contacto eliminado porque no había mensajes.");
                return result;
            } else {
                // Hay mensajes, no eliminar el chat
                deleteContactStmt = conn.prepareStatement(deleteContactSql);
                deleteContactStmt.setInt(1, ownerId);
                deleteContactStmt.setString(2, contactUsername);
                boolean result = deleteContactStmt.executeUpdate() > 0;
                System.out.println("✅ Contacto eliminado, pero se conservaron los mensajes.");
                return result;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al eliminar el contacto: " + e.getMessage());
            return false;
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {
                System.err.println("Error cerrando ResultSet: " + e.getMessage());
            }
            try { if (checkStmt != null) checkStmt.close(); } catch (Exception e) {
                System.err.println("Error cerrando PreparedStatement (checkStmt): " + e.getMessage());
            }
            try { if (deleteContactStmt != null) deleteContactStmt.close(); } catch (Exception e) {
                System.err.println("Error cerrando PreparedStatement (deleteContactStmt): " + e.getMessage());
            }
            try { if (conn != null) conn.close(); } catch (Exception e) {
                System.err.println("Error cerrando conexión: " + e.getMessage());
            }
        }
    }

    // Obtener todos los chats relevantes, incluyendo contactos y usuarios con mensajes
    public List<User> getRelevantChats(int userId) {
        List<User> chats = new ArrayList<>();
        String sql = "SELECT DISTINCT u.id_usuario, u.nombre_usuario, u.actividad " +
                     "FROM usuarios u " +
                     "LEFT JOIN mensajes m ON (u.id_usuario = m.id_emisor OR u.id_usuario = m.id_destinatario) " +
                     "LEFT JOIN contactos c ON (u.id_usuario = c.id_contacto AND c.id_propietario = ?) " +
                     "WHERE (m.id_emisor = ? OR m.id_destinatario = ? OR c.id_contacto IS NOT NULL) " +
                     "AND u.id_usuario != ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId); // Para contactos
            pstmt.setInt(2, userId); // Para mensajes como emisor
            pstmt.setInt(3, userId); // Para mensajes como receptor
            pstmt.setInt(4, userId); // No incluir al propio usuario
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                User chat = new User(
                    rs.getInt("id_usuario"),
                    rs.getString("nombre_usuario"),
                    rs.getString("actividad")
                );
                chats.add(chat);
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo chats relevantes: " + e.getMessage());
        }
        return chats;
    }
}
