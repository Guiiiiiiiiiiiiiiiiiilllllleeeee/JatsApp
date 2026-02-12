package com.jatsapp.client.network;

import com.jatsapp.client.view.ChatFrame; // (Aún no me lo pasas, pero lo preparo)
import com.jatsapp.client.view.LoginFrame;
import com.jatsapp.client.view.ContactsFrame;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;
import com.jatsapp.common.User;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientSocket {

    private static ClientSocket instance;

    private Socket socket;
    private ObjectOutputStream out; // CAMBIO: Usamos ObjectOutput
    private ObjectInputStream in;   // CAMBIO: Usamos ObjectInput

    private LoginFrame loginFrame;
    private ChatFrame chatFrame;
    private ContactsFrame contactsFrame;

    private String myUsername;
    private int myUserId = -1;  // ID del usuario logueado
    private boolean isConnected = false;

    private final List<User> contacts = new ArrayList<>(); // Lista de contactos inicializada

    // Singleton
    public static synchronized ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        }
        return instance;
    }

    /**
     * Conecta al servidor. Llamado desde MainClient.
     */
    public void connect(String host, int port) throws IOException {
        this.socket = new Socket(host, port);

        // IMPORTANTE: Primero crear el Output, luego el Input (regla de Java Sockets)
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());

        this.isConnected = true;
        System.out.println("✅ Conectado a " + host + ":" + port);

        // Registrar ShutdownHook para desconexión limpia al cerrar la app
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🔌 Cerrando conexión...");
            disconnect();
        }, "ShutdownHook-Disconnect"));

        // Iniciar hilo de escucha
        new Thread(this::listen).start();
    }

    /**
     * Desconecta del servidor de forma limpia
     */
    public void disconnect() {
        if (!isConnected) return;

        try {
            // Enviar mensaje de desconexión al servidor
            Message disconnectMsg = new Message();
            disconnectMsg.setType(MessageType.DISCONNECT);
            send(disconnectMsg);

            isConnected = false;

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("✅ Desconectado correctamente");
        } catch (Exception e) {
            System.err.println("Error al desconectar: " + e.getMessage());
        }
    }

    public void setLoginFrame(LoginFrame frame) { this.loginFrame = frame; }
    public void setChatFrame(ChatFrame frame) { this.chatFrame = frame; }
    public void setContactsFrame(ContactsFrame frame) { this.contactsFrame = frame; }

    public void setMyUsername(String user) { this.myUsername = user; }
    public String getMyUsername() { return myUsername; }

    public void setMyUserId(int id) { this.myUserId = id; }
    public int getMyUserId() { return myUserId; }

    /**
     * Envía un objeto Message al servidor
     */
    public void send(Message msg) {
        if (!isConnected || socket.isClosed()) {
            System.err.println("❌ No se puede enviar el mensaje: conexión cerrada");
            return;
        }
        try {
            out.writeObject(msg);
            out.flush();
            System.out.println("📤 Enviado: " + msg.getType());
        } catch (IOException e) {
            System.err.println("❌ Error enviando mensaje: " + e.getMessage());
            closeConnection();
        }
    }

    public void sendMessage(Message msg) {
        send(msg);
    }

    /**
     * Bucle infinito para recibir mensajes
     */
    private void listen() {
        try {
            while (isConnected) {
                // Leemos el OBJETO directamente (igual que en el servidor)
                Message msg = (Message) in.readObject();
                System.out.println("📥 Recibido: " + msg.getType());

                // Procesamos en el hilo de la interfaz (EDT)
                SwingUtilities.invokeLater(() -> handleMessage(msg));
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("❌ Desconectado del servidor");
            closeConnection();
        }
    }

    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            // --- RESPUESTAS DE LOGIN/REGISTRO ---
            case require_2FA:
                if (loginFrame != null) loginFrame.onRequire2FA();
                break;

            case LOGIN_OK:
                // Almacenar el ID del usuario logueado
                this.myUserId = msg.getSenderId();
                if (msg.getSenderName() != null) {
                    this.myUsername = msg.getSenderName();
                }
                System.out.println("✅ Login exitoso. UserID: " + myUserId + ", Username: " + myUsername);
                if (loginFrame != null) loginFrame.onLoginSuccess();
                break;

            case LOGIN_FAIL:
                if (loginFrame != null) loginFrame.onLoginFail(msg.getContent());
                break;

            case REGISTER_FAIL: // Usamos el mismo método de fail para simplificar o uno específico
                JOptionPane.showMessageDialog(loginFrame, "Error Registro: " + msg.getContent());
                break;

            // --- CHAT ---
            case TEXT_MESSAGE:
            case FILE_MESSAGE:
            case ARCHIVO:
                if (chatFrame != null) {
                    chatFrame.recibirMensaje(msg);
                }
                break;

            // --- CONFIRMACIONES DE LECTURA ---
            case MESSAGE_DELIVERED:
                // Mensaje entregado al receptor
                if (chatFrame != null) {
                    chatFrame.actualizarEstadoMensaje(msg.getMessageId(), true, false);
                }
                break;

            case UPDATE_MESSAGE_STATUS:
                // Actualización completa del estado (entregado + leído)
                if (chatFrame != null) {
                    chatFrame.actualizarEstadoMensaje(msg.getMessageId(), msg.isDelivered(), msg.isRead());
                }
                break;

            // --- NUEVO CHAT DE DESCONOCIDO ---
            case NEW_CHAT_REQUEST:
                // Ya no usamos este tipo - los mensajes se reciben directamente
                // Mantenemos el case por compatibilidad pero no hacemos nada
                break;

            case LIST_CONTACTS:
                // Si ContactsFrame está abierto, es una respuesta para él
                // NO actualizar ChatFrame para no sobrescribir los chats activos
                if (contactsFrame != null) {
                    contactsFrame.actualizarContactos(msg.getContactList());
                } else if (chatFrame != null) {
                    // Solo actualizar ChatFrame si ContactsFrame NO está abierto
                    // Esto puede ser cuando se añade un contacto desde ChatFrame
                    chatFrame.actualizarContactos(msg.getContactList());
                }
                break;

            case HISTORY_RESPONSE:
                if (chatFrame != null) {
                    chatFrame.cargarHistorial(msg.getHistoryList());
                }
                break;

            // --- BÚSQUEDA DE USUARIOS ---
            case SEARCH_USER_RESULT:
                if (chatFrame != null) {
                    chatFrame.mostrarResultadosBusqueda(msg.getContactList());
                }
                break;

            // --- BÚSQUEDA GLOBAL DE MENSAJES ---
            case SEARCH_MESSAGES_RESULT:
                if (chatFrame != null) {
                    chatFrame.mostrarResultadosBusquedaGlobal(msg.getHistoryList());
                }
                break;

            case ADD_CONTACT_OK:
                // Contacto añadido exitosamente
                System.out.println("✅ Contacto añadido correctamente");
                break;

            case ADD_CONTACT_FAIL:
                JOptionPane.showMessageDialog(chatFrame, "No se pudo añadir el contacto: " + msg.getContent());
                break;

            case REMOVE_CONTACT:
                JOptionPane.showMessageDialog(null, msg.getContent(), "Eliminar Contacto", JOptionPane.INFORMATION_MESSAGE);
                // Solicitar la lista actualizada de contactos
                sendMessage(new Message(MessageType.GET_CONTACTS, ""));
                break;

            case STATUS_UPDATE:
                // Un usuario cambió su estado (conectado/desconectado)
                if (chatFrame != null) {
                    chatFrame.actualizarEstadoUsuario(msg.getSenderId(), msg.getSenderName(), msg.getContent());
                }
                break;

            case FILE_DOWNLOAD_RESPONSE:
                // El servidor envía los bytes de un archivo solicitado
                if (chatFrame != null) {
                    chatFrame.recibirArchivoDescargado(msg);
                }
                break;

            case ERROR:
                JOptionPane.showMessageDialog(chatFrame, "Error del servidor: " + msg.getContent(), "Error", JOptionPane.ERROR_MESSAGE);
                break;

            default:
                System.out.println("❓ Tipo desconocido: " + msg.getType());
        }
    }

    private void closeConnection() {
        isConnected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error cerrando socket: " + e.getMessage());
        }
        JOptionPane.showMessageDialog(null, "Conexión perdida con el servidor.");
        System.exit(0);
    }

    public void deleteContact(User user) {
        try {
            Message deleteContactMessage = new Message();
            deleteContactMessage.setType(MessageType.DELETE_CONTACT);
            deleteContactMessage.setSenderName(myUsername);
            deleteContactMessage.setReceiverId(user.getId());
            out.writeObject(deleteContactMessage);

            contacts.remove(user);
            System.out.println("Contacto eliminado: " + user.getUsername());
        } catch (IOException e) {
            System.err.println("Error eliminando contacto: " + e.getMessage());
        }
    }
}