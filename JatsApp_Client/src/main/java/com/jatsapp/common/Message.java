package com.jatsapp.common;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;

    // IDs para la Base de Datos
    private int senderId;
    private int receiverId; // Puede ser ID de Usuario o ID de Grupo

    // Nombres para mostrar en el Cliente (UI)
    private String senderName;
    private String receiverName;

    private boolean isGroupChat;

    // Contenido
    private String content;

    // Archivos
    private String fileName;
    private byte[] fileData;
    private String serverFilePath; // Ruta donde se guardó en el servidor (para BD)

    public Message() {}

    // Constructor rápido
    public Message(MessageType type, String content) {
        this.type = type;
        this.content = content;
    }

    // Getters y Setters
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public int getReceiverId() { return receiverId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public boolean isGroupChat() { return isGroupChat; }
    public void setGroupChat(boolean groupChat) { isGroupChat = groupChat; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }

    public String getServerFilePath() { return serverFilePath; }
    public void setServerFilePath(String serverFilePath) { this.serverFilePath = serverFilePath; }

}