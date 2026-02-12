-- Script de migración para añadir estado de mensajes (lectura y entrega)
-- Ejecutar si la base de datos ya existe

USE jatsapp_db;

-- Añadir campos de estado de mensaje si no existen
ALTER TABLE mensajes ADD COLUMN IF NOT EXISTS entregado BOOLEAN DEFAULT FALSE;
ALTER TABLE mensajes ADD COLUMN IF NOT EXISTS fecha_entrega DATETIME DEFAULT NULL;
ALTER TABLE mensajes ADD COLUMN IF NOT EXISTS leido BOOLEAN DEFAULT FALSE;
ALTER TABLE mensajes ADD COLUMN IF NOT EXISTS fecha_lectura DATETIME DEFAULT NULL;

-- Marcar mensajes existentes como entregados y leídos (para no mostrar notificaciones antiguas)
UPDATE mensajes SET entregado = TRUE, fecha_entrega = fecha_envio WHERE entregado = FALSE;
UPDATE mensajes SET leido = TRUE, fecha_lectura = fecha_envio WHERE leido = FALSE;

-- Verificar que se aplicó correctamente
SELECT id_mensaje, id_emisor, id_destinatario, entregado, fecha_entrega, leido, fecha_lectura
FROM mensajes
ORDER BY id_mensaje DESC
LIMIT 10;

