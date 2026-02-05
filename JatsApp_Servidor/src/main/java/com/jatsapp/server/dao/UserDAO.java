package com.jatsapp.server.dao;

import com.jatsapp.common.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // --- REGISTRO ---
    public boolean registerUser(User user) {
        String sql = "INSERT INTO usuarios (nombre_usuario, email, password_hash, actividad, fecha_registro) VALUES (?, ?, ?, 'desconectado', NOW())";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            // Nota: Aquí la contraseña ya debería venir hasheada desde el servicio, o hashearla aquí.
            pstmt.setString(3, user.getPassword());

            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) user.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error al registrar usuario: " + e.getMessage());
        }
        return false;
    }

    // --- LOGIN ---
    public User login(String username, String password) {
        String sql = "SELECT * FROM usuarios WHERE nombre_usuario = ? AND password_hash = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password); // Comparación directa (en producción usarías BCrypt.checkpw)

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

    // --- BUSCAR USUARIOS (Para añadir amigos) ---
    public User findUserByUsername(String username) {
        String sql = "SELECT id_usuario, nombre_usuario, actividad FROM usuarios WHERE nombre_usuario = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id_usuario"), rs.getString("nombre_usuario"), rs.getString("actividad"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- GESTIÓN DE CONTACTOS ---
    public boolean addContact(int ownerId, int contactId) {
        String sql = "INSERT INTO contactos (id_propietario, id_contacto) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ownerId);
            pstmt.setInt(2, contactId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            // Ignorar error si ya son amigos (Duplicate entry)
            return false;
        }
    }

    public List<User> getContacts(int userId) {
        List<User> contacts = new ArrayList<>();
        String sql = "SELECT u.id_usuario, u.nombre_usuario, u.actividad FROM usuarios u " +
                "JOIN contactos c ON u.id_usuario = c.id_contacto " +
                "WHERE c.id_propietario = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                contacts.add(new User(
                        rs.getInt("id_usuario"),
                        rs.getString("nombre_usuario"),
                        rs.getString("actividad")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return contacts;
    }
}