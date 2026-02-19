package com.jatsapp.client.view;

import com.jatsapp.client.util.StyleUtil;
import com.jatsapp.common.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Renderer moderno para la lista de contactos
 */
public class ContactRenderer extends JPanel implements ListCellRenderer<User> {

    private User usuarioActual;
    private boolean seleccionado;

    public ContactRenderer() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setOpaque(false);
        setPreferredSize(new Dimension(200, 72));
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
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        boolean esGrupo = "grupo".equals(usuarioActual.getActivityStatus());

        // 1. FONDO con transición suave
        if (seleccionado) {
            g2.setColor(StyleUtil.BG_SELECTED);
        } else {
            g2.setColor(StyleUtil.BG_MEDIUM);
        }
        g2.fillRoundRect(4, 2, getWidth() - 8, getHeight() - 4, 12, 12);

        // 2. AVATAR
        int size = 48;
        int xAvatar = 16;
        int yPos = (getHeight() - size) / 2;

        if (esGrupo) {
            // Avatar para grupos
            GradientPaint gp = new GradientPaint(xAvatar, yPos, StyleUtil.PRIMARY,
                    xAvatar + size, yPos + size, StyleUtil.PRIMARY_DARK);
            g2.setPaint(gp);
            g2.fillOval(xAvatar, yPos, size, size);

            // Letra G para grupos
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
            FontMetrics fm = g2.getFontMetrics();
            int textX = xAvatar + (size - fm.stringWidth("G")) / 2;
            int textY = yPos + ((size - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString("G", textX, textY);
        } else {
            // Avatar para usuarios con gradiente
            Color baseColor = getColorPorNombre(usuarioActual.getUsername());
            GradientPaint gp = new GradientPaint(xAvatar, yPos, baseColor,
                    xAvatar + size, yPos + size, StyleUtil.darken(baseColor, 0.2f));
            g2.setPaint(gp);
            g2.fillOval(xAvatar, yPos, size, size);

            // Inicial
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
            String inicial = usuarioActual.getUsername().substring(0, 1).toUpperCase();
            FontMetrics fm = g2.getFontMetrics();
            int textX = xAvatar + (size - fm.stringWidth(inicial)) / 2;
            int textY = yPos + ((size - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(inicial, textX, textY - 2);

            // Indicador de estado online/offline
            String estado = usuarioActual.getActivityStatus();
            if ("activo".equalsIgnoreCase(estado)) {
                g2.setColor(StyleUtil.SUCCESS);
                g2.fillOval(xAvatar + size - 14, yPos + size - 14, 14, 14);
                g2.setColor(StyleUtil.BG_MEDIUM);
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(xAvatar + size - 14, yPos + size - 14, 14, 14);
            }
        }

        // 3. NOMBRE
        int xTexto = 78;
        g2.setColor(StyleUtil.TEXT_PRIMARY);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 15));

        String nombreMostrar = usuarioActual.getUsername();
        g2.drawString(nombreMostrar, xTexto, getHeight() / 2 - 4);

        // 4. SUBTÍTULO (estado o tipo)
        String estado = (usuarioActual.getActivityStatus() != null) ? usuarioActual.getActivityStatus() : "desconocido";
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        if (esGrupo) {
            g2.setColor(StyleUtil.PRIMARY);
            g2.drawString("Grupo de chat", xTexto, getHeight() / 2 + 14);
        } else if ("activo".equalsIgnoreCase(estado)) {
            g2.setColor(StyleUtil.SUCCESS);
            g2.drawString("En línea", xTexto, getHeight() / 2 + 14);
        } else {
            g2.setColor(StyleUtil.TEXT_MUTED);
            g2.drawString("Desconectado", xTexto, getHeight() / 2 + 14);
        }

        g2.dispose();
    }

    /**
     * Genera un color consistente basado en el nombre del usuario
     */
    private Color getColorPorNombre(String nombre) {
        if (nombre == null || nombre.isEmpty()) return StyleUtil.ACCENT;

        // Colores modernos para avatares
        Color[] colores = {
                new Color(229, 115, 115), // Rojo suave
                new Color(186, 104, 200), // Púrpura
                new Color(121, 134, 203), // Índigo
                new Color(79, 195, 247),  // Azul claro
                new Color(77, 208, 225),  // Cyan
                new Color(129, 199, 132), // Verde
                new Color(255, 213, 79),  // Amarillo
                new Color(255, 138, 101), // Naranja
                new Color(240, 98, 146),  // Rosa
                new Color(149, 117, 205)  // Violeta
        };

        int hash = Math.abs(nombre.hashCode());
        return colores[hash % colores.length];
    }
}