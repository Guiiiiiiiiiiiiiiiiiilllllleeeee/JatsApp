package com.jatsapp.client.network;

import com.jatsapp.client.view.ChatFrame;
import com.jatsapp.client.view.LoginFrame;
import com.jatsapp.client.view.ContactsFrame;
import com.jatsapp.client.view.GroupsFrame;
import com.jatsapp.common.Group;
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
    private GroupsFrame groupsFrame;

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
    public ChatFrame getChatFrame() { return this.chatFrame; }
    public void setContactsFrame(ContactsFrame frame) { this.contactsFrame = frame; }
    public void setGroupsFrame(GroupsFrame frame) { this.groupsFrame = frame; }

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
                System.out.println("📥 ClientSocket recibió mensaje: tipo=" + msg.getType() +
                                 ", senderId=" + msg.getSenderId() +
                                 ", receiverId=" + msg.getReceiverId() +
                                 ", isGroupChat=" + msg.isGroupChat());
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
                    // El mensaje incluye receiverId y isGroupChat para verificar
                    chatFrame.cargarHistorial(msg.getHistoryList(), msg.getReceiverId(), msg.isGroupChat());
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

            // ========================================
            // --- GRUPOS ---
            // ========================================

            case LIST_GROUPS:
                // Lista de grupos del usuario
                if (groupsFrame != null) {
                    groupsFrame.actualizarGrupos(msg.getGroupList());
                }
                if (chatFrame != null) {
                    chatFrame.actualizarGrupos(msg.getGroupList());
                }
                break;

            case CREATE_GROUP_OK:
                System.out.println("✅ Grupo creado: " + msg.getGroup().getNombre());
                JOptionPane.showMessageDialog(groupsFrame,
                    "Grupo '" + msg.getGroup().getNombre() + "' creado exitosamente",
                    "Grupo Creado", JOptionPane.INFORMATION_MESSAGE);
                // Recargar lista de grupos
                send(new Message(MessageType.GET_GROUPS, ""));
                break;

            case CREATE_GROUP_FAIL:
                JOptionPane.showMessageDialog(groupsFrame,
                    "Error al crear grupo: " + msg.getContent(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                break;

            case ADD_GROUP_MEMBER_OK:
                System.out.println("✅ Miembro añadido al grupo");
                if (groupsFrame != null && msg.getGroup() != null) {
                    groupsFrame.actualizarInfoGrupo(msg.getGroup());
                }
                // También actualizar el ChatFrame si tiene el grupo abierto
                if (chatFrame != null && msg.getGroup() != null) {
                    chatFrame.actualizarInfoGrupoActual(msg.getGroup());
                }
                break;

            case ADD_GROUP_MEMBER_FAIL:
                JOptionPane.showMessageDialog(groupsFrame,
                    "Error al añadir miembro: " + msg.getContent(),
                    "Error", JOptionPane.WARNING_MESSAGE);
                break;

            case REMOVE_GROUP_MEMBER_OK:
                System.out.println("✅ Miembro eliminado del grupo");
                if (groupsFrame != null && msg.getGroup() != null) {
                    groupsFrame.actualizarInfoGrupo(msg.getGroup());
                }
                // También actualizar el ChatFrame si tiene el grupo abierto
                if (chatFrame != null && msg.getGroup() != null) {
                    chatFrame.actualizarInfoGrupoActual(msg.getGroup());
                }
                break;

            case REMOVE_GROUP_MEMBER_FAIL:
                JOptionPane.showMessageDialog(chatFrame,
                    "Error al eliminar miembro: " + msg.getContent(),
                    "Error", JOptionPane.WARNING_MESSAGE);
                break;

            case PROMOTE_TO_ADMIN_OK:
                System.out.println("✅ Usuario promovido a admin");
                JOptionPane.showMessageDialog(chatFrame,
                    msg.getContent(),
                    "Admin Promovido", JOptionPane.INFORMATION_MESSAGE);
                if (groupsFrame != null && msg.getGroup() != null) {
                    groupsFrame.actualizarInfoGrupo(msg.getGroup());
                }
                if (chatFrame != null && msg.getGroup() != null) {
                    chatFrame.actualizarInfoGrupoActual(msg.getGroup());
                }
                break;

            case PROMOTE_TO_ADMIN_FAIL:
                JOptionPane.showMessageDialog(chatFrame,
                    "Error al promover a admin: " + msg.getContent(),
                    "Error", JOptionPane.WARNING_MESSAGE);
                break;

            case DEMOTE_FROM_ADMIN_OK:
                System.out.println("✅ Usuario degradado de admin");
                JOptionPane.showMessageDialog(chatFrame,
                    msg.getContent(),
                    "Admin Removido", JOptionPane.INFORMATION_MESSAGE);
                if (groupsFrame != null && msg.getGroup() != null) {
                    groupsFrame.actualizarInfoGrupo(msg.getGroup());
                }
                if (chatFrame != null && msg.getGroup() != null) {
                    chatFrame.actualizarInfoGrupoActual(msg.getGroup());
                }
                break;

            case DEMOTE_FROM_ADMIN_FAIL:
                JOptionPane.showMessageDialog(chatFrame,
                    "Error al quitar admin: " + msg.getContent(),
                    "Error", JOptionPane.WARNING_MESSAGE);
                break;

            case LEAVE_GROUP_OK:
                System.out.println("✅ Has abandonado el grupo");
                JOptionPane.showMessageDialog(groupsFrame,
                    "Has abandonado el grupo",
                    "Grupo Abandonado", JOptionPane.INFORMATION_MESSAGE);
                // Recargar lista de grupos
                send(new Message(MessageType.GET_GROUPS, ""));
                break;

            case LEAVE_GROUP_FAIL:
                JOptionPane.showMessageDialog(groupsFrame,
                    "Error al abandonar grupo: " + msg.getContent(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                break;

            case GROUP_INFO_RESPONSE:
                // Información detallada de un grupo
                if (groupsFrame != null && msg.getGroup() != null) {
                    groupsFrame.actualizarInfoGrupo(msg.getGroup());
                }
                // También actualizar el ChatFrame si tiene el grupo abierto
                if (chatFrame != null && msg.getGroup() != null) {
                    chatFrame.actualizarInfoGrupoActual(msg.getGroup());
                }
                break;

            case LIST_GROUP_MEMBERS:
                // Lista de miembros de un grupo (usado si necesitamos actualizar solo miembros)
                if (groupsFrame != null) {
                    // Se maneja a través de GROUP_INFO_RESPONSE normalmente
                }
                break;

            case GROUP_NOTIFICATION:
                // Notificación de cambios en un grupo (añadido, eliminado, etc.)
                System.out.println("📢 Notificación de grupo: " + msg.getContent());

                // Siempre recargar la lista de grupos para que aparezcan nuevos grupos
                send(new Message(MessageType.GET_GROUPS, ""));

                if (groupsFrame != null && groupsFrame.isVisible()) {
                    groupsFrame.mostrarNotificacion(msg.getContent(), msg.getGroup());
                } else {
                    // Si GroupsFrame no está abierto, mostrar notificación simple
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(chatFrame,
                            msg.getContent(),
                            "Notificación de Grupo", JOptionPane.INFORMATION_MESSAGE);
                    });
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