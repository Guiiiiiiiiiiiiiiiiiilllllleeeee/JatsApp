package com.jatsapp.common;

import java.io.Serializable;

public class Message implements Serializable {
    private MessageType type;

    // Datos de usuario
    private String sender;          // Quién envía
    private String receiver;        // A quién (Usuario o Grupo)
    private boolean isGroupChat;    // ¿Es para un grupo?

    // Contenido
    private String content;         // Texto del mensaje, Contraseña hash, o Código 2FA

    // Datos de Ficheros (Opcional)
    private String fileName;
    private byte[] fileData;        // Bytes de la imagen (solo se llena si type == FILE_MESSAGE)

    // Constructor vacío (necesario para Gson)
    public Message() {}

    // Constructor rápido para mensajes de texto
    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
    }

    // Getters y Setters (OBLIGATORIOS para que Gson funcione)
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public boolean isGroupChat() { return isGroupChat; }
    public void setGroupChat(boolean groupChat) { isGroupChat = groupChat; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }
}