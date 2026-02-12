package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;
import com.jatsapp.common.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

public class ChatFrame extends JFrame {

    // Componentes principales
    private JList<User> listaContactos;
    private DefaultListModel<User> modeloContactos;
    private JTextPane areaChat;
    private HTMLEditorKit kit;
    private HTMLDocument doc;
    private JTextField txtMensaje;
    private JLabel lblTituloChat;

    // Contacto con el que estamos hablando actualmente
    private User contactoActual = null;

    public ChatFrame() {
        // 1. Vincular esta ventana al Socket para recibir mensajes
        ClientSocket.getInstance().setChatFrame(this);

        // 2. Configuración de la Ventana
        String miUsuario = ClientSocket.getInstance().getMyUsername();
        setTitle("JatsApp - " + (miUsuario != null ? miUsuario : "Desconectado"));
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Layout Principal: Un panel dividido (Izquierda: Lista, Derecha: Chat)
        JSplitPane splitPane = new JSplitPane();
        splitPane.setDividerLocation(280); // Ancho del panel de contactos
        splitPane.setDividerSize(2); // Borde fino
        splitPane.setBorder(null);

        // ==========================================
        // PANEL IZQUIERDO (Contactos y Botones)
        // ==========================================
        JPanel panelIzquierdo = new JPanel(new BorderLayout());
        panelIzquierdo.setBackground(new Color(30, 30, 30));

        // -- Cabecera Izquierda (Logo) --
        JLabel lblLogo = new JLabel("JatsApp");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setBorder(new EmptyBorder(20, 20, 20, 20));
        panelIzquierdo.add(lblLogo, BorderLayout.NORTH);

        // -- Lista de Contactos --
        modeloContactos = new DefaultListModel<>();
        listaContactos = new JList<>(modeloContactos);
        listaContactos.setCellRenderer(new ContactRenderer()); // Tu renderizador personalizado
        listaContactos.setBackground(new Color(30, 30, 30));
        listaContactos.setBorder(null);
        listaContactos.setFixedCellHeight(70); // Altura para que quepa el avatar

        // Evento: Clic en un contacto
        listaContactos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                User seleccion = listaContactos.getSelectedValue();
                if (seleccion != null) {
                    cambiarChat(seleccion);
                }
            }
        });
        panelIzquierdo.add(new JScrollPane(listaContactos), BorderLayout.CENTER);

        // -- Panel de Botones Inferior (Configuración y Añadir) --
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panelBotones.setBackground(new Color(25, 25, 25));

        // Botón Configuración (⚙)
        JButton btnConfig = crearBotonImagen("/images/setting.png", "⚙");
        btnConfig.setToolTipText("Cerrar Sesión");
        btnConfig.addActionListener(e -> cerrarSesion());

        // Botón Añadir Contacto (+)
        JButton btnAdd = new JButton("+");
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 24));
        btnAdd.setForeground(new Color(0, 200, 150)); // Verde JatsApp
        btnAdd.setBackground(null);
        btnAdd.setBorder(null);
        btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdd.setToolTipText("Añadir nuevo contacto");
        btnAdd.addActionListener(e -> accionAnadirContacto());

        panelBotones.add(btnConfig);
        panelBotones.add(btnAdd);

        panelIzquierdo.add(panelBotones, BorderLayout.SOUTH);

        // ==========================================
        // PANEL DERECHO (Chat)
        // ==========================================
        JPanel panelDerecho = new JPanel(new BorderLayout());
        panelDerecho.setBackground(new Color(20, 20, 20));

        // -- Cabecera del Chat --
        JPanel headerChat = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerChat.setBackground(new Color(25, 25, 25));
        headerChat.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 40, 40)));
        headerChat.setPreferredSize(new Dimension(0, 60));

        lblTituloChat = new JLabel("Selecciona un contacto");
        lblTituloChat.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTituloChat.setForeground(Color.WHITE);
        lblTituloChat.setBorder(new EmptyBorder(10, 20, 0, 0));
        headerChat.add(lblTituloChat);
        panelDerecho.add(headerChat, BorderLayout.NORTH);

        // -- Área de Mensajes (HTML) --
        areaChat = new JTextPane();
        areaChat.setEditable(false);
        areaChat.setContentType("text/html");
        areaChat.setBackground(new Color(20, 20, 20));

        kit = new HTMLEditorKit();
        doc = new HTMLDocument();
        areaChat.setEditorKit(kit);
        areaChat.setDocument(doc);

        // CSS para las burbujas
        String css = "body { font-family: 'Segoe UI', sans-serif; background-color: #141414; color: #ddd; padding: 10px; }"
                + ".msg-container { width: 100%; overflow: hidden; margin-bottom: 5px; }"
                + ".bubble-me { background-color: #008f6d; padding: 8px 12px; border-radius: 12px; float: right; color: white; }"
                + ".bubble-other { background-color: #333333; padding: 8px 12px; border-radius: 12px; float: left; color: white; }"
                + ".sender { font-size: 9px; color: #aaa; display: block; margin-bottom: 2px; }";

        try {
            ((HTMLDocument) areaChat.getDocument()).getStyleSheet().addRule(css);
        } catch (Exception e) { e.printStackTrace(); }

        panelDerecho.add(new JScrollPane(areaChat), BorderLayout.CENTER);

        // -- Área de Input (Escribir) --
        JPanel panelInput = new JPanel(new BorderLayout(10, 0));
        panelInput.setBackground(new Color(25, 25, 25));
        panelInput.setBorder(new EmptyBorder(10, 15, 10, 15));

        // Botón Archivo
        JButton btnArchivo = new JButton("📎");
        btnArchivo.setBackground(new Color(40,40,40));
        btnArchivo.setForeground(Color.WHITE);
        btnArchivo.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btnArchivo.addActionListener(e -> enviarArchivo());

        txtMensaje = new JTextField();
        txtMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtMensaje.putClientProperty("JTextField.placeholderText", "Escribe un mensaje...");
        txtMensaje.putClientProperty("JComponent.roundRect", true);

        // Al pulsar Enter, enviar
        txtMensaje.addActionListener(e -> enviarMensajeTexto());

        // Botón Enviar
        JButton btnEnviar = crearBotonImagen("/images/send.png", "➤");
        btnEnviar.addActionListener(e -> enviarMensajeTexto());

        panelInput.add(btnArchivo, BorderLayout.WEST);
        panelInput.add(txtMensaje, BorderLayout.CENTER);
        panelInput.add(btnEnviar, BorderLayout.EAST);

        panelDerecho.add(panelInput, BorderLayout.SOUTH);

        // ==========================================
        // FINALIZAR
        // ==========================================
        splitPane.setLeftComponent(panelIzquierdo);
        splitPane.setRightComponent(panelDerecho);
        add(splitPane);

        // Pedir contactos al iniciar
        pedirListaContactos();

        setVisible(true);
    }

    // =================================================================
    // LÓGICA DE NEGOCIO
    // =================================================================

    private void pedirListaContactos() {
        Message msg = new Message();
        msg.setType(MessageType.GET_CONTACTS);
        ClientSocket.getInstance().send(msg);
    }

    private void cambiarChat(User usuario) {
        this.contactoActual = usuario;
        lblTituloChat.setText("Chat con: " + usuario.getUsername());
        areaChat.setText(""); // Limpiar chat visualmente
        txtMensaje.requestFocus();

        // Pedir historial al servidor
        Message msg = new Message();
        msg.setType(MessageType.GET_HISTORY);
        msg.setReceiverId(usuario.getId());
        msg.setGroupChat(false); // Por ahora individual
        ClientSocket.getInstance().send(msg);
    }

    // --- NUEVA FUNCIÓN: AÑADIR CONTACTO ---
    private void accionAnadirContacto() {
        String nombre = JOptionPane.showInputDialog(this,
                "Escribe el nombre de usuario de tu amigo:",
                "Añadir Contacto",
                JOptionPane.PLAIN_MESSAGE);

        if (nombre != null && !nombre.trim().isEmpty()) {
            Message msg = new Message();
            msg.setType(MessageType.ADD_CONTACT); // Asegúrate de tener esto en tu Enum
            msg.setContent(nombre.trim());
            msg.setSenderName(ClientSocket.getInstance().getMyUsername());

            ClientSocket.getInstance().send(msg);
        }
    }

    private void cerrarSesion() {
        int opt = JOptionPane.showConfirmDialog(this, "¿Seguro que quieres salir?", "Salir", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    // =================================================================
    // ENVÍO DE MENSAJES
    // =================================================================

    private void enviarMensajeTexto() {
        String texto = txtMensaje.getText().trim();
        if (texto.isEmpty() || contactoActual == null) return;

        Message msg = new Message();
        msg.setType(MessageType.TEXT_MESSAGE);
        msg.setSenderName(ClientSocket.getInstance().getMyUsername());
        msg.setReceiverId(contactoActual.getId()); // Usamos ID
        msg.setGroupChat(false);
        msg.setContent(texto);

        ClientSocket.getInstance().send(msg);

        // Feedback inmediato (lo mostramos nosotros mismos)
        agregarBurbuja(ClientSocket.getInstance().getMyUsername(), texto, true);

        txtMensaje.setText("");
        txtMensaje.requestFocus();
    }

    private void enviarArchivo() {
        if (contactoActual == null) return;

        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.length() > 10 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "Archivo muy grande (>10MB)");
                return;
            }
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                Message msg = new Message();
                msg.setType(MessageType.FILE_MESSAGE);
                msg.setSenderName(ClientSocket.getInstance().getMyUsername());
                msg.setReceiverId(contactoActual.getId());
                msg.setFileName(file.getName());
                msg.setFileData(bytes);
                msg.setGroupChat(false);

                ClientSocket.getInstance().send(msg);
                agregarBurbuja(ClientSocket.getInstance().getMyUsername(), "📎 Archivo enviado: " + file.getName(), true);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error leyendo archivo.");
            }
        }
    }

    // =================================================================
    // MÉTODOS QUE LLAMA EL CLIENTSOCKET (RESPUESTAS DEL SERVIDOR)
    // =================================================================

    public void actualizarContactos(List<User> contactos) {
        SwingUtilities.invokeLater(() -> {
            modeloContactos.clear();
            String myName = ClientSocket.getInstance().getMyUsername();
            for (User u : contactos) {
                // No mostrarme a mí mismo
                if (myName != null && !myName.equals(u.getUsername())) {
                    modeloContactos.addElement(u);
                }
            }
        });
    }

    public void cargarHistorial(List<Message> historial) {
        SwingUtilities.invokeLater(() -> {
            areaChat.setText(""); // Limpiar
            for (Message m : historial) {
                recibirMensaje(m);
            }
        });
    }

    public void recibirMensaje(Message msg) {
        SwingUtilities.invokeLater(() -> {
            // Solo mostramos el mensaje si pertenece al chat abierto actualmente
            // O si es del historial
            boolean esMio = msg.getSenderName().equals(ClientSocket.getInstance().getMyUsername());

            // Si el mensaje es de la persona con la que hablo, O si es mio (historial)
            if (contactoActual != null && (msg.getSenderId() == contactoActual.getId() || esMio)) {

                String contenido = msg.getContent();
                if (msg.getType() == MessageType.FILE_MESSAGE || msg.getType() == MessageType.ARCHIVO) {
                    contenido = "📎 Archivo: " + msg.getFileName();
                }

                agregarBurbuja(msg.getSenderName(), contenido, esMio);
            }
        });
    }

    // Método auxiliar para pintar HTML
    private void agregarBurbuja(String usuario, String texto, boolean esMio) {
        String html;
        if (esMio) {
            html = "<div class='msg-container'><div class='bubble-me'>" + texto + "</div></div>";
        } else {
            html = "<div class='msg-container'><div class='bubble-other'><span class='sender'>" + usuario + "</span>" + texto + "</div></div>";
        }
        try {
            kit.insertHTML(doc, doc.getLength(), html, 0, 0, null);
            areaChat.setCaretPosition(doc.getLength()); // Auto-scroll al fondo
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Helper para cargar imágenes sin que explote si faltan
    private JButton crearBotonImagen(String path, String textoAlt) {
        JButton btn = new JButton();
        URL url = getClass().getResource(path);
        if (url != null) {
            ImageIcon icon = new ImageIcon(new ImageIcon(url).getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
            btn.setIcon(icon);
        } else {
            btn.setText(textoAlt);
            btn.setForeground(Color.WHITE);
        }
        btn.setBackground(null);
        btn.setBorder(null);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}