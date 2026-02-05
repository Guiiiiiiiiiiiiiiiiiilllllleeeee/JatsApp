package com.jatsapp.client;

import com.formdev.flatlaf.themes.FlatMacDarkLaf; // Usamos el tema Mac (más limpio)
import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.client.view.LoginFrame;

import javax.swing.*;
import java.awt.*;

public class MainClient {

    public static void main(String[] args) {

        // Configuración de Estilo Global
        try {
            // 1. Color de acento (Ese verde bonito para focus, bordes y selecciones)
            UIManager.put("Component.accentColor", new Color(0, 200, 150));

            // 2. Redondeo EXTREMO (Estilo píldora)
            UIManager.put("Button.arc", 999);       // Botones redondos
            UIManager.put("Component.arc", 999);    // Inputs redondos
            UIManager.put("TextComponent.arc", 999);

            // 3. Grosor de bordes y foco
            UIManager.put("Component.focusWidth", 2);
            UIManager.put("Component.innerFocusWidth", 1);

            // 4. Scrollbars más finas e invisibles si no se usan
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("ScrollBar.thumbArc", 999);

            // Cargar el tema
            FlatMacDarkLaf.setup();

        } catch (Exception ex) {
            System.err.println("No se pudo cargar FlatLaf");
        }

        // Arrancar
        SwingUtilities.invokeLater(() -> {
            System.out.println("🚀 Iniciando JatsApp (Diseño Premium)...");
            ClientSocket.getInstance().connect("localhost", 8888);
            new LoginFrame();
        });
    }
}