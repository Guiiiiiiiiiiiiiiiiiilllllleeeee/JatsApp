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

    // --- GRUPOS ---
    CREATE_GROUP,       // Cliente solicita crear un grupo
    CREATE_GROUP_OK,    // Grupo creado exitosamente
    CREATE_GROUP_FAIL,  // Error al crear grupo

    GET_GROUPS,         // Solicitar lista de grupos del usuario
    LIST_GROUPS,        // Respuesta con lista de grupos

    ADD_GROUP_MEMBER,   // Añadir miembro a un grupo (solo admin)
    ADD_GROUP_MEMBER_OK,
    ADD_GROUP_MEMBER_FAIL,

    REMOVE_GROUP_MEMBER, // Eliminar miembro de un grupo (solo admin)
    REMOVE_GROUP_MEMBER_OK,
    REMOVE_GROUP_MEMBER_FAIL,

    PROMOTE_TO_ADMIN,        // Promover miembro a admin
    PROMOTE_TO_ADMIN_OK,
    PROMOTE_TO_ADMIN_FAIL,

    DEMOTE_FROM_ADMIN,       // Quitar rol de admin a un miembro
    DEMOTE_FROM_ADMIN_OK,
    DEMOTE_FROM_ADMIN_FAIL,

    LEAVE_GROUP,        // Usuario abandona un grupo
    LEAVE_GROUP_OK,
    LEAVE_GROUP_FAIL,

    GET_GROUP_MEMBERS,  // Solicitar lista de miembros de un grupo
    LIST_GROUP_MEMBERS, // Respuesta con lista de miembros

    GET_GROUP_INFO,     // Solicitar información de un grupo
    GROUP_INFO_RESPONSE, // Respuesta con información del grupo

    GROUP_NOTIFICATION, // Notificación de cambios en el grupo (nuevo miembro, etc.)

    ERROR               // Error genérico
}