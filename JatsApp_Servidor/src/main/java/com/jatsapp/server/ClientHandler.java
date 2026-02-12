package com.jatsapp.server;

import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;
import com.jatsapp.common.User;
import com.jatsapp.server.dao.MessageDAO;
import com.jatsapp.server.dao.UserDAO;
import com.jatsapp.server.service.EmailService;
import com.jatsapp.server.service.FileService;
import com.jatsapp.server.service.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private static final Logger activityLogger = LoggerFactory.getLogger("com.jatsapp.server.activity");

    private Socket socket;
    private ServerCore serverCore;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private int userId = -1; // -1 significa "no identificado"

    private User currentUser;      // Usuario totalmente logueado
    private User tempUser;         // Usuario en proceso de 2FA
    private boolean running = true;

    private String clientAddress;  // Para logs

    // Servicios y DAOs
    private UserDAO userDAO = new UserDAO();
    private MessageDAO messageDAO = new MessageDAO();
    private FileService fileService = new FileService();
    private EmailService emailService = new EmailService();

    public ClientHandler(Socket socket, ServerCore serverCore) {
        this.socket = socket;
        this.serverCore = serverCore;
        this.clientAddress = socket.getInetAddress().getHostAddress();
    }

    @Override
    public void run() {
        logger.info("ClientHandler iniciado para: {}", clientAddress);

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            logger.debug("Streams establecidos con cliente: {}", clientAddress);

            while (running) {
                try {
                    Message receivedMsg = (Message) in.readObject();
                    logger.debug("Mensaje recibido de {}: Tipo={}", clientAddress, receivedMsg.getType());
                    handleMessage(receivedMsg);
                } catch (ClassNotFoundException e) {
                    logger.error("Clase no encontrada al deserializar mensaje de {}", clientAddress, e);
                }
            }
        } catch (IOException e) {
            if (running) {
                logger.warn("Cliente {} desconectado inesperadamente", clientAddress);
            } else {
                logger.debug("Cliente {} desconectado normalmente", clientAddress);
            }
        } finally {
            closeConnection();
        }
    }

    private void handleMessage(Message msg) {
        // Bloquear mensajes si no está logueado (excepto los de auth)
        if (currentUser == null &&
                msg.getType() != MessageType.LOGIN &&
                msg.getType() != MessageType.REGISTER &&
                msg.getType() != MessageType.VERIFY_2FA) {
            logger.warn("Intento de operación sin login desde {}", clientAddress);
            return;
        }

        switch (msg.getType()) {
            // --- AUTENTICACIÓN ---
            case LOGIN:
                logger.info("Intento de login desde {}: Usuario={}", clientAddress, msg.getSenderName());
                activityLogger.info("INTENTO LOGIN | IP: {} | Usuario: {}", clientAddress, msg.getSenderName());

                String usuarioLogin = msg.getSenderName();
                String passwordLogin = msg.getContent();

                User loginUser = userDAO.login(usuarioLogin, passwordLogin);
                if (loginUser != null) {
                    this.tempUser = loginUser;
                    logger.info("Credenciales válidas para usuario: {}", loginUser.getUsername());

                    // Generar código 2FA
                    String code = SecurityService.generate2FACode();
                    long expiration = System.currentTimeMillis() + 5 * 60 * 1000; // 5 minutos
                    userDAO.set2FACode(loginUser.getId(), code, expiration);
                    emailService.sendEmail(loginUser.getEmail(), "Tu código de acceso JatsApp", "Tu código es: " + code);

                    // Guardar usuario temporalmente hasta que verifique el código
                    this.userId = loginUser.getId();

                    logger.info("Código 2FA generado y enviado a: {}", loginUser.getEmail());
                    activityLogger.info("2FA ENVIADO | UserID: {} | Email: {}", loginUser.getId(), loginUser.getEmail());

                    // Pedir al cliente el código
                    sendMessage(new Message(MessageType.require_2FA, "Revisa tu email"));
                } else {
                    logger.warn("Credenciales inválidas para usuario: {} desde {}", msg.getSenderName(), clientAddress);
                    activityLogger.info("LOGIN FALLIDO | IP: {} | Usuario: {}", clientAddress, msg.getSenderName());
                    sendMessage(new Message(MessageType.LOGIN_FAIL, "Datos incorrectos"));
                }
                break;

            case VERIFY_2FA:
                if (tempUser == null) {
                    logger.warn("Intento de verificación 2FA sin usuario temporal desde {}", clientAddress);
                    return;
                }

                String inputCode = msg.getContent();
                logger.info("Verificando código 2FA para usuario: {}", tempUser.getUsername());

                boolean isCodeValid = userDAO.check2FA(tempUser.getId(), inputCode);

                if (isCodeValid) {
                    this.currentUser = this.tempUser;
                    this.tempUser = null;

                    serverCore.addClient(currentUser.getId(), this);

                    logger.info("✓ Login completado exitosamente: UserID={}, Username={}",
                              currentUser.getId(), currentUser.getUsername());
                    activityLogger.info("2FA VERIFICADO | UserID: {} | Username: {} | IP: {}",
                                      currentUser.getId(), currentUser.getUsername(), clientAddress);

                    // Confirmar Login
                    Message okMsg = new Message(MessageType.LOGIN_OK, "Bienvenido");
                    okMsg.setSenderId(currentUser.getId());
                    okMsg.setSenderName(currentUser.getUsername());
                    sendMessage(okMsg);
                } else {
                    logger.warn("Código 2FA incorrecto para usuario: {}", tempUser.getUsername());
                    activityLogger.info("2FA FALLIDO | IP: {} | Usuario: {}", clientAddress, tempUser.getUsername());
                    sendMessage(new Message(MessageType.LOGIN_FAIL, "Código 2FA incorrecto"));
                }
                break;

            case REGISTER:
                String[] parts = msg.getContent().split(":");
                if (parts.length >= 3) {
                    User newUser = new User(parts[0], parts[1], parts[2]);
                    logger.info("Registrando nuevo usuario: {} con email: {}", parts[0], parts[1]);
                    activityLogger.info("REGISTRO NUEVO | Usuario: {} | Email: {} | IP: {}",
                                      parts[0], parts[1], clientAddress);

                    boolean registered = userDAO.registerUser(newUser);
                    if (registered) {
                        this.userId = newUser.getId();
                        logger.info("✓ Usuario registrado exitosamente: {}", parts[0]);
                        sendMessage(new Message(MessageType.REGISTER_OK, "Registro completado"));
                    } else {
                        logger.error("Error registrando usuario: {}", parts[0]);
                        sendMessage(new Message(MessageType.REGISTER_FAIL, "Error en el registro"));
                    }
                } else {
                    sendMessage(new Message(MessageType.REGISTER_FAIL, "Datos de registro incompletos"));
                }
                break;

            // --- CHAT ---
            case TEXT_MESSAGE:
                logger.debug("Mensaje de texto: UserID {} -> {}", currentUser.getId(),
                           msg.isGroupChat() ? "Grupo " + msg.getReceiverId() : "UserID " + msg.getReceiverId());
                processChatMessage(msg, "TEXTO");
                break;

            case FILE_MESSAGE:
                try {
                    logger.info("Archivo recibido de UserID {}: {} ({} bytes)",
                              currentUser.getId(), msg.getFileName(), msg.getFileData().length);

                    // Guardar archivo
                    String path = fileService.saveFile(msg.getFileData(), msg.getFileName());
                    msg.setServerFilePath(path);
                    msg.setFileData(null); // Limpiar bytes para aligerar

                    activityLogger.info("ARCHIVO GUARDADO | UserID: {} | Nombre: {} | Ruta: {}",
                                      currentUser.getId(), msg.getFileName(), path);

                    processChatMessage(msg, "ARCHIVO");
                } catch (IOException e) {
                    logger.error("Error guardando archivo de UserID {}", currentUser.getId(), e);
                }
                break;

            // --- FUNCIONALIDADES ---
            case GET_CONTACTS:
                logger.debug("Solicitando contactos para UserID: {}", currentUser.getId());
                List<User> contacts = userDAO.getContacts(currentUser.getId());
                Message contactResponse = new Message(MessageType.LIST_CONTACTS, "Lista de contactos");
                contactResponse.setContactList(contacts);
                sendMessage(contactResponse);
                logger.debug("Enviados {} contactos a UserID: {}", contacts.size(), currentUser.getId());
                break;

            case GET_HISTORY:
                int targetId = msg.getReceiverId();
                boolean isGroup = msg.isGroupChat();
                logger.debug("Solicitando historial: UserID {} con {} {}",
                           currentUser.getId(), isGroup ? "Grupo" : "Usuario", targetId);

                List<Message> history;
                if (isGroup) {
                    history = messageDAO.getGroupHistory(targetId);
                } else {
                    history = messageDAO.getPrivateHistory(currentUser.getId(), targetId);
                }

                Message historyResponse = new Message(MessageType.HISTORY_RESPONSE, "Historial recuperado");
                historyResponse.setHistoryList(history);
                sendMessage(historyResponse);

                logger.debug("Enviados {} mensajes de historial a UserID: {}", history.size(), currentUser.getId());
                break;

            case ADD_CONTACT:
                String targetUser = msg.getContent();
                boolean added = userDAO.addContact(this.userId, targetUser);

                if (added) {
                    logger.info("Contacto añadido: UserID {} añadió a {}", this.userId, targetUser);
                    sendMessage(new Message(MessageType.ADD_CONTACT_OK, "Usuario añadido."));

                    // Actualizar lista de contactos inmediatamente
                    List<User> newContacts = userDAO.getContacts(this.userId);
                    Message listMsg = new Message();
                    listMsg.setType(MessageType.LIST_CONTACTS);
                    listMsg.setContactList(newContacts);
                    sendMessage(listMsg);
                } else {
                    logger.warn("Intento fallido de añadir contacto: UserID {} intentó añadir {}", this.userId, targetUser);
                    sendMessage(new Message(MessageType.ADD_CONTACT_FAIL, "Usuario no encontrado o ya añadido."));
                }
                break;

            default:
                logger.warn("Comando no reconocido: {} desde UserID: {}", msg.getType(),
                          currentUser != null ? currentUser.getId() : "NO_AUTENTICADO");
        }
    }

    // Helper para no repetir lógica de envío
    private void processChatMessage(Message msg, String tipoBD) {
        msg.setSenderId(currentUser.getId());
        msg.setSenderName(currentUser.getUsername());

        // El servidor decide a quién enviarlo
        if (msg.isGroupChat()) {
            serverCore.sendGroupMessage(msg);
        } else {
            serverCore.sendPrivateMessage(msg);
        }
    }

    public void sendMessage(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            logger.error("Error enviando mensaje a cliente {}", clientAddress, e);
            running = false;
        }
    }

    private void closeConnection() {
        if (currentUser != null) {
            logger.info("Cerrando conexión para UserID: {} ({})", currentUser.getId(), currentUser.getUsername());
            serverCore.removeClient(currentUser.getId());
        } else {
            logger.debug("Cerrando conexión para cliente no autenticado: {}", clientAddress);
        }

        try {
            if (in != null) in.close();
        } catch (IOException e) {
            logger.debug("Error cerrando input stream", e);
        }
        try {
            if (out != null) out.close();
        } catch (IOException e) {
            logger.debug("Error cerrando output stream", e);
        }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            logger.debug("Error cerrando socket", e);
        }

        logger.debug("Conexión cerrada completamente para: {}", clientAddress);
    }

    public User getCurrentUser() {
        return currentUser;
    }
}