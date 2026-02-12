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
        String sql = "INSERT INTO usuarios (nombre_usuario, email, password_hash, actividad, fecha_registro) VALUES (?, ?, ?, 'desconectado', NOW())";

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
        // Nueva consulta: contactos + usuarios con los que ha habido mensajes
        String sql = "SELECT DISTINCT u.id_usuario, u.nombre_usuario, u.actividad " +
                "FROM usuarios u " +
                "WHERE u.id_usuario != ? " +
                "AND ( " +
                "    u.id_usuario IN (SELECT c.id_contacto FROM contactos c WHERE c.id_propietario = ?) " +
                "    OR " +
                "    u.id_usuario IN ( " +
                "        SELECT m.id_emisor FROM mensajes m WHERE m.id_destinatario = ? " +
                "        UNION " +
                "        SELECT m.id_destinatario FROM mensajes m WHERE m.id_emisor = ? " +
                "    ) " +
                ")";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId); // u.id_usuario != ?
            pstmt.setInt(2, userId); // contactos
            pstmt.setInt(3, userId); // mensajes recibidos
            pstmt.setInt(4, userId); // mensajes enviados
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
            e.printStackTrace();
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
    public int getIdByUsername(String username) {
        String sql = "SELECT id_usuario FROM usuarios WHERE nombre_usuario = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

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
}