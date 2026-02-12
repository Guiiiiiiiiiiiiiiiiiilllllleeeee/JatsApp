package com.jatsapp.common;

import java.io.*;
import java.time.LocalDateTime;
import java.util.List;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private MessageType type;

    // --- Identificadores ---
    private int senderId;
    private String senderName; // Útil para mostrar quién envía sin hacer consultas extra
    private int receiverId;    // ID del Usuario destino o ID del Grupo

    private boolean isGroupChat; // true = receiverId es un Grupo; false = es un Usuario

    // --- Contenido ---
    private String content;           // Texto del mensaje, contraseña, o código 2FA
    private transient LocalDateTime timestamp;  // Fecha y hora (transient para serialización personalizada)

    // --- Archivos (Para FILE_MESSAGE) ---
    private String fileName;
    private byte[] fileData;       // Los bytes del archivo (solo al subir/descargar)
    private String serverFilePath; // Ruta interna en el servidor

    // --- Listas (Para respuestas del servidor) ---
    private List<User> contactList;      // Para responder a GET_CONTACTS
    private List<Message> historyList;   // Para responder a GET_HISTORY

    // --- Estado del mensaje (confirmaciones de lectura) ---
    private int messageId;               // ID del mensaje en la base de datos
    private boolean delivered;           // true = mensaje entregado al receptor
    private boolean read;                // true = mensaje leído por el receptor
    private LocalDateTime deliveredTime; // Cuándo se entregó
    private LocalDateTime readTime;      // Cuándo se leyó

    // Constructor vacío
    public Message() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor rápido para mensajes simples
    public Message(MessageType type, String content) {
        this.type = type;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    // Serialización personalizada para LocalDateTime
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(timestamp != null ? timestamp.toString() : null);
        out.writeObject(deliveredTime != null ? deliveredTime.toString() : null);
        out.writeObject(readTime != null ? readTime.toString() : null);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        String timestampStr = (String) in.readObject();
        timestamp = timestampStr != null ? LocalDateTime.parse(timestampStr) : null;
        String deliveredStr = (String) in.readObject();
        deliveredTime = deliveredStr != null ? LocalDateTime.parse(deliveredStr) : null;
        String readStr = (String) in.readObject();
        readTime = readStr != null ? LocalDateTime.parse(readStr) : null;
    }

    // --- Getters y Setters ---

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public int getReceiverId() { return receiverId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }

    public boolean isGroupChat() { return isGroupChat; }
    public void setGroupChat(boolean groupChat) { isGroupChat = groupChat; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }

    public String getServerFilePath() { return serverFilePath; }
    public void setServerFilePath(String serverFilePath) { this.serverFilePath = serverFilePath; }

    public List<User> getContactList() { return contactList; }
    public void setContactList(List<User> contactList) { this.contactList = contactList; }

    public List<Message> getHistoryList() { return historyList; }
    public void setHistoryList(List<Message> historyList) { this.historyList = historyList; }

    public int getMessageId() { return messageId; }
    public void setMessageId(int messageId) { this.messageId = messageId; }

    public boolean isDelivered() { return delivered; }
    public void setDelivered(boolean delivered) { this.delivered = delivered; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getDeliveredTime() { return deliveredTime; }
    public void setDeliveredTime(LocalDateTime deliveredTime) { this.deliveredTime = deliveredTime; }

    public LocalDateTime getReadTime() { return readTime; }
    public void setReadTime(LocalDateTime readTime) { this.readTime = readTime; }
}