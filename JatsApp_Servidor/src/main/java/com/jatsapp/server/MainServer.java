package com.jatsapp.server; 

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainServer {

    public static void main(String[] args) {
        System.out.println("Arrancando JatsApp Server...");

        ServerCore core = new ServerCore();

        Thread serverThread = new Thread(core::startServer, "ServerCore-Thread");
        serverThread.start();

        // Añadir shutdown hook para parar el servidor limpiamente
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook recibido. Parando servidor...");
            core.stopServer();
            try {
                serverThread.join(3000);
            } catch (InterruptedException e) {
                // ignore
            }
        }));

        // Opcional: permitir comando 'exit' por consola para detener
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            System.out.println("Escribe 'exit' y pulsa Enter para detener el servidor.");
            while ((line = reader.readLine()) != null) {
                if ("exit".equalsIgnoreCase(line.trim())) {
                    System.out.println("Comando exit recibido.");
                    core.stopServer();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Servidor terminado.");
    }
}
