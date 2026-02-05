package com.jatsapp.server.dao;

import com.jatsapp.common.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // 1. REGISTRAR USUARIO
    public boolean registerUser(User user) {
        // Nota: Asegúrate de que user.getPassword() llegue hasheado o hashealo aquí
        String sql = "INSERT INTO usuarios (nombre_usuario, email, password_hash, actividad, fecha_registro) VALUES (?, ?, ?, 'desconectado', NOW())";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPassword());

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
        String sql = "SELECT * FROM usuarios WHERE nombre_usuario = ? AND password_hash = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password); // Recuerda comparar hashes si usas seguridad

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id_usuario"));
                user.setUsername(rs.getString("nombre_usuario"));
                user.setEmail(rs.getString("email"));
                user.setActivityStatus(rs.getString("actividad"));
                // NO devolvemos la password por seguridad
                return user;
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
                    // Opcional: Borrar código tras uso para evitar reuso
                    // clear2FACode(userId);
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 5. OBTENER CONTACTOS
    public List<User> getContacts(int userId) {
        List<User> contacts = new ArrayList<>();
        // Query con JOIN para sacar los datos del usuario amigo
        String sql = "SELECT u.id_usuario, u.nombre_usuario, u.actividad FROM usuarios u " +
                "JOIN contactos c ON u.id_usuario = c.id_contacto " +
                "WHERE c.id_propietario = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
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
}