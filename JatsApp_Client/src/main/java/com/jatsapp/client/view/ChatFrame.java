package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ChatFrame extends JFrame {

    private JList<String> listaContactos;
    private DefaultListModel<String> modeloContactos;
    private JTextArea areaChat;
    private JTextField txtMensaje;
    private JButton btnEnviar;
    private JLabel lblTituloChat;

    private String chatActual = "General";

    // COLORES DE LA APP (Paleta Dark Moderno)
    private final Color COLOR_FONDO_LISTA = new Color(30, 30, 30);
    private final Color COLOR_HEADER = new Color(0, 128, 105); // Verde WhatsApp
    private final Color COLOR_FONDO_CHAT = new Color(20, 20, 20);
    private final Color COLOR_TEXTO_BLANCO = Color.WHITE;

    public ChatFrame() {
        ClientSocket.getInstance().setChatFrame(this);
        String miUsuario = ClientSocket.getInstance().getMyUsername();
        if (miUsuario == null) miUsuario = "Usuario";

        setTitle("JatsApp - " + miUsuario);
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JSplitPane splitPane = new JSplitPane();
        splitPane.setDividerLocation(280);
        splitPane.setDividerSize(2); // Divisor fino
        splitPane.setBorder(null);

        // --- IZQUIERDA: LISTA DE CONTACTOS ---
        JPanel panelIzquierdo = new JPanel(new BorderLayout());
        panelIzquierdo.setBackground(COLOR_FONDO_LISTA);

        // Título estilizado
        JLabel lblContactos = new JLabel("💬 Mis Chats");
        lblContactos.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblContactos.setForeground(new Color(200, 200, 200));
        lblContactos.setBorder(new EmptyBorder(20, 20, 20, 20));
        panelIzquierdo.add(lblContactos, BorderLayout.NORTH);

        modeloContactos = new DefaultListModel<>();
        modeloContactos.addElement("📢 Chat General");
        modeloContactos.addElement("👥 Grupo: Clase PSP");
        modeloContactos.addElement("👤 Pepe");
        modeloContactos.addElement("👤 María");

        listaContactos = new JList<>(modeloContactos);
        listaContactos.setBackground(COLOR_FONDO_LISTA);
        listaContactos.setForeground(new Color(220, 220, 220));
        listaContactos.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        listaContactos.setFixedCellHeight(50); // Celdas más altas
        listaContactos.setSelectionBackground(new Color(50, 50, 50)); // Color al seleccionar
        listaContactos.setSelectionForeground(COLOR_TEXTO_BLANCO);
        listaContactos.setBorder(new EmptyBorder(10, 10, 10, 10));

        listaContactos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String seleccionado = listaContactos.getSelectedValue();
                if (seleccionado != null) cambiarChat(seleccionado);
            }
        });

        panelIzquierdo.add(new JScrollPane(listaContactos), BorderLayout.CENTER);

        // --- DERECHA: CONVERSACIÓN ---
        JPanel panelDerecho = new JPanel(new BorderLayout());

        // Header
        JPanel headerChat = new JPanel(new BorderLayout());
        headerChat.setBackground(COLOR_HEADER);
        headerChat.setBorder(new EmptyBorder(15, 20, 15, 20));

        lblTituloChat = new JLabel("Selecciona un chat...");
        lblTituloChat.setForeground(COLOR_TEXTO_BLANCO);
        lblTituloChat.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerChat.add(lblTituloChat, BorderLayout.WEST);

        // Icono de usuario o menú a la derecha (Simulado)
        JLabel lblIcono = new JLabel("⋮");
        lblIcono.setForeground(COLOR_TEXTO_BLANCO);
        lblIcono.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerChat.add(lblIcono, BorderLayout.EAST);

        panelDerecho.add(headerChat, BorderLayout.NORTH);

        // Area Chat
        areaChat = new JTextArea();
        areaChat.setEditable(false);
        areaChat.setBackground(COLOR_FONDO_CHAT);
        areaChat.setForeground(new Color(230, 230, 230));
        areaChat.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        areaChat.setLineWrap(true);
        areaChat.setWrapStyleWord(true);
        areaChat.setBorder(new EmptyBorder(20, 20, 20, 20)); // Márgenes internos

        JScrollPane scrollChat = new JScrollPane(areaChat);
        scrollChat.setBorder(null); // Quitar bordes feos del scroll
        panelDerecho.add(scrollChat, BorderLayout.CENTER);

        // Panel Escribir
        JPanel panelEscribir = new JPanel(new BorderLayout(10, 10));
        panelEscribir.setBackground(COLOR_FONDO_LISTA);
        panelEscribir.setBorder(new EmptyBorder(15, 20, 15, 20));

        txtMensaje = new JTextField();
        txtMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        txtMensaje.putClientProperty("JTextField.placeholderText", "Escribe un mensaje aquí..."); // Propiedad de FlatLaf

        btnEnviar = new JButton("➤");
        btnEnviar.setBackground(COLOR_HEADER);
        btnEnviar.setForeground(COLOR_TEXTO_BLANCO);
        btnEnviar.setFont(new Font("Segoe UI", Font.BOLD, 20));
        btnEnviar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEnviar.setPreferredSize(new Dimension(60, 40));

        panelEscribir.add(txtMensaje, BorderLayout.CENTER);
        panelEscribir.add(btnEnviar, BorderLayout.EAST);
        panelDerecho.add(panelEscribir, BorderLayout.SOUTH);

        // Acciones
        btnEnviar.addActionListener(e -> enviarMensaje());
        txtMensaje.addActionListener(e -> enviarMensaje());

        // Unir
        splitPane.setLeftComponent(panelIzquierdo);
        splitPane.setRightComponent(panelDerecho);

        add(splitPane);
        setVisible(true);
    }

    private void cambiarChat(String nombre) {
        this.chatActual = nombre;
        lblTituloChat.setText(nombre);
        areaChat.setText("");
        // Header decorativo en el chat
        areaChat.append("------------------------------------------------\n");
        areaChat.append(" 🔒 Estás chateando con " + nombre + "\n");
        areaChat.append("------------------------------------------------\n\n");
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
        // Decoración simple: si soy yo, pongo "Tú"
        if(user.equals(ClientSocket.getInstance().getMyUsername())) {
            areaChat.append(" ⭐ Tú:\n " + msg.getContent() + "\n\n");
        } else {
            areaChat.append(" 👤 " + user + ":\n " + msg.getContent() + "\n\n");
        }
        areaChat.setCaretPosition(areaChat.getDocument().getLength());
    }
}