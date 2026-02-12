CREATE DATABASE IF NOT EXISTS jatsapp_db;

USE jatsapp_db;

-- 1. TABLA DE USUARIOS
-- Contiene login, seguridad (Pass + 2FA) y estado.
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario INTEGER PRIMARY KEY AUTO_INCREMENT,
    nombre_usuario TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE NOT NULL,       -- Necesario para enviar el código 2FA
    password_hash TEXT NOT NULL,      -- Contraseña cifrada (SHA-256)

    -- Campos para el 2FA
    codigo_2fa TEXT DEFAULT NULL,
    fecha_expiracion_codigo INTEGER DEFAULT NULL, -- Guardamos milisegundos (LONG)

    -- Verificación de email
    email_verificado BOOLEAN DEFAULT FALSE,  -- TRUE cuando el usuario verifica su email

    -- Campos de estado e información
    actividad TEXT CHECK(actividad IN ('activo', 'desconectado')) DEFAULT 'desconectado',
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso DATETIME
);

-- 2. TABLA DE GRUPOS
-- Define el grupo y quién es el "Jefe" (Admin).
CREATE TABLE IF NOT EXISTS grupos (
    id_grupo INTEGER PRIMARY KEY AUTO_INCREMENT,
    nombre_grupo TEXT NOT NULL,
    id_admin INTEGER NOT NULL,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_admin) REFERENCES usuarios(id_usuario)
);

-- 3. TABLA DE MIEMBROS DE GRUPO
-- Relaciona usuarios con grupos (Tabla N:M).
CREATE TABLE IF NOT EXISTS miembros_grupo (
    id_grupo INTEGER NOT NULL,
    id_usuario INTEGER NOT NULL,
    fecha_union DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_grupo, id_usuario), -- Clave compuesta para no repetir miembros
    FOREIGN KEY (id_grupo) REFERENCES grupos(id_grupo),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- 4. TABLA DE CONTACTOS
-- Agenda personal de cada usuario.
CREATE TABLE IF NOT EXISTS contactos (
    id_propietario INTEGER NOT NULL,  -- El usuario que "tiene" el contacto
    id_contacto INTEGER NOT NULL,     -- El amigo agregado
    fecha_agregado DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_propietario, id_contacto),
    FOREIGN KEY (id_propietario) REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_contacto) REFERENCES usuarios(id_usuario)
);

-- 5. TABLA DE MENSAJES (UNIFICADA)
-- Guarda tanto chats privados como grupales y soporta archivos.
CREATE TABLE IF NOT EXISTS mensajes (
    id_mensaje INTEGER PRIMARY KEY AUTO_INCREMENT,

    -- QUIÉN
    id_emisor INTEGER NOT NULL,
    id_destinatario INTEGER NOT NULL, -- Puede ser un ID de Usuario O un ID de Grupo

    -- DÓNDE (Define si id_destinatario es un User o un Grupo)
    tipo_destinatario TEXT NOT NULL CHECK(tipo_destinatario IN ('USUARIO', 'GRUPO')),

    -- QUÉ (Texto o Fichero)
    tipo_contenido TEXT NOT NULL CHECK(tipo_contenido IN ('TEXTO', 'IMAGEN', 'ARCHIVO')),
    contenido TEXT,       -- El mensaje escrito (si es TEXTO)
    ruta_fichero TEXT,    -- La ruta "server_files/foto.jpg" (si es ARCHIVO/IMAGEN)
    nombre_fichero TEXT,  -- El nombre original "vacaciones.jpg"

    -- CUÁNDO
    fecha_envio DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (id_emisor) REFERENCES usuarios(id_usuario)
);