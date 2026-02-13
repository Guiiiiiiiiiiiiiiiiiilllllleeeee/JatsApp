package com.jatsapp.server.dao;

import com.jatsapp.common.Group;
import com.jatsapp.common.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupDAO {

    private static final Logger logger = LoggerFactory.getLogger(GroupDAO.class);

    /**
     * Crear grupo requiere una TRANSACCIÓN: Crear grupo -> Añadir al admin como miembro
     * @return El ID del grupo creado, o -1 si falla
     */
    public int createGroup(String groupName, int adminId) {
        Connection conn = null;
        int groupId = -1;
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
            if (rs.next()) {
                groupId = rs.getInt(1);
            } else {
                throw new SQLException("Error al obtener ID del grupo");
            }

            // 2. Insertar al creador como miembro Y admin
            String sqlMember = "INSERT INTO miembros_grupo (id_grupo, id_usuario, es_admin, fecha_union) VALUES (?, ?, TRUE, NOW())";
            PreparedStatement pstmtMember = conn.prepareStatement(sqlMember);
            pstmtMember.setInt(1, groupId);
            pstmtMember.setInt(2, adminId);
            pstmtMember.executeUpdate();

            conn.commit(); // Confirmar cambios
            conn.setAutoCommit(true);

            logger.info("✓ Grupo '{}' creado con ID {} por admin {}", groupName, groupId, adminId);
            return groupId;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); conn.setAutoCommit(true); } catch (SQLException ex) {}
            }
            logger.error("Error creando grupo '{}': {}", groupName, e.getMessage());
            return -1;
        }
    }

    /**
     * Añade un miembro a un grupo
     * @return true si se añadió correctamente
     */
    public boolean addMemberToGroup(int groupId, int userId) {
        // Verificar límite de miembros antes de añadir
        int currentCount = getMemberCount(groupId);
        if (currentCount >= Group.MAX_MEMBERS) {
            logger.warn("Grupo {} ha alcanzado el límite de {} miembros", groupId, Group.MAX_MEMBERS);
            return false;
        }

        String sql = "INSERT INTO miembros_grupo (id_grupo, id_usuario, es_admin, fecha_union) VALUES (?, ?, FALSE, NOW())";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setInt(2, userId);
            boolean result = pstmt.executeUpdate() > 0;
            if (result) {
                logger.info("✓ Usuario {} añadido al grupo {}", userId, groupId);
            }
            return result;
        } catch (SQLException e) {
            logger.error("Error añadiendo usuario {} al grupo {}: {}", userId, groupId, e.getMessage());
            return false;
        }
    }

    /**
     * Elimina un miembro de un grupo
     * @return true si se eliminó correctamente
     */
    public boolean removeMemberFromGroup(int groupId, int userId) {
        // Verificar si es admin y si es el único
        if (isGroupAdmin(groupId, userId)) {
            int adminCount = getAdminCount(groupId);
            if (adminCount <= 1) {
                logger.warn("No se puede eliminar al único admin del grupo {}", groupId);
                return false;
            }
        }

        String sql = "DELETE FROM miembros_grupo WHERE id_grupo = ? AND id_usuario = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setInt(2, userId);
            boolean result = pstmt.executeUpdate() > 0;
            if (result) {
                logger.info("✓ Usuario {} eliminado del grupo {}", userId, groupId);
            }
            return result;
        } catch (SQLException e) {
            logger.error("Error eliminando usuario {} del grupo {}: {}", userId, groupId, e.getMessage());
            return false;
        }
    }

    /**
     * Permite a un usuario abandonar un grupo voluntariamente
     * Si es el único admin, no puede salir (debe nombrar otro admin primero o eliminar el grupo)
     * @return true si se procesó correctamente
     */
    public boolean leaveGroup(int groupId, int userId) {
        // Si es admin, verificar que no sea el único
        if (isGroupAdmin(groupId, userId)) {
            int adminCount = getAdminCount(groupId);
            int memberCount = getMemberCount(groupId);

            if (adminCount <= 1 && memberCount > 1) {
                // Es el único admin pero hay más miembros - no puede salir
                logger.warn("Usuario {} es el único admin del grupo {} y hay otros miembros", userId, groupId);
                return false;
            }

            if (memberCount <= 1) {
                // Es el único miembro - eliminar el grupo
                return deleteGroup(groupId);
            }
        }

        // Eliminar del grupo
        String sql = "DELETE FROM miembros_grupo WHERE id_grupo = ? AND id_usuario = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setInt(2, userId);
            boolean result = pstmt.executeUpdate() > 0;
            if (result) {
                logger.info("✓ Usuario {} abandonó el grupo {}", userId, groupId);
            }
            return result;
        } catch (SQLException e) {
            logger.error("Error al abandonar grupo {} por usuario {}: {}", groupId, userId, e.getMessage());
            return false;
        }
    }

    /**
     * Elimina un grupo y todos sus miembros
     */
    public boolean deleteGroup(int groupId) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);

            // 1. Eliminar miembros
            String sqlMembers = "DELETE FROM miembros_grupo WHERE id_grupo = ?";
            PreparedStatement pstmtMembers = conn.prepareStatement(sqlMembers);
            pstmtMembers.setInt(1, groupId);
            pstmtMembers.executeUpdate();

            // 2. Eliminar grupo
            String sqlGroup = "DELETE FROM grupos WHERE id_grupo = ?";
            PreparedStatement pstmtGroup = conn.prepareStatement(sqlGroup);
            pstmtGroup.setInt(1, groupId);
            pstmtGroup.executeUpdate();

            conn.commit();
            conn.setAutoCommit(true);

            logger.info("✓ Grupo {} eliminado completamente", groupId);
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); conn.setAutoCommit(true); } catch (SQLException ex) {}
            }
            logger.error("Error eliminando grupo {}: {}", groupId, e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene todos los grupos a los que pertenece un usuario
     */
    public List<Group> getGroupsByUser(int userId) {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT g.id_grupo, g.nombre_grupo, g.id_admin, g.fecha_creacion FROM grupos g " +
                "JOIN miembros_grupo mg ON g.id_grupo = mg.id_grupo " +
                "WHERE mg.id_usuario = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Group group = new Group(
                        rs.getInt("id_grupo"),
                        rs.getString("nombre_grupo"),
                        rs.getInt("id_admin")
                );
                group.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                groups.add(group);
            }
            logger.debug("Usuario {} pertenece a {} grupos", userId, groups.size());
        } catch (SQLException e) {
            logger.error("Error obteniendo grupos del usuario {}: {}", userId, e.getMessage());
        }
        return groups;
    }

    /**
     * Obtiene IDs de miembros de un grupo (para envío de mensajes)
     */
    public List<Integer> getGroupMemberIds(int groupId) {
        List<Integer> memberIds = new ArrayList<>();
        String sql = "SELECT id_usuario FROM miembros_grupo WHERE id_grupo = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                memberIds.add(rs.getInt("id_usuario"));
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo miembros del grupo {}: {}", groupId, e.getMessage());
        }
        return memberIds;
    }

    /**
     * Obtiene la lista completa de miembros de un grupo con sus datos de usuario
     */
    public List<User> getGroupMembers(int groupId) {
        List<User> members = new ArrayList<>();
        String sql = "SELECT u.id_usuario, u.nombre_usuario, u.email, u.actividad, mg.es_admin " +
                "FROM usuarios u " +
                "JOIN miembros_grupo mg ON u.id_usuario = mg.id_usuario " +
                "WHERE mg.id_grupo = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id_usuario"));
                user.setUsername(rs.getString("nombre_usuario"));
                user.setEmail(rs.getString("email"));
                user.setActivityStatus(rs.getString("actividad"));
                user.setGroupAdmin(rs.getBoolean("es_admin"));
                members.add(user);
            }
            logger.debug("Grupo {} tiene {} miembros", groupId, members.size());
        } catch (SQLException e) {
            logger.error("Error obteniendo miembros del grupo {}: {}", groupId, e.getMessage());
        }
        return members;
    }

    /**
     * Obtiene información completa de un grupo incluyendo sus miembros
     */
    public Group getGroupById(int groupId) {
        String sql = "SELECT id_grupo, nombre_grupo, id_admin, fecha_creacion FROM grupos WHERE id_grupo = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Group group = new Group(
                        rs.getInt("id_grupo"),
                        rs.getString("nombre_grupo"),
                        rs.getInt("id_admin")
                );
                group.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                // Cargar miembros
                group.setMiembros(getGroupMembers(groupId));
                return group;
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo grupo {}: {}", groupId, e.getMessage());
        }
        return null;
    }

    /**
     * Verifica si un usuario es administrador de un grupo
     * Ahora soporta múltiples admins verificando en miembros_grupo
     */
    public boolean isGroupAdmin(int groupId, int userId) {
        String sql = "SELECT es_admin FROM miembros_grupo WHERE id_grupo = ? AND id_usuario = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("es_admin");
            }
        } catch (SQLException e) {
            logger.error("Error verificando admin del grupo {}: {}", groupId, e.getMessage());
        }
        return false;
    }

    /**
     * Verifica si un usuario es miembro de un grupo
     */
    public boolean isMember(int groupId, int userId) {
        String sql = "SELECT 1 FROM miembros_grupo WHERE id_grupo = ? AND id_usuario = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setInt(2, userId);
            return pstmt.executeQuery().next();
        } catch (SQLException e) {
            logger.error("Error verificando membresía: grupo={}, usuario={}: {}", groupId, userId, e.getMessage());
        }
        return false;
    }

    /**
     * Obtiene el número de miembros de un grupo
     */
    public int getMemberCount(int groupId) {
        String sql = "SELECT COUNT(*) FROM miembros_grupo WHERE id_grupo = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error contando miembros del grupo {}: {}", groupId, e.getMessage());
        }
        return 0;
    }

    /**
     * Promueve a un miembro a administrador del grupo
     */
    public boolean promoteToAdmin(int groupId, int userId) {
        String sql = "UPDATE miembros_grupo SET es_admin = TRUE WHERE id_grupo = ? AND id_usuario = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setInt(2, userId);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                logger.info("Usuario {} promovido a admin del grupo {}", userId, groupId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error promoviendo a admin en grupo {}: {}", groupId, e.getMessage());
        }
        return false;
    }

    /**
     * Quita el rol de administrador a un miembro
     */
    public boolean demoteFromAdmin(int groupId, int userId) {
        // Verificar que hay al menos otro admin antes de quitar el rol
        int adminCount = getAdminCount(groupId);
        if (adminCount <= 1) {
            logger.warn("No se puede quitar admin del grupo {} - es el único admin", groupId);
            return false;
        }

        String sql = "UPDATE miembros_grupo SET es_admin = FALSE WHERE id_grupo = ? AND id_usuario = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setInt(2, userId);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                logger.info("Usuario {} degradado de admin del grupo {}", userId, groupId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error degradando admin en grupo {}: {}", groupId, e.getMessage());
        }
        return false;
    }

    /**
     * Cuenta el número de administradores en un grupo
     */
    public int getAdminCount(int groupId) {
        String sql = "SELECT COUNT(*) FROM miembros_grupo WHERE id_grupo = ? AND es_admin = TRUE";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error contando admins del grupo {}: {}", groupId, e.getMessage());
        }
        return 0;
    }

    /**
     * Obtiene el nombre de un grupo
     */
    public String getGroupName(int groupId) {
        String sql = "SELECT nombre_grupo FROM grupos WHERE id_grupo = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("nombre_grupo");
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo nombre del grupo {}: {}", groupId, e.getMessage());
        }
        return null;
    }
}