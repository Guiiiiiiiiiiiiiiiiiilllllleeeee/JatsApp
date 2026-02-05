package com.jatsapp.server.dao;

import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    public boolean saveMessage(Message msg) {
        String sql = "INSERT INTO mensajes (id_emisor, id_destinatario, tipo_destinatario, tipo_contenido, contenido, ruta_fichero, nombre_fichero, fecha_envio) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, msg.getSenderId());
            pstmt.setInt(2, msg.getReceiverId());

            // Tipo Destinatario
            pstmt.setString(3, msg.isGroupChat() ? "GRUPO" : "USUARIO");

            // Lógica para Archivos vs Texto
            if (msg.getType() == MessageType.ARCHIVO || msg.getType() == MessageType.IMAGEN) {
                pstmt.setString(4, "ARCHIVO");
                pstmt.setString(5, null); // Contenido texto null
                pstmt.setString(6, msg.getServerFilePath()); // Ruta guardada en disco
                pstmt.setString(7, msg.getFileName());
            } else {
                pstmt.setString(4, "TEXTO");
                pstmt.setString(5, msg.getContent());
                pstmt.setString(6, null);
                pstmt.setString(7, null);
            }

            int affected = pstmt.executeUpdate();
            return affected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Historial Privado (A <-> B)
    public List<Message> getPrivateHistory(int user1, int user2) {
        String sql = "SELECT * FROM mensajes WHERE tipo_destinatario = 'USUARIO' AND " +
                "((id_emisor = ? AND id_destinatario = ?) OR (id_emisor = ? AND id_destinatario = ?)) " +
                "ORDER BY fecha_envio ASC";
        return fetchMessages(sql, user1, user2, user2, user1);
    }

    // Historial Grupo
    public List<Message> getGroupHistory(int groupId) {
        String sql = "SELECT * FROM mensajes WHERE tipo_destinatario = 'GRUPO' AND id_destinatario = ? ORDER BY fecha_envio ASC";
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
                m.setSenderId(rs.getInt("id_emisor"));
                m.setReceiverId(rs.getInt("id_destinatario"));

                String tipoContenido = rs.getString("tipo_contenido");
                if ("ARCHIVO".equals(tipoContenido)) {
                    m.setType(MessageType.ARCHIVO);
                    m.setFileName(rs.getString("nombre_fichero"));
                    // Nota: No cargamos los bytes del archivo aquí para no saturar la memoria,
                    // solo la metadata. El cliente pedirá descargar el archivo si quiere.
                } else {
                    m.setType(MessageType.TEXTO);
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
}