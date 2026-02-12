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

    GET_HISTORY,
    HISTORY_RESPONSE,
    ADD_CONTACT,        // Cliente pide añadir a alguien
    ADD_CONTACT_OK,     // Servidor dice "Hecho"
    ADD_CONTACT_FAIL,   // Servidor dice "No existe ese usuario"

    // --- BÚSQUEDA DE USUARIOS ---
    SEARCH_USER,        // Buscar usuario por nombre
    SEARCH_USER_RESULT  // Resultado de búsqueda
}