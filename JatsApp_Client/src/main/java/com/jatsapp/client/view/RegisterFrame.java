package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;
import com.jatsapp.common.User; // Importamos la clase que acabamos de crear
import com.google.gson.Gson;

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
        setTitle("JatsApp - Crear Cuenta");
        setSize(350, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Solo cierra esta ventana, no la app
        setLocationRelativeTo(null);
        setLayout(new GridLayout(6, 2, 10, 10));

        // Campos
        add(new JLabel(" Usuario:"));
        txtUser = new JTextField();
        add(txtUser);

        add(new JLabel(" Email:"));
        txtEmail = new JTextField();
        add(txtEmail);

        add(new JLabel(" Contraseña:"));
        txtPass = new JPasswordField();
        add(txtPass);

        add(new JLabel(" Repetir Pass:"));
        txtConfirmPass = new JPasswordField();
        add(txtConfirmPass);

        // Botones
        btnBack = new JButton("<< Volver");
        btnRegister = new JButton("Registrarse");

        add(btnBack);
        add(btnRegister);

        // Lógica de botones
        btnRegister.addActionListener(e -> attemptRegister());
        btnBack.addActionListener(e -> {
            new LoginFrame(); // Vuelve al login
            this.dispose();
        });

        setVisible(true);
    }

    private void attemptRegister() {
        String user = txtUser.getText().trim();
        String email = txtEmail.getText().trim();
        String pass = new String(txtPass.getPassword());
        String confirm = new String(txtConfirmPass.getPassword());

        // 1. Validaciones básicas
        if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Rellena todos los campos.");
            return;
        }
        if (!pass.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.");
            return;
        }

        // 2. Crear objeto User con los datos
        User newUser = new User(0, user, email);

        // 3. Empaquetarlo en un Message de tipo REGISTER
        // Truco: Usamos el campo 'content' para la contraseña y 'sender' para el JSON del usuario
        // O mejor: Enviamos el User como JSON en 'content' y la pass en otro lado?
        // Simplifiquemos: Mandamos un JSON especial en el contenido.

        RegistrationData data = new RegistrationData(user, email, pass);
        String jsonData = new Gson().toJson(data);

        Message msg = new Message();
        msg.setType(MessageType.REGISTER);
        msg.setContent(jsonData); // Enviamos todo junto
        msg.setSenderName(user);

        // 4. Enviar
        ClientSocket.getInstance().send(msg);

        JOptionPane.showMessageDialog(this, "Solicitud enviada. Si todo va bien, podrás loguearte.");
        this.dispose();
        new LoginFrame();
    }

    // Clase interna auxiliar solo para empaquetar los datos del registro
    class RegistrationData {
        String username;
        String email;
        String password;

        public RegistrationData(String u, String e, String p) {
            this.username = u; this.email = e; this.password = p;
        }
    }
}