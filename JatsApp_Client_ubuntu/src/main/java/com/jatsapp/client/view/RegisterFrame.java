package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.client.util.StyleUtil;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class RegisterFrame extends JFrame {

    private JTextField txtUser;
    private JTextField txtEmail;
    private JPasswordField txtPass;
    private JPasswordField txtConfirmPass;
    private JButton btnRegister;
    private JButton btnBack;
    private JLabel lblStatus;

    public RegisterFrame() {
        super("JatsApp - Crear Cuenta");
        StyleUtil.applyDarkTheme();

        setSize(450, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Panel principal con gradiente
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, StyleUtil.BG_DARK, 0, getHeight(), StyleUtil.BG_MEDIUM);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setBorder(new EmptyBorder(25, 50, 25, 50));

        // Panel de contenido
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Título
        JLabel lblTitle = new JLabel("Crear Cuenta");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblTitle.setForeground(StyleUtil.PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(lblTitle);

        contentPanel.add(Box.createVerticalStrut(8));

        JLabel lblSubtitle = new JLabel("Únete a JatsApp hoy");
        lblSubtitle.setFont(StyleUtil.FONT_BODY);
        lblSubtitle.setForeground(StyleUtil.TEXT_SECONDARY);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(lblSubtitle);

        contentPanel.add(Box.createVerticalStrut(25));

        // Campo usuario
        JPanel userPanel = new JPanel();
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
        userPanel.setOpaque(false);
        userPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        userPanel.setMaximumSize(new Dimension(320, 80));

        JLabel lblUser = StyleUtil.createLabel("Nombre de usuario", StyleUtil.FONT_SMALL, StyleUtil.TEXT_SECONDARY);
        userPanel.add(lblUser);
        userPanel.add(Box.createVerticalStrut(6));

        txtUser = StyleUtil.createStyledTextField("Elige un nombre de usuario");
        txtUser.setMaximumSize(new Dimension(320, 50));
        txtUser.setPreferredSize(new Dimension(320, 50));
        userPanel.add(txtUser);

        contentPanel.add(userPanel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Campo email
        JPanel emailPanel = new JPanel();
        emailPanel.setLayout(new BoxLayout(emailPanel, BoxLayout.Y_AXIS));
        emailPanel.setOpaque(false);
        emailPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        emailPanel.setMaximumSize(new Dimension(320, 80));

        JLabel lblEmail = StyleUtil.createLabel("Correo electrónico", StyleUtil.FONT_SMALL, StyleUtil.TEXT_SECONDARY);
        emailPanel.add(lblEmail);
        emailPanel.add(Box.createVerticalStrut(6));

        txtEmail = StyleUtil.createStyledTextField("tu@email.com");
        txtEmail.setMaximumSize(new Dimension(320, 50));
        txtEmail.setPreferredSize(new Dimension(320, 50));
        emailPanel.add(txtEmail);

        contentPanel.add(emailPanel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Campo contraseña
        JPanel passPanel = new JPanel();
        passPanel.setLayout(new BoxLayout(passPanel, BoxLayout.Y_AXIS));
        passPanel.setOpaque(false);
        passPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passPanel.setMaximumSize(new Dimension(320, 80));

        JLabel lblPass = StyleUtil.createLabel("Contraseña", StyleUtil.FONT_SMALL, StyleUtil.TEXT_SECONDARY);
        passPanel.add(lblPass);
        passPanel.add(Box.createVerticalStrut(6));

        txtPass = StyleUtil.createStyledPasswordField("Mínimo 6 caracteres");
        txtPass.setMaximumSize(new Dimension(320, 50));
        txtPass.setPreferredSize(new Dimension(320, 50));
        passPanel.add(txtPass);

        contentPanel.add(passPanel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Campo confirmar contraseña
        JPanel confirmPanel = new JPanel();
        confirmPanel.setLayout(new BoxLayout(confirmPanel, BoxLayout.Y_AXIS));
        confirmPanel.setOpaque(false);
        confirmPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmPanel.setMaximumSize(new Dimension(320, 80));

        JLabel lblConfirm = StyleUtil.createLabel("Confirmar contraseña", StyleUtil.FONT_SMALL, StyleUtil.TEXT_SECONDARY);
        confirmPanel.add(lblConfirm);
        confirmPanel.add(Box.createVerticalStrut(6));

        txtConfirmPass = StyleUtil.createStyledPasswordField("Repite tu contraseña");
        txtConfirmPass.setMaximumSize(new Dimension(320, 50));
        txtConfirmPass.setPreferredSize(new Dimension(320, 50));
        confirmPanel.add(txtConfirmPass);

        contentPanel.add(confirmPanel);
        contentPanel.add(Box.createVerticalStrut(18));

        // Botón de registro
        btnRegister = StyleUtil.createPrimaryButton("Crear Cuenta");
        btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRegister.setMaximumSize(new Dimension(320, 45));
        contentPanel.add(btnRegister);

        contentPanel.add(Box.createVerticalStrut(15));

        // Botón volver - con fondo visible
        btnBack = new JButton("← Volver al inicio de sesión");
        btnBack.setFont(StyleUtil.FONT_BODY);
        btnBack.setForeground(StyleUtil.TEXT_PRIMARY);
        btnBack.setBackground(StyleUtil.BG_LIGHT);
        btnBack.setOpaque(true);
        btnBack.setBorderPainted(false);
        btnBack.setFocusPainted(false);
        btnBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBack.setBorder(new EmptyBorder(12, 24, 12, 24));
        btnBack.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnBack.setMaximumSize(new Dimension(320, 45));
        contentPanel.add(btnBack);

        contentPanel.add(Box.createVerticalStrut(15));

        // Label de estado
        lblStatus = new JLabel(" ");
        lblStatus.setFont(StyleUtil.FONT_SMALL);
        lblStatus.setForeground(StyleUtil.TEXT_MUTED);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(lblStatus);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel);

        // --- Eventos ---
        btnRegister.addActionListener(e -> attemptRegister());
        btnBack.addActionListener(e -> abrirLogin());

        setVisible(true);
    }

    private void attemptRegister() {
        String user = txtUser.getText().trim();
        String email = txtEmail.getText().trim();
        String pass = new String(txtPass.getPassword());
        String confirm = new String(txtConfirmPass.getPassword());

        // Validaciones
        if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            showError("Por favor, rellena todos los campos.");
            return;
        }
        if (pass.length() < 6) {
            showError("La contraseña debe tener al menos 6 caracteres.");
            return;
        }
        if (!pass.equals(confirm)) {
            showError("Las contraseñas no coinciden.");
            return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            showError("El email no parece válido.");
            return;
        }
        if (user.contains(":") || email.contains(":") || pass.contains(":")) {
            showError("El carácter ':' no está permitido.");
            return;
        }

        // Preparar mensaje
        String payload = user + ":" + email + ":" + pass;

        Message msg = new Message();
        msg.setType(MessageType.REGISTER);
        msg.setContent(payload);
        msg.setSenderName(user);

        btnRegister.setEnabled(false);
        lblStatus.setText("Creando cuenta...");
        lblStatus.setForeground(StyleUtil.TEXT_SECONDARY);

        try {
            ClientSocket.getInstance().send(msg);

            // Mostrar éxito
            lblStatus.setText("¡Cuenta creada! Redirigiendo...");
            lblStatus.setForeground(StyleUtil.SUCCESS);

            // Volver al login después de un momento
            Timer timer = new Timer(1500, e -> abrirLogin());
            timer.setRepeats(false);
            timer.start();

        } catch (Exception ex) {
            showError("Error de conexión con el servidor.");
            btnRegister.setEnabled(true);
        }
    }

    private void showError(String message) {
        lblStatus.setText(message);
        lblStatus.setForeground(StyleUtil.DANGER);
    }

    private void abrirLogin() {
        SwingUtilities.invokeLater(() -> {
            new LoginFrame();
            this.dispose();
        });
    }
}
