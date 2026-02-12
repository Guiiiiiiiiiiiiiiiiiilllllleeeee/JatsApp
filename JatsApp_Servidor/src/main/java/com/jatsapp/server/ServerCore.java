package com.jatsapp.server;

import com.jatsapp.common.Message;
import com.jatsapp.server.dao.GroupDAO;
import com.jatsapp.server.dao.MessageDAO;
import com.jatsapp.server.dao.UserDAO;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ServerCore {

    private static final int PORT = 5555;
    private volatile boolean isRunning = true;

    // Mapa Thread-Safe para guardar usuarios online: ID_USUARIO -> MANEJADOR
    private final ConcurrentHashMap<Integer, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    // ServerSocket como campo para poder cerrarlo desde stopServer()
    private ServerSocket serverSocket;

    // DAOs
    private final UserDAO userDAO = new UserDAO();
    private final GroupDAO groupDAO = new GroupDAO();
    private final MessageDAO messageDAO = new MessageDAO();

    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Servidor iniciado en puerto " + PORT + ". Esperando conexiones...");

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Nueva conexión entrante: " + clientSocket.getInetAddress());

                    // Creamos un gestor para este cliente y lo lanzamos en un hilo aparte
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    new Thread(handler).start();
                } catch (IOException e) {
                    if (isRunning) {
                        // Si todavía estamos corriendo, reportamos el error
                        e.printStackTrace();
                    } else {
                        // Si no estamos corriendo, probablemente se pidió shutdown y accept lanzó por cierre del socket
                        System.out.println("Accept interrumpido por shutdown.");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeServerSocket();
        }
    }

    // Permite detener el servidor de forma controlada
    public void stopServer() {
        System.out.println("Deteniendo servidor...");
        isRunning = false;
        closeServerSocket();
    }

    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Registrar cliente cuando hace LOGIN con éxito
    public void addClient(int userId, ClientHandler handler) {
        connectedClients.put(userId, handler);
        userDAO.updateActivityStatus(userId, "activo");
        System.out.println("Usuario ID " + userId + " registrado en la lista de activos.");
    }

    // Eliminar cliente cuando se desconecta
    public void removeClient(int userId) {
        connectedClients.remove(userId);
        userDAO.updateActivityStatus(userId, "desconectado");
        System.out.println("Usuario ID " + userId + " desconectado.");
    }

    // Enviar mensaje privado (1 a 1)
    public void sendPrivateMessage(Message msg) {
        // 1. Guardar en Base de Datos SIEMPRE (para historial)
        messageDAO.saveMessage(msg);

        // 2. Si el destinatario está online, enviárselo directamente
        ClientHandler recipient = connectedClients.get(msg.getReceiverId());
        if (recipient != null) {
            recipient.sendMessage(msg);
        }
    }

    // Enviar mensaje a un grupo (Broadcast selectivo)
    public void sendGroupMessage(Message msg) {
        // 1. Guardar en Base de Datos
        messageDAO.saveMessage(msg);

        // 2. Recuperar miembros del grupo y enviar solo a ellos
        List<Integer> memberIds = groupDAO.getGroupMemberIds(msg.getReceiverId());

        for (Integer memberId : memberIds) {
            // No enviar al emisor
            if (memberId.equals(msg.getSenderId())) {
                continue;
            }

            ClientHandler handler = connectedClients.get(memberId);
            if (handler != null) {
                handler.sendMessage(msg);
            }
        }
    }
}