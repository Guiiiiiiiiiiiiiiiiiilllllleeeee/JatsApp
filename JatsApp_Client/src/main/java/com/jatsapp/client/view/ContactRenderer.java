package com.jatsapp.client.view;

import com.jatsapp.common.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// CAMBIO: Ahora renderizamos objetos 'User', no 'String'
public class ContactRenderer extends JPanel implements ListCellRenderer<User> {

    private User usuarioActual;
    private boolean seleccionado;

    public ContactRenderer() {
        // Configuramos el layout y tamaño base
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setOpaque(false); // Importante para que paintComponent controle el fondo
        setPreferredSize(new Dimension(200, 70)); // Altura fija para cada fila
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends User> list, User value, int index, boolean isSelected, boolean cellHasFocus) {
        this.usuarioActual = value;
        this.seleccionado = isSelected;
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (usuarioActual == null) return;

        Graphics2D g2 = (Graphics2D) g.create();

        // Calidad de renderizado (Antialiasing)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. FONDO
        if (seleccionado) {
            g2.setColor(new Color(45, 55, 65)); // Seleccionado
        } else {
            g2.setColor(new Color(30, 30, 30)); // Normal
        }
        g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 10, 10); // Bordes redondeados

        // 2. AVATAR (Círculo con inicial)
        int size = 45;
        int xAvatar = 15;
        int yPos = (getHeight() - size) / 2;

        g2.setColor(getColorPorNombre(usuarioActual.getUsername()));
        g2.fillOval(xAvatar, yPos, size, size);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        String inicial = usuarioActual.getUsername().substring(0, 1).toUpperCase();

        FontMetrics fm = g2.getFontMetrics();
        int textX = xAvatar + (size - fm.stringWidth(inicial)) / 2;
        int textY = yPos + ((size - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(inicial, textX, textY - 4);

        // 3. NOMBRE
        int xTexto = 75;
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        g2.drawString(usuarioActual.getUsername(), xTexto, getHeight() / 2 - 2);

        // 4. ESTADO (Conectado / Desconectado)
        // Usamos el campo activityStatus del objeto User
        String estado = (usuarioActual.getActivityStatus() != null) ? usuarioActual.getActivityStatus() : "desconocido";

        if ("activo".equalsIgnoreCase(estado)) {
            g2.setColor(new Color(0, 200, 100)); // Verde
        } else {
            g2.setColor(Color.GRAY);
        }
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString(estado, xTexto, getHeight() / 2 + 15);

        g2.dispose();
    }

    private Color getColorPorNombre(String nombre) {
        if (nombre == null) return Color.GRAY;
        int hash = nombre.hashCode();
        return new Color((hash & 0xFF0000) >> 16, (hash & 0x00FF00) >> 8, hash & 0x0000FF).brighter();
    }
}