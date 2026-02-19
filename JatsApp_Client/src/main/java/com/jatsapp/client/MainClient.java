package com.jatsapp.client;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.client.view.LoginFrame;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Properties;

public class MainClient {

    private static String serverIp = "127.0.0.1";
    private static int serverPort = 5555;

    public static void main(String[] args) {

        // Cargar configuración de conexión
        loadConfig();

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
            System.out.println("📡 Conectando a: " + serverIp + ":" + serverPort);

            try {
                ClientSocket.getInstance().connect(serverIp, serverPort);

                // Abrir pantalla de Login
                new LoginFrame();

            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        "No se pudo conectar al servidor en " + serverIp + ":" + serverPort + "\n¿Está encendido?",
                        "Error de Conexión",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    private static void loadConfig() {
        Properties props = new Properties();

        // 1. Intentar cargar desde archivo externo (junto al JAR)
        File externalConfig = new File("config.properties");
        if (externalConfig.exists()) {
            try (FileInputStream fis = new FileInputStream(externalConfig)) {
                props.load(fis);
                System.out.println("✓ Configuración cargada desde: " + externalConfig.getAbsolutePath());
            } catch (Exception e) {
                System.err.println("Error cargando config externo: " + e.getMessage());
            }
        } else {
            // 2. Cargar desde recursos internos
            try (InputStream input = MainClient.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (input != null) {
                    props.load(input);
                    System.out.println("✓ Configuración cargada desde recursos internos");
                }
            } catch (Exception e) {
                System.err.println("Error cargando config interno: " + e.getMessage());
            }
        }

        // Leer valores (o usar defaults)
        serverIp = props.getProperty("server.ip", "127.0.0.1");
        serverPort = Integer.parseInt(props.getProperty("server.port", "5555"));
    }
}