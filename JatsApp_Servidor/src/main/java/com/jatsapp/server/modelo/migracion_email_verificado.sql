-- Script de migración para añadir verificación de email
-- Ejecutar si la base de datos ya existe

USE jatsapp_db;

-- Añadir columna email_verificado si no existe
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email_verificado BOOLEAN DEFAULT FALSE;

-- Marcar usuarios existentes como verificados (para no bloquearlos)
UPDATE usuarios SET email_verificado = TRUE WHERE email_verificado IS NULL OR email_verificado = FALSE;

-- Verificar que se aplicó correctamente
SELECT id_usuario, nombre_usuario, email, email_verificado FROM usuarios;

