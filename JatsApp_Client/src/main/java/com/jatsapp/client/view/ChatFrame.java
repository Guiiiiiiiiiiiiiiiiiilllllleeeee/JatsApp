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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    // Mapa para trackear estado de mensajes: messageId -> estado visual en HTML
    private Map<Integer, String> messageSentMap = new ConcurrentHashMap<>();

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
                + ".msg-container { width: 100%; overflow: hidden; margin-bottom: 5px; clear: both; }"
                + ".bubble-me { background-color: #008f6d; padding: 8px 12px; border-radius: 12px; float: right; color: white; max-width: 60%; position: relative; }"
                + ".bubble-other { background-color: #333333; padding: 8px 12px; border-radius: 12px; float: left; color: white; max-width: 60%; position: relative; }"
                + ".sender { font-size: 10px; color: #4fc3f7; font-weight: bold; display: block; margin-bottom: 3px; }"
                + ".timestamp { font-size: 9px; color: rgba(255,255,255,0.6); display: block; text-align: right; margin-top: 4px; }"
                + ".status { font-size: 10px; margin-left: 5px; }";

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
            boolean esMio = msg.getSenderName().equals(ClientSocket.getInstance().getMyUsername());

            // Si el contacto actual está seleccionado y es de él o mío, mostrar
            if (contactoActual != null && (msg.getSenderId() == contactoActual.getId() || esMio)) {
                String contenido = msg.getContent();
                if (msg.getType() == MessageType.FILE_MESSAGE || msg.getType() == MessageType.ARCHIVO) {
                    contenido = "📎 Archivo: " + msg.getFileName();
                }

                // Si es mensaje mío del historial, mostrar con estado
                if (esMio) {
                    agregarBurbuja(msg.getSenderName(), contenido, true, msg.getMessageId(), msg.isDelivered(), msg.isRead());
                } else {
                    agregarBurbuja(msg.getSenderName(), contenido, false);

                    // Enviar confirmación de lectura al servidor si el chat está abierto
                    if (msg.getMessageId() > 0 && !msg.isRead()) {
                        enviarConfirmacionLectura(msg.getMessageId());
                    }
                }
            }
            // Si NO es mío y NO es del contacto actual, es un mensaje nuevo de otra persona
            else if (!esMio && (contactoActual == null || msg.getSenderId() != contactoActual.getId())) {
                // Verificar si el emisor ya está en la lista de contactos
                boolean existeEnLista = false;
                for (int i = 0; i < modeloContactos.size(); i++) {
                    if (modeloContactos.get(i).getId() == msg.getSenderId()) {
                        existeEnLista = true;
                        break;
                    }
                }

                // Si no existe en la lista, añadirlo temporalmente
                if (!existeEnLista) {
                    User nuevoUsuario = new User(msg.getSenderId(), msg.getSenderName(), "activo");
                    modeloContactos.addElement(nuevoUsuario);
                }

                // Mostrar notificación de mensaje nuevo
                mostrarNotificacionMensaje(msg.getSenderName());
            }
        });
    }

    /**
     * Envía confirmación de lectura al servidor
     */
    private void enviarConfirmacionLectura(int messageId) {
        Message readConfirmation = new Message(MessageType.MESSAGE_READ, "");
        readConfirmation.setMessageId(messageId);
        ClientSocket.getInstance().send(readConfirmation);
    }

    /**
     * Muestra una notificación visual de que hay un mensaje nuevo
     */
    private void mostrarNotificacionMensaje(String remitente) {
        // Pequeña notificación en la barra de título
        String tituloActual = getTitle();
        if (!tituloActual.contains("📩")) {
            setTitle("📩 " + tituloActual + " - Mensaje de " + remitente);
        }

        // También podemos hacer que parpadee o emita un sonido
        // Por ahora solo cambiamos el título
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    // Método auxiliar para pintar HTML
    private void agregarBurbuja(String usuario, String texto, boolean esMio) {
        agregarBurbuja(usuario, texto, esMio, 0, false, false);
    }

    // Versión completa con estado de mensaje
    private void agregarBurbuja(String usuario, String texto, boolean esMio, int messageId, boolean delivered, boolean read) {
        // Obtener hora actual en formato HH:mm
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

        String html;
        if (esMio) {
            // Mensaje mío: mostrar "Tú" + indicador de estado + hora
            String statusIcon = getStatusIcon(delivered, read);
            String statusId = messageId > 0 ? "msg-status-" + messageId : "";
            html = "<div class='msg-container'><div class='bubble-me'>" +
                   "<span class='sender'>Tú</span>" +
                   texto +
                   "<span class='timestamp'>" + timestamp + " <span class='status' id='" + statusId + "'>" + statusIcon + "</span></span>" +
                   "</div></div>";

            // Guardar en el mapa para futuras actualizaciones
            if (messageId > 0) {
                messageSentMap.put(messageId, statusId);
            }
        } else {
            // Mensaje de otro: mostrar nombre + hora
            html = "<div class='msg-container'><div class='bubble-other'>" +
                   "<span class='sender'>" + usuario + "</span>" +
                   texto +
                   "<span class='timestamp'>" + timestamp + "</span>" +
                   "</div></div>";
        }
        try {
            kit.insertHTML(doc, doc.getLength(), html, 0, 0, null);
            areaChat.setCaretPosition(doc.getLength()); // Auto-scroll al fondo
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Obtiene el icono de estado según el estado del mensaje
     */
    private String getStatusIcon(boolean delivered, boolean read) {
        if (read) {
            return "<span style='color: #4FC3F7;'>✓✓</span>"; // Azul: leído
        } else if (delivered) {
            return "<span style='color: #999;'>✓✓</span>"; // Gris: entregado
        } else {
            return "<span style='color: #999;'>✓</span>"; // Gris: enviado
        }
    }

    /**
     * Actualiza el estado visual de un mensaje en el chat
     */
    public void actualizarEstadoMensaje(int messageId, boolean delivered, boolean read) {
        SwingUtilities.invokeLater(() -> {
            String statusId = messageSentMap.get(messageId);
            if (statusId != null) {
                try {
                    // Buscar y actualizar el elemento en el HTML
                    String htmlContent = areaChat.getText();
                    String statusIcon = getStatusIcon(delivered, read);
                    String newContent = htmlContent.replaceAll(
                        "id='" + statusId + "'>.*?</span>",
                        "id='" + statusId + "'>" + statusIcon + "</span>"
                    );

                    if (!newContent.equals(htmlContent)) {
                        areaChat.setText(newContent);
                        areaChat.setCaretPosition(doc.getLength());
                    }
                } catch (Exception e) {
                    System.err.println("Error actualizando estado del mensaje: " + e.getMessage());
                }
            }
        });
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

    // =================================================================
    // NUEVO CHAT DE USUARIO DESCONOCIDO
    // =================================================================

    /**
     * Llamado cuando recibimos un mensaje de alguien que no tenemos como contacto.
     * Muestra una notificación y pregunta si queremos aceptar el chat.
     */
    public void onNuevoChatDesconocido(int senderId, String senderName) {
        SwingUtilities.invokeLater(() -> {
            // Mostrar notificación al usuario
            int opcion = JOptionPane.showConfirmDialog(
                this,
                "📬 " + senderName + " te ha enviado un mensaje.\n\n¿Deseas aceptar el chat y añadirlo a tus contactos?",
                "Nuevo mensaje de " + senderName,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );

            if (opcion == JOptionPane.YES_OPTION) {
                // Aceptar el chat - enviar mensaje al servidor
                aceptarChat(senderId);

                // Actualizar lista de contactos
                pedirListaContactos();

                // Crear usuario temporal y abrir el chat
                User nuevoContacto = new User(senderId, senderName, "activo");

                // Añadir a la lista de contactos localmente si no está
                boolean yaExiste = false;
                for (int i = 0; i < modeloContactos.size(); i++) {
                    if (modeloContactos.get(i).getId() == senderId) {
                        yaExiste = true;
                        break;
                    }
                }
                if (!yaExiste) {
                    modeloContactos.addElement(nuevoContacto);
                }

                // Abrir el chat con el nuevo contacto
                cambiarChat(nuevoContacto);
            }
        });
    }

    /**
     * Envía al servidor la aceptación del chat (añade como contacto)
     */
    private void aceptarChat(int senderId) {
        Message msg = new Message();
        msg.setType(MessageType.ACCEPT_CHAT);
        msg.setSenderId(senderId); // El ID del usuario que nos escribió
        ClientSocket.getInstance().send(msg);
    }

    /**
     * Muestra los resultados de búsqueda de usuarios para enviar mensajes
     */
    public void mostrarResultadosBusqueda(List<User> usuarios) {
        SwingUtilities.invokeLater(() -> {
            if (usuarios == null || usuarios.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No se encontraron usuarios con ese nombre.",
                    "Sin resultados",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Mostrar diálogo con los resultados
            String[] opciones = new String[usuarios.size()];
            for (int i = 0; i < usuarios.size(); i++) {
                User u = usuarios.get(i);
                opciones[i] = u.getUsername() + " (" + u.getActivityStatus() + ")";
            }

            String seleccion = (String) JOptionPane.showInputDialog(
                this,
                "Selecciona un usuario para iniciar chat:",
                "Resultados de búsqueda",
                JOptionPane.PLAIN_MESSAGE,
                null,
                opciones,
                opciones[0]
            );

            if (seleccion != null) {
                // Encontrar el usuario seleccionado
                int index = -1;
                for (int i = 0; i < opciones.length; i++) {
                    if (opciones[i].equals(seleccion)) {
                        index = i;
                        break;
                    }
                }

                if (index >= 0) {
                    User usuarioSeleccionado = usuarios.get(index);

                    // Añadir a lista de contactos si no existe
                    boolean yaExiste = false;
                    for (int i = 0; i < modeloContactos.size(); i++) {
                        if (modeloContactos.get(i).getId() == usuarioSeleccionado.getId()) {
                            yaExiste = true;
                            break;
                        }
                    }
                    if (!yaExiste) {
                        modeloContactos.addElement(usuarioSeleccionado);
                    }

                    // Abrir el chat con ese usuario
                    cambiarChat(usuarioSeleccionado);
                }
            }
        });
    }

    /**
     * Buscar usuarios para enviar mensajes (no necesitan ser contactos)
     */
    public void buscarUsuarios(String termino) {
        Message msg = new Message();
        msg.setType(MessageType.SEARCH_USER);
        msg.setContent(termino);
        ClientSocket.getInstance().send(msg);
    }
}