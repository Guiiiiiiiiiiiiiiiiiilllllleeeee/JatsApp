package com.jatsapp.server;

import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;
import com.jatsapp.common.User;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerCore {

    private static final Logger logger = LoggerFactory.getLogger(ServerCore.class);
    private static final Logger activityLogger = LoggerFactory.getLogger("com.jatsapp.server.activity");

    private static final int PORT = 5555;
    private static final int HEARTBEAT_INTERVAL_SECONDS = 30; // Verificar cada 30 segundos
    private volatile boolean isRunning = true;

    // Mapa Thread-Safe para guardar usuarios online: ID_USUARIO -> MANEJADOR
    private final ConcurrentHashMap<Integer, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    // ServerSocket como campo para poder cerrarlo desde stopServer()
    private ServerSocket serverSocket;

    // Scheduler para el heartbeat
    private ScheduledExecutorService heartbeatScheduler;

    // DAOs
    private final UserDAO userDAO = new UserDAO();
    private final GroupDAO groupDAO = new GroupDAO();
    private final MessageDAO messageDAO = new MessageDAO();

    public void startServer() {
        try {
            // Limpiar estados de conexión al iniciar (por si hubo cierre abrupto anterior)
            logger.info("Limpiando estados de conexión anteriores...");
            userDAO.setAllUsersOffline();

            serverSocket = new ServerSocket(PORT);
            logger.info("✓ Servidor iniciado en puerto {}", PORT);
            logger.info("Esperando conexiones...");
            activityLogger.info("SERVIDOR ESCUCHANDO en puerto {}", PORT);

            // Iniciar el sistema de heartbeat
            startHeartbeat();

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
            stopHeartbeat();
            closeServerSocket();
        }
    }

    /**
     * Inicia el sistema de heartbeat que verifica periódicamente los clientes conectados
     */
    private void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                checkConnectedClients();
            } catch (Exception e) {
                logger.error("Error en heartbeat: {}", e.getMessage());
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);

        logger.info("Sistema de heartbeat iniciado (intervalo: {}s)", HEARTBEAT_INTERVAL_SECONDS);
    }

    /**
     * Detiene el sistema de heartbeat
     */
    private void stopHeartbeat() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdown();
            logger.info("Sistema de heartbeat detenido");
        }
    }

    /**
     * Verifica el estado de todos los clientes conectados
     * Envía un PING y elimina los que no responden
     */
    private void checkConnectedClients() {
        if (connectedClients.isEmpty()) return;

        logger.debug("Verificando {} clientes conectados...", connectedClients.size());

        for (var entry : connectedClients.entrySet()) {
            int userId = entry.getKey();
            ClientHandler handler = entry.getValue();

            if (!handler.isAlive()) {
                logger.warn("Cliente {} detectado como desconectado (socket cerrado)", userId);
                removeClient(userId);
            }
        }
    }

    // Permite detener el servidor de forma controlada
    public void stopServer() {
        logger.info("Deteniendo servidor...");
        isRunning = false;
        stopHeartbeat();
        closeServerSocket();

        // Desconectar todos los clientes y actualizar sus estados
        logger.info("Desconectando {} clientes activos...", connectedClients.size());
        for (Integer userId : connectedClients.keySet()) {
            userDAO.updateActivityStatus(userId, "desconectado");
        }
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

        // Notificar a todos los usuarios conectados sobre el cambio de estado
        broadcastStatusUpdate(userId, "activo");
    }

    // Eliminar cliente cuando se desconecta
    public void removeClient(int userId) {
        connectedClients.remove(userId);
        userDAO.updateActivityStatus(userId, "desconectado");

        logger.info("Usuario ID {} desconectado. Total clientes activos: {}", userId, connectedClients.size());
        activityLogger.info("DESCONEXIÓN | UserID: {} | Clientes activos: {}", userId, connectedClients.size());

        // Notificar a todos los usuarios conectados sobre el cambio de estado
        broadcastStatusUpdate(userId, "desconectado");
    }

    /**
     * Notifica a todos los clientes conectados sobre un cambio de estado de un usuario
     */
    private void broadcastStatusUpdate(int userId, String status) {
        // Obtener el nombre del usuario
        User user = userDAO.getUserById(userId);
        String username = (user != null) ? user.getUsername() : "Usuario " + userId;

        Message statusMsg = new Message(MessageType.STATUS_UPDATE, status);
        statusMsg.setSenderId(userId);
        statusMsg.setSenderName(username);

        logger.debug("Enviando STATUS_UPDATE a {} clientes: {} -> {}", connectedClients.size(), username, status);

        // Enviar a todos los clientes conectados (excepto al propio usuario)
        for (var entry : connectedClients.entrySet()) {
            if (entry.getKey() != userId) {
                try {
                    entry.getValue().sendMessage(statusMsg);
                } catch (Exception e) {
                    logger.warn("Error enviando STATUS_UPDATE a UserID {}: {}", entry.getKey(), e.getMessage());
                }
            }
        }
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
            // Sistema estilo WhatsApp: enviar mensaje directamente sin notificaciones especiales
            // El cliente se encarga de mostrar el mensaje y añadir al emisor a la lista si es necesario

            logger.debug("Enviando mensaje {} a destinatario online", msg.getMessageId());


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
        logger.debug("Enviando mensaje a grupo: UserID {} -> Grupo {}, isGroupChat={}",
                    msg.getSenderId(), msg.getReceiverId(), msg.isGroupChat());

        // 1. Guardar en Base de Datos
        boolean saved = messageDAO.saveMessage(msg);
        if (saved) {
            activityLogger.info("MENSAJE GRUPO | De: {} | Grupo: {} | Tipo: {} | MsgID: {}",
                              msg.getSenderId(), msg.getReceiverId(), msg.getType(), msg.getMessageId());
        }

        // 2. Recuperar miembros del grupo y enviar solo a ellos
        List<Integer> memberIds = groupDAO.getGroupMemberIds(msg.getReceiverId());
        logger.info("Grupo {} tiene {} miembros, enviando mensaje...", msg.getReceiverId(), memberIds.size());

        int deliveredCount = 0;
        int totalRecipients = memberIds.size() - 1; // Excluir al emisor

        for (Integer memberId : memberIds) {
            // No enviar al emisor
            if (memberId.equals(msg.getSenderId())) {
                logger.debug("Saltando emisor {}", memberId);
                continue;
            }

            ClientHandler handler = connectedClients.get(memberId);
            if (handler != null) {
                logger.debug("Enviando a miembro {} (online)", memberId);
                handler.sendMessage(msg);
                deliveredCount++;
            } else {
                logger.debug("Miembro {} está offline", memberId);
            }
        }

        logger.info("Mensaje de grupo {} entregado a {}/{} miembros online",
                   msg.getReceiverId(), deliveredCount, totalRecipients);

        // 3. Enviar confirmación de entrega al emisor
        // Si al menos un miembro lo recibió, marcar como entregado
        if (deliveredCount > 0) {
            messageDAO.markAsDelivered(msg.getMessageId());

            ClientHandler sender = connectedClients.get(msg.getSenderId());
            if (sender != null) {
                Message deliveryConfirmation = new Message(MessageType.MESSAGE_DELIVERED, "");
                deliveryConfirmation.setMessageId(msg.getMessageId());
                deliveryConfirmation.setDelivered(true);
                // Indicar cuántos recibieron vs total (ej: "3/5")
                deliveryConfirmation.setContent(deliveredCount + "/" + totalRecipients);
                sender.sendMessage(deliveryConfirmation);
                logger.debug("Confirmación de entrega de grupo enviada al emisor {} ({}/{})",
                           msg.getSenderId(), deliveredCount, totalRecipients);
            }
        }
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