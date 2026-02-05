package com.jatsapp.client;

import com.formdev.flatlaf.FlatDarkLaf; // IMPORTANTE: Importar esto
import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.client.view.LoginFrame;

import javax.swing.*;
import java.awt.*;

public class MainClient {
    public static void main(String[] args) {

        // 1. ACTIVAR EL DISEÑO MODERNO (Antes de crear ninguna ventana)
        try {
            // Puedes cambiar FlatDarkLaf por FlatLightLaf si prefieres modo claro
            FlatDarkLaf.setup();

            // Opcional: Personalizar el color de acento (el verde WhatsApp)
            UIManager.put("Button.arc", 10); // Bordes redondeados en botones
            UIManager.put("Component.arc", 10); // Bordes redondeados en inputs
            UIManager.put("ProgressBar.arc", 10);

        } catch (Exception ex) {
            System.err.println("No se pudo cargar el tema visual");
        }

        // 2. Arrancar la App
        SwingUtilities.invokeLater(() -> {
            System.out.println("🚀 Iniciando JatsApp Cliente (Estilo Moderno)...");
            ClientSocket.getInstance().connect("localhost", 8888);
            new LoginFrame();
        });
    }
}