package com.jatsapp.client;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.jatsapp.client.network.ClientSocket; // Asumo que esta clase existe, pásamela luego
import com.jatsapp.client.view.LoginFrame;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;
import com.jatsapp.client.view.ChatFrame;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class MainClient {

    public static void main(String[] args) {

        // --- Configuración de Estilo (FlatLaf) ---
        try {
            UIManager.put("Component.accentColor", new Color(0, 200, 150));
            UIManager.put("Button.arc", 999);
            UIManager.put("Component.arc", 999);
            UIManager.put("TextComponent.arc", 999);
            UIManager.put("Component.focusWidth", 2);
            UIManager.put("ScrollBar.width", 10);

            FlatMacDarkLaf.setup();
        } catch (Exception ex) {
            System.err.println("Advertencia: No se pudo cargar el tema FlatLaf. Se usará el por defecto.");
        }

        // --- Arranque de la UI y Red ---
        SwingUtilities.invokeLater(() -> {
            System.out.println("🚀 Iniciando JatsApp Client...");

            try {
                // CORRECCIÓN: Puerto cambiado a 5555 (el mismo que ServerCore)
                ClientSocket.getInstance().connect("127.0.0.1", 5555);

                // Abrir pantalla de Login
                new LoginFrame();

            } catch (IOException e) {
                // Si el servidor está apagado, mostramos aviso y cerramos
                JOptionPane.showMessageDialog(null,
                        "No se pudo conectar al servidor en 172.19.16.12:5555.\n¿Está encendido?",
                        "Error de Conexión",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}