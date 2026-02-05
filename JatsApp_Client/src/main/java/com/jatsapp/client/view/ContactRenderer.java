package com.jatsapp.client.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ContactRenderer extends JPanel implements ListCellRenderer<String> {

    private String nombreActual;
    private boolean seleccionado;

    public ContactRenderer() {
        setLayout(new BorderLayout(15, 0)); // Más espacio entre icono y texto
        setBorder(new EmptyBorder(10, 15, 10, 15)); // Padding generoso
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
        this.nombreActual = value;
        this.seleccionado = isSelected;

        // Colores de fondo de la celda
        if (isSelected) {
            setBackground(new Color(45, 55, 65)); // Gris azulado al seleccionar
        } else {
            setBackground(new Color(30, 30, 30)); // Fondo normal
        }

        // El texto lo pintaremos manualmente en paintComponent para más control
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        // No llamamos a super.paintComponent para tener control total del fondo
        Graphics2D g2 = (Graphics2D) g.create();

        // Activar antialiasing para bordes suaves
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 0. PINTAR FONDO (Manual para evitar glitches)
        if (seleccionado) {
            g2.setColor(new Color(45, 55, 65)); // Gris azulado (Seleccionado)
        } else {
            g2.setColor(new Color(30, 30, 30)); // Fondo normal
        }
        g2.fillRect(0, 0, getWidth(), getHeight());

        // 1. DIBUJAR AVATAR (CÍRCULO)
        int size = 45; // Un poco más grande
        int xAvatar = 15; // Margen izquierdo
        int yPos = (getHeight() - size) / 2; // Centrado vertical

        g2.setColor(getColorPorNombre(nombreActual));
        g2.fillOval(xAvatar, yPos, size, size);

        // Letra inicial
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        String inicial = nombreActual.length() > 0 ? nombreActual.substring(0, 1).toUpperCase() : "?";
        FontMetrics fm = g2.getFontMetrics();

        // Centrar letra matemáticamente en el círculo
        int textX = xAvatar + (size - fm.stringWidth(inicial)) / 2;
        int textY = yPos + ((size - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(inicial, textX, textY);

        // ---------------------------------------------------------
        // AQUÍ ESTABA EL PROBLEMA: MOVER TEXTO A LA DERECHA (X=80)
        // ---------------------------------------------------------
        int xTexto = 80;

        // 2. DIBUJAR NOMBRE
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        g2.drawString(nombreActual, xTexto, getHeight() / 2 - 5);

        // 3. DIBUJAR ESTADO
        g2.setColor(new Color(170, 170, 170)); // Gris claro
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString("Haz clic para chatear", xTexto, getHeight() / 2 + 15);

        g2.dispose();
    }

    // Genera un color pastel bonito basado en el nombre
    private Color getColorPorNombre(String nombre) {
        int hash = nombre.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;
        return new Color((r + 50) % 200, (g + 50) % 200, (b + 100) % 255);
    }
}