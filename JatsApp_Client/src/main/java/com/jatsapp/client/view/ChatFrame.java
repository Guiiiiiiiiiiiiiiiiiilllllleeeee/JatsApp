package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ChatFrame extends JFrame {

    private JList<String> listaContactos;
    private DefaultListModel<String> modeloContactos;
    private JTextPane areaChat;
    private HTMLEditorKit kit;
    private HTMLDocument doc;
    private JTextField txtMensaje;
    private JLabel lblTituloChat;
    private String chatActual = "General";

    public ChatFrame() {
        ClientSocket.getInstance().setChatFrame(this);
        String miUsuario = ClientSocket.getInstance().getMyUsername();
        if (miUsuario == null) miUsuario = "Usuario";

        setTitle("JatsApp Premium - " + miUsuario);
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Layout Principal
        JSplitPane splitPane = new JSplitPane();
        splitPane.setDividerLocation(300);
        splitPane.setDividerSize(0);
        splitPane.setBorder(null);

        // ================= IZQUIERDA (CONTACTOS) =================
        JPanel panelIzquierdo = new JPanel(new BorderLayout());
        panelIzquierdo.setBackground(new Color(30, 30, 30));

        // Header Izq
        JLabel lblLogo = new JLabel("JatsApp");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setBorder(new EmptyBorder(20, 20, 20, 20));
        panelIzquierdo.add(lblLogo, BorderLayout.NORTH);

        // 1. PRIMERO CREAMOS EL MODELO (DATOS)
        modeloContactos = new DefaultListModel<>();
        modeloContactos.addElement("Chat General");
        modeloContactos.addElement("Grupo: Java Fans");
        modeloContactos.addElement("Pepe");
        modeloContactos.addElement("María");

        // 2. LUEGO CREAMOS LA LISTA (SOLO UNA VEZ)
        listaContactos = new JList<>(modeloContactos);
        listaContactos.setCellRenderer(new ContactRenderer()); // Tu renderizador pro
        listaContactos.setBackground(new Color(30, 30, 30));
        listaContactos.setBorder(null);

        // --- AQUÍ ESTÁ LA LÍNEA MÁGICA ---
        listaContactos.setFixedCellHeight(70); // Altura fija para que quepa el avatar
        // ---------------------------------

        listaContactos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String sel = listaContactos.getSelectedValue();
                if (sel != null) cambiarChat(sel);
            }
        });
        panelIzquierdo.add(new JScrollPane(listaContactos), BorderLayout.CENTER);

        // Botón de Configuración con Imagen (Ajuste de tamaño)
        JButton btnConfiguracion = new JButton(new ImageIcon(new ImageIcon(getClass().getResource("/images/setting.png")).getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
        btnConfiguracion.setBackground(null);
        btnConfiguracion.setBorder(null);
        btnConfiguracion.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConfiguracion.setToolTipText("Configuración");

        btnConfiguracion.addActionListener(e -> abrirVentanaConfiguracion());

        JPanel panelConfiguracion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelConfiguracion.setBackground(new Color(30, 30, 30));
        panelConfiguracion.add(btnConfiguracion);
        panelIzquierdo.add(panelConfiguracion, BorderLayout.SOUTH); // Agregar el botón al final del panel izquierdo

        // ================= DERECHA (CHAT) =================
        JPanel panelDerecho = new JPanel(new BorderLayout());
        panelDerecho.setBackground(new Color(20, 20, 20));

        // Header Chat
        JPanel headerChat = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerChat.setBackground(new Color(25, 25, 25));
        headerChat.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40,40,40)));
        headerChat.setPreferredSize(new Dimension(0, 70));

        lblTituloChat = new JLabel("Selecciona un chat");
        lblTituloChat.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTituloChat.setForeground(Color.WHITE);
        lblTituloChat.setBorder(new EmptyBorder(10, 20, 0, 0));
        headerChat.add(lblTituloChat);
        panelDerecho.add(headerChat, BorderLayout.NORTH);


        // Area Chat (HTML)
        areaChat = new JTextPane();
        areaChat.setEditable(false);
        areaChat.setContentType("text/html");
        areaChat.setBackground(new Color(20, 20, 20));

        kit = new HTMLEditorKit();
        doc = new HTMLDocument();
        areaChat.setEditorKit(kit);
        areaChat.setDocument(doc);

        // CSS Mejorado
        String css = "body { font-family: 'Segoe UI'; background-color: #141414; color: #ddd; padding: 20px; }"
                + ".bubble-me { background-color: #008f6d; padding: 10px 15px; margin-bottom: 8px; border-radius: 15px; text-align: right; color: white; margin-left: 80px; }"
                + ".bubble-other { background-color: #333333; padding: 10px 15px; margin-bottom: 8px; border-radius: 15px; text-align: left; color: white; margin-right: 80px; }"
                + ".sender { font-size: 11px; color: #bbb; margin-bottom: 4px; display:block; }";
        ((HTMLDocument)areaChat.getDocument()).getStyleSheet().addRule(css);

        panelDerecho.add(new JScrollPane(areaChat), BorderLayout.CENTER);

        // Input Area
        JPanel panelInput = new JPanel(new BorderLayout(15, 0));
        panelInput.setBackground(new Color(25, 25, 25));
        panelInput.setBorder(new EmptyBorder(15, 20, 15, 20));

        txtMensaje = new JTextField();
        txtMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        // Magia FlatLaf
        txtMensaje.putClientProperty("JTextField.placeholderText", "Escribe un mensaje...");
        txtMensaje.putClientProperty("JTextField.showClearButton", true);
        txtMensaje.putClientProperty("JComponent.roundRect", true);

        // Botón de Enviar con Imagen (Ajuste de tamaño)
        JButton btnEnviar = new JButton(new ImageIcon(new ImageIcon(getClass().getResource("/images/send.png")).getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
        btnEnviar.setBackground(null);
        btnEnviar.setBorder(null);
        btnEnviar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        panelInput.add(txtMensaje, BorderLayout.CENTER);
        panelInput.add(btnEnviar, BorderLayout.EAST);

        btnEnviar.addActionListener(e -> enviarMensaje());
        txtMensaje.addActionListener(e -> enviarMensaje());

        panelDerecho.add(panelInput, BorderLayout.SOUTH);

        splitPane.setLeftComponent(panelIzquierdo);
        splitPane.setRightComponent(panelDerecho);
        add(splitPane);
        setVisible(true);
    }

    private void cambiarChat(String nombre) {
        this.chatActual = nombre;
        lblTituloChat.setText(nombre);
        areaChat.setText("");
        // Reset CSS al limpiar
        ((HTMLDocument)areaChat.getDocument()).getStyleSheet().addRule("body { font-family: 'Segoe UI'; background-color: #141414; }");
    }

    private void enviarMensaje() {
        String texto = txtMensaje.getText().trim();
        if (texto.isEmpty()) return;

        Message msg = new Message();
        msg.setType(MessageType.TEXT_MESSAGE);
        msg.setSenderName(ClientSocket.getInstance().getMyUsername());
        msg.setReceiverName(chatActual);
        msg.setContent(texto);

        ClientSocket.getInstance().send(msg);
        txtMensaje.setText("");
        txtMensaje.requestFocus();
    }

    public void recibirMensaje(Message msg) {
        String user = msg.getSenderName();
        String content = msg.getContent();
        boolean soyYo = user.equals(ClientSocket.getInstance().getMyUsername());

        String html;
        if (soyYo) {
            html = "<div style='text-align: right;'><div class='bubble-me'>" + content + "</div></div>";
        } else {
            html = "<div style='text-align: left;'><div class='bubble-other'><span class='sender'>" + user + "</span>" + content + "</div></div>";
        }

        try {
            kit.insertHTML(doc, doc.getLength(), html, 0, 0, null);
            areaChat.setCaretPosition(doc.getLength());
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Método para abrir la ventana de configuración
    private void abrirVentanaConfiguracion() {
        int opcion = JOptionPane.showConfirmDialog(this, "¿Deseas cerrar sesión?", "Configuración", JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            // Lógica para cerrar sesión
            System.exit(0);
        }
    }
}