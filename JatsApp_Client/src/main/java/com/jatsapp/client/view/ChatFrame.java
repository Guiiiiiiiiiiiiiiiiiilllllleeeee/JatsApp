package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;
import com.jatsapp.common.User; // IMPORTANTE

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.List;

public class ChatFrame extends JFrame {

    // CAMBIO: Usamos User, no String
    private JList<User> listaContactos;
    private DefaultListModel<User> modeloContactos;

    private JTextPane areaChat;
    private HTMLEditorKit kit;
    private HTMLDocument doc;
    private JTextField txtMensaje;
    private JLabel lblTituloChat;

    // Guardamos el OBJETO User con el que hablamos, no solo el nombre
    private User contactoActual = null;

    public ChatFrame() {
        // Registramos esta ventana en el Socket
        ClientSocket.getInstance().setChatFrame(this);

        String miUsuario = ClientSocket.getInstance().getMyUsername();
        setTitle("JatsApp Premium - " + (miUsuario != null ? miUsuario : "Sin Conexión"));
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Layout Principal
        JSplitPane splitPane = new JSplitPane();
        splitPane.setDividerLocation(300);
        splitPane.setDividerSize(2); // Un borde fino queda mejor que 0
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

        // 1. MODELO DE DATOS (User)
        modeloContactos = new DefaultListModel<>();

        // 2. LISTA
        listaContactos = new JList<>(modeloContactos);
        listaContactos.setCellRenderer(new ContactRenderer()); // Usa tu renderer visual
        listaContactos.setBackground(new Color(30, 30, 30));
        listaContactos.setBorder(null);
        listaContactos.setFixedCellHeight(70);

        // Evento Click
        listaContactos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                User sel = listaContactos.getSelectedValue();
                if (sel != null) cambiarChat(sel);
            }
        });
        panelIzquierdo.add(new JScrollPane(listaContactos), BorderLayout.CENTER);

        // Botón Configuración (Protegido contra falta de imagen)
        JButton btnConfiguracion = crearBotonImagen("/images/setting.png", "⚙");
        btnConfiguracion.addActionListener(e -> abrirVentanaConfiguracion());

        JPanel panelConfiguracion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelConfiguracion.setBackground(new Color(30, 30, 30));
        panelConfiguracion.add(btnConfiguracion);
        panelIzquierdo.add(panelConfiguracion, BorderLayout.SOUTH);

        // ================= DERECHA (CHAT) =================
        JPanel panelDerecho = new JPanel(new BorderLayout());
        panelDerecho.setBackground(new Color(20, 20, 20));

        // Header Chat
        JPanel headerChat = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerChat.setBackground(new Color(25, 25, 25));
        headerChat.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40,40,40)));
        headerChat.setPreferredSize(new Dimension(0, 70));

        lblTituloChat = new JLabel("Selecciona un contacto");
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

        // CSS: Añadimos font-family global para evitar Times New Roman
        String css = "body { font-family: 'Segoe UI', sans-serif; background-color: #141414; color: #ddd; padding: 20px; }"
                + ".bubble-me { background-color: #008f6d; padding: 10px 15px; margin-bottom: 8px; border-radius: 15px; text-align: right; color: white; float: right; clear: both; }"
                + ".bubble-other { background-color: #333333; padding: 10px 15px; margin-bottom: 8px; border-radius: 15px; text-align: left; color: white; float: left; clear: both; }"
                + ".sender { font-size: 10px; color: #bbb; margin-bottom: 2px; display:block; }"
                + ".msg-container { width: 100%; overflow: hidden; }"; // Contenedor para limpiar floats

        try {
            ((HTMLDocument)areaChat.getDocument()).getStyleSheet().addRule(css);
        } catch (Exception e) { e.printStackTrace(); }

        panelDerecho.add(new JScrollPane(areaChat), BorderLayout.CENTER);

        // Input Area
        JPanel panelInput = new JPanel(new BorderLayout(15, 0));
        panelInput.setBackground(new Color(25, 25, 25));
        panelInput.setBorder(new EmptyBorder(15, 20, 15, 20));

        txtMensaje = new JTextField();
        txtMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        txtMensaje.putClientProperty("JTextField.placeholderText", "Escribe un mensaje...");
        txtMensaje.putClientProperty("JComponent.roundRect", true);

        // Botón Enviar (Protegido)
        JButton btnEnviar = crearBotonImagen("/images/send.png", "➤");

        panelInput.add(txtMensaje, BorderLayout.CENTER);
        panelInput.add(btnEnviar, BorderLayout.EAST);

        // Eventos de envío
        btnEnviar.addActionListener(e -> enviarMensaje());
        txtMensaje.addActionListener(e -> enviarMensaje());

        panelDerecho.add(panelInput, BorderLayout.SOUTH);

        splitPane.setLeftComponent(panelIzquierdo);
        splitPane.setRightComponent(panelDerecho);
        add(splitPane);

        // 3. PEDIR CONTACTOS AL SERVIDOR AL INICIAR
        pedirListaContactos();

        setVisible(true);
    }

    // --- MÉTODOS DE LÓGICA ---

    private void pedirListaContactos() {
        Message msg = new Message();
        msg.setType(MessageType.GET_CONTACTS);
        ClientSocket.getInstance().send(msg);
    }

    // Método llamado por ClientSocket cuando llega la lista (LIST_CONTACTS)
    public void actualizarContactos(List<User> contactos) {
        SwingUtilities.invokeLater(() -> {
            modeloContactos.clear();
            String myName = ClientSocket.getInstance().getMyUsername();

            for (User u : contactos) {
                // No mostrarme a mí mismo
                if (myName == null || !myName.equals(u.getUsername())) {
                    modeloContactos.addElement(u);
                }
            }
        });
    }

    private void cambiarChat(User usuario) {
        this.contactoActual = usuario;
        lblTituloChat.setText(usuario.getUsername());
        areaChat.setText(""); // Limpiar chat
        txtMensaje.requestFocus();

        // Pedir historial (HISTORICAL_REQUEST)
        Message msg = new Message();
        msg.setType(MessageType.GET_HISTORY);
        msg.setReceiverId(usuario.getId());
        msg.setGroupChat(false); // Asumimos chat individual por ahora
        ClientSocket.getInstance().send(msg);
    }

    private void enviarMensaje() {
        String texto = txtMensaje.getText().trim();
        if (texto.isEmpty() || contactoActual == null) return;

        Message msg = new Message();
        msg.setType(MessageType.TEXT_MESSAGE);

        // DATOS CRÍTICOS PARA EL SERVIDOR
        msg.setSenderName(ClientSocket.getInstance().getMyUsername());
        msg.setReceiverId(contactoActual.getId()); // Usamos ID, no nombre
        msg.setGroupChat(false);
        msg.setContent(texto);

        ClientSocket.getInstance().send(msg);

        // Limpiamos input pero NO añadimos el mensaje manualmente aquí.
        // Esperamos a que el servidor nos lo devuelva o lo añadimos directamente:
        mostrarMensajeEnPantalla(msg);

        txtMensaje.setText("");
        txtMensaje.requestFocus();
    }

    // Método llamado por ClientSocket para mostrar mensaje entrante o historial
    public void recibirMensaje(Message msg) {
        SwingUtilities.invokeLater(() -> mostrarMensajeEnPantalla(msg));
    }

    // Método para cargar historial completo
    public void cargarHistorial(List<Message> history) {
        SwingUtilities.invokeLater(() -> {
            areaChat.setText("");
            for (Message msg : history) {
                mostrarMensajeEnPantalla(msg);
            }
        });
    }

    private void mostrarMensajeEnPantalla(Message msg) {
        String user = msg.getSenderName();
        String content = msg.getContent();

        String myName = ClientSocket.getInstance().getMyUsername();
        boolean soyYo = (user != null && user.equals(myName));

        String html;
        if (soyYo) {
            html = "<div class='msg-container'><div class='bubble-me'>" + content + "</div></div>";
        } else {
            html = "<div class='msg-container'><div class='bubble-other'><span class='sender'>" + user + "</span>" + content + "</div></div>";
        }

        try {
            kit.insertHTML(doc, doc.getLength(), html, 0, 0, null);
            areaChat.setCaretPosition(doc.getLength());
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void abrirVentanaConfiguracion() {
        int opcion = JOptionPane.showConfirmDialog(this, "¿Cerrar sesión y salir?", "Salir", JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    // Helper para cargar imágenes con seguridad
    private JButton crearBotonImagen(String path, String textoAlternativo) {
        JButton btn = new JButton();
        URL url = getClass().getResource(path);

        if (url != null) {
            ImageIcon icon = new ImageIcon(new ImageIcon(url).getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
            btn.setIcon(icon);
        } else {
            btn.setText(textoAlternativo); // Si no hay imagen, pone texto
            btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btn.setForeground(Color.WHITE);
        }

        btn.setBackground(null);
        btn.setBorder(null);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}