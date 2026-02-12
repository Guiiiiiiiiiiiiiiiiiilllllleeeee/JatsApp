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
        super(parent, "Verificación 2FA", true); // Modal
        setSize(350, 200);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(15, 15));

        // Panel superior (Instrucciones)
        JPanel panelInfo = new JPanel();
        panelInfo.setBorder(BorderFactory.createEmptyBorder(15, 10, 0, 10));
        JLabel lblInfo = new JLabel("<html><center>Hemos enviado un código a tu email.<br><b>Introdúcelo aquí para continuar:</b></center></html>");
        lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
        panelInfo.add(lblInfo);
        add(panelInfo, BorderLayout.NORTH);

        // Panel central (Input)
        JPanel panelInput = new JPanel();
        txtCodigo = new JTextField(10);
        txtCodigo.setFont(new Font("Monospaced", Font.BOLD, 24)); // Fuente tipo código
        txtCodigo.setHorizontalAlignment(JTextField.CENTER);
        panelInput.add(txtCodigo);
        add(panelInput, BorderLayout.CENTER);

        // Panel inferior (Botones)
        JPanel panelBoton = new JPanel();
        btnVerificar = new JButton("Verificar Código");
        btnVerificar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnVerificar.setBackground(new Color(0, 200, 150)); // Acento verde
        btnVerificar.setForeground(Color.WHITE);

        panelBoton.add(btnVerificar);
        add(panelBoton, BorderLayout.SOUTH);

        // Acciones
        btnVerificar.addActionListener(e -> enviarCodigo());

        // Permitir pulsar ENTER para enviar
        getRootPane().setDefaultButton(btnVerificar);

        setVisible(true);
    }

    private void enviarCodigo() {
        String codigo = txtCodigo.getText().trim();
        if (codigo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, escribe el código.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 1. Crear mensaje
        Message msg = new Message();
        msg.setType(MessageType.VERIFY_2FA);
        msg.setContent(codigo);
        // Nota: No hace falta setSenderName aquí, el servidor ya sabe quién eres por el socket (tempUser)

        // 2. Enviar
        try {
            ClientSocket.getInstance().send(msg);

            // 3. Cerrar diálogo
            // Asumimos que el código se ha enviado. Si es incorrecto,
            // el ClientSocket recibirá un LOGIN_FAIL y mostrará un error en el LoginFrame.
            this.dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de conexión al enviar el código.");
        }
    }
}