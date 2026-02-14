package com.jatsapp.server.dao;

import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    public boolean saveMessage(Message msg) {
        String sql = "INSERT INTO mensajes (id_emisor, id_destinatario, tipo_destinatario, tipo_contenido, contenido, ruta_fichero, nombre_fichero, datos_fichero, fecha_envio, entregado, leido) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), FALSE, FALSE)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, msg.getSenderId());
            pstmt.setInt(2, msg.getReceiverId());

            // Tipo Destinatario
            pstmt.setString(3, msg.isGroupChat() ? "GRUPO" : "USUARIO");

            // Lógica para Archivos vs Texto
            if (msg.getType() == MessageType.FILE_MESSAGE || msg.getType() == MessageType.ARCHIVO || msg.getType() == MessageType.IMAGEN) {
                pstmt.setString(4, "ARCHIVO");
                pstmt.setString(5, null); // Contenido texto null
                pstmt.setString(6, msg.getServerFilePath()); // Ruta (opcional, backup)
                pstmt.setString(7, msg.getFileName());
                pstmt.setBytes(8, msg.getFileData()); // Guardar bytes del archivo
            } else {
                pstmt.setString(4, "TEXTO");
                pstmt.setString(5, msg.getContent());
                pstmt.setString(6, null);
                pstmt.setString(7, null);
                pstmt.setBytes(8, null);
            }

            int affected = pstmt.executeUpdate();

            if (affected > 0) {
                // Obtener el ID generado
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        msg.setMessageId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Historial Privado (A <-> B)
    public List<Message> getPrivateHistory(int user1, int user2) {
        String sql = "SELECT m.*, u.nombre_usuario AS sender_name FROM mensajes m " +
                "JOIN usuarios u ON m.id_emisor = u.id_usuario " +
                "WHERE m.tipo_destinatario = 'USUARIO' AND " +
                "((m.id_emisor = ? AND m.id_destinatario = ?) OR (m.id_emisor = ? AND m.id_destinatario = ?)) " +
                "ORDER BY m.fecha_envio ASC";
        return fetchMessages(sql, user1, user2, user2, user1);
    }

    // Historial Grupo
    public List<Message> getGroupHistory(int groupId) {
        String sql = "SELECT m.*, u.nombre_usuario AS sender_name FROM mensajes m " +
                "JOIN usuarios u ON m.id_emisor = u.id_usuario " +
                "WHERE m.tipo_destinatario = 'GRUPO' AND m.id_destinatario = ? ORDER BY m.fecha_envio ASC";
        return fetchMessages(sql, groupId);
    }

    // Método auxiliar para no repetir código de lectura
    private List<Message> fetchMessages(String sql, int... params) {
        List<Message> history = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                pstmt.setInt(i + 1, params[i]);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Message m = new Message();
                m.setMessageId(rs.getInt("id_mensaje"));
                m.setSenderId(rs.getInt("id_emisor"));
                m.setReceiverId(rs.getInt("id_destinatario"));
                m.setSenderName(rs.getString("sender_name"));

                // Estado del mensaje
                m.setDelivered(rs.getBoolean("entregado"));
                m.setRead(rs.getBoolean("leido"));

                // Fecha de envío del mensaje
                Timestamp fechaEnvio = rs.getTimestamp("fecha_envio");
                if (fechaEnvio != null) {
                    m.setTimestamp(fechaEnvio.toLocalDateTime());
                }

                String tipoContenido = rs.getString("tipo_contenido");
                if ("ARCHIVO".equals(tipoContenido)) {
                    m.setType(MessageType.FILE_MESSAGE);
                    m.setFileName(rs.getString("nombre_fichero"));
                    m.setServerFilePath(rs.getString("ruta_fichero"));
                    // Los bytes del archivo se cargarán bajo demanda cuando el cliente lo solicite
                } else {
                    m.setType(MessageType.TEXT_MESSAGE);
                    m.setContent(rs.getString("contenido"));
                }

                m.setGroupChat("GRUPO".equals(rs.getString("tipo_destinatario")));

                // Opcional: Cargar nombres de usuarios si es necesario con un JOIN extra
                history.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    // =================================================================
    // CONFIRMACIONES DE LECTURA
    // =================================================================

    /**
     * Marca un mensaje como entregado
     */
    public boolean markAsDelivered(int messageId) {
        String sql = "UPDATE mensajes SET entregado = TRUE, fecha_entrega = NOW() WHERE id_mensaje = ? AND entregado = FALSE";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, messageId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Marca un mensaje como leído
     */
    public boolean markAsRead(int messageId) {
        String sql = "UPDATE mensajes SET leido = TRUE, fecha_lectura = NOW() WHERE id_mensaje = ? AND leido = FALSE";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, messageId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Marca todos los mensajes de un chat como leídos (cuando el usuario abre el chat)
     */
    public boolean markChatAsRead(int userId, int contactId) {
        String sql = "UPDATE mensajes SET leido = TRUE, fecha_lectura = NOW() " +
                     "WHERE id_destinatario = ? AND id_emisor = ? AND tipo_destinatario = 'USUARIO' AND leido = FALSE";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, contactId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene el estado actual de un mensaje por su ID
     */
    public Message getMessageById(int messageId) {
        String sql = "SELECT m.*, u.nombre_usuario AS sender_name FROM mensajes m " +
                "JOIN usuarios u ON m.id_emisor = u.id_usuario " +
                "WHERE m.id_mensaje = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, messageId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Message m = new Message();
                m.setMessageId(rs.getInt("id_mensaje"));
                m.setSenderId(rs.getInt("id_emisor"));
                m.setReceiverId(rs.getInt("id_destinatario"));
                m.setSenderName(rs.getString("sender_name"));
                m.setContent(rs.getString("contenido"));
                m.setDelivered(rs.getBoolean("entregado"));
                m.setRead(rs.getBoolean("leido"));

                // Determinar el tipo de mensaje
                String tipoContenido = rs.getString("tipo_contenido");
                if ("ARCHIVO".equals(tipoContenido)) {
                    m.setType(MessageType.FILE_MESSAGE);
                    m.setFileName(rs.getString("nombre_fichero"));
                    m.setServerFilePath(rs.getString("ruta_fichero"));
                    m.setFileData(rs.getBytes("datos_fichero")); // Cargar bytes del archivo
                } else {
                    m.setType(MessageType.TEXT_MESSAGE);
                }

                m.setGroupChat("GRUPO".equals(rs.getString("tipo_destinatario")));

                return m;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Obtiene los chats relevantes de un usuario (últimos mensajes de cada contacto o grupo)
     */
    public List<Message> getRelevantChats(int userId) {
        String sql = "SELECT m.*, u.nombre_usuario AS sender_name FROM mensajes m " +
                "JOIN usuarios u ON m.id_emisor = u.id_usuario " +
                "WHERE m.id_emisor = ? OR m.id_destinatario = ? ORDER BY m.fecha_envio DESC";
        List<Message> messages = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Message msg = new Message();
                    msg.setMessageId(rs.getInt("id_mensaje"));
                    msg.setSenderId(rs.getInt("id_emisor"));
                    msg.setReceiverId(rs.getInt("id_destinatario"));
                    msg.setSenderName(rs.getString("sender_name"));
                    msg.setContent(rs.getString("contenido"));

                    // Determinar el tipo de mensaje según el contenido
                    String tipoContenido = rs.getString("tipo_contenido");
                    if ("ARCHIVO".equals(tipoContenido)) {
                        msg.setType(MessageType.ARCHIVO);
                        msg.setFileName(rs.getString("nombre_fichero"));
                    } else {
                        msg.setType(MessageType.TEXT_MESSAGE);
                    }

                    // Estado del mensaje
                    msg.setDelivered(rs.getBoolean("entregado"));
                    msg.setRead(rs.getBoolean("leido"));
                    msg.setGroupChat("GRUPO".equals(rs.getString("tipo_destinatario")));

                    messages.add(msg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
    }

    /**
     * Busca mensajes en todos los chats del usuario que contengan el término de búsqueda
     * @param userId ID del usuario que busca
     * @param searchTerm Término de búsqueda
     * @return Lista de mensajes que coinciden con la búsqueda
     */
    public List<Message> searchMessages(int userId, String searchTerm) {
        String sql = "SELECT m.*, u.nombre_usuario AS sender_name, " +
                "CASE WHEN m.id_emisor = ? THEN u2.nombre_usuario ELSE u.nombre_usuario END AS contact_name " +
                "FROM mensajes m " +
                "JOIN usuarios u ON m.id_emisor = u.id_usuario " +
                "LEFT JOIN usuarios u2 ON m.id_destinatario = u2.id_usuario " +
                "WHERE (m.id_emisor = ? OR m.id_destinatario = ?) " +
                "AND m.tipo_contenido = 'TEXTO' " +
                "AND m.contenido LIKE ? " +
                "ORDER BY m.fecha_envio DESC " +
                "LIMIT 50";

        List<Message> messages = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, userId);
            pstmt.setString(4, "%" + searchTerm + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Message msg = new Message();
                    msg.setMessageId(rs.getInt("id_mensaje"));
                    msg.setSenderId(rs.getInt("id_emisor"));
                    msg.setReceiverId(rs.getInt("id_destinatario"));
                    msg.setSenderName(rs.getString("sender_name"));
                    msg.setContent(rs.getString("contenido"));
                    msg.setType(MessageType.TEXT_MESSAGE);
                    msg.setDelivered(rs.getBoolean("entregado"));
                    msg.setRead(rs.getBoolean("leido"));
                    msg.setGroupChat("GRUPO".equals(rs.getString("tipo_destinatario")));

                    messages.add(msg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
    }
}

