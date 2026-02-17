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
        super(parent, "Verificación 2FA", true);
        setSize(400, 280);
        setLocationRelativeTo(parent);
        setResizable(false);

        // Panel principal con gradiente
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15)) {
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
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        // Icono y título
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel lblIcon = new JLabel("🔐");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblIcon);

        headerPanel.add(Box.createVerticalStrut(10));

        JLabel lblTitle = new JLabel("Verificación de Seguridad");
        lblTitle.setFont(StyleUtil.FONT_SUBTITLE);
        lblTitle.setForeground(StyleUtil.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblTitle);

        headerPanel.add(Box.createVerticalStrut(5));

        JLabel lblInfo = new JLabel("Introduce el código enviado a tu email");
        lblInfo.setFont(StyleUtil.FONT_SMALL);
        lblInfo.setForeground(StyleUtil.TEXT_SECONDARY);
        lblInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblInfo);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Campo de código
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        inputPanel.setOpaque(false);

        txtCodigo = new JTextField(8);
        txtCodigo.setFont(new Font("Consolas", Font.BOLD, 28));
        txtCodigo.setHorizontalAlignment(JTextField.CENTER);
        txtCodigo.setBackground(StyleUtil.BG_LIGHT);
        txtCodigo.setForeground(StyleUtil.TEXT_PRIMARY);
        txtCodigo.setCaretColor(StyleUtil.PRIMARY);
        txtCodigo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(StyleUtil.BORDER_LIGHT, 2, true),
            new EmptyBorder(10, 15, 10, 15)
        ));
        inputPanel.add(txtCodigo);

        mainPanel.add(inputPanel, BorderLayout.CENTER);

        // Botón verificar
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);

        btnVerificar = StyleUtil.createPrimaryButton("Verificar Código");
        btnVerificar.addActionListener(e -> enviarCodigo());
        buttonPanel.add(btnVerificar);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        getRootPane().setDefaultButton(btnVerificar);

        setVisible(true);
    }

    private void enviarCodigo() {
        String codigo = txtCodigo.getText().trim();
        if (codigo.isEmpty()) {
            txtCodigo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(StyleUtil.DANGER, 2, true),
                new EmptyBorder(10, 15, 10, 15)
            ));
            return;
        }

        Message msg = new Message();
        msg.setType(MessageType.VERIFY_2FA);
        msg.setContent(codigo);

        try {
            ClientSocket.getInstance().send(msg);
            this.dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de conexión al enviar el código.");
        }
    }
}