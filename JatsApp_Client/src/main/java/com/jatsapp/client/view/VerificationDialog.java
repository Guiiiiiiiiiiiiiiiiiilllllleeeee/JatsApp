package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.client.util.StyleUtil;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class VerificationDialog extends JDialog {

    private JTextField txtCodigo;
    private JButton btnVerificar;

    public VerificationDialog(JFrame parent) {
        super(parent, "Verificación 2FA", true);  // true = modal
        System.out.println("🔐 Construyendo VerificationDialog...");

        setSize(400, 300);
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Usar un layout simple
        setLayout(new BorderLayout());
        getContentPane().setBackground(StyleUtil.BG_DARK);

        // Panel superior con título
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(StyleUtil.BG_DARK);
        headerPanel.setBorder(new EmptyBorder(20, 20, 10, 20));

        JLabel lblIcon = new JLabel("🔐", SwingConstants.CENTER);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblIcon);

        headerPanel.add(Box.createVerticalStrut(10));

        JLabel lblTitle = new JLabel("Verificación de Seguridad", SwingConstants.CENTER);
        lblTitle.setFont(StyleUtil.FONT_SUBTITLE);
        lblTitle.setForeground(StyleUtil.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblTitle);

        headerPanel.add(Box.createVerticalStrut(5));

        JLabel lblInfo = new JLabel("Introduce el código enviado a tu email", SwingConstants.CENTER);
        lblInfo.setFont(StyleUtil.FONT_SMALL);
        lblInfo.setForeground(StyleUtil.TEXT_SECONDARY);
        lblInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblInfo);

        add(headerPanel, BorderLayout.NORTH);

        // Panel central con campo de texto
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        centerPanel.setBackground(StyleUtil.BG_DARK);

        txtCodigo = new JTextField(10);
        txtCodigo.setFont(new Font("Consolas", Font.BOLD, 24));
        txtCodigo.setHorizontalAlignment(JTextField.CENTER);
        txtCodigo.setPreferredSize(new Dimension(200, 50));
        centerPanel.add(txtCodigo);

        add(centerPanel, BorderLayout.CENTER);

        // Panel inferior con botón
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        buttonPanel.setBackground(StyleUtil.BG_DARK);

        btnVerificar = new JButton("Verificar Código");
        btnVerificar.setFont(StyleUtil.FONT_BODY);
        btnVerificar.setBackground(StyleUtil.PRIMARY);
        btnVerificar.setForeground(Color.WHITE);
        btnVerificar.setFocusPainted(false);
        btnVerificar.setPreferredSize(new Dimension(180, 40));
        btnVerificar.addActionListener(e -> enviarCodigo());
        buttonPanel.add(btnVerificar);

        add(buttonPanel, BorderLayout.SOUTH);

        System.out.println("🔐 VerificationDialog construido, mostrando...");

        // Mostrar el diálogo (esto bloquea porque es modal)
        setVisible(true);
    }

    private void enviarCodigo() {
        System.out.println("🔑 enviarCodigo() llamado");
        String codigo = txtCodigo.getText().trim();
        System.out.println("🔑 Código ingresado: '" + codigo + "'");

        if (codigo.isEmpty()) {
            System.out.println("🔑 Código vacío, mostrando error");
            JOptionPane.showMessageDialog(this, "Por favor, introduce el código de verificación", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Message msg = new Message();
        msg.setType(MessageType.VERIFY_2FA);
        msg.setContent(codigo);

        try {
            btnVerificar.setEnabled(false);
            btnVerificar.setText("Verificando...");

            System.out.println("🔑 Enviando VERIFY_2FA al servidor...");
            ClientSocket.getInstance().send(msg);
            System.out.println("🔑 VERIFY_2FA enviado, cerrando diálogo...");

            // Cerrar este diálogo
            this.dispose();

            // Mostrar panel de verificación exitosa y volver al login
            // (esto se ejecutará mientras esperamos la respuesta del servidor)
            mostrarExitoYVolverAlLogin();

        } catch (Exception ex) {
            System.err.println("🔑 Error enviando código: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error de conexión al enviar el código.");
            btnVerificar.setEnabled(true);
            btnVerificar.setText("Verificar Código");
        }
    }

    private void mostrarExitoYVolverAlLogin() {
        // Obtener el frame padre (RegisterFrame)
        Window owner = getOwner();

        // Crear diálogo de éxito
        JDialog successDialog = new JDialog((Frame) owner, "Verificación Completada", true);
        successDialog.setSize(350, 200);
        successDialog.setLocationRelativeTo(owner);
        successDialog.setResizable(false);
        successDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(17, 27, 33));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Icono de verificado
        JLabel iconLabel = new JLabel("✅", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(iconLabel);

        panel.add(Box.createVerticalStrut(15));

        // Texto de verificado
        JLabel textLabel = new JLabel("¡Cuenta Verificada!");
        textLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        textLabel.setForeground(new Color(46, 204, 113));
        textLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(textLabel);

        panel.add(Box.createVerticalStrut(5));

        JLabel subTextLabel = new JLabel("Redirigiendo al login...");
        subTextLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subTextLabel.setForeground(new Color(145, 160, 170));
        subTextLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(subTextLabel);

        successDialog.setContentPane(panel);

        // Timer para cerrar el diálogo después de 2 segundos
        Timer timer = new Timer(2000, e -> {
            successDialog.dispose();
            // Cerrar RegisterFrame y abrir LoginFrame
            if (owner instanceof JFrame) {
                owner.dispose();
                // Limpiar referencia en ClientSocket
                ClientSocket.getInstance().setRegisterFrame(null);
                new LoginFrame();
            }
        });
        timer.setRepeats(false);
        timer.start();

        // Mostrar el diálogo de éxito
        successDialog.setVisible(true);
    }
}