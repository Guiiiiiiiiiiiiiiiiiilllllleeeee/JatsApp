package com.jatsapp.client.util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * Utilidades de estilo para dar una apariencia moderna y profesional a la aplicación
 */
public class StyleUtil {

    // ===== PALETA DE COLORES PRINCIPAL =====
    public static final Color PRIMARY = new Color(0, 168, 132);        // Verde JatsApp
    public static final Color PRIMARY_DARK = new Color(0, 140, 110);   // Verde oscuro
    public static final Color PRIMARY_LIGHT = new Color(37, 211, 169); // Verde claro

    public static final Color ACCENT = new Color(52, 152, 219);        // Azul acento
    public static final Color ACCENT_DARK = new Color(41, 128, 185);   // Azul oscuro

    public static final Color DANGER = new Color(231, 76, 60);         // Rojo peligro
    public static final Color DANGER_DARK = new Color(192, 57, 43);    // Rojo oscuro

    public static final Color WARNING = new Color(241, 196, 15);       // Amarillo advertencia
    public static final Color SUCCESS = new Color(46, 204, 113);       // Verde éxito

    // ===== COLORES DE FONDO =====
    public static final Color BG_DARK = new Color(17, 27, 33);         // Fondo principal oscuro
    public static final Color BG_MEDIUM = new Color(30, 42, 48);       // Fondo medio
    public static final Color BG_LIGHT = new Color(42, 57, 66);        // Fondo claro
    public static final Color BG_HOVER = new Color(50, 68, 78);        // Fondo hover
    public static final Color BG_SELECTED = new Color(42, 93, 80);     // Fondo seleccionado

    // ===== COLORES DE TEXTO =====
    public static final Color TEXT_PRIMARY = new Color(233, 237, 239); // Texto principal
    public static final Color TEXT_SECONDARY = new Color(145, 160, 170); // Texto secundario
    public static final Color TEXT_MUTED = new Color(100, 115, 125);   // Texto atenuado

    // ===== COLORES DE BORDES =====
    public static final Color BORDER_LIGHT = new Color(55, 70, 80);    // Borde claro
    public static final Color BORDER_DARK = new Color(35, 48, 55);     // Borde oscuro

    // ===== FUENTES =====
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_TINY = new Font("Segoe UI", Font.PLAIN, 11);

    // ===== DIMENSIONES =====
    public static final int BORDER_RADIUS = 12;
    public static final int PADDING_SMALL = 8;
    public static final int PADDING_MEDIUM = 15;
    public static final int PADDING_LARGE = 20;

    /**
     * Aplica el tema oscuro global a la aplicación
     */
    public static void applyDarkTheme() {
        // NO usar el look and feel del sistema para tener control total
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Configuración global de UI
        UIManager.put("Panel.background", BG_DARK);
        UIManager.put("OptionPane.background", BG_MEDIUM);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);

        // Botones
        UIManager.put("Button.background", BG_LIGHT);
        UIManager.put("Button.foreground", TEXT_PRIMARY);
        UIManager.put("Button.select", BG_HOVER);
        UIManager.put("Button.focus", BG_LIGHT);

        // Campos de texto
        UIManager.put("TextField.background", BG_LIGHT);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground", TEXT_PRIMARY);
        UIManager.put("PasswordField.background", BG_LIGHT);
        UIManager.put("PasswordField.foreground", TEXT_PRIMARY);
        UIManager.put("PasswordField.caretForeground", TEXT_PRIMARY);

        // Combos y listas
        UIManager.put("ComboBox.background", BG_LIGHT);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("List.background", BG_MEDIUM);
        UIManager.put("List.foreground", TEXT_PRIMARY);
        UIManager.put("List.selectionBackground", BG_SELECTED);
        UIManager.put("List.selectionForeground", TEXT_PRIMARY);

        // Scroll
        UIManager.put("ScrollPane.background", BG_DARK);
        UIManager.put("Viewport.background", BG_DARK);
        UIManager.put("Label.foreground", TEXT_PRIMARY);

