package com.jatsapp.server;

import com.jatsapp.common.Message;
import com.jatsapp.server.dao.MessageDAO;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ServerCore {

    private static final int PORT = 5555;
    private boolean isRunning = true;

    // Mapa Thread-Safe para guardar usuarios online: ID_USUARIO -> MANEJADOR
    private final ConcurrentHashMap<Integer, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado en puerto " + PORT + ". Esperando conexiones...");

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nueva conexión entrante: " + clientSocket.getInetAddress());

                // Creamos un gestor para este cliente y lo lanzamos en un hilo aparte
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Registrar cliente cuando hace LOGIN con éxito
    public void addClient(int userId, ClientHandler handler) {
        connectedClients.put(userId, handler);
        System.out.println("Usuario ID " + userId + " registrado en la lista de activos.");
    }

    // Eliminar cliente cuando se desconecta
    public void removeClient(int userId) {
        connectedClients.remove(userId);
        System.out.println("Usuario ID " + userId + " desconectado.");
    }

    // Enviar mensaje privado (1 a 1)
    public void sendPrivateMessage(Message msg) {
        // 1. Guardar en Base de Datos SIEMPRE (para historial)
        new MessageDAO().saveMessage(msg);

        // 2. Si el destinatario está online, enviárselo directamente
        ClientHandler recipient = connectedClients.get(msg.getReceiverId());
        if (recipient != null) {
            recipient.sendMessage(msg);
        }
    }

    // Enviar mensaje a un grupo (Broadcast selectivo)
    public void sendGroupMessage(Message msg) {
        // 1. Guardar en Base de Datos
        new MessageDAO().saveMessage(msg);

        // 2. Recuperar miembros del grupo (Necesitarás un método en GroupDAO para obtener los IDs de los miembros)
        // Por ahora, como simplificación, podrías enviarlo a todos o implementar esa lógica:
        // List<Integer> memberIds = new GroupDAO().getGroupMemberIds(msg.getReceiverId());
        // for (Integer id : memberIds) { ... }

        // EJEMPLO GENÉRICO (Enviar a todos los conectados - Broadcast global para pruebas):
        for (ClientHandler client : connectedClients.values()) {
            if (client.getCurrentUser().getId() != msg.getSenderId()) { // No enviarse a sí mismo
                client.sendMessage(msg);
            }
        }
    }
}