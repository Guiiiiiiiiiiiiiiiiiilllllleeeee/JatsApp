package com.jatsapp.server.dao;

import com.jatsapp.common.Group;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupDAO {

    // Crear grupo requiere una TRANSACCIÓN: Crear grupo -> Añadir al admin como miembro
    public boolean createGroup(String groupName, int adminId) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false); // Inicio transacción

            // 1. Insertar Grupo
            String sqlGroup = "INSERT INTO grupos (nombre_grupo, id_admin, fecha_creacion) VALUES (?, ?, NOW())";
            PreparedStatement pstmtGroup = conn.prepareStatement(sqlGroup, Statement.RETURN_GENERATED_KEYS);
            pstmtGroup.setString(1, groupName);
            pstmtGroup.setInt(2, adminId);
            pstmtGroup.executeUpdate();

            ResultSet rs = pstmtGroup.getGeneratedKeys();
            int groupId = 0;
            if (rs.next()) {
                groupId = rs.getInt(1);
            } else {
                throw new SQLException("Error al obtener ID del grupo");
            }

            // 2. Insertar al creador como miembro
            String sqlMember = "INSERT INTO miembros_grupo (id_grupo, id_usuario, fecha_union) VALUES (?, ?, NOW())";
            PreparedStatement pstmtMember = conn.prepareStatement(sqlMember);
            pstmtMember.setInt(1, groupId);
            pstmtMember.setInt(2, adminId);
            pstmtMember.executeUpdate();

            conn.commit(); // Confirmar cambios
            conn.setAutoCommit(true);
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); conn.setAutoCommit(true); } catch (SQLException ex) {}
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean addMemberToGroup(int groupId, int userId) {
        String sql = "INSERT INTO miembros_grupo (id_grupo, id_usuario, fecha_union) VALUES (?, ?, NOW())";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public List<Group> getGroupsByUser(int userId) {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT g.id_grupo, g.nombre_grupo, g.id_admin FROM grupos g " +
                "JOIN miembros_grupo mg ON g.id_grupo = mg.id_grupo " +
                "WHERE mg.id_usuario = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                groups.add(new Group(
                        rs.getInt("id_grupo"),
                        rs.getString("nombre_grupo"),
                        rs.getInt("id_admin")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return groups;
    }
}