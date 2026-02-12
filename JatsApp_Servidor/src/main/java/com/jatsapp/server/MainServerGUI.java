package com.jatsapp.server;

import com.jatsapp.server.ui.ServerGUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Punto de entrada principal del servidor con interfaz gráfica
 */
public class MainServerGUI {

    private static final Logger logger = LoggerFactory.getLogger(MainServerGUI.class);

    public static void main(String[] args) {
        logger.info("Iniciando JatsApp Server con interfaz gráfica...");

        // Configurar Look and Feel del sistema para mejor apariencia
        try {
            // Intentar usar el Look and Feel del sistema operativo
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            logger.debug("Look and Feel configurado: {}", UIManager.getLookAndFeel().getName());
        } catch (Exception e) {
            logger.warn("No se pudo configurar Look and Feel del sistema, usando por defecto", e);
        }

        // Configurar propiedades de renderizado para mejor calidad visual
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Crear y mostrar GUI en el Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                ServerGUI gui = new ServerGUI();
                gui.setVisible(true);
                logger.info("Interfaz gráfica del servidor iniciada correctamente");
            } catch (Exception e) {
                logger.error("Error al iniciar interfaz gráfica", e);
                JOptionPane.showMessageDialog(null,
                    "Error al iniciar la interfaz gráfica:\n" + e.getMessage(),
                    "Error Fatal",
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}

