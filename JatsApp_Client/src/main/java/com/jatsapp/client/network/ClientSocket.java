package com.jatsapp.client.network;

import com.jatsapp.client.view.ChatFrame; // (Aún no me lo pasas, pero lo preparo)
import com.jatsapp.client.view.LoginFrame;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientSocket {

    private static ClientSocket instance;

    private Socket socket;
    private ObjectOutputStream out; // CAMBIO: Usamos ObjectOutput
    private ObjectInputStream in;   // CAMBIO: Usamos ObjectInput

    private LoginFrame loginFrame;
    private ChatFrame chatFrame;

    private String myUsername;
    private boolean isConnected = false;

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

        // Iniciar hilo de escucha
        new Thread(this::listen).start();
    }

    public void setLoginFrame(LoginFrame frame) { this.loginFrame = frame; }
    public void setChatFrame(ChatFrame frame) { this.chatFrame = frame; }

    public void setMyUsername(String user) { this.myUsername = user; }
    public String getMyUsername() { return myUsername; }
    public boolean isOfflineMode() { return !isConnected; }

    /**
     * Envía un objeto Message al servidor
     */
    public void send(Message msg) {
        if (!isConnected) return;
        try {
            out.writeObject(msg);
            out.flush();
            System.out.println("📤 Enviado: " + msg.getType());
        } catch (IOException e) {
            System.err.println("❌ Error enviando mensaje: " + e.getMessage());
            closeConnection();
        }
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
                // Alguien que no tenemos como contacto nos ha escrito
                if (chatFrame != null) {
                    chatFrame.onNuevoChatDesconocido(msg.getSenderId(), msg.getSenderName());
                }
                break;

            case LIST_CONTACTS:
                if (chatFrame != null) {
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

            case ADD_CONTACT_OK:
                // Contacto añadido exitosamente
                System.out.println("✅ Contacto añadido correctamente");
                break;

            case ADD_CONTACT_FAIL:
                JOptionPane.showMessageDialog(chatFrame, "No se pudo añadir el contacto: " + msg.getContent());
                break;

            default:
                System.out.println("❓ Tipo desconocido: " + msg.getType());
        }
    }

    private void closeConnection() {
        isConnected = false;
        try { if (socket != null) socket.close(); } catch (IOException e) {}
        JOptionPane.showMessageDialog(null, "Conexión perdida con el servidor.");
        System.exit(0);
    }
}