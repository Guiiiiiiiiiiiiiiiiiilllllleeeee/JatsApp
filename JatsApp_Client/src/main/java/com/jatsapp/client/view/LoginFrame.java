package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField txtUser;
    private JPasswordField txtPass;
    private JButton btnLogin;
    private JButton btnRegister;

    public LoginFrame() {
        super("JatsApp - Iniciar Sesión");
        setSize(350, 280);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panel principal con margen (Padding)
        JPanel mainPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- Componentes ---
        mainPanel.add(new JLabel("Usuario:"));
        txtUser = new JTextField();
        mainPanel.add(txtUser);

        mainPanel.add(new JLabel("Contraseña:"));
        txtPass = new JPasswordField();
        mainPanel.add(txtPass);

        // Botones
        btnRegister = new JButton("Crear Cuenta");
        btnLogin = new JButton("Entrar");
        btnLogin.setBackground(new Color(0, 200, 150)); // Verde
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 12));

        mainPanel.add(btnRegister);
        mainPanel.add(btnLogin);

        add(mainPanel);

        // --- Acciones ---
        btnLogin.addActionListener(e -> doLogin());
        btnRegister.addActionListener(e -> irARegistro());

        // Permitir pulsar Enter en el campo de contraseña para loguearse
        getRootPane().setDefaultButton(btnLogin);

        // *** IMPORTANTE ***
        // Registramos esta ventana en el ClientSocket para recibir las respuestas
        ClientSocket.getInstance().setLoginFrame(this);

        setVisible(true);
    }

    private void doLogin() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, rellena usuario y contraseña.");
            return;
        }

        // Bloquear botón para evitar doble clic
        btnLogin.setEnabled(false);
        setTitle("Conectando...");

        // 1. Guardar usuario en memoria (para usarlo luego en el 2FA)
        ClientSocket.getInstance().setMyUsername(user);

        // 2. Crear mensaje
        Message msg = new Message();
        msg.setType(MessageType.LOGIN);
        msg.setSenderName(user);
        msg.setContent(pass);

        // 3. Enviar
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

    // =======================================================
    // MÉTODOS DE RESPUESTA (Llamados por ClientSocket)
    // =======================================================

    /**
     * CASO 1: El servidor dice que las credenciales son correctas,
     * pero pide el código 2FA.
     */
    public void onRequire2FA() {
        SwingUtilities.invokeLater(() -> {
            setTitle("Verificando...");
            // Abrimos el diálogo de verificación modal
            new VerificationDialog(this);
            // Nota: No cerramos el LoginFrame aún, esperamos a que el 2FA sea OK
        });
    }

    /**
     * CASO 2: Todo correcto (después del 2FA).
     */
    public void onLoginSuccess() {
        SwingUtilities.invokeLater(() -> {
            // 1. Cerramos la ventana de Login
            this.dispose();

            System.out.println("✅ Login correcto. Abriendo ChatFrame...");

            // 2. ABRIMOS LA VENTANA PRINCIPAL DEL CHAT
            ChatFrame chatFrame = new ChatFrame();

            // 3. Solicitar chats relevantes al servidor
            Message requestChats = new Message();
            requestChats.setType(MessageType.GET_RELEVANT_CHATS);
            ClientSocket.getInstance().send(requestChats);
        });
    }

    /**
     * CASO 3: Error (pass incorrecta o código 2FA mal)
     */
    public void onLoginFail(String reason) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Error: " + reason);
            setTitle("JatsApp - Iniciar Sesión");
            btnLogin.setEnabled(true);
            txtPass.setText(""); // Limpiar contraseña
        });
    }
}