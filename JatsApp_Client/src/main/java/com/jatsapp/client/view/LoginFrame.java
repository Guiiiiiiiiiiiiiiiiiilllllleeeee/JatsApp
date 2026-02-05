package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    // Componentes de la ventana
    private JTextField txtUser;
    private JPasswordField txtPass;
    private JButton btnLogin;
    private JButton btnRegister;

    public LoginFrame() {
        // 1. Configuración básica de la ventana
        setTitle("JatsApp - Iniciar Sesión");
        setSize(350, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Al cerrar esto, se acaba el programa
        setLocationRelativeTo(null); // Centrar en pantalla
        setLayout(new GridLayout(4, 2, 10, 10)); // Rejilla de 4 filas, 2 columnas

        // 2. Crear y añadir componentes
        add(new JLabel("  Usuario:"));
        txtUser = new JTextField();
        add(txtUser);

        add(new JLabel("  Contraseña:"));
        txtPass = new JPasswordField();
        add(txtPass);

        // Botón Login
        btnLogin = new JButton("Entrar");
        btnLogin.setBackground(new Color(100, 200, 100)); // Un verde suave

        // Botón Registro (importante para ir a la otra ventana)
        btnRegister = new JButton("Crear Cuenta");

        // Añadimos los botones a la ventana
        add(btnRegister);
        add(btnLogin);

        // 3. Dar vida a los botones (Listeners)

        // Acción al pulsar "Entrar"
        btnLogin.addActionListener(e -> doLogin());

        // Acción al pulsar "Crear Cuenta"
        btnRegister.addActionListener(e -> irARegistro());

        // 4. Conectar esta ventana con el Socket
        // (Para que el Socket sepa a quién avisar si el login es OK)
        ClientSocket.getInstance().setLoginFrame(this);

        // Mostrar ventana
        setVisible(true);
    }

    /**
     * Lógica para enviar los datos al servidor
     */
    private void doLogin() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword()); // Leer contraseña

        // Validación básica: no enviar vacíos
        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, rellena usuario y contraseña.");
            return;
        }

        // 1. Guardamos el nombre en el cliente (memoria)
        ClientSocket.getInstance().setMyUsername(user);

        // 2. Preparamos el mensaje JSON
        Message msg = new Message();
        msg.setType(MessageType.LOGIN);
        msg.setSenderName(user);
        msg.setContent(pass); // En un caso real, esto debería ir hasheado aquí también, pero el servidor lo hará

        // 3. Enviamos por la red
        ClientSocket.getInstance().send(msg);

        // Feedback visual (opcional)
        setTitle("JatsApp - Conectando...");
        btnLogin.setEnabled(false);
    }

    /**
     * Cierra esta ventana y abre la de Registro
     */
    private void irARegistro() {
        new RegisterFrame(); // Abrir la nueva
        this.dispose();      // Cerrar la actual
    }

    /**
     * Este método será llamado por ClientSocket cuando el servidor responda "LOGIN_OK"
     */
    public void onLoginSuccess() {
        // Cerrar ventana de login
        this.dispose();
        // Abrir la ventana del chat principal
        SwingUtilities.invokeLater(() -> new ChatFrame());
    }
}