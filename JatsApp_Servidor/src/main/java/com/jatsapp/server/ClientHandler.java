package com.jatsapp.server;

import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;
import com.jatsapp.common.User;
import com.jatsapp.server.dao.MessageDAO;
import com.jatsapp.server.dao.UserDAO;
import com.jatsapp.server.service.EmailService;
import com.jatsapp.server.service.FileService;
import com.jatsapp.server.service.SecurityService; // Asumiendo que creaste la clase con generate2FACode

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {

    private Socket socket;
    private ServerCore serverCore;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private int userId = -1; // -1 significa "no identificado"

    private User currentUser;      // Usuario totalmente logueado
    private User tempUser;         // Usuario en proceso de 2FA
    private boolean running = true;

    // Servicios y DAOs
    private UserDAO userDAO = new UserDAO();
    private MessageDAO messageDAO = new MessageDAO();
    private FileService fileService = new FileService();
    private EmailService emailService = new EmailService();

    public ClientHandler(Socket socket, ServerCore serverCore) {
        this.socket = socket;
        this.serverCore = serverCore;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (running) {
                try {
                    Message receivedMsg = (Message) in.readObject();
                    handleMessage(receivedMsg);
                } catch (ClassNotFoundException e) {
                    System.err.println("Clase no encontrada: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            // Cliente desconectado
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
            return;
        }

        switch (msg.getType()) {
            // --- AUTENTICACIÓN ---
            case LOGIN:
                String usuarioLogin = msg.getSenderName(); // O msg.getContent() según como lo mandes
                String passwordLogin = msg.getContent();

                User loginUser = userDAO.login(usuarioLogin, passwordLogin);
                if (loginUser != null) {
                    // Generar código 2FA
                    String code = SecurityService.generate2FACode();
                    long expiration = System.currentTimeMillis() + 5 * 60 * 1000; // 5 minutos
                    userDAO.set2FACode(loginUser.getId(), code, expiration);
                    emailService.sendEmail(loginUser.getEmail(), "Tu código de acceso JatsApp", "Tu código es: " + code);

                    // Guardar usuario temporalmente hasta que verifique el código
                    this.tempUser = loginUser;
                    this.userId = loginUser.getId();

                    // Pedir al cliente el código
                    sendMessage(new Message(MessageType.require_2FA, "Revisa tu email"));
                } else {
                    sendMessage(new Message(MessageType.LOGIN_FAIL, "Datos incorrectos"));
                }
                break;

            case VERIFY_2FA:
                // Paso 2: Verificar el código que envía el cliente
                if (tempUser == null) return;

                String inputCode = msg.getContent();
                // Necesitas un método en UserDAO para verificar el código: check2FA(userId, code)
                // O implementar la lógica aquí recuperando el código de la BD.
                boolean isCodeValid = userDAO.check2FA(tempUser.getId(), inputCode);

                if (isCodeValid) {
                    this.currentUser = this.tempUser;
                    this.tempUser = null;

                    serverCore.addClient(currentUser.getId(), this);

                    // Confirmar Login
                    Message okMsg = new Message(MessageType.LOGIN_OK, "Bienvenido");
                    okMsg.setSenderId(currentUser.getId());
                    okMsg.setSenderName(currentUser.getUsername());
                    sendMessage(okMsg);
                } else {
                    sendMessage(new Message(MessageType.LOGIN_FAIL, "Código 2FA incorrecto"));
                }
                break;

            case REGISTER:
                String[] parts = msg.getContent().split(":"); // Formato "user:email:pass"
                if (parts.length >= 3) {
                    User newUser = new User(parts[0], parts[1], parts[2]);
                    boolean registered = userDAO.registerUser(newUser);
                    if (registered) {
                        this.userId = newUser.getId();
                        sendMessage(new Message(MessageType.REGISTER_OK, "Registro completado"));
                    } else {
                        sendMessage(new Message(MessageType.REGISTER_FAIL, "Error en el registro"));
                    }
                } else {
                    sendMessage(new Message(MessageType.REGISTER_FAIL, "Datos de registro incompletos"));
                }
                break;

            // --- CHAT ---
            case TEXT_MESSAGE:
                processChatMessage(msg, "TEXTO");
                break;

            case FILE_MESSAGE:
                try {
                    // Guardar archivo
                    String path = fileService.saveFile(msg.getFileData(), msg.getFileName());
                    msg.setServerFilePath(path);
                    msg.setFileData(null); // Limpiar bytes para aligerar

                    processChatMessage(msg, "ARCHIVO");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            // --- FUNCIONALIDADES ---
            case GET_CONTACTS:
                List<User> contacts = userDAO.getContacts(currentUser.getId());
                Message contactResponse = new Message(MessageType.LIST_CONTACTS, "Lista de contactos");
                contactResponse.setContactList(contacts);
                sendMessage(contactResponse);
                break;

            case GET_HISTORY:
                // msg.getReceiverId() indica de qué usuario/grupo queremos el historial
                List<Message> history;
                if (msg.isGroupChat()) {
                    history = messageDAO.getGroupHistory(msg.getReceiverId());
                } else {
                    history = messageDAO.getPrivateHistory(currentUser.getId(), msg.getReceiverId());
                }

                Message historyResponse = new Message(MessageType.HISTORY_RESPONSE, "Historial recuperado");
                historyResponse.setHistoryList(history);
                sendMessage(historyResponse);
                break;
            // En com.jatsapp.server.ClientHandler -> handleMessage -> switch

            case ADD_CONTACT:
                String targetUser = msg.getContent();
                boolean added = userDAO.addContact(this.userId, targetUser);

                if (added) {
                    sendMessage(new Message(MessageType.ADD_CONTACT_OK, "Usuario añadido."));
                    // TRUCO PRO: Forzamos una actualización de la lista de contactos inmediatamente
                    // para que le aparezca al usuario sin tener que reiniciar.
                    List<User> newContacts = userDAO.getContacts(this.userId);
                    Message listMsg = new Message();
                    listMsg.setType(MessageType.LIST_CONTACTS);
                    listMsg.setContactList(newContacts);
                    sendMessage(listMsg);
                } else {
                    sendMessage(new Message(MessageType.ADD_CONTACT_FAIL, "Usuario no encontrado o ya añadido."));
                }
                break;
            default:
                System.out.println("Comando no reconocido: " + msg.getType());
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
            running = false;
        }
    }

    private void closeConnection() {
        if (currentUser != null) {
            serverCore.removeClient(currentUser.getId());
        }
        try {
            if (in != null) in.close();
        } catch (IOException e) {
            // Ignorar
        }
        try {
            if (out != null) out.close();
        } catch (IOException e) {
            // Ignorar
        }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // Ignorar
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }
}