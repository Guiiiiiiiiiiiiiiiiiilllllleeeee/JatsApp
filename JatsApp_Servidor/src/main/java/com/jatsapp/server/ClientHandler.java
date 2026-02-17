package com.jatsapp.server;

import com.jatsapp.common.Group;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;
import com.jatsapp.common.User;
import com.jatsapp.server.dao.GroupDAO;
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
    private GroupDAO groupDAO = new GroupDAO();
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
                    // Verificar si el usuario ha verificado su email
                    if (!userDAO.isEmailVerified(loginUser.getId())) {
                        logger.warn("Usuario {} no ha verificado su email", loginUser.getUsername());
                        sendMessage(new Message(MessageType.LOGIN_FAIL, "Debes verificar tu email primero"));
                        break;
                    }

                    // Login directo sin 2FA (ya verificado en registro)
                    this.currentUser = loginUser;
                    this.userId = loginUser.getId();
                    serverCore.addClient(currentUser.getId(), this);

                    logger.info("✓ Login completado exitosamente: UserID={}, Username={}",
                              currentUser.getId(), currentUser.getUsername());
                    activityLogger.info("LOGIN EXITOSO | UserID: {} | Username: {} | IP: {}",
                                      currentUser.getId(), currentUser.getUsername(), clientAddress);

                    // Confirmar Login
                    Message okMsg = new Message(MessageType.LOGIN_OK, "Login exitoso");
                    okMsg.setSenderId(currentUser.getId());
                    okMsg.setSenderName(currentUser.getUsername());
                    sendMessage(okMsg);
                } else {
                    logger.warn("Credenciales inválidas para usuario: {} desde {}", msg.getSenderName(), clientAddress);
                    activityLogger.info("LOGIN FALLIDO | IP: {} | Usuario: {}", clientAddress, msg.getSenderName());
                    sendMessage(new Message(MessageType.LOGIN_FAIL, "Usuario o contraseña incorrectos"));
                }
                break;

            case VERIFY_2FA:
                // Verificación del código 2FA durante el REGISTRO
                if (tempUser == null) {
                    logger.warn("Intento de verificación 2FA sin usuario temporal desde {}", clientAddress);
                    return;
                }

                String inputCode = msg.getContent();
                logger.info("Verificando código 2FA para nuevo usuario: {}", tempUser.getUsername());

                boolean isCodeValid = userDAO.check2FA(tempUser.getId(), inputCode);

                if (isCodeValid) {
                    // Marcar email como verificado
                    userDAO.setEmailVerified(tempUser.getId(), true);

                    logger.info("✓ Email verificado para usuario: {}", tempUser.getUsername());
                    activityLogger.info("EMAIL VERIFICADO | UserID: {} | Username: {}",
                                      tempUser.getId(), tempUser.getUsername());

                    // Enviar confirmación - el usuario debe hacer login ahora
                    sendMessage(new Message(MessageType.REGISTER_OK, "Email verificado. Ya puedes iniciar sesión."));
                    this.tempUser = null;
                } else {
                    logger.warn("Código 2FA incorrecto para usuario: {}", tempUser.getUsername());
                    activityLogger.info("VERIFICACIÓN FALLIDA | Usuario: {}", tempUser.getUsername());
                    sendMessage(new Message(MessageType.LOGIN_FAIL, "Código incorrecto. Intenta de nuevo."));
                }
                break;

            case REGISTER:
                String[] parts = msg.getContent().split(":");
                if (parts.length >= 3) {
                    String username = parts[0];
                    String email = parts[1];
                    String password = parts[2];

                    User newUser = new User(username, email, password);
                    logger.info("Registrando nuevo usuario: {} con email: {}", username, email);
                    activityLogger.info("REGISTRO NUEVO | Usuario: {} | Email: {} | IP: {}",
                                      username, email, clientAddress);

                    boolean registered = userDAO.registerUser(newUser);
                    if (registered) {
                        this.userId = newUser.getId();
                        this.tempUser = newUser; // Guardar para verificación

                        // Generar código 2FA y enviar por email
                        String code = SecurityService.generate2FACode();
                        long expiration = System.currentTimeMillis() + 10 * 60 * 1000; // 10 minutos
                        userDAO.set2FACode(newUser.getId(), code, expiration);

                        emailService.sendEmail(email,
                            "Verificación de cuenta JatsApp",
                            "¡Bienvenido a JatsApp!\n\nTu código de verificación es: " + code +
                            "\n\nEste código expira en 10 minutos.");

                        logger.info("✓ Usuario registrado, código de verificación enviado a: {}", email);
                        activityLogger.info("CÓDIGO VERIFICACIÓN ENVIADO | Usuario: {} | Email: {}", username, email);

                        // Pedir al cliente que ingrese el código
                        sendMessage(new Message(MessageType.require_2FA, "Revisa tu email para verificar tu cuenta"));
                    } else {
                        logger.error("Error registrando usuario: {}", username);
                        sendMessage(new Message(MessageType.REGISTER_FAIL, "Error en el registro. El usuario o email ya existe."));
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
                              currentUser.getId(), msg.getFileName(),
                              msg.getFileData() != null ? msg.getFileData().length : 0);

                    // Guardar archivo en el servidor
                    if (msg.getFileData() != null && msg.getFileData().length > 0) {
                        String path = fileService.saveFile(msg.getFileData(), msg.getFileName());
                        msg.setServerFilePath(path);
                        // NO limpiar fileData - el receptor necesita los bytes para descargar
                        // msg.setFileData(null);

                        activityLogger.info("ARCHIVO GUARDADO | UserID: {} | Nombre: {} | Ruta: {}",
                                          currentUser.getId(), msg.getFileName(), path);
                    }

                    processChatMessage(msg, "ARCHIVO");
                } catch (IOException e) {
                    logger.error("Error guardando archivo de UserID {}", currentUser.getId(), e);
                }
                break;

            case DISCONNECT:
                // El cliente notifica que se va a desconectar
                logger.info("Cliente solicita desconexión: UserID={}",
                          currentUser != null ? currentUser.getId() : "no autenticado");
                running = false; // Terminar el bucle de escucha
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
                // Incluir info del chat para que el cliente verifique que corresponde al chat actual
                historyResponse.setReceiverId(targetId);
                historyResponse.setGroupChat(isGroup);
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

            case SEARCH_USER:
                // Buscar usuarios por nombre
                String searchTerm = msg.getContent();
                logger.debug("Búsqueda de usuarios: '{}' por UserID {}", searchTerm, currentUser.getId());

                List<User> searchResults = userDAO.searchUsers(searchTerm, currentUser.getId());
                Message searchResponse = new Message(MessageType.SEARCH_USER_RESULT, "Resultados de búsqueda");
                searchResponse.setContactList(searchResults);
                sendMessage(searchResponse);

                logger.debug("Encontrados {} usuarios para búsqueda '{}'", searchResults.size(), searchTerm);
                break;

            case SEARCH_MESSAGES:
                // Buscar mensajes en todos los chats
                String messageSearchTerm = msg.getContent();
                logger.debug("Búsqueda de mensajes: '{}' por UserID {}", messageSearchTerm, currentUser.getId());

                List<Message> messageSearchResults = messageDAO.searchMessages(currentUser.getId(), messageSearchTerm);
                Message messageSearchResponse = new Message(MessageType.SEARCH_MESSAGES_RESULT, "Resultados de búsqueda de mensajes");
                messageSearchResponse.setHistoryList(messageSearchResults);
                sendMessage(messageSearchResponse);

                logger.debug("Encontrados {} mensajes para búsqueda '{}'", messageSearchResults.size(), messageSearchTerm);
                break;

            case ACCEPT_CHAT:
                // YA NO SE USA - Sistema estilo WhatsApp: no hay aceptar/rechazar
                // Los mensajes se reciben automáticamente
                // Se mantiene por compatibilidad pero no hace nada
                logger.debug("ACCEPT_CHAT recibido pero ya no se usa (sistema WhatsApp)");
                break;

            case MESSAGE_READ:
                // El receptor confirma que ha leído un mensaje
                int messageIdRead = msg.getMessageId();
                boolean markedRead = messageDAO.markAsRead(messageIdRead);

                if (markedRead) {
                    logger.debug("Mensaje {} marcado como leído", messageIdRead);

                    // Obtener el mensaje para saber quién es el emisor
                    Message originalMsg = messageDAO.getMessageById(messageIdRead);
                    if (originalMsg != null) {
                        // Enviar confirmación al emisor
                        ClientHandler sender = serverCore.getClientHandler(originalMsg.getSenderId());
                        if (sender != null) {
                            Message readConfirmation = new Message(MessageType.UPDATE_MESSAGE_STATUS, "");
                            readConfirmation.setMessageId(messageIdRead);
                            readConfirmation.setDelivered(true);
                            readConfirmation.setRead(true);
                            sender.sendMessage(readConfirmation);

                            logger.debug("Confirmación de lectura enviada al emisor {}", originalMsg.getSenderId());
                        }
                    }
                }
                break;

            case REMOVE_CONTACT:
                logger.info("Eliminando contacto: {} para usuario {}", msg.getContent(), currentUser.getUsername());
                boolean removed = userDAO.removeContact(currentUser.getId(), msg.getContent());
                if (removed) {
                    sendMessage(new Message(MessageType.REMOVE_CONTACT, "Contacto eliminado correctamente"));
                } else {
                    sendMessage(new Message(MessageType.REMOVE_CONTACT, "No se pudo eliminar el contacto"));
                }
                break;

            case GET_RELEVANT_CHATS:
                logger.info("Procesando solicitud de chats relevantes para el usuario: {}", currentUser.getUsername());
                try {
                    // Obtener usuarios con los que ha conversado + contactos
                    List<User> relevantChats = userDAO.getRelevantChats(currentUser.getId());
                    Message response = new Message(MessageType.LIST_CONTACTS, "Lista de chats activos");
                    response.setContactList(relevantChats);
                    sendMessage(response);
                    logger.debug("Enviados {} chats relevantes a UserID: {}", relevantChats.size(), currentUser.getId());
                } catch (Exception e) {
                    logger.error("Error al obtener chats relevantes para el usuario {}: {}", currentUser.getUsername(), e.getMessage());
                    sendMessage(new Message(MessageType.ERROR, "No se pudieron obtener los chats relevantes"));
                }
                break;

            case DOWNLOAD_FILE:
                // Cliente solicita descargar un archivo del historial
                int fileMessageId = msg.getMessageId();
                logger.info("Solicitud de descarga de archivo: messageId {} por UserID {}", fileMessageId, currentUser.getId());

                try {
                    // Obtener el mensaje con los bytes del archivo desde la BD
                    Message originalFileMsg = messageDAO.getMessageById(fileMessageId);

                    if (originalFileMsg == null) {
                        logger.warn("Mensaje no encontrado en BD: messageId {}", fileMessageId);
                        sendMessage(new Message(MessageType.ERROR, "Mensaje no encontrado"));
                        break;
                    }

                    byte[] fileBytes = originalFileMsg.getFileData();

                    if (fileBytes != null && fileBytes.length > 0) {
                        Message downloadResponse = new Message(MessageType.FILE_DOWNLOAD_RESPONSE, "Archivo descargado");
                        downloadResponse.setMessageId(fileMessageId);
                        downloadResponse.setFileName(originalFileMsg.getFileName());
                        downloadResponse.setFileData(fileBytes);
                        sendMessage(downloadResponse);

                        logger.info("Archivo enviado: {} ({} bytes) a UserID {}",
                                   originalFileMsg.getFileName(), fileBytes.length, currentUser.getId());
                    } else {
                        logger.warn("Archivo sin datos en BD: messageId {}", fileMessageId);
                        sendMessage(new Message(MessageType.ERROR, "Archivo no disponible"));
                    }
                } catch (Exception e) {
                    logger.error("Error descargando archivo: {}", e.getMessage(), e);
                    sendMessage(new Message(MessageType.ERROR, "Error al descargar archivo"));
                }
                break;

            // ========================================
            // --- GESTIÓN DE GRUPOS ---
            // ========================================

            case CREATE_GROUP:
                // Crear un nuevo grupo (el usuario actual será el admin)
                String groupName = msg.getContent();
                logger.info("Creando grupo '{}' por UserID {}", groupName, currentUser.getId());

                if (groupName == null || groupName.trim().isEmpty()) {
                    sendMessage(new Message(MessageType.CREATE_GROUP_FAIL, "El nombre del grupo no puede estar vacío"));
                    break;
                }

                int newGroupId = groupDAO.createGroup(groupName.trim(), currentUser.getId());
                if (newGroupId > 0) {
                    logger.info("✓ Grupo '{}' creado con ID {}", groupName, newGroupId);
                    activityLogger.info("GRUPO CREADO | ID: {} | Nombre: {} | Admin: {}",
                                      newGroupId, groupName, currentUser.getUsername());

                    // Obtener el grupo completo para enviarlo al cliente
                    Group createdGroup = groupDAO.getGroupById(newGroupId);
                    Message response = new Message(MessageType.CREATE_GROUP_OK, "Grupo creado exitosamente");
                    response.setGroup(createdGroup);
                    sendMessage(response);
                } else {
                    sendMessage(new Message(MessageType.CREATE_GROUP_FAIL, "Error al crear el grupo"));
                }
                break;

            case GET_GROUPS:
                // Obtener todos los grupos del usuario
                logger.debug("Solicitando grupos para UserID: {}", currentUser.getId());
                List<Group> userGroups = groupDAO.getGroupsByUser(currentUser.getId());
                Message groupsResponse = new Message(MessageType.LIST_GROUPS, "Lista de grupos");
                groupsResponse.setGroupList(userGroups);
                sendMessage(groupsResponse);
                logger.debug("Enviados {} grupos a UserID: {}", userGroups.size(), currentUser.getId());
                break;

            case ADD_GROUP_MEMBER:
                // Añadir miembro a un grupo (solo admin puede hacerlo)
                int groupIdToAdd = msg.getReceiverId(); // ID del grupo
                String usernameToAdd = msg.getContent(); // Nombre del usuario a añadir

                logger.info("Añadiendo '{}' al grupo {} por UserID {}", usernameToAdd, groupIdToAdd, currentUser.getId());

                // Verificar que es admin
                if (!groupDAO.isGroupAdmin(groupIdToAdd, currentUser.getId())) {
                    logger.warn("Usuario {} no es admin del grupo {}", currentUser.getId(), groupIdToAdd);
                    sendMessage(new Message(MessageType.ADD_GROUP_MEMBER_FAIL, "Solo el administrador puede añadir miembros"));
                    break;
                }

                // Verificar límite de miembros
                if (groupDAO.getMemberCount(groupIdToAdd) >= Group.MAX_MEMBERS) {
                    sendMessage(new Message(MessageType.ADD_GROUP_MEMBER_FAIL,
                              "El grupo ha alcanzado el límite de " + Group.MAX_MEMBERS + " miembros"));
                    break;
                }

                // Buscar el usuario por nombre
                User userToAdd = userDAO.getUserByUsername(usernameToAdd);
                if (userToAdd == null) {
                    sendMessage(new Message(MessageType.ADD_GROUP_MEMBER_FAIL, "Usuario no encontrado"));
                    break;
                }

                // Verificar que no esté ya en el grupo
                if (groupDAO.isMember(groupIdToAdd, userToAdd.getId())) {
                    sendMessage(new Message(MessageType.ADD_GROUP_MEMBER_FAIL, "El usuario ya es miembro del grupo"));
                    break;
                }

                // Añadir al grupo
                if (groupDAO.addMemberToGroup(groupIdToAdd, userToAdd.getId())) {
                    activityLogger.info("MIEMBRO AÑADIDO | Grupo: {} | Usuario: {} | Por: {}",
                                      groupIdToAdd, userToAdd.getUsername(), currentUser.getUsername());

                    // Enviar confirmación con el grupo actualizado
                    Group updatedGroup = groupDAO.getGroupById(groupIdToAdd);
                    Message addOkResponse = new Message(MessageType.ADD_GROUP_MEMBER_OK, "Miembro añadido correctamente");
                    addOkResponse.setGroup(updatedGroup);
                    sendMessage(addOkResponse);

                    // Notificar al nuevo miembro si está online
                    ClientHandler newMemberHandler = serverCore.getClientHandler(userToAdd.getId());
                    if (newMemberHandler != null) {
                        Message notification = new Message(MessageType.GROUP_NOTIFICATION,
                                                          "Has sido añadido al grupo: " + updatedGroup.getNombre());
                        notification.setGroup(updatedGroup);
                        newMemberHandler.sendMessage(notification);
                    }

                    // Notificar a los demás miembros del grupo
                    notifyGroupMembers(groupIdToAdd, currentUser.getId(),
                                      userToAdd.getUsername() + " se ha unido al grupo", updatedGroup);
                } else {
                    sendMessage(new Message(MessageType.ADD_GROUP_MEMBER_FAIL, "Error al añadir miembro"));
                }
                break;

            case REMOVE_GROUP_MEMBER:
                // Eliminar miembro de un grupo (solo admin puede hacerlo)
                int groupIdToRemove = msg.getReceiverId();
                String usernameToRemove = msg.getContent();

                logger.info("Eliminando '{}' del grupo {} por UserID {}", usernameToRemove, groupIdToRemove, currentUser.getId());

                // Verificar que es admin
                if (!groupDAO.isGroupAdmin(groupIdToRemove, currentUser.getId())) {
                    sendMessage(new Message(MessageType.REMOVE_GROUP_MEMBER_FAIL, "Solo el administrador puede eliminar miembros"));
                    break;
                }

                // Buscar el usuario
                User userToRemove = userDAO.getUserByUsername(usernameToRemove);
                if (userToRemove == null) {
                    sendMessage(new Message(MessageType.REMOVE_GROUP_MEMBER_FAIL, "Usuario no encontrado"));
                    break;
                }

                // No permitir eliminar al admin
                if (groupDAO.isGroupAdmin(groupIdToRemove, userToRemove.getId())) {
                    sendMessage(new Message(MessageType.REMOVE_GROUP_MEMBER_FAIL, "No se puede eliminar al administrador"));
                    break;
                }

                if (groupDAO.removeMemberFromGroup(groupIdToRemove, userToRemove.getId())) {
                    activityLogger.info("MIEMBRO ELIMINADO | Grupo: {} | Usuario: {} | Por: {}",
                                      groupIdToRemove, userToRemove.getUsername(), currentUser.getUsername());

                    Group groupAfterRemove = groupDAO.getGroupById(groupIdToRemove);
                    Message removeOkResponse = new Message(MessageType.REMOVE_GROUP_MEMBER_OK, "Miembro eliminado");
                    removeOkResponse.setGroup(groupAfterRemove);
                    sendMessage(removeOkResponse);

                    // Notificar al usuario eliminado si está online
                    ClientHandler removedHandler = serverCore.getClientHandler(userToRemove.getId());
                    if (removedHandler != null) {
                        Message notification = new Message(MessageType.GROUP_NOTIFICATION,
                                                          "Has sido eliminado del grupo: " + groupAfterRemove.getNombre());
                        notification.setReceiverId(groupIdToRemove);
                        removedHandler.sendMessage(notification);
                    }

                    // Notificar a los demás miembros
                    notifyGroupMembers(groupIdToRemove, currentUser.getId(),
                                      userToRemove.getUsername() + " ha sido eliminado del grupo", groupAfterRemove);
                } else {
                    sendMessage(new Message(MessageType.REMOVE_GROUP_MEMBER_FAIL, "Error al eliminar miembro"));
                }
                break;

            case PROMOTE_TO_ADMIN:
                // Promover a un miembro a administrador
                int groupIdPromote = msg.getReceiverId();
                String promoteUsername = msg.getContent();

                logger.info("Promoviendo '{}' a admin del grupo {} por UserID {}",
                           promoteUsername, groupIdPromote, currentUser.getId());

                // Verificar que el usuario actual es admin
                if (!groupDAO.isGroupAdmin(groupIdPromote, currentUser.getId())) {
                    sendMessage(new Message(MessageType.PROMOTE_TO_ADMIN_FAIL,
                                          "Solo un administrador puede promover a otros"));
                    break;
                }

                // Buscar el usuario a promover
                User userToPromote = userDAO.getUserByUsername(promoteUsername);
                if (userToPromote == null) {
                    sendMessage(new Message(MessageType.PROMOTE_TO_ADMIN_FAIL, "Usuario no encontrado"));
                    break;
                }

                // Verificar que es miembro del grupo
                if (!groupDAO.isMember(groupIdPromote, userToPromote.getId())) {
                    sendMessage(new Message(MessageType.PROMOTE_TO_ADMIN_FAIL,
                                          "El usuario no es miembro del grupo"));
                    break;
                }

                // Verificar que no es ya admin
                if (groupDAO.isGroupAdmin(groupIdPromote, userToPromote.getId())) {
                    sendMessage(new Message(MessageType.PROMOTE_TO_ADMIN_FAIL,
                                          "El usuario ya es administrador"));
                    break;
                }

                // Promover
                if (groupDAO.promoteToAdmin(groupIdPromote, userToPromote.getId())) {
                    activityLogger.info("ADMIN PROMOVIDO | Grupo: {} | Usuario: {} | Por: {}",
                                      groupIdPromote, userToPromote.getUsername(), currentUser.getUsername());

                    Group groupAfterPromote = groupDAO.getGroupById(groupIdPromote);
                    Message promoteOkResponse = new Message(MessageType.PROMOTE_TO_ADMIN_OK,
                                                           userToPromote.getUsername() + " ahora es administrador");
                    promoteOkResponse.setGroup(groupAfterPromote);
                    sendMessage(promoteOkResponse);

                    // Notificar al nuevo admin si está online
                    ClientHandler promotedHandler = serverCore.getClientHandler(userToPromote.getId());
                    if (promotedHandler != null) {
                        Message notification = new Message(MessageType.GROUP_NOTIFICATION,
                                                          "Ahora eres administrador del grupo: " + groupAfterPromote.getNombre());
                        notification.setGroup(groupAfterPromote);
                        promotedHandler.sendMessage(notification);
                    }

                    // Notificar a los demás miembros
                    notifyGroupMembers(groupIdPromote, currentUser.getId(),
                                      userToPromote.getUsername() + " ahora es administrador", groupAfterPromote);
                } else {
                    sendMessage(new Message(MessageType.PROMOTE_TO_ADMIN_FAIL,
                                          "Error al promover a administrador"));
                }
                break;

            case DEMOTE_FROM_ADMIN:
                // Quitar rol de admin a un miembro
                int groupIdDemote = msg.getReceiverId();
                String demoteUsername = msg.getContent();

                logger.info("Quitando admin a '{}' del grupo {} por UserID {}",
                           demoteUsername, groupIdDemote, currentUser.getId());

                // Verificar que el usuario actual es admin
                if (!groupDAO.isGroupAdmin(groupIdDemote, currentUser.getId())) {
                    sendMessage(new Message(MessageType.DEMOTE_FROM_ADMIN_FAIL,
                                          "Solo un administrador puede quitar el rol de admin"));
                    break;
                }

                // Buscar el usuario a degradar
                User userToDemote = userDAO.getUserByUsername(demoteUsername);
                if (userToDemote == null) {
                    sendMessage(new Message(MessageType.DEMOTE_FROM_ADMIN_FAIL, "Usuario no encontrado"));
                    break;
                }

                // Verificar que es admin
                if (!groupDAO.isGroupAdmin(groupIdDemote, userToDemote.getId())) {
                    sendMessage(new Message(MessageType.DEMOTE_FROM_ADMIN_FAIL,
                                          "El usuario no es administrador"));
                    break;
                }

                // Degradar
                if (groupDAO.demoteFromAdmin(groupIdDemote, userToDemote.getId())) {
                    activityLogger.info("ADMIN DEGRADADO | Grupo: {} | Usuario: {} | Por: {}",
                                      groupIdDemote, userToDemote.getUsername(), currentUser.getUsername());

                    Group groupAfterDemote = groupDAO.getGroupById(groupIdDemote);
                    Message demoteOkResponse = new Message(MessageType.DEMOTE_FROM_ADMIN_OK,
                                                          userToDemote.getUsername() + " ya no es administrador");
                    demoteOkResponse.setGroup(groupAfterDemote);
                    sendMessage(demoteOkResponse);

                    // Notificar al usuario degradado si está online
                    ClientHandler demotedHandler = serverCore.getClientHandler(userToDemote.getId());
                    if (demotedHandler != null) {
                        Message notification = new Message(MessageType.GROUP_NOTIFICATION,
                                                          "Ya no eres administrador del grupo: " + groupAfterDemote.getNombre());
                        notification.setGroup(groupAfterDemote);
                        demotedHandler.sendMessage(notification);
                    }

                    // Notificar a los demás miembros
                    notifyGroupMembers(groupIdDemote, currentUser.getId(),
                                      userToDemote.getUsername() + " ya no es administrador", groupAfterDemote);
                } else {
                    sendMessage(new Message(MessageType.DEMOTE_FROM_ADMIN_FAIL,
                                          "Error al quitar rol de administrador. Debe haber al menos un admin."));
                }
                break;

            case LEAVE_GROUP:
                // Usuario abandona voluntariamente un grupo
                int groupIdToLeave = msg.getReceiverId();
                String groupNameToLeave = groupDAO.getGroupName(groupIdToLeave);

                logger.info("UserID {} abandonando grupo {}", currentUser.getId(), groupIdToLeave);

                boolean isAdmin = groupDAO.isGroupAdmin(groupIdToLeave, currentUser.getId());

                if (groupDAO.leaveGroup(groupIdToLeave, currentUser.getId())) {
                    activityLogger.info("ABANDONO GRUPO | Grupo: {} | Usuario: {} | EraAdmin: {}",
                                      groupIdToLeave, currentUser.getUsername(), isAdmin);

                    sendMessage(new Message(MessageType.LEAVE_GROUP_OK, "Has abandonado el grupo"));

                    if (isAdmin) {
                        // Si era admin, el grupo fue eliminado - notificar a todos los ex-miembros
                        // (En este caso el grupo ya no existe, así que los demás miembros recibirán error si intentan acceder)
                        logger.info("Grupo {} eliminado porque el admin abandonó", groupIdToLeave);
                    } else {
                        // Notificar a los demás miembros
                        Group groupAfterLeave = groupDAO.getGroupById(groupIdToLeave);
                        if (groupAfterLeave != null) {
                            notifyGroupMembers(groupIdToLeave, currentUser.getId(),
                                              currentUser.getUsername() + " ha abandonado el grupo", groupAfterLeave);
                        }
                    }
                } else {
                    sendMessage(new Message(MessageType.LEAVE_GROUP_FAIL, "Error al abandonar el grupo"));
                }
                break;

            case GET_GROUP_MEMBERS:
                // Obtener lista de miembros de un grupo
                int groupIdForMembers = msg.getReceiverId();
                logger.debug("Solicitando miembros del grupo {} por UserID {}", groupIdForMembers, currentUser.getId());

                // Verificar que el usuario es miembro del grupo
                if (!groupDAO.isMember(groupIdForMembers, currentUser.getId())) {
                    sendMessage(new Message(MessageType.ERROR, "No eres miembro de este grupo"));
                    break;
                }

                List<User> members = groupDAO.getGroupMembers(groupIdForMembers);
                Message membersResponse = new Message(MessageType.LIST_GROUP_MEMBERS, "Lista de miembros");
                membersResponse.setContactList(members);
                membersResponse.setReceiverId(groupIdForMembers);
                sendMessage(membersResponse);
                break;

            case GET_GROUP_INFO:
                // Obtener información completa de un grupo
                int groupIdForInfo = msg.getReceiverId();
                logger.debug("Solicitando info del grupo {} por UserID {}", groupIdForInfo, currentUser.getId());

                Group groupInfo = groupDAO.getGroupById(groupIdForInfo);
                if (groupInfo != null) {
                    Message infoResponse = new Message(MessageType.GROUP_INFO_RESPONSE, "Información del grupo");
                    infoResponse.setGroup(groupInfo);
                    sendMessage(infoResponse);
                } else {
                    sendMessage(new Message(MessageType.ERROR, "Grupo no encontrado"));
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

    /**
     * Notifica a todos los miembros de un grupo sobre un evento
     * @param groupId ID del grupo
     * @param excludeUserId Usuario a excluir de la notificación (normalmente quien generó el evento)
     * @param notificationText Texto de la notificación
     * @param group Objeto Group actualizado para incluir en la notificación
     */
    private void notifyGroupMembers(int groupId, int excludeUserId, String notificationText, Group group) {
        List<Integer> memberIds = groupDAO.getGroupMemberIds(groupId);

        for (Integer memberId : memberIds) {
            if (memberId != excludeUserId) {
                ClientHandler memberHandler = serverCore.getClientHandler(memberId);
                if (memberHandler != null) {
                    Message notification = new Message(MessageType.GROUP_NOTIFICATION, notificationText);
                    notification.setGroup(group);
                    notification.setReceiverId(groupId);
                    memberHandler.sendMessage(notification);
                }
            }
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
        running = false;

        if (currentUser != null) {
            logger.info("Cerrando conexión para UserID: {} ({})", currentUser.getId(), currentUser.getUsername());
            serverCore.removeClient(currentUser.getId());
        } else {
            logger.debug("Cerrando conexión para cliente no autenticado: {}", clientAddress);
        }

        // Cerrar streams y socket de forma segura
        // Primero verificar si el socket ya está cerrado para evitar excepciones innecesarias
        boolean socketAlreadyClosed = socket == null || socket.isClosed();

        try {
            if (in != null) in.close();
        } catch (IOException e) {
            logger.debug("Error cerrando input stream", e);
        }

        try {
            // Solo intentar cerrar output si el socket no estaba ya cerrado
            if (out != null && !socketAlreadyClosed) {
                out.close();
            }
        } catch (IOException e) {
            // Es normal que falle si el cliente cerró la conexión abruptamente
            logger.debug("Error cerrando output stream (cliente probablemente desconectado): {}", e.getMessage());
        }

        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            logger.debug("Error cerrando socket", e);
        }

        logger.debug("Conexión cerrada completamente para: {}", clientAddress);
    }

    /**
     * Verifica si el cliente sigue conectado
     */
    public boolean isAlive() {
        return running && socket != null && !socket.isClosed() && socket.isConnected();
    }

    public User getCurrentUser() {
        return currentUser;
    }
}

