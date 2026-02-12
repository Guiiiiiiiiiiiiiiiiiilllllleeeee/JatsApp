package com.jatsapp.server; 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainServer {

    private static final Logger logger = LoggerFactory.getLogger(MainServer.class);
    private static final Logger activityLogger = LoggerFactory.getLogger("com.jatsapp.server.activity");

    public static void main(String[] args) {
        logger.info("===========================================");
        logger.info("    JatsApp Server - Iniciando");
        logger.info("===========================================");
        activityLogger.info("SERVIDOR INICIADO");

        ServerCore core = new ServerCore();

        Thread serverThread = new Thread(core::startServer, "ServerCore-Thread");
        serverThread.start();

        // Añadir shutdown hook para parar el servidor limpiamente
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.warn("Shutdown hook recibido. Parando servidor...");
            activityLogger.info("SERVIDOR DETENIENDO - Shutdown hook");
            core.stopServer();
            try {
                serverThread.join(3000);
            } catch (InterruptedException e) {
                logger.error("Error esperando shutdown del servidor", e);
            }
            activityLogger.info("SERVIDOR DETENIDO");
            logger.info("Servidor terminado correctamente");
        }));

        // Opcional: permitir comando 'exit' por consola para detener
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            logger.info("Servidor listo. Escribe 'exit' y pulsa Enter para detener.");
            System.out.println("\n>> Comandos disponibles: 'exit', 'status', 'help'");

            while ((line = reader.readLine()) != null) {
                String command = line.trim().toLowerCase();

                if ("exit".equals(command)) {
                    logger.info("Comando EXIT recibido desde consola");
                    activityLogger.info("SERVIDOR DETENIENDO - Comando manual");
                    core.stopServer();
                    break;
                } else if ("status".equals(command)) {
                    logger.info("Estado del servidor: ACTIVO - {} clientes conectados",
                               core.getConnectedClientsCount());
                } else if ("help".equals(command)) {
                    System.out.println("\nComandos disponibles:");
                    System.out.println("  exit   - Detener el servidor");
                    System.out.println("  status - Ver estado del servidor");
                    System.out.println("  help   - Mostrar esta ayuda");
                } else if (!command.isEmpty()) {
                    logger.warn("Comando desconocido: '{}'. Escribe 'help' para ver comandos.", command);
                }
            }
        } catch (Exception e) {
            logger.error("Error leyendo comandos de consola", e);
        }

        logger.info("Proceso principal terminado");
    }
}
