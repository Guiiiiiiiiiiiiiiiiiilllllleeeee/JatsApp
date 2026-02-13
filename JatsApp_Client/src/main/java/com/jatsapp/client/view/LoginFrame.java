package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.client.util.StyleUtil;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField txtUser;
    private JPasswordField txtPass;
    private JButton btnLogin;
    private JButton btnRegister;
    private JLabel lblStatus;

    public LoginFrame() {
        super("JatsApp");
        StyleUtil.applyDarkTheme();

        setSize(420, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Panel principal con fondo oscuro
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
        mainPanel.setBorder(new EmptyBorder(40, 50, 40, 50));

        // Panel de contenido centrado
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Logo / Título
        JLabel lblLogo = new JLabel("💬 JatsApp");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 36));
        lblLogo.setForeground(StyleUtil.PRIMARY);
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(lblLogo);

        contentPanel.add(Box.createVerticalStrut(8));

        JLabel lblSubtitle = new JLabel("Inicia sesión para continuar");
        lblSubtitle.setFont(StyleUtil.FONT_BODY);
        lblSubtitle.setForeground(StyleUtil.TEXT_SECONDARY);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(lblSubtitle);

        contentPanel.add(Box.createVerticalStrut(40));

        // Campo de usuario
        JPanel userPanel = new JPanel();
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
        userPanel.setOpaque(false);
        userPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        userPanel.setMaximumSize(new Dimension(300, 80));

        JLabel lblUser = StyleUtil.createLabel("Usuario", StyleUtil.FONT_SMALL, StyleUtil.TEXT_SECONDARY);
        userPanel.add(lblUser);
        userPanel.add(Box.createVerticalStrut(8));

        txtUser = StyleUtil.createStyledTextField("Introduce tu usuario");
        txtUser.setMaximumSize(new Dimension(300, 45));
        userPanel.add(txtUser);

        contentPanel.add(userPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Campo de contraseña
        JPanel passPanel = new JPanel();
        passPanel.setLayout(new BoxLayout(passPanel, BoxLayout.Y_AXIS));
        passPanel.setOpaque(false);
        passPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passPanel.setMaximumSize(new Dimension(300, 80));

        JLabel lblPass = StyleUtil.createLabel("Contraseña", StyleUtil.FONT_SMALL, StyleUtil.TEXT_SECONDARY);
        passPanel.add(lblPass);
        passPanel.add(Box.createVerticalStrut(8));

        txtPass = StyleUtil.createStyledPasswordField("Introduce tu contraseña");
        txtPass.setMaximumSize(new Dimension(300, 45));
        passPanel.add(txtPass);

        contentPanel.add(passPanel);
        contentPanel.add(Box.createVerticalStrut(30));

        // Botón de login
        btnLogin = StyleUtil.createPrimaryButton("Iniciar Sesión");
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(300, 45));
        contentPanel.add(btnLogin);

        contentPanel.add(Box.createVerticalStrut(15));

        // Botón de registro - estilo link
        btnRegister = StyleUtil.createTextButton("¿No tienes cuenta? Crear una");
        btnRegister.setForeground(StyleUtil.TEXT_PRIMARY);
        btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(btnRegister);

        contentPanel.add(Box.createVerticalStrut(20));

        // Label de estado
        lblStatus = new JLabel(" ");
        lblStatus.setFont(StyleUtil.FONT_SMALL);
        lblStatus.setForeground(StyleUtil.TEXT_MUTED);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(lblStatus);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel);

        // --- Acciones ---
        btnLogin.addActionListener(e -> doLogin());
        btnRegister.addActionListener(e -> irARegistro());

        // Permitir pulsar Enter para loguearse
        getRootPane().setDefaultButton(btnLogin);

        // Registrar en ClientSocket
        ClientSocket.getInstance().setLoginFrame(this);

        setVisible(true);
    }

    private void doLogin() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            showError("Por favor, rellena usuario y contraseña.");
            return;
        }

        // Bloquear botón y mostrar estado
        btnLogin.setEnabled(false);
        lblStatus.setText("Conectando...");
        lblStatus.setForeground(StyleUtil.TEXT_SECONDARY);

        // Guardar usuario en memoria
        ClientSocket.getInstance().setMyUsername(user);

        // Crear y enviar mensaje
        Message msg = new Message();
        msg.setType(MessageType.LOGIN);
        msg.setSenderName(user);
        msg.setContent(pass);

        try {
            ClientSocket.getInstance().send(msg);
        } catch (Exception e) {
            onLoginFail("Error de conexión con el servidor.");
        }
    }

    private void irARegistro() {
        new RegisterFrame();
        this.dispose();
    }

    private void showError(String message) {
        lblStatus.setText(message);
        lblStatus.setForeground(StyleUtil.DANGER);
    }

    // =======================================================
    // MÉTODOS DE RESPUESTA (Llamados por ClientSocket)
    // =======================================================

    public void onRequire2FA() {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("Verificación 2FA requerida...");
            lblStatus.setForeground(StyleUtil.WARNING);
            new VerificationDialog(this);
        });
    }

    public void onLoginSuccess() {
        SwingUtilities.invokeLater(() -> {
            this.dispose();
            System.out.println("✅ Login correcto. Abriendo ChatFrame...");
            ChatFrame chatFrame = new ChatFrame();
            Message requestChats = new Message();
            requestChats.setType(MessageType.GET_RELEVANT_CHATS);
            ClientSocket.getInstance().send(requestChats);
        });
    }

    public void onLoginFail(String reason) {
        SwingUtilities.invokeLater(() -> {
            showError(reason);
            btnLogin.setEnabled(true);
            txtPass.setText("");
        });
    }
}