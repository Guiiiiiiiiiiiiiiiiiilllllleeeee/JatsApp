-- Script para migrar la base de datos a UTF8MB4 (soporte de emojis)
-- Ejecutar este script en MySQL para habilitar emojis

-- 1. Cambiar la base de datos a utf8mb4
ALTER DATABASE jatsapp_db CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 2. Cambiar la tabla de mensajes a utf8mb4
ALTER TABLE mensajes CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3. Cambiar específicamente la columna de contenido a utf8mb4
ALTER TABLE mensajes MODIFY contenido TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 4. Cambiar la tabla de usuarios (por si acaso nombres con emojis)
ALTER TABLE usuarios CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 5. Cambiar la tabla de grupos (por si acaso nombres con emojis)
ALTER TABLE grupos CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE grupos MODIFY nombre_grupo VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Verificar que todo esté correcto
SHOW VARIABLES LIKE 'character_set%';
SHOW VARIABLES LIKE 'collation%';