        // Menu
        UIManager.put("Menu.background", BG_MEDIUM);
        UIManager.put("Menu.foreground", TEXT_PRIMARY);
        UIManager.put("MenuItem.background", BG_MEDIUM);
        UIManager.put("MenuItem.foreground", TEXT_PRIMARY);
        UIManager.put("PopupMenu.background", BG_MEDIUM);
    }

    /**
     * Crea un botón con estilo moderno
     */
    public static JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);

        button.setFont(FONT_BODY);
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(12, 24, 12, 24));

        // Efectos hover
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            Color originalBg = bgColor;

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(lighten(originalBg, 0.15f));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(originalBg);
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                button.setBackground(darken(originalBg, 0.15f));
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                button.setBackground(originalBg);
            }
        });

        return button;
    }

    /**
     * Crea un botón primario (verde)
     */
    public static JButton createPrimaryButton(String text) {
        return createStyledButton(text, PRIMARY);
    }

    /**
     * Crea un botón secundario (gris más claro visible)
     */
    public static JButton createSecondaryButton(String text) {
        return createStyledButton(text, new Color(70, 85, 95));
    }

    /**
     * Crea un botón de texto/link (transparente con texto visible)
     */
    public static JButton createTextButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FONT_BODY);
        button.setForeground(TEXT_SECONDARY);
        button.setBackground(null);
        button.setOpaque(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 20, 10, 20));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setForeground(PRIMARY);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setForeground(TEXT_SECONDARY);
            }
        });

        return button;
    }

    /**
     * Crea un botón de peligro (rojo)
     */
    public static JButton createDangerButton(String text) {
        return createStyledButton(text, DANGER);
    }

    /**
     * Crea un botón de acento (azul)
     */
    public static JButton createAccentButton(String text) {
        return createStyledButton(text, ACCENT);
    }

    /**
     * Crea un campo de texto con estilo moderno
     */
    public static JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), BORDER_RADIUS, BORDER_RADIUS);
                g2.dispose();
                super.paintComponent(g);

                // Placeholder
                if (getText().isEmpty() && !isFocusOwner()) {
                    g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(TEXT_MUTED);
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(placeholder, getInsets().left, y);
                    g2.dispose();
                }
            }
        };

        field.setOpaque(false);
        field.setBackground(BG_LIGHT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setFont(FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT, 1, true),
            new EmptyBorder(12, 15, 12, 15)
        ));

        // Efecto focus
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(PRIMARY, 2, true),
                    new EmptyBorder(11, 14, 11, 14)
                ));
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_LIGHT, 1, true),
                    new EmptyBorder(12, 15, 12, 15)
                ));
            }
        });

        return field;
    }

    /**
     * Crea un campo de contraseña con estilo moderno
     */
    public static JPasswordField createStyledPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), BORDER_RADIUS, BORDER_RADIUS);
                g2.dispose();
                super.paintComponent(g);

                // Placeholder
                if (getPassword().length == 0 && !isFocusOwner()) {
                    g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(TEXT_MUTED);
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(placeholder, getInsets().left, y);
                    g2.dispose();
                }
            }
        };

        field.setOpaque(false);
        field.setBackground(BG_LIGHT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setFont(FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT, 1, true),
            new EmptyBorder(12, 15, 12, 15)
        ));

        // Efecto focus
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(PRIMARY, 2, true),
                    new EmptyBorder(11, 14, 11, 14)
                ));
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_LIGHT, 1, true),
                    new EmptyBorder(12, 15, 12, 15)
                ));
            }
        });

        return field;
    }

    /**
     * Crea un panel con esquinas redondeadas
     */
    public static JPanel createRoundedPanel(Color bgColor) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), BORDER_RADIUS * 2, BORDER_RADIUS * 2);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Personaliza un JScrollPane con estilo moderno
     */
    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.setBackground(BG_DARK);

        // Scrollbar vertical personalizada
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getVerticalScrollBar().setBackground(BG_DARK);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

        // Scrollbar horizontal personalizada
        scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getHorizontalScrollBar().setBackground(BG_DARK);
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 8));
    }

    /**
     * Crea una etiqueta con estilo
     */
    public static JLabel createLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    /**
     * Oscurece un color
     */
    public static Color darken(Color color, float factor) {
        int r = Math.max(0, (int) (color.getRed() * (1 - factor)));
        int g = Math.max(0, (int) (color.getGreen() * (1 - factor)));
        int b = Math.max(0, (int) (color.getBlue() * (1 - factor)));
        return new Color(r, g, b);
    }

    /**
     * Aclara un color
     */
    public static Color lighten(Color color, float factor) {
        int r = Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * factor));
        int g = Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * factor));
        int b = Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * factor));
        return new Color(r, g, b);
    }

    /**
     * ScrollBar UI moderna
     */
    private static class ModernScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = BORDER_LIGHT;
            this.trackColor = BG_DARK;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 1,
                           thumbBounds.width - 2, thumbBounds.height - 2, 6, 6);
            g2.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            // Track transparente
        }
    }
}

