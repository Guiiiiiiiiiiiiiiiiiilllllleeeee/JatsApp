package com.jatsapp.common;

public enum MessageType {
    // --- AUTENTICACIÓN ---
    LOGIN,              // Enviar usuario y contraseña
    LOGIN_OK,           // Login correcto
    LOGIN_FAIL,         // Login fallido

    REGISTER,           // Solicitud de registro
    REGISTER_OK,        // <--- NUEVO: Registro exitoso
    REGISTER_FAIL,      // <--- NUEVO: Fallo en registro (usuario duplicado, etc.)

    require_2FA,        // Servidor pide código
    VERIFY_2FA,         // Cliente envía código
    VERIFY_2FA_OK,      // <--- NUEVO: (Opcional, si lo usas en el ClientSocket)
    VERIFY_2FA_FAIL,    // <--- NUEVO: (Opcional, si lo usas en el ClientSocket)

    // --- CHAT ---
    TEXT_MESSAGE,
    FILE_MESSAGE,
    ARCHIVO,
    IMAGEN,

    // --- DATOS ---
    GET_CONTACTS,
    LIST_CONTACTS,

    GET_HISTORY,
    HISTORY_RESPONSE
}