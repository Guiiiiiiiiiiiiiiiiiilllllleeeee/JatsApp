package com.jatsapp.server;

import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;
import com.jatsapp.server.dao.GroupDAO;
import com.jatsapp.server.dao.MessageDAO;
import com.jatsapp.server.dao.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ServerCore {

    private static final Logger logger = LoggerFactory.getLogger(ServerCore.class);
    private static final Logger activityLogger = LoggerFactory.getLogger("com.jatsapp.server.activity");

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
            logger.info("✓ Servidor iniciado en puerto {}", PORT);
            logger.info("Esperando conexiones...");
            activityLogger.info("SERVIDOR ESCUCHANDO en puerto {}", PORT);

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientAddress = clientSocket.getInetAddress().getHostAddress();

                    logger.info("Nueva conexión entrante desde: {}", clientAddress);
                    activityLogger.info("CONEXIÓN ENTRANTE | IP: {}", clientAddress);

                    // Creamos un gestor para este cliente y lo lanzamos en un hilo aparte
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    new Thread(handler, "ClientHandler-" + clientAddress).start();

                } catch (IOException e) {
                    if (isRunning) {
                        logger.error("Error aceptando conexión de cliente", e);
                    } else {
                        logger.debug("Accept interrumpido por shutdown del servidor");
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error fatal iniciando el servidor en puerto {}", PORT, e);
            activityLogger.info("ERROR FATAL SERVIDOR | Puerto: {}", PORT);
        } finally {
            closeServerSocket();
        }
    }

    // Permite detener el servidor de forma controlada
    public void stopServer() {
        logger.info("Deteniendo servidor...");
        isRunning = false;
        closeServerSocket();

        // Desconectar todos los clientes
        logger.info("Desconectando {} clientes activos...", connectedClients.size());
        connectedClients.clear();
    }

    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                logger.info("Socket del servidor cerrado correctamente");
            } catch (IOException e) {
                logger.error("Error cerrando socket del servidor", e);
            }
        }
    }

    // Registrar cliente cuando hace LOGIN con éxito
    public void addClient(int userId, ClientHandler handler) {
        connectedClients.put(userId, handler);
        userDAO.updateActivityStatus(userId, "activo");

        logger.info("Usuario ID {} conectado. Total clientes activos: {}", userId, connectedClients.size());
        activityLogger.info("LOGIN EXITOSO | UserID: {} | Clientes activos: {}", userId, connectedClients.size());
    }

    // Eliminar cliente cuando se desconecta
    public void removeClient(int userId) {
        connectedClients.remove(userId);
        userDAO.updateActivityStatus(userId, "desconectado");

        logger.info("Usuario ID {} desconectado. Total clientes activos: {}", userId, connectedClients.size());
        activityLogger.info("DESCONEXIÓN | UserID: {} | Clientes activos: {}", userId, connectedClients.size());
    }

    // Enviar mensaje privado (1 a 1)
    public void sendPrivateMessage(Message msg) {
        logger.debug("Enviando mensaje privado: UserID {} -> UserID {}", msg.getSenderId(), msg.getReceiverId());

        // 1. Guardar en Base de Datos SIEMPRE (para historial)
        boolean saved = messageDAO.saveMessage(msg);
        if (saved) {
            activityLogger.info("MENSAJE PRIVADO | De: {} | Para: {} | Tipo: {} | ID: {}",
                              msg.getSenderId(), msg.getReceiverId(), msg.getType(), msg.getMessageId());
        } else {
            logger.warn("No se pudo guardar mensaje privado en BD");
            return;
        }

        // 2. Si el destinatario está online, enviárselo directamente
        ClientHandler recipient = connectedClients.get(msg.getReceiverId());
        if (recipient != null) {
            // Verificar si es el PRIMER mensaje entre estos usuarios (nuevo chat)
            boolean isFirstMessage = !userDAO.hasMessageHistory(msg.getSenderId(), msg.getReceiverId());
            boolean isContact = userDAO.isContact(msg.getReceiverId(), msg.getSenderId());

            if (isFirstMessage || !isContact) {
                // Enviar notificación de nuevo chat al receptor
                Message newChatNotification = new Message(MessageType.NEW_CHAT_REQUEST, "Nuevo mensaje");
                newChatNotification.setSenderId(msg.getSenderId());
                newChatNotification.setSenderName(msg.getSenderName());
                newChatNotification.setReceiverId(msg.getReceiverId());
                recipient.sendMessage(newChatNotification);

                logger.info("Notificación de nuevo chat enviada: {} -> {}", msg.getSenderId(), msg.getReceiverId());
                activityLogger.info("NUEVO CHAT | De: {} ({}) | Para: {}",
                                  msg.getSenderId(), msg.getSenderName(), msg.getReceiverId());
            }

            // Enviar el mensaje al receptor
            recipient.sendMessage(msg);

            // Marcar como entregado
            messageDAO.markAsDelivered(msg.getMessageId());
            msg.setDelivered(true);

            logger.debug("Mensaje {} entregado a destinatario online", msg.getMessageId());

            // Enviar confirmación de entrega al emisor
            ClientHandler sender = connectedClients.get(msg.getSenderId());
            if (sender != null) {
                Message deliveryConfirmation = new Message(MessageType.MESSAGE_DELIVERED, "");
                deliveryConfirmation.setMessageId(msg.getMessageId());
                deliveryConfirmation.setDelivered(true);
                sender.sendMessage(deliveryConfirmation);
                logger.debug("Confirmación de entrega enviada al emisor {}", msg.getSenderId());
            }
        } else {
            logger.debug("Destinatario offline. Mensaje {} guardado para recuperar después.", msg.getMessageId());
        }
    }

    // Enviar mensaje a un grupo (Broadcast selectivo)
    public void sendGroupMessage(Message msg) {
        logger.debug("Enviando mensaje a grupo: UserID {} -> Grupo {}", msg.getSenderId(), msg.getReceiverId());

        // 1. Guardar en Base de Datos
        boolean saved = messageDAO.saveMessage(msg);
        if (saved) {
            activityLogger.info("MENSAJE GRUPO | De: {} | Grupo: {} | Tipo: {}",
                              msg.getSenderId(), msg.getReceiverId(), msg.getType());
        }

        // 2. Recuperar miembros del grupo y enviar solo a ellos
        List<Integer> memberIds = groupDAO.getGroupMemberIds(msg.getReceiverId());
        logger.debug("Grupo {} tiene {} miembros", msg.getReceiverId(), memberIds.size());

        int deliveredCount = 0;
        for (Integer memberId : memberIds) {
            // No enviar al emisor
            if (memberId.equals(msg.getSenderId())) {
                continue;
            }

            ClientHandler handler = connectedClients.get(memberId);
            if (handler != null) {
                handler.sendMessage(msg);
                deliveredCount++;
            }
        }

        logger.debug("Mensaje de grupo entregado a {}/{} miembros online", deliveredCount, memberIds.size() - 1);
    }

    // Método para obtener número de clientes conectados (usado por MainServer)
    public int getConnectedClientsCount() {
        return connectedClients.size();
    }

    // Obtener ClientHandler de un usuario conectado
    public ClientHandler getClientHandler(int userId) {
        return connectedClients.get(userId);
    }
}