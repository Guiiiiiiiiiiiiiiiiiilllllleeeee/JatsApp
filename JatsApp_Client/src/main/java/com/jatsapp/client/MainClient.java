package com.jatsapp.client;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.client.view.LoginFrame;

import javax.swing.*;

public class MainClient {

    public static void main(String[] args) {
        // En Swing (ventanas), siempre se recomienda iniciar la interfaz en este hilo especial
        SwingUtilities.invokeLater(() -> {

            System.out.println("🚀 Iniciando JatsApp Cliente...");

            // 1. Intentamos conectar primero
            // Si el servidor no está, tu ClientSocket activará el "Modo Offline" automáticamente.
            ClientSocket.getInstance().connect("localhost", 8888);

            // 2. Abrimos la ventana de Login
            new LoginFrame();
        });
    }
}