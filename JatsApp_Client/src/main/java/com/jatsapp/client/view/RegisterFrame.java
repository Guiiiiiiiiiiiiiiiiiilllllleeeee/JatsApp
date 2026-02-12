package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;

import javax.swing.*;
import java.awt.*;

public class RegisterFrame extends JFrame {

    private JTextField txtUser;
    private JTextField txtEmail;
    private JPasswordField txtPass;
    private JPasswordField txtConfirmPass;
    private JButton btnRegister;
    private JButton btnBack;

    public RegisterFrame() {
        super("JatsApp - Crear Cuenta");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Usamos un panel con padding para que no se pegue a los bordes
        JPanel mainPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- Campos ---
        mainPanel.add(new JLabel("Usuario:"));
        txtUser = new JTextField();
        mainPanel.add(txtUser);

        mainPanel.add(new JLabel("Email:"));
        txtEmail = new JTextField();
        mainPanel.add(txtEmail);

        mainPanel.add(new JLabel("Contraseña:"));
        txtPass = new JPasswordField();
        mainPanel.add(txtPass);

        mainPanel.add(new JLabel("Repetir Contraseña:"));
        txtConfirmPass = new JPasswordField();
        mainPanel.add(txtConfirmPass);

        // --- Botones ---
        btnBack = new JButton("<< Volver");
        btnRegister = new JButton("Registrarse");

        // Estilo botón principal
        btnRegister.setBackground(new Color(0, 200, 150));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setFont(new Font("SansSerif", Font.BOLD, 12));

        mainPanel.add(btnBack);
        mainPanel.add(btnRegister);

        add(mainPanel);

        // --- Eventos ---
        btnRegister.addActionListener(e -> attemptRegister());

        btnBack.addActionListener(e -> {
            abrirLogin();
        });

        setVisible(true);
    }

    private void attemptRegister() {
        String user = txtUser.getText().trim();
        String email = txtEmail.getText().trim();
        String pass = new String(txtPass.getPassword());
        String confirm = new String(txtConfirmPass.getPassword());

        // 1. Validaciones locales
        if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, rellena todos los campos.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!pass.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Validación básica para evitar romper el protocolo del servidor
        if (user.contains(":") || email.contains(":") || pass.contains(":")) {
            JOptionPane.showMessageDialog(this, "El carácter ':' no está permitido.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. Preparar mensaje con el formato que espera el SERVIDOR (user:email:pass)
        // IMPORTANTE: Esto debe coincidir con el split(":") de ClientHandler
        String payload = user + ":" + email + ":" + pass;

        Message msg = new Message();
        msg.setType(MessageType.REGISTER);
        msg.setContent(payload);
        msg.setSenderName(user);

        // 3. Enviar
        try {
            ClientSocket.getInstance().send(msg);

            // Feedback inmediato al usuario
            JOptionPane.showMessageDialog(this, "Solicitud de registro enviada.\nSi los datos son correctos, podrás iniciar sesión.");

            // Volvemos al login
            abrirLogin();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de conexión con el servidor.");
        }
    }

    private void abrirLogin() {
        SwingUtilities.invokeLater(() -> {
            new LoginFrame();
            this.dispose();
        });
    }
}