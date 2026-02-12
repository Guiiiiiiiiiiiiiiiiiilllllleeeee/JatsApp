package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.client.util.EncryptionUtil;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;
import com.jatsapp.common.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    // Mapa para almacenar mensajes de archivo: messageId -> Message
    private Map<Integer, Message> fileMessagesMap = new ConcurrentHashMap<>();

    // Lista de mensajes del chat actual (para búsqueda)
    private java.util.ArrayList<Message> mensajesActuales = new java.util.ArrayList<>();

    // Panel de búsqueda dentro del chat
    private JPanel panelBusqueda;
    private JTextField txtBusqueda;
    private JLabel lblResultadoBusqueda;
    private int indiceResultadoActual = -1;
    private java.util.ArrayList<Integer> indicesResultados = new java.util.ArrayList<>();

    // Panel de búsqueda global (en panel izquierdo)
    private JPanel panelBusquedaGlobal;
    private JTextField txtBusquedaGlobal;

    private JButton btnContactos;

    // Nueva referencia a ContactsFrame
    private ContactsFrame contactsFrame;

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

        // -- Cabecera Izquierda (Logo + Búsqueda Global) --
        JPanel panelCabeceraIzq = new JPanel(new BorderLayout());
        panelCabeceraIzq.setBackground(new Color(30, 30, 30));

        // Panel superior con Logo y botón de búsqueda
        JPanel panelLogoYBusqueda = new JPanel(new BorderLayout());
        panelLogoYBusqueda.setBackground(new Color(30, 30, 30));

        JLabel lblLogo = new JLabel("JatsApp");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setBorder(new EmptyBorder(20, 20, 10, 10));
        panelLogoYBusqueda.add(lblLogo, BorderLayout.CENTER);

        // Botón de búsqueda global
        JButton btnBusquedaGlobal = crearBotonImagen("/images/research.png", "🔍");
        btnBusquedaGlobal.setToolTipText("Buscar en todos los chats");
        btnBusquedaGlobal.setBorder(new EmptyBorder(15, 5, 5, 15));
        btnBusquedaGlobal.addActionListener(e -> toggleBusquedaGlobal());
        panelLogoYBusqueda.add(btnBusquedaGlobal, BorderLayout.EAST);

        panelCabeceraIzq.add(panelLogoYBusqueda, BorderLayout.NORTH);

        // Panel de búsqueda global (oculto por defecto)
        panelBusquedaGlobal = new JPanel(new BorderLayout(5, 0));
        panelBusquedaGlobal.setBackground(new Color(35, 35, 35));
        panelBusquedaGlobal.setBorder(BorderFactory.createEmptyBorder(5, 15, 10, 15));
        panelBusquedaGlobal.setVisible(false);

        txtBusquedaGlobal = new JTextField();
        txtBusquedaGlobal.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtBusquedaGlobal.setBackground(new Color(50, 50, 50));
        txtBusquedaGlobal.setForeground(Color.WHITE);
        txtBusquedaGlobal.setCaretColor(Color.WHITE);
        txtBusquedaGlobal.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 70)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        txtBusquedaGlobal.putClientProperty("JTextField.placeholderText", "Buscar mensajes...");
        txtBusquedaGlobal.addActionListener(e -> ejecutarBusquedaGlobal());
        txtBusquedaGlobal.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    cerrarBusquedaGlobal();
                }
            }
        });

        JButton btnCerrarBusquedaGlobal = new JButton("✕");
        btnCerrarBusquedaGlobal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCerrarBusquedaGlobal.setBackground(null);
        btnCerrarBusquedaGlobal.setForeground(Color.WHITE);
        btnCerrarBusquedaGlobal.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btnCerrarBusquedaGlobal.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCerrarBusquedaGlobal.setToolTipText("Cerrar búsqueda");
        btnCerrarBusquedaGlobal.addActionListener(e -> cerrarBusquedaGlobal());

        panelBusquedaGlobal.add(txtBusquedaGlobal, BorderLayout.CENTER);
        panelBusquedaGlobal.add(btnCerrarBusquedaGlobal, BorderLayout.EAST);

        panelCabeceraIzq.add(panelBusquedaGlobal, BorderLayout.CENTER);

        panelIzquierdo.add(panelCabeceraIzq, BorderLayout.NORTH);

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
        btnAdd.setToolTipText("Nuevo chat o añadir a favoritos");
        btnAdd.addActionListener(e -> accionAnadirContacto());

        btnContactos = new JButton("Contactos");
        btnContactos.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Siempre crear una instancia nueva para evitar problemas
                if (contactsFrame != null && contactsFrame.isVisible()) {
                    contactsFrame.toFront(); // Si ya está abierta, traerla al frente
                } else {
                    contactsFrame = new ContactsFrame();
                    contactsFrame.setVisible(true);
                }
            }
        });
        panelBotones.add(btnConfig);
        panelBotones.add(btnAdd);
        panelBotones.add(btnContactos);

        panelIzquierdo.add(panelBotones, BorderLayout.SOUTH);

        // ==========================================
        // PANEL DERECHO (Chat)
        // ==========================================
        JPanel panelDerecho = new JPanel(new BorderLayout());
        panelDerecho.setBackground(new Color(20, 20, 20));

        // -- Panel superior (cabecera + búsqueda) --
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBackground(new Color(25, 25, 25));

        // -- Cabecera del Chat --
        JPanel headerChat = new JPanel(new BorderLayout());
        headerChat.setBackground(new Color(25, 25, 25));
        headerChat.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 40, 40)));
        headerChat.setPreferredSize(new Dimension(0, 60));

        lblTituloChat = new JLabel("Selecciona un contacto");
        lblTituloChat.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTituloChat.setForeground(Color.WHITE);
        lblTituloChat.setBorder(new EmptyBorder(10, 20, 0, 0));
        headerChat.add(lblTituloChat, BorderLayout.CENTER);

        // Botón de búsqueda
        JButton btnBuscar = new JButton("🔍");
        btnBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        btnBuscar.setBackground(null);
        btnBuscar.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        btnBuscar.setForeground(Color.WHITE);
        btnBuscar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBuscar.setToolTipText("Buscar mensajes (Ctrl+F)");
        btnBuscar.addActionListener(e -> togglePanelBusqueda());
        headerChat.add(btnBuscar, BorderLayout.EAST);

        panelSuperior.add(headerChat, BorderLayout.NORTH);

        // -- Panel de Búsqueda (oculto por defecto) --
        panelBusqueda = new JPanel(new BorderLayout(5, 0));
        panelBusqueda.setBackground(new Color(35, 35, 35));
        panelBusqueda.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 40, 40)),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        panelBusqueda.setVisible(false);

        txtBusqueda = new JTextField();
        txtBusqueda.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtBusqueda.setBackground(new Color(50, 50, 50));
        txtBusqueda.setForeground(Color.WHITE);
        txtBusqueda.setCaretColor(Color.WHITE);
        txtBusqueda.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 70)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        txtBusqueda.addActionListener(e -> buscarSiguiente());
        txtBusqueda.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    cerrarBusqueda();
                } else {
                    buscarMensajes();
                }
            }
        });

        JPanel panelBotonesBusqueda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        panelBotonesBusqueda.setBackground(new Color(35, 35, 35));

        lblResultadoBusqueda = new JLabel("0/0");
        lblResultadoBusqueda.setForeground(new Color(150, 150, 150));
        lblResultadoBusqueda.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton btnAnterior = new JButton("▲");
        btnAnterior.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnAnterior.setBackground(new Color(60, 60, 60));
        btnAnterior.setForeground(Color.WHITE);
        btnAnterior.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btnAnterior.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAnterior.setToolTipText("Resultado anterior");
        btnAnterior.addActionListener(e -> buscarAnterior());

        JButton btnSiguiente = new JButton("▼");
        btnSiguiente.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnSiguiente.setBackground(new Color(60, 60, 60));
        btnSiguiente.setForeground(Color.WHITE);
        btnSiguiente.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btnSiguiente.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSiguiente.setToolTipText("Resultado siguiente");
        btnSiguiente.addActionListener(e -> buscarSiguiente());

        JButton btnCerrarBusqueda = new JButton("✕");
        btnCerrarBusqueda.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCerrarBusqueda.setBackground(null);
        btnCerrarBusqueda.setForeground(Color.WHITE);
        btnCerrarBusqueda.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btnCerrarBusqueda.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCerrarBusqueda.setToolTipText("Cerrar búsqueda");
        btnCerrarBusqueda.addActionListener(e -> cerrarBusqueda());

        panelBotonesBusqueda.add(lblResultadoBusqueda);
        panelBotonesBusqueda.add(btnAnterior);
        panelBotonesBusqueda.add(btnSiguiente);
        panelBotonesBusqueda.add(btnCerrarBusqueda);

        panelBusqueda.add(txtBusqueda, BorderLayout.CENTER);
        panelBusqueda.add(panelBotonesBusqueda, BorderLayout.EAST);

        panelSuperior.add(panelBusqueda, BorderLayout.CENTER);

        panelDerecho.add(panelSuperior, BorderLayout.NORTH);

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

        // Agregar listener para clics en enlaces (descargar archivos)
        areaChat.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                String href = e.getDescription();
                if (href != null && href.startsWith("#download-")) {
                    try {
                        int messageId = Integer.parseInt(href.substring(10));
                        descargarArchivo(messageId);
                    } catch (NumberFormatException ex) {
                        System.err.println("Error parseando messageId del enlace: " + href);
                    }
                }
            }
        });

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
        // Solicitar todos los chats activos (contactos + conversaciones recientes)
        // Esto permite ver tanto favoritos como usuarios con los que has chateado
        Message msg = new Message();
        msg.setType(MessageType.GET_RELEVANT_CHATS);
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

        // Encriptar el mensaje antes de enviarlo
        String textoEncriptado = EncryptionUtil.encrypt(texto);

        Message msg = new Message();
        msg.setType(MessageType.TEXT_MESSAGE);
        msg.setSenderName(ClientSocket.getInstance().getMyUsername());
        msg.setReceiverId(contactoActual.getId()); // Usamos ID
        msg.setGroupChat(false);
        msg.setContent(textoEncriptado); // Enviar mensaje encriptado

        ClientSocket.getInstance().send(msg);

        // Feedback inmediato (mostramos el texto SIN encriptar)
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

    /**
     * Actualiza el estado de un usuario en la lista de contactos en tiempo real
     */
    public void actualizarEstadoUsuario(int userId, String username, String nuevoEstado) {
        SwingUtilities.invokeLater(() -> {
            // Buscar el usuario en la lista y actualizar su estado
            for (int i = 0; i < modeloContactos.size(); i++) {
                User user = modeloContactos.get(i);
                if (user.getId() == userId) {
                    user.setActivityStatus(nuevoEstado);
                    // Forzar repintado de la lista
                    modeloContactos.set(i, user);

                    System.out.println("📡 Estado actualizado: " + username + " -> " + nuevoEstado);
                    break;
                }
            }

            // Repintar la lista para reflejar los cambios
            listaContactos.repaint();
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
            // Verificar si el mensaje es mío (protegido contra null)
            String myUsername = ClientSocket.getInstance().getMyUsername();
            boolean esMio = msg.getSenderName() != null && msg.getSenderName().equals(myUsername);

            // Si el contacto actual está seleccionado y es de él o mío, mostrar
            if (contactoActual != null && (msg.getSenderId() == contactoActual.getId() || esMio)) {
                String contenido = msg.getContent();

                // Manejar archivos
                if (msg.getType() == MessageType.FILE_MESSAGE || msg.getType() == MessageType.ARCHIVO) {
                    // Generar un ID temporal si no tenemos messageId
                    int fileId = msg.getMessageId();
                    if (fileId <= 0) {
                        // Usar un hash negativo como ID temporal para archivos sin ID
                        fileId = -System.identityHashCode(msg);
                    }

                    // Guardar mensaje de archivo en el mapa
                    fileMessagesMap.put(fileId, msg);

                    // Crear enlace clickeable para descargar con diseño mejorado
                    String fileName = msg.getFileName() != null ? msg.getFileName() : "archivo";
                    long fileSize = msg.getFileData() != null ? msg.getFileData().length : 0;
                    String fileSizeStr = formatFileSize(fileSize);

                    contenido = "<div style='background: rgba(79, 195, 247, 0.1); padding: 10px; border-radius: 8px; border-left: 3px solid #4FC3F7;'>" +
                              "<span style='font-size: 24px;'>📎</span> " +
                              "<a href='#download-" + fileId + "' style='color: #4FC3F7; text-decoration: none; font-weight: bold;'>" +
                              fileName + "</a><br/>" +
                              "<span style='color: #999; font-size: 11px;'>Tamaño: " + fileSizeStr + " • Clic para descargar</span>" +
                              "</div>";
                } else if (msg.getType() == MessageType.TEXT_MESSAGE) {
                    // Desencriptar el contenido si es un mensaje de texto
                    contenido = EncryptionUtil.decrypt(contenido);
                }

                // Obtener el nombre del remitente (protegido contra null)
                String senderName = msg.getSenderName() != null ? msg.getSenderName() : "Usuario";

                // Si es mensaje mío del historial, mostrar con estado
                if (esMio) {
                    agregarBurbuja(senderName, contenido, true, msg.getMessageId(), msg.isDelivered(), msg.isRead());
                } else {
                    agregarBurbuja(senderName, contenido, false);

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
                if (!existeEnLista && msg.getSenderName() != null) {
                    User nuevoUsuario = new User(msg.getSenderId(), msg.getSenderName(), "activo");
                    modeloContactos.addElement(nuevoUsuario);
                }

                // Mostrar notificación de mensaje nuevo
                if (msg.getSenderName() != null) {
                    mostrarNotificacionMensaje(msg.getSenderName());
                }
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

    /**
     * Carga los chats relevantes desde el servidor
     */
    public void loadRelevantChats(List<User> chats) {
        modeloContactos.clear();
        for (User chat : chats) {
            modeloContactos.addElement(chat);
        }
    }

    /**
     * Descarga un archivo recibido
     */
    private void descargarArchivo(int messageId) {
        Message fileMessage = fileMessagesMap.get(messageId);

        // Si no tenemos el mensaje en el mapa
        if (fileMessage == null) {
            // Si es un ID negativo (temporal), no podemos solicitar al servidor
            if (messageId < 0) {
                JOptionPane.showMessageDialog(this,
                    "El archivo ya no está disponible.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Crear mensaje temporal para solicitar al servidor
            fileMessage = new Message();
            fileMessage.setMessageId(messageId);
            fileMessagesMap.put(messageId, fileMessage);
        }

        // Si el archivo tiene datos localmente, guardar directamente
        if (fileMessage.getFileData() != null && fileMessage.getFileData().length > 0) {
            guardarArchivoEnDisco(fileMessage);
            return;
        }

        // Si no tiene datos y es un ID temporal, no podemos descargar
        if (messageId < 0) {
            JOptionPane.showMessageDialog(this,
                "El archivo ya no está disponible en memoria.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Solicitar descarga al servidor
        Message downloadRequest = new Message();
        downloadRequest.setType(MessageType.DOWNLOAD_FILE);
        downloadRequest.setMessageId(messageId);
        ClientSocket.getInstance().send(downloadRequest);

        JOptionPane.showMessageDialog(this,
            "Solicitando archivo al servidor...\nEl archivo se descargará en unos momentos.",
            "Descargando",
            JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Guarda un archivo en el disco
     */
    private void guardarArchivoEnDisco(Message fileMessage) {
        // Pedir al usuario dónde guardar el archivo
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File(fileMessage.getFileName()));
        fileChooser.setDialogTitle("Guardar archivo");

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            try {
                // Guardar el archivo
                java.nio.file.Files.write(file.toPath(), fileMessage.getFileData());
                JOptionPane.showMessageDialog(this,
                    "Archivo descargado correctamente:\n" + file.getAbsolutePath(),
                    "Descarga completa",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (java.io.IOException e) {
                JOptionPane.showMessageDialog(this,
                    "Error al guardar el archivo: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Recibe un archivo descargado del servidor
     */
    public void recibirArchivoDescargado(Message msg) {
        SwingUtilities.invokeLater(() -> {
            int messageId = msg.getMessageId();

            // Actualizar el mapa con los datos del archivo
            Message fileMessage = fileMessagesMap.get(messageId);
            if (fileMessage == null) {
                fileMessage = new Message();
                fileMessage.setMessageId(messageId);
            }
            fileMessage.setFileName(msg.getFileName());
            fileMessage.setFileData(msg.getFileData());
            fileMessagesMap.put(messageId, fileMessage);

            // Mostrar diálogo para guardar
            guardarArchivoEnDisco(fileMessage);
        });
    }

    /**
     * Formatea el tamaño de un archivo en un formato legible (KB, MB, etc.)
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    // =================================================================
    // BÚSQUEDA DE MENSAJES
    // =================================================================

    /**
     * Muestra u oculta el panel de búsqueda
     */
    private void togglePanelBusqueda() {
        boolean visible = !panelBusqueda.isVisible();
        panelBusqueda.setVisible(visible);
        if (visible) {
            txtBusqueda.requestFocus();
            txtBusqueda.selectAll();
        } else {
            cerrarBusqueda();
        }
    }

    /**
     * Cierra el panel de búsqueda y limpia los resultados
     */
    private void cerrarBusqueda() {
        panelBusqueda.setVisible(false);
        txtBusqueda.setText("");
        indicesResultados.clear();
        indiceResultadoActual = -1;
        lblResultadoBusqueda.setText("0/0");

        // Recargar el chat sin resaltado
        if (contactoActual != null && !mensajesActuales.isEmpty()) {
            recargarChatSinResaltado();
        }
    }

    /**
     * Busca mensajes que contengan el texto especificado
     */
    private void buscarMensajes() {
        String termino = txtBusqueda.getText().toLowerCase().trim();
        indicesResultados.clear();
        indiceResultadoActual = -1;

        if (termino.isEmpty()) {
            lblResultadoBusqueda.setText("0/0");
            recargarChatSinResaltado();
            return;
        }

        // Buscar en los mensajes actuales
        for (int i = 0; i < mensajesActuales.size(); i++) {
            Message msg = mensajesActuales.get(i);
            String contenido = msg.getContent();

            // Desencriptar si es mensaje de texto
            if (msg.getType() == MessageType.TEXT_MESSAGE && contenido != null) {
                contenido = EncryptionUtil.decrypt(contenido);
            }

            if (contenido != null && contenido.toLowerCase().contains(termino)) {
                indicesResultados.add(i);
            }
        }

        if (!indicesResultados.isEmpty()) {
            indiceResultadoActual = 0;
            mostrarResultadoBusqueda();
        } else {
            lblResultadoBusqueda.setText("0/0");
            recargarChatConResaltado(termino);
        }
    }

    /**
     * Navega al resultado anterior
     */
    private void buscarAnterior() {
        if (indicesResultados.isEmpty()) return;

        indiceResultadoActual--;
        if (indiceResultadoActual < 0) {
            indiceResultadoActual = indicesResultados.size() - 1;
        }
        mostrarResultadoBusqueda();
    }

    /**
     * Navega al siguiente resultado
     */
    private void buscarSiguiente() {
        if (indicesResultados.isEmpty()) return;

        indiceResultadoActual++;
        if (indiceResultadoActual >= indicesResultados.size()) {
            indiceResultadoActual = 0;
        }
        mostrarResultadoBusqueda();
    }

    /**
     * Muestra el resultado actual de la búsqueda
     */
    private void mostrarResultadoBusqueda() {
        lblResultadoBusqueda.setText((indiceResultadoActual + 1) + "/" + indicesResultados.size());

        String termino = txtBusqueda.getText().toLowerCase().trim();
        recargarChatConResaltado(termino);
    }

    /**
     * Recarga el chat resaltando los términos de búsqueda
     */
    private void recargarChatConResaltado(String termino) {
        SwingUtilities.invokeLater(() -> {
            areaChat.setText("");
            String myUsername = ClientSocket.getInstance().getMyUsername();

            for (int i = 0; i < mensajesActuales.size(); i++) {
                Message msg = mensajesActuales.get(i);
                boolean esMio = msg.getSenderName() != null && msg.getSenderName().equals(myUsername);
                String contenido = msg.getContent();

                // Desencriptar si es mensaje de texto
                if (msg.getType() == MessageType.TEXT_MESSAGE && contenido != null) {
                    contenido = EncryptionUtil.decrypt(contenido);
                } else if (msg.getType() == MessageType.FILE_MESSAGE || msg.getType() == MessageType.ARCHIVO) {
                    contenido = "📎 " + msg.getFileName();
                }

                // Resaltar el término buscado
                if (contenido != null && !termino.isEmpty()) {
                    contenido = resaltarTexto(contenido, termino, indicesResultados.contains(i) &&
                        indicesResultados.indexOf(i) == indiceResultadoActual);
                }

                String senderName = msg.getSenderName() != null ? msg.getSenderName() : "Usuario";
                agregarBurbujaBusqueda(senderName, contenido, esMio,
                    indicesResultados.contains(i) && indicesResultados.indexOf(i) == indiceResultadoActual);
            }
        });
    }

    /**
     * Recarga el chat sin resaltado
     */
    private void recargarChatSinResaltado() {
        SwingUtilities.invokeLater(() -> {
            areaChat.setText("");
            String myUsername = ClientSocket.getInstance().getMyUsername();

            for (Message msg : mensajesActuales) {
                boolean esMio = msg.getSenderName() != null && msg.getSenderName().equals(myUsername);
                String contenido = msg.getContent();

                if (msg.getType() == MessageType.TEXT_MESSAGE && contenido != null) {
                    contenido = EncryptionUtil.decrypt(contenido);
                } else if (msg.getType() == MessageType.FILE_MESSAGE || msg.getType() == MessageType.ARCHIVO) {
                    contenido = "📎 " + msg.getFileName();
                }

                String senderName = msg.getSenderName() != null ? msg.getSenderName() : "Usuario";
                agregarBurbuja(senderName, contenido, esMio);
            }
        });
    }

    /**
     * Resalta el texto buscado en amarillo (o naranja si es el resultado actual)
     */
    private String resaltarTexto(String texto, String termino, boolean esResultadoActual) {
        if (termino.isEmpty()) return texto;

        String color = esResultadoActual ? "#FF9800" : "#FFEB3B";
        String colorTexto = "#000000";

        // Buscar y reemplazar ignorando mayúsculas
        StringBuilder resultado = new StringBuilder();
        String textoLower = texto.toLowerCase();
        String terminoLower = termino.toLowerCase();

        int inicio = 0;
        int pos;
        while ((pos = textoLower.indexOf(terminoLower, inicio)) != -1) {
            resultado.append(texto.substring(inicio, pos));
            resultado.append("<span style='background-color: ").append(color)
                    .append("; color: ").append(colorTexto)
                    .append("; padding: 2px 4px; border-radius: 3px;'>")
                    .append(texto.substring(pos, pos + termino.length()))
                    .append("</span>");
            inicio = pos + termino.length();
        }
        resultado.append(texto.substring(inicio));

        return resultado.toString();
    }

    /**
     * Agrega una burbuja durante la búsqueda (con posible resaltado de resultado actual)
     */
    private void agregarBurbujaBusqueda(String usuario, String texto, boolean esMio, boolean esResultadoActual) {
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        String bordeExtra = esResultadoActual ? "border: 2px solid #FF9800;" : "";

        String html;
        if (esMio) {
            html = "<div class='msg-container'><div class='bubble-me' style='" + bordeExtra + "'>" +
                   "<span class='sender'>Tú</span>" +
                   texto +
                   "<span class='timestamp'>" + timestamp + "</span>" +
                   "</div></div>";
        } else {
            html = "<div class='msg-container'><div class='bubble-other' style='" + bordeExtra + "'>" +
                   "<span class='sender'>" + usuario + "</span>" +
                   texto +
                   "<span class='timestamp'>" + timestamp + "</span>" +
                   "</div></div>";
        }

        try {
            kit.insertHTML(doc, doc.getLength(), html, 0, 0, null);
            if (esResultadoActual) {
                areaChat.setCaretPosition(doc.getLength());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // =================================================================
    // BÚSQUEDA GLOBAL DE MENSAJES (Panel Izquierdo)
    // =================================================================

    /**
     * Muestra u oculta el panel de búsqueda global
     */
    private void toggleBusquedaGlobal() {
        boolean visible = !panelBusquedaGlobal.isVisible();
        panelBusquedaGlobal.setVisible(visible);
        if (visible) {
            txtBusquedaGlobal.requestFocus();
            txtBusquedaGlobal.selectAll();
        } else {
            cerrarBusquedaGlobal();
        }
    }

    /**
     * Cierra el panel de búsqueda global
     */
    private void cerrarBusquedaGlobal() {
        panelBusquedaGlobal.setVisible(false);
        txtBusquedaGlobal.setText("");
        // Restaurar la lista de contactos original
        pedirListaContactos();
    }

    /**
     * Ejecuta la búsqueda global de mensajes en todos los chats
     */
    private void ejecutarBusquedaGlobal() {
        String termino = txtBusquedaGlobal.getText().trim();
        if (termino.isEmpty()) {
            return;
        }

        // Enviar solicitud de búsqueda al servidor
        Message msg = new Message();
        msg.setType(MessageType.SEARCH_MESSAGES);
        msg.setContent(termino);
        ClientSocket.getInstance().send(msg);
    }

    /**
     * Muestra los resultados de la búsqueda global de mensajes
     */
    public void mostrarResultadosBusquedaGlobal(List<Message> mensajes) {
        SwingUtilities.invokeLater(() -> {
            if (mensajes == null || mensajes.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No se encontraron mensajes con ese término.",
                    "Sin resultados",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Crear un diálogo para mostrar los resultados
            JDialog dialogo = new JDialog(this, "Resultados de búsqueda", true);
            dialogo.setSize(500, 400);
            dialogo.setLocationRelativeTo(this);

            JPanel panelPrincipal = new JPanel(new BorderLayout());
            panelPrincipal.setBackground(new Color(30, 30, 30));

            // Título
            JLabel lblTitulo = new JLabel("Se encontraron " + mensajes.size() + " mensaje(s)");
            lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblTitulo.setForeground(Color.WHITE);
            lblTitulo.setBorder(new EmptyBorder(15, 15, 10, 15));
            panelPrincipal.add(lblTitulo, BorderLayout.NORTH);

            // Lista de resultados
            DefaultListModel<String> modeloResultados = new DefaultListModel<>();
            JList<String> listaResultados = new JList<>(modeloResultados);
            listaResultados.setBackground(new Color(40, 40, 40));
            listaResultados.setForeground(Color.WHITE);
            listaResultados.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            listaResultados.setSelectionBackground(new Color(0, 150, 136));
            listaResultados.setSelectionForeground(Color.WHITE);
            listaResultados.setFixedCellHeight(50);

            // Mapear índices a mensajes para poder abrir el chat
            java.util.ArrayList<Message> mensajesLista = new java.util.ArrayList<>(mensajes);

            for (Message m : mensajes) {
                String contenido = m.getContent();
                // Desencriptar el contenido
                if (m.getType() == MessageType.TEXT_MESSAGE && contenido != null) {
                    contenido = EncryptionUtil.decrypt(contenido);
                }

                // Truncar si es muy largo
                if (contenido != null && contenido.length() > 50) {
                    contenido = contenido.substring(0, 47) + "...";
                }

                String remitente = m.getSenderName() != null ? m.getSenderName() : "Usuario";
                modeloResultados.addElement("💬 " + remitente + ": " + contenido);
            }

            // Evento al hacer doble clic: abrir el chat correspondiente
            listaResultados.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int index = listaResultados.getSelectedIndex();
                        if (index >= 0 && index < mensajesLista.size()) {
                            Message msgSeleccionado = mensajesLista.get(index);

                            // Determinar con quién es el chat
                            int myId = ClientSocket.getInstance().getMyUserId();
                            int contactId;
                            String contactName;

                            if (msgSeleccionado.getSenderId() == myId) {
                                // Yo envié el mensaje, abrir chat con el receptor
                                contactId = msgSeleccionado.getReceiverId();
                                // Buscar nombre del receptor en la lista de contactos
                                contactName = buscarNombreUsuario(contactId);
                            } else {
                                // Otro me envió el mensaje, abrir chat con el emisor
                                contactId = msgSeleccionado.getSenderId();
                                contactName = msgSeleccionado.getSenderName();
                            }

                            if (contactName == null) {
                                contactName = "Usuario";
                            }

                            // Crear usuario y cambiar al chat
                            User contacto = new User(contactId, contactName, "activo");
                            cambiarChat(contacto);

                            // Cerrar diálogo
                            dialogo.dispose();
                            cerrarBusquedaGlobal();
                        }
                    }
                }
            });

            JScrollPane scrollResultados = new JScrollPane(listaResultados);
            scrollResultados.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
            panelPrincipal.add(scrollResultados, BorderLayout.CENTER);

            // Botón cerrar
            JButton btnCerrar = new JButton("Cerrar");
            btnCerrar.setBackground(new Color(60, 60, 60));
            btnCerrar.setForeground(Color.WHITE);
            btnCerrar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            btnCerrar.addActionListener(ev -> dialogo.dispose());

            JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            panelBoton.setBackground(new Color(30, 30, 30));
            panelBoton.setBorder(new EmptyBorder(0, 15, 15, 15));
            panelBoton.add(btnCerrar);
            panelPrincipal.add(panelBoton, BorderLayout.SOUTH);

            dialogo.setContentPane(panelPrincipal);
            dialogo.setVisible(true);
        });
    }

    /**
     * Busca el nombre de un usuario por su ID en la lista de contactos
     */
    private String buscarNombreUsuario(int userId) {
        for (int i = 0; i < modeloContactos.size(); i++) {
            if (modeloContactos.get(i).getId() == userId) {
                return modeloContactos.get(i).getUsername();
            }
        }
        return null;
    }
}
