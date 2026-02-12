package com.jatsapp.common;

public enum MessageType {
    // --- AUTENTICACIÓN ---
    LOGIN,              // Enviar usuario y contraseña
    LOGIN_OK,           // Login correcto
    LOGIN_FAIL,         // Login fallido

    REGISTER,           // Solicitud de registro
    REGISTER_OK,        // Registro exitoso
    REGISTER_FAIL,      // Fallo en registro (usuario duplicado, etc.)

    require_2FA,        // Servidor pide código
    VERIFY_2FA,         // Cliente envía código
    VERIFY_2FA_OK,      // (Opcional, si lo usas en el ClientSocket)
    VERIFY_2FA_FAIL,    // (Opcional, si lo usas en el ClientSocket)

    DISCONNECT,         // Cliente notifica que se desconecta
    STATUS_UPDATE,      // Notificación de cambio de estado de un usuario (conectado/desconectado)

    // --- CHAT ---
    TEXT_MESSAGE,
    FILE_MESSAGE,
    ARCHIVO,
    IMAGEN,

    // --- SOLICITUDES DE CHAT (usuarios desconocidos) ---
    NEW_CHAT_REQUEST,   // Notifica al receptor que alguien nuevo le escribió
    ACCEPT_CHAT,        // El receptor acepta el chat (añade contacto automáticamente)
    REJECT_CHAT,        // El receptor rechaza el chat

    // --- CONFIRMACIONES DE LECTURA ---
    MESSAGE_DELIVERED,  // Confirma que el mensaje fue entregado al receptor
    MESSAGE_READ,       // Confirma que el mensaje fue leído por el receptor
    UPDATE_MESSAGE_STATUS, // Actualiza el estado de un mensaje (para el emisor)

    // --- DATOS ---
    GET_CONTACTS,
    LIST_CONTACTS,
    GET_RELEVANT_CHATS, // Obtener chats relevantes (contactos + usuarios con mensajes)

    GET_HISTORY,
    HISTORY_RESPONSE,
    ADD_CONTACT,        // Cliente pide añadir a alguien
    ADD_CONTACT_OK,     // Servidor dice "Hecho"
    ADD_CONTACT_FAIL,   // Servidor dice "No existe ese usuario"

    // --- BÚSQUEDA DE USUARIOS ---
    SEARCH_USER,        // Buscar usuario por nombre
    SEARCH_USER_RESULT, // Resultado de búsqueda

    // --- BÚSQUEDA GLOBAL DE MENSAJES ---
    SEARCH_MESSAGES,        // Buscar mensajes en todos los chats
    SEARCH_MESSAGES_RESULT, // Resultado de búsqueda de mensajes

    // --- GESTIÓN DE CONTACTOS ---
    REMOVE_CONTACT,     // Eliminar contacto
    DELETE_CONTACT,     // Borrar contacto (alternativo)

    // --- DESCARGA DE ARCHIVOS ---
    DOWNLOAD_FILE,      // Cliente solicita descargar un archivo por messageId
    FILE_DOWNLOAD_RESPONSE, // Servidor envía los bytes del archivo

    ERROR               // Error genérico
}