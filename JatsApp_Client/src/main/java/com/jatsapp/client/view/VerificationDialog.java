package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;

import javax.swing.*;
import java.awt.*;

public class VerificationDialog extends JDialog {

    private JTextField txtCodigo;
    private JButton btnVerificar;

    public VerificationDialog(JFrame parent) {
        super(parent, "Verificación 2FA", true); // 'true' hace que sea modal (bloquea la ventana de atrás)
        setSize(300, 180);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        // Panel superior (Instrucciones)
        JPanel panelInfo = new JPanel();
        panelInfo.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        panelInfo.add(new JLabel("<html><center>Hemos enviado un código a tu email.<br>Introdúcelo abajo:</center></html>"));
        add(panelInfo, BorderLayout.NORTH);

        // Panel central (Input)
        JPanel panelInput = new JPanel();
        txtCodigo = new JTextField(10);
        txtCodigo.setFont(new Font("Arial", Font.BOLD, 16));
        txtCodigo.setHorizontalAlignment(JTextField.CENTER);
        panelInput.add(txtCodigo);
        add(panelInput, BorderLayout.CENTER);

        // Panel inferior (Botón)
        JPanel panelBoton = new JPanel();
        btnVerificar = new JButton("Verificar Código");
        panelBoton.add(btnVerificar);
        add(panelBoton, BorderLayout.SOUTH);

        // Acción del botón
        btnVerificar.addActionListener(e -> enviarCodigo());

        setVisible(true);
    }

    private void enviarCodigo() {
        String codigo = txtCodigo.getText().trim();
        if (codigo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escribe el código primero.");
            return;
        }

        String miUsuario = ClientSocket.getInstance().getMyUsername();

        // Crear mensaje de verificación
        Message msg = new Message();
        msg.setType(MessageType.VERIFY_2FA);
        msg.setSenderName(miUsuario);
        msg.setContent(codigo);

        // Enviar
        ClientSocket.getInstance().send(msg);

        // Cerramos este diálogo (la respuesta la gestionará ClientSocket)
        this.dispose();
    }
}