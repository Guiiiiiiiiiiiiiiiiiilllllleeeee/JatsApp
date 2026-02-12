package com.jatsapp.common;

public enum MessageType {
    LOGIN,              // Enviar usuario y contraseña
    REGISTER,           // Registrarse
    LOGIN_OK,           // Servidor dice: Entraste
    LOGIN_FAIL,         // Servidor dice: Contraseña mal
    require_2FA,        // Servidor dice: Dame el código del email
    VERIFY_2FA,         // Cliente envía el código

    TEXT_MESSAGE,       // Chat normal
    FILE_MESSAGE,       // Envío de imagen/archivo
    ARCHIVO,            // Alias para compatibilidad con BD
    IMAGEN,             // Tipo específico de archivo imagen

    GET_CONTACTS,       // Pedir lista de amigos
    LIST_CONTACTS,      // Respuesta con la lista

    GET_HISTORY,        // Pedir mensajes antiguos
    HISTORY_RESPONSE    // Respuesta con la lista de mensajes
}