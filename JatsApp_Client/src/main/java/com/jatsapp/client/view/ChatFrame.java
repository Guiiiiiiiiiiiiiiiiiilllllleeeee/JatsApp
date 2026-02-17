package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.client.util.EncryptionUtil;
import com.jatsapp.client.util.StyleUtil;
import com.jatsapp.common.Group;
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

    // Variables para chat de grupo
    private boolean chatActualEsGrupo = false;
    private Group grupoActual = null;
    private JButton btnConfigGrupo; // Botón de configuración del grupo

    // Mapa para trackear estado de mensajes: messageId -> estado visual en HTML
    private Map<Integer, String> messageSentMap = new ConcurrentHashMap<>();

    // Mapa para almacenar mensajes de archivo: messageId -> Message
    private Map<Integer, Message> fileMessagesMap = new ConcurrentHashMap<>();

    // Lista de mensajes del chat actual (para búsqueda)
    private java.util.ArrayList<Message> mensajesActuales = new java.util.ArrayList<>();

    // Variable para rastrear la última fecha mostrada (separadores de día)
    private java.time.LocalDate ultimaFechaMostrada = null;

    // Panel de búsqueda dentro del chat
    private JPanel panelBusqueda;
    private JTextField txtBusqueda;
    private JLabel lblResultadoBusqueda;
    private int indiceResultadoActual = -1;
    private java.util.ArrayList<Integer> indicesResultados = new java.util.ArrayList<>();

    // Panel de búsqueda global (en panel izquierdo) - Filtrado de chats
    private JPanel panelBusquedaGlobal;
    private JTextField txtBusquedaGlobal;
    private java.util.ArrayList<User> listaCompletaContactos = new java.util.ArrayList<>();

    private JButton btnContactos;

    // Nueva referencia a ContactsFrame
    private ContactsFrame contactsFrame;

    public ChatFrame() {
        // 1. Vincular esta ventana al Socket para recibir mensajes
        ClientSocket.getInstance().setChatFrame(this);

        // Aplicar tema oscuro
        StyleUtil.applyDarkTheme();

        // 2. Configuración de la Ventana
        String miUsuario = ClientSocket.getInstance().getMyUsername();
        setTitle("JatsApp - " + (miUsuario != null ? miUsuario : "Desconectado"));
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 600));

        // Layout Principal: Un panel dividido (Izquierda: Lista, Derecha: Chat)
        JSplitPane splitPane = new JSplitPane();
        splitPane.setDividerLocation(320);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);
        splitPane.setBackground(StyleUtil.BORDER_DARK);

        // ==========================================
        // PANEL IZQUIERDO (Contactos y Botones)
        // ==========================================
        JPanel panelIzquierdo = new JPanel(new BorderLayout());
        panelIzquierdo.setBackground(StyleUtil.BG_MEDIUM);
        panelIzquierdo.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, StyleUtil.BORDER_DARK));

        // -- Cabecera Izquierda (Logo + Búsqueda Global) --
        JPanel panelCabeceraIzq = new JPanel(new BorderLayout());
        panelCabeceraIzq.setBackground(StyleUtil.BG_DARK);
        panelCabeceraIzq.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, StyleUtil.BORDER_DARK));

        // Panel superior con Logo y botón de búsqueda
        JPanel panelLogoYBusqueda = new JPanel(new BorderLayout());
        panelLogoYBusqueda.setBackground(StyleUtil.BG_DARK);

        JLabel lblLogo = new JLabel("JatsApp");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblLogo.setForeground(StyleUtil.PRIMARY);
        lblLogo.setBorder(new EmptyBorder(18, 20, 12, 10));
        panelLogoYBusqueda.add(lblLogo, BorderLayout.CENTER);

        // Botón de búsqueda global
        JButton btnBusquedaGlobal = crearBotonImagen("/images/research.png", "Q");
        btnBusquedaGlobal.setToolTipText("Buscar chats");
        btnBusquedaGlobal.setBorder(new EmptyBorder(15, 5, 5, 15));
        btnBusquedaGlobal.addActionListener(e -> toggleBusquedaGlobal());
        panelLogoYBusqueda.add(btnBusquedaGlobal, BorderLayout.EAST);

        panelCabeceraIzq.add(panelLogoYBusqueda, BorderLayout.NORTH);

        // Panel de búsqueda global (oculto por defecto)
        panelBusquedaGlobal = new JPanel(new BorderLayout(5, 0));
        panelBusquedaGlobal.setBackground(StyleUtil.BG_DARK);
        panelBusquedaGlobal.setBorder(BorderFactory.createEmptyBorder(5, 15, 12, 15));
        panelBusquedaGlobal.setVisible(false);

        txtBusquedaGlobal = StyleUtil.createStyledTextField("Buscar chat...");
        // Filtrar en tiempo real mientras se escribe
        txtBusquedaGlobal.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    cerrarBusquedaGlobal();
                } else {
                    filtrarListaChats();
                }
            }
        });

        JButton btnCerrarBusquedaGlobal = new JButton("X");
        btnCerrarBusquedaGlobal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCerrarBusquedaGlobal.setBackground(null);
        btnCerrarBusquedaGlobal.setForeground(StyleUtil.TEXT_SECONDARY);
        btnCerrarBusquedaGlobal.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 5));
        btnCerrarBusquedaGlobal.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCerrarBusquedaGlobal.setToolTipText("Cerrar búsqueda");
        btnCerrarBusquedaGlobal.addActionListener(e -> cerrarBusquedaGlobal());

        panelBusquedaGlobal.add(txtBusquedaGlobal, BorderLayout.CENTER);
        panelBusquedaGlobal.add(btnCerrarBusquedaGlobal, BorderLayout.EAST);

        panelCabeceraIzq.add(panelBusquedaGlobal, BorderLayout.CENTER);

        panelIzquierdo.add(panelCabeceraIzq, BorderLayout.NORTH);

        // -- Lista de Contactos (incluye usuarios y grupos) --
        modeloContactos = new DefaultListModel<>();
        listaContactos = new JList<>(modeloContactos);
        listaContactos.setCellRenderer(new ContactRenderer()); // Tu renderizador personalizado
        listaContactos.setBackground(new Color(30, 30, 30));
        listaContactos.setBorder(null);
        listaContactos.setFixedCellHeight(70); // Altura para que quepa el avatar

        // Evento: Clic en un contacto o grupo
        listaContactos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Solo procesar clic simple (evitar doble clic duplicado)
                if (e.getClickCount() != 1) return;

                int index = listaContactos.locationToIndex(e.getPoint());
                if (index < 0) return;

                User seleccion = modeloContactos.getElementAt(index);
                listaContactos.setSelectedIndex(index);

                if (e.getButton() == MouseEvent.BUTTON3) {
                    // Clic derecho: mostrar menú contextual
                    mostrarMenuContextual(seleccion, e.getComponent(), e.getX(), e.getY());
                } else if (e.getButton() == MouseEvent.BUTTON1 && seleccion != null) {
                    System.out.println("👆 Clic en lista: ID=" + seleccion.getId() + ", nombre=" + seleccion.getUsername() + ", status=" + seleccion.getActivityStatus());

                    // Clic izquierdo: abrir chat
                    if ("grupo".equals(seleccion.getActivityStatus())) {
                        // Es un grupo - el ID está negativo, convertir a positivo
                        int realGroupId = -seleccion.getId(); // Convertir de negativo a positivo
                        String nombreGrupo = seleccion.getUsername();
                        System.out.println("👆 Es GRUPO: ID real=" + realGroupId + ", nombre=" + nombreGrupo);
                        Group grupo = new Group(realGroupId, nombreGrupo, 0);
                        abrirChatGrupo(grupo);
                    } else {
                        // Es un usuario normal
                        System.out.println("👆 Es USUARIO: ID=" + seleccion.getId() + ", nombre=" + seleccion.getUsername());
                        cambiarChat(seleccion);
                    }
                }
            }
        });

        JScrollPane scrollContactos = new JScrollPane(listaContactos);
        scrollContactos.setBorder(null);
        StyleUtil.styleScrollPane(scrollContactos);
        panelIzquierdo.add(scrollContactos, BorderLayout.CENTER);

        // -- Panel de Botones Inferior --
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        panelBotones.setBackground(StyleUtil.BG_DARK);
        panelBotones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, StyleUtil.BORDER_DARK));

        // Botón Configuración (⚙) - Muestra menú con opciones
        JButton btnConfig = crearBotonImagen("/images/setting.png", "⚙");
        btnConfig.setToolTipText("Opciones");
        btnConfig.addActionListener(e -> mostrarMenuConfiguracion(btnConfig));

        // Botón Contactos
        btnContactos = new JButton("Contactos");
        btnContactos.setFont(StyleUtil.FONT_SMALL);
        btnContactos.setForeground(StyleUtil.TEXT_PRIMARY);
        btnContactos.setBackground(StyleUtil.BG_LIGHT);
        btnContactos.setOpaque(true);
        btnContactos.setBorderPainted(false);
        btnContactos.setFocusPainted(false);
        btnContactos.setBorder(new EmptyBorder(10, 18, 10, 18));
        btnContactos.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnContactos.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (contactsFrame != null && contactsFrame.isVisible()) {
                    contactsFrame.toFront();
                } else {
                    contactsFrame = new ContactsFrame();
                    contactsFrame.setVisible(true);
                }
            }
        });

        // Botón Grupos
        JButton btnGrupos = new JButton("Grupos");
        btnGrupos.setFont(StyleUtil.FONT_SMALL);
        btnGrupos.setForeground(StyleUtil.TEXT_PRIMARY);
        btnGrupos.setBackground(StyleUtil.BG_LIGHT);
        btnGrupos.setOpaque(true);
        btnGrupos.setBorderPainted(false);
        btnGrupos.setFocusPainted(false);
        btnGrupos.setBorder(new EmptyBorder(10, 18, 10, 18));
        btnGrupos.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGrupos.setToolTipText("Gestionar grupos");
        btnGrupos.addActionListener(e -> abrirVentanaGrupos());

        panelBotones.add(btnConfig);
        panelBotones.add(btnContactos);
        panelBotones.add(btnGrupos);

        panelIzquierdo.add(panelBotones, BorderLayout.SOUTH);

        // ==========================================
        // PANEL DERECHO (Chat)
        // ==========================================
        JPanel panelDerecho = new JPanel(new BorderLayout());
        panelDerecho.setBackground(StyleUtil.BG_DARK);

        // -- Panel superior (cabecera + búsqueda) --
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBackground(StyleUtil.BG_DARK);

        // -- Cabecera del Chat --
        JPanel headerChat = new JPanel(new BorderLayout());
        headerChat.setBackground(StyleUtil.BG_DARK);
        headerChat.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, StyleUtil.BORDER_DARK));
        headerChat.setPreferredSize(new Dimension(0, 65));

        lblTituloChat = new JLabel("Selecciona un chat");
        lblTituloChat.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTituloChat.setForeground(StyleUtil.TEXT_PRIMARY);
        lblTituloChat.setBorder(new EmptyBorder(12, 24, 0, 0));
        headerChat.add(lblTituloChat, BorderLayout.CENTER);

        // Panel para botones del header (búsqueda + configuración de grupo)
        JPanel panelBotonesHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelBotonesHeader.setBackground(StyleUtil.BG_DARK);

        // Botón de configuración de grupo (solo visible en chats de grupo)
        btnConfigGrupo = crearBotonImagen("/images/setting.png", "⚙");
        btnConfigGrupo.setToolTipText("Configuración del grupo");
        btnConfigGrupo.setVisible(false); // Oculto por defecto
        btnConfigGrupo.addActionListener(e -> mostrarConfiguracionGrupo());
        panelBotonesHeader.add(btnConfigGrupo);

        // Botón de búsqueda
        JButton btnBuscar = new JButton("🔍");
        btnBuscar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        btnBuscar.setForeground(StyleUtil.TEXT_PRIMARY);
        btnBuscar.setBackground(StyleUtil.BG_LIGHT);
        btnBuscar.setOpaque(true);
        btnBuscar.setBorderPainted(false);
        btnBuscar.setFocusPainted(false);
        btnBuscar.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        btnBuscar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBuscar.setToolTipText("Buscar mensajes (Ctrl+F)");
        btnBuscar.addActionListener(e -> togglePanelBusqueda());
        panelBotonesHeader.add(btnBuscar);

        headerChat.add(panelBotonesHeader, BorderLayout.EAST);

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
        txtBusqueda.setForeground(StyleUtil.TEXT_PRIMARY);
        txtBusqueda.setCaretColor(StyleUtil.TEXT_PRIMARY);
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
        lblResultadoBusqueda.setForeground(StyleUtil.TEXT_SECONDARY);
        lblResultadoBusqueda.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton btnAnterior = new JButton("<");
        btnAnterior.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAnterior.setBackground(StyleUtil.BG_LIGHT);
        btnAnterior.setForeground(StyleUtil.TEXT_PRIMARY);
        btnAnterior.setOpaque(true);
        btnAnterior.setBorderPainted(false);
        btnAnterior.setFocusPainted(false);
        btnAnterior.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btnAnterior.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAnterior.setToolTipText("Resultado anterior");
        btnAnterior.addActionListener(e -> buscarAnterior());

        JButton btnSiguiente = new JButton(">");
        btnSiguiente.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSiguiente.setBackground(StyleUtil.BG_LIGHT);
        btnSiguiente.setForeground(StyleUtil.TEXT_PRIMARY);
        btnSiguiente.setOpaque(true);
        btnSiguiente.setBorderPainted(false);
        btnSiguiente.setFocusPainted(false);
        btnSiguiente.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btnSiguiente.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSiguiente.setToolTipText("Resultado siguiente");
        btnSiguiente.addActionListener(e -> buscarSiguiente());

        JButton btnCerrarBusqueda = new JButton("X");
        btnCerrarBusqueda.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCerrarBusqueda.setBackground(StyleUtil.DANGER);
        btnCerrarBusqueda.setForeground(StyleUtil.TEXT_PRIMARY);
        btnCerrarBusqueda.setOpaque(true);
        btnCerrarBusqueda.setBorderPainted(false);
        btnCerrarBusqueda.setFocusPainted(false);
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
        areaChat.setBackground(StyleUtil.BG_DARK);

        kit = new HTMLEditorKit();
        doc = new HTMLDocument();
        areaChat.setEditorKit(kit);
        areaChat.setDocument(doc);

        // CSS moderno para las burbujas - DISEÑO MEJORADO
        String css = "body { font-family: 'Segoe UI', sans-serif; background-color: #111b21; color: #e9edef; padding: 15px; margin: 0; }"
                + ".msg-container { width: 100%; overflow: hidden; margin-bottom: 12px; clear: both; }"
                // Separador de día
                + ".day-separator { width: 100%; text-align: center; margin: 20px 0; clear: both; }"
                + ".day-separator-label { display: inline-block; background: rgba(32, 44, 51, 0.9); color: #8696a0; font-size: 12px; font-weight: 500; padding: 6px 16px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.2); }"
                // Burbuja mensaje propio (verde)
                + ".bubble-me { background: linear-gradient(135deg, #005c4b 0%, #00756a 100%); padding: 8px 12px 6px 12px; border-radius: 12px 12px 4px 12px; float: right; color: white; max-width: 70%; min-width: 120px; box-shadow: 0 2px 8px rgba(0,0,0,0.25); position: relative; }"
                // Burbuja mensaje de otros (gris oscuro)
                + ".bubble-other { background: linear-gradient(135deg, #202c33 0%, #2a3942 100%); padding: 8px 12px 6px 12px; border-radius: 12px 12px 12px 4px; float: left; color: #e9edef; max-width: 70%; min-width: 120px; box-shadow: 0 2px 8px rgba(0,0,0,0.2); position: relative; }"
                // Cabecera del mensaje (nombre del emisor)
                + ".msg-header { display: block; margin-bottom: 4px; padding-bottom: 4px; border-bottom: 1px solid rgba(255,255,255,0.1); }"
                + ".sender { font-size: 12px; color: #00d9a6; font-weight: 600; letter-spacing: 0.3px; }"
                + ".sender-other { font-size: 12px; color: #53bdeb; font-weight: 600; letter-spacing: 0.3px; }"
                // Contenido del mensaje
                + ".msg-content { font-size: 14px; line-height: 1.4; word-wrap: break-word; padding: 4px 0; color: #e9edef; }"
                // Footer del mensaje (hora y estado)
                + ".msg-footer { display: block; text-align: right; margin-top: 6px; padding-top: 4px; border-top: 1px solid rgba(255,255,255,0.05); }"
                + ".timestamp { font-size: 11px; color: rgba(255,255,255,0.6); }"
                + ".status { font-size: 13px; margin-left: 6px; font-weight: bold; }"
                + ".status-sent { color: rgba(255,255,255,0.5); }"
                + ".status-delivered { color: rgba(255,255,255,0.7); }"
                + ".status-read { color: #53bdeb; }";

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

        JScrollPane scrollChat = new JScrollPane(areaChat);
        StyleUtil.styleScrollPane(scrollChat);
        panelDerecho.add(scrollChat, BorderLayout.CENTER);

        // -- Área de Input (Escribir) --
        JPanel panelInput = new JPanel(new BorderLayout(12, 0));
        panelInput.setBackground(StyleUtil.BG_DARK);
        panelInput.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, StyleUtil.BORDER_DARK),
            new EmptyBorder(12, 20, 12, 20)
        ));

        // Panel izquierdo con botones de archivo y emoji
        JPanel panelBotonesIzq = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panelBotonesIzq.setBackground(StyleUtil.BG_DARK);

        // Botón Archivo
        JButton btnArchivo = new JButton("+");
        btnArchivo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnArchivo.setForeground(StyleUtil.TEXT_PRIMARY);
        btnArchivo.setBackground(StyleUtil.BG_LIGHT);
        btnArchivo.setOpaque(true);
        btnArchivo.setBorderPainted(false);
        btnArchivo.setFocusPainted(false);
        btnArchivo.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        btnArchivo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnArchivo.setToolTipText("Adjuntar archivo");
        btnArchivo.addActionListener(e -> enviarArchivo());

        // Botón Emoji
        JButton btnEmoji = new JButton(":)");
        btnEmoji.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnEmoji.setForeground(StyleUtil.TEXT_PRIMARY);
        btnEmoji.setBackground(StyleUtil.BG_LIGHT);
        btnEmoji.setOpaque(true);
        btnEmoji.setBorderPainted(false);
        btnEmoji.setFocusPainted(false);
        btnEmoji.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        btnEmoji.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEmoji.setToolTipText("Insertar emoji");
        btnEmoji.addActionListener(e -> mostrarSelectorEmojis(btnEmoji));

        panelBotonesIzq.add(btnArchivo);
        panelBotonesIzq.add(btnEmoji);

        txtMensaje = StyleUtil.createStyledTextField("Escribe un mensaje...");
        txtMensaje.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));

        // Al pulsar Enter, enviar
        txtMensaje.addActionListener(e -> enviarMensajeTexto());

        // Botón Enviar con icono
        JButton btnEnviar = new JButton();
        try {
            ImageIcon sendIcon = new ImageIcon(getClass().getResource("/images/send.png"));
            // Escalar el icono a un tamaño apropiado
            Image scaledImg = sendIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            btnEnviar.setIcon(new ImageIcon(scaledImg));
        } catch (Exception ex) {
            btnEnviar.setText(">"); // Fallback si no se carga el icono
        }
        btnEnviar.setBackground(StyleUtil.PRIMARY);
        btnEnviar.setOpaque(true);
        btnEnviar.setBorderPainted(false);
        btnEnviar.setFocusPainted(false);
        btnEnviar.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        btnEnviar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEnviar.setToolTipText("Enviar mensaje");
        btnEnviar.addActionListener(e -> enviarMensajeTexto());

        panelInput.add(panelBotonesIzq, BorderLayout.WEST);
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

        // También solicitar los grupos del usuario
        Message msgGrupos = new Message();
        msgGrupos.setType(MessageType.GET_GROUPS);
        ClientSocket.getInstance().send(msgGrupos);
    }

    private void cambiarChat(User usuario) {
        System.out.println("🔄 cambiarChat: Cambiando a usuario ID=" + usuario.getId() + " (" + usuario.getUsername() + ")");

        // IMPORTANTE: Resetear estado de grupo al cambiar a chat privado
        this.chatActualEsGrupo = false;
        this.grupoActual = null;

        // Ocultar botón de configuración de grupo
        if (btnConfigGrupo != null) {
            btnConfigGrupo.setVisible(false);
        }

        this.contactoActual = usuario;
        lblTituloChat.setText("Chat con: " + usuario.getUsername());
        areaChat.setText(""); // Limpiar chat visualmente
        mensajesActuales.clear();
        txtMensaje.requestFocus();

        // Pedir historial al servidor
        Message msg = new Message();
        msg.setType(MessageType.GET_HISTORY);
        msg.setReceiverId(usuario.getId());
        msg.setGroupChat(false); // Chat privado
        ClientSocket.getInstance().send(msg);

        System.out.println("🔄 cambiarChat: Solicitado historial para usuario ID=" + usuario.getId() + ", isGroupChat=false");
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

    /**
     * Muestra un menú popup con opciones para crear nuevo chat o grupo
     */
    private void mostrarMenuNuevo(Component invoker) {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBackground(new Color(40, 40, 40));

        // Opción: Añadir contacto
        JMenuItem itemContacto = new JMenuItem("👤 Añadir Contacto");
        itemContacto.setBackground(new Color(40, 40, 40));
        itemContacto.setForeground(StyleUtil.TEXT_PRIMARY);
        itemContacto.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        itemContacto.addActionListener(e -> accionAnadirContacto());

        // Opción: Crear grupo
        JMenuItem itemGrupo = new JMenuItem("Crear Grupo");
        itemGrupo.setBackground(new Color(40, 40, 40));
        itemGrupo.setForeground(StyleUtil.TEXT_PRIMARY);
        itemGrupo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        itemGrupo.addActionListener(e -> crearGrupoRapido());

        // Opción: Buscar usuario
        JMenuItem itemBuscar = new JMenuItem("🔍 Buscar Usuario");
        itemBuscar.setBackground(new Color(40, 40, 40));
        itemBuscar.setForeground(StyleUtil.TEXT_PRIMARY);
        itemBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        itemBuscar.addActionListener(e -> {
            String termino = JOptionPane.showInputDialog(this,
                "Buscar usuario por nombre:",
                "Buscar Usuario",
                JOptionPane.PLAIN_MESSAGE);
            if (termino != null && !termino.trim().isEmpty()) {
                buscarUsuarios(termino.trim());
            }
        });

        popupMenu.add(itemContacto);
        popupMenu.add(itemGrupo);
        popupMenu.addSeparator();
        popupMenu.add(itemBuscar);

        popupMenu.show(invoker, 0, invoker.getHeight());
    }

    /**
     * Crea un grupo rápidamente desde el menú principal
     */
    private void crearGrupoRapido() {
        String nombre = JOptionPane.showInputDialog(this,
                "Introduce el nombre del nuevo grupo:",
                "Crear Grupo",
                JOptionPane.PLAIN_MESSAGE);

        if (nombre != null && !nombre.trim().isEmpty()) {
            Message msg = new Message();
            msg.setType(MessageType.CREATE_GROUP);
            msg.setContent(nombre.trim());
            ClientSocket.getInstance().send(msg);
        }
    }

    /**
     * Muestra el menú de configuración con opciones
     */
    private void mostrarMenuConfiguracion(JButton source) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(StyleUtil.BG_MEDIUM);
        menu.setBorder(BorderFactory.createLineBorder(StyleUtil.BORDER_LIGHT));

        // Opción: Cerrar sesión
        JMenuItem itemCerrarSesion = new JMenuItem("Cerrar sesión");
        itemCerrarSesion.setFont(StyleUtil.FONT_BODY);
        itemCerrarSesion.setBackground(StyleUtil.BG_MEDIUM);
        itemCerrarSesion.setForeground(StyleUtil.TEXT_PRIMARY);
        itemCerrarSesion.setCursor(new Cursor(Cursor.HAND_CURSOR));
        itemCerrarSesion.addActionListener(e -> cerrarSesion());
        menu.add(itemCerrarSesion);

        menu.addSeparator();

        // Opción: Salir de la aplicación
        JMenuItem itemSalir = new JMenuItem("Salir de la aplicación");
        itemSalir.setFont(StyleUtil.FONT_BODY);
        itemSalir.setBackground(StyleUtil.BG_MEDIUM);
        itemSalir.setForeground(StyleUtil.DANGER);
        itemSalir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        itemSalir.addActionListener(e -> salirAplicacion());
        menu.add(itemSalir);

        // Mostrar menú encima del botón
        menu.show(source, 0, -menu.getPreferredSize().height);
    }

    /**
     * Cierra la sesión actual y vuelve al login
     */
    private void cerrarSesion() {
        int opt = JOptionPane.showConfirmDialog(this,
            "¿Deseas cerrar la sesión actual?\n\nVolverás a la pantalla de inicio de sesión.",
            "Cerrar Sesión",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            // Cerrar la conexión actual (logout intencional)
            ClientSocket.getInstance().logout();
            // Cerrar esta ventana
            this.dispose();

            // Reconectar y abrir login
            try {
                ClientSocket.getInstance().reconnect();
                new LoginFrame();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "Error al reconectar con el servidor: " + e.getMessage(),
                    "Error de conexión",
                    JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
    }

    /**
     * Cierra completamente la aplicación
     */
    private void salirAplicacion() {
        int opt = JOptionPane.showConfirmDialog(this,
            "¿Deseas salir de JatsApp?",
            "Salir",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
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
        msg.setReceiverId(contactoActual.getId()); // ID del usuario o grupo
        msg.setGroupChat(chatActualEsGrupo); // IMPORTANTE: usar la variable para saber si es grupo
        msg.setContent(textoEncriptado); // Enviar mensaje encriptado

        System.out.println("📤 Enviando mensaje: receiverId=" + msg.getReceiverId() + ", isGroupChat=" + msg.isGroupChat());

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
                msg.setGroupChat(chatActualEsGrupo); // IMPORTANTE: usar la variable para saber si es grupo

                ClientSocket.getInstance().send(msg);
                agregarBurbuja(ClientSocket.getInstance().getMyUsername(), "[Archivo] " + file.getName(), true);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error leyendo archivo.");
            }
        }
    }

    /**
     * Muestra un selector de emojis en un popup
     */
    private void mostrarSelectorEmojis(JButton source) {
        JPopupMenu popupEmojis = new JPopupMenu();
        popupEmojis.setBackground(StyleUtil.BG_MEDIUM);
        popupEmojis.setBorder(BorderFactory.createLineBorder(StyleUtil.BORDER_LIGHT));

        // Panel principal del selector
        JPanel panelEmojis = new JPanel(new BorderLayout());
        panelEmojis.setBackground(StyleUtil.BG_MEDIUM);
        panelEmojis.setPreferredSize(new Dimension(320, 250));

        // Título
        JLabel lblTitulo = new JLabel("  Emojis");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitulo.setForeground(StyleUtil.TEXT_PRIMARY);
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panelEmojis.add(lblTitulo, BorderLayout.NORTH);

        // Panel con grid de emojis
        JPanel gridEmojis = new JPanel(new GridLayout(0, 8, 2, 2));
        gridEmojis.setBackground(StyleUtil.BG_MEDIUM);
        gridEmojis.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Lista de emojis comunes organizados por categorías
        String[] emojis = {
            // Caras felices
            "\uD83D\uDE00", "\uD83D\uDE01", "\uD83D\uDE02", "\uD83D\uDE03", "\uD83D\uDE04", "\uD83D\uDE05", "\uD83D\uDE06", "\uD83D\uDE09",
            "\uD83D\uDE0A", "\uD83D\uDE0B", "\uD83D\uDE0C", "\uD83D\uDE0D", "\uD83D\uDE18", "\uD83D\uDE17", "\uD83D\uDE19", "\uD83D\uDE1A",
            // Caras tristes/otras
            "\uD83D\uDE14", "\uD83D\uDE1E", "\uD83D\uDE22", "\uD83D\uDE2D", "\uD83D\uDE29", "\uD83D\uDE21", "\uD83D\uDE20", "\uD83D\uDE24",
            "\uD83D\uDE31", "\uD83D\uDE28", "\uD83D\uDE30", "\uD83D\uDE2F", "\uD83D\uDE33", "\uD83D\uDE16", "\uD83D\uDE1F", "\uD83D\uDE34",
            // Gestos
            "\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83D\uDC4C", "\uD83D\uDC4A", "\u270C\uFE0F", "\uD83D\uDC4B", "\uD83D\uDC4F", "\uD83D\uDE4C",
            "\uD83D\uDE4F", "\uD83D\uDCAA", "\u261D\uFE0F", "\u270B", "\uD83D\uDD90\uFE0F", "\uD83E\uDD1D", "\uD83E\uDD1E", "\uD83E\uDD18",
            // Corazones y símbolos
            "\u2764\uFE0F", "\uD83D\uDC9B", "\uD83D\uDC9A", "\uD83D\uDC99", "\uD83D\uDC9C", "\uD83D\uDDA4", "\uD83D\uDC94", "\u2728",
            "\uD83C\uDF1F", "\uD83D\uDD25", "\uD83C\uDF89", "\uD83C\uDF8A", "\uD83C\uDF88", "\uD83D\uDCA5", "\uD83D\uDCAF", "\u2705",
            // Objetos y otros
            "\uD83D\uDCF1", "\uD83D\uDCBB", "\uD83C\uDFAE", "\uD83C\uDFB5", "\uD83C\uDFB6", "\uD83D\uDCF7", "\uD83C\uDF82", "\uD83C\uDF70",
            "\u2615", "\uD83C\uDF7A", "\uD83C\uDF55", "\uD83C\uDF54", "\uD83D\uDE80", "\u2708\uFE0F", "\uD83D\uDE97", "\uD83C\uDFE0"
        };

        for (String emoji : emojis) {
            JButton btnEmoji = new JButton(emoji);
            btnEmoji.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
            btnEmoji.setBackground(StyleUtil.BG_LIGHT);
            btnEmoji.setForeground(StyleUtil.TEXT_PRIMARY);
            btnEmoji.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            btnEmoji.setFocusPainted(false);
            btnEmoji.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnEmoji.setOpaque(true);

            // Efecto hover
            btnEmoji.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btnEmoji.setBackground(StyleUtil.BG_HOVER);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    btnEmoji.setBackground(StyleUtil.BG_LIGHT);
                }
            });

            // Al hacer clic, insertar el emoji en el campo de texto
            btnEmoji.addActionListener(e -> {
                int pos = txtMensaje.getCaretPosition();
                String textoActual = txtMensaje.getText();
                String nuevoTexto = textoActual.substring(0, pos) + emoji + textoActual.substring(pos);
                txtMensaje.setText(nuevoTexto);
                txtMensaje.setCaretPosition(pos + emoji.length());
                txtMensaje.requestFocus();
                popupEmojis.setVisible(false);
            });

            gridEmojis.add(btnEmoji);
        }

        JScrollPane scrollEmojis = new JScrollPane(gridEmojis);
        scrollEmojis.setBorder(null);
        scrollEmojis.getViewport().setBackground(StyleUtil.BG_MEDIUM);
        StyleUtil.styleScrollPane(scrollEmojis);
        panelEmojis.add(scrollEmojis, BorderLayout.CENTER);

        popupEmojis.add(panelEmojis);

        // Mostrar el popup encima del botón
        popupEmojis.show(source, 0, -popupEmojis.getPreferredSize().height - 5);
    }

    // =================================================================
    // MÉTODOS QUE LLAMA EL CLIENTSOCKET (RESPUESTAS DEL SERVIDOR)
    // =================================================================

    public void actualizarContactos(List<User> contactos) {
        SwingUtilities.invokeLater(() -> {
            // Guardar los grupos existentes (no borrarlos)
            java.util.List<User> gruposExistentes = new java.util.ArrayList<>();
            for (int i = 0; i < modeloContactos.size(); i++) {
                User u = modeloContactos.get(i);
                if ("grupo".equals(u.getActivityStatus())) {
                    gruposExistentes.add(u);
                }
            }

            modeloContactos.clear();
            listaCompletaContactos.clear(); // Limpiar lista completa

            // Primero añadir los grupos preservados
            for (User grupo : gruposExistentes) {
                modeloContactos.addElement(grupo);
                listaCompletaContactos.add(grupo); // Guardar en lista completa
            }

            // Luego añadir los contactos nuevos
            String myName = ClientSocket.getInstance().getMyUsername();
            for (User u : contactos) {
                // No mostrarme a mí mismo y no añadir si es un grupo (ya están añadidos)
                if (myName != null && !myName.equals(u.getUsername()) && !"grupo".equals(u.getActivityStatus())) {
                    modeloContactos.addElement(u);
                    listaCompletaContactos.add(u); // Guardar en lista completa
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

    public void cargarHistorial(List<Message> historial, int chatId, boolean esGrupo) {
        SwingUtilities.invokeLater(() -> {
            // DEBUG: Mostrar información recibida
            System.out.println("📋 Historial recibido: chatId=" + chatId + ", esGrupo=" + esGrupo + ", mensajes=" + (historial != null ? historial.size() : 0));
            System.out.println("📋 Chat actual: contactoActual=" + (contactoActual != null ? contactoActual.getId() + " (" + contactoActual.getUsername() + ")" : "null") +
                             ", chatActualEsGrupo=" + chatActualEsGrupo);

            // Verificar que el historial corresponde al chat actualmente abierto
            if (contactoActual == null) {
                System.out.println("⚠️ Historial recibido pero no hay chat abierto - IGNORANDO");
                return;
            }

            // Verificar que el tipo de chat coincide
            if (esGrupo != chatActualEsGrupo) {
                System.out.println("⚠️ Tipo de chat no coincide: recibido " + (esGrupo ? "GRUPO" : "USUARIO") +
                                 " pero actual es " + (chatActualEsGrupo ? "GRUPO" : "USUARIO") + " - IGNORANDO");
                return;
            }

            // Verificar que el ID coincide
            if (chatId != contactoActual.getId()) {
                System.out.println("⚠️ ID de chat no coincide: recibido " + chatId + " pero actual es " + contactoActual.getId() + " - IGNORANDO");
                return;
            }

            System.out.println("✅ Historial válido, cargando " + (historial != null ? historial.size() : 0) + " mensajes");

            areaChat.setText(""); // Limpiar
            mensajesActuales.clear();
            ultimaFechaMostrada = null; // Resetear fecha para separadores de día

            String myUsername = ClientSocket.getInstance().getMyUsername();

            for (Message m : historial) {
                // Guardar para búsqueda
                mensajesActuales.add(m);

                // Verificar si necesitamos añadir separador de día
                if (m.getTimestamp() != null) {
                    java.time.LocalDate fechaMensaje = m.getTimestamp().toLocalDate();
                    if (ultimaFechaMostrada == null || !fechaMensaje.equals(ultimaFechaMostrada)) {
                        agregarSeparadorDia(fechaMensaje);
                        ultimaFechaMostrada = fechaMensaje;
                    }
                }

                // Mostrar el mensaje directamente (el servidor ya filtró los mensajes relevantes)
                boolean esMio = m.getSenderName() != null && m.getSenderName().equals(myUsername);
                String contenido = m.getContent();

                // Manejar archivos
                if (m.getType() == MessageType.FILE_MESSAGE || m.getType() == MessageType.ARCHIVO) {
                    int fileId = m.getMessageId();
                    if (fileId <= 0) {
                        fileId = -System.identityHashCode(m);
                    }
                    fileMessagesMap.put(fileId, m);

                    String fileName = m.getFileName() != null ? m.getFileName() : "archivo";
                    long fileSize = m.getFileData() != null ? m.getFileData().length : 0;
                    String fileSizeStr = formatFileSize(fileSize);

                    contenido = "<div style='background: rgba(79, 195, 247, 0.1); padding: 10px; border-radius: 8px; border-left: 3px solid #4FC3F7;'>" +
                              "<b>[Archivo]</b> " +
                              "<a href='#download-" + fileId + "' style='color: #4FC3F7; text-decoration: none; font-weight: bold;'>" +
                              fileName + "</a><br/>" +
                              "<span style='color: #999; font-size: 11px;'>Tamaño: " + fileSizeStr + " - Clic para descargar</span>" +
                              "</div>";
                } else if (m.getType() == MessageType.TEXT_MESSAGE) {
                    contenido = EncryptionUtil.decrypt(contenido);
                }

                String senderName = m.getSenderName() != null ? m.getSenderName() : "Usuario";

                // Usar el timestamp del mensaje si está disponible
                String timestamp = m.getTimestamp() != null
                    ? m.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                if (esMio) {
                    agregarBurbujaConHora(senderName, contenido, true, m.getMessageId(), m.isDelivered(), m.isRead(), timestamp);
                } else {
                    agregarBurbujaConHora(senderName, contenido, false, 0, false, false, timestamp);
                }
            }
        });
    }

    public void recibirMensaje(Message msg) {
        SwingUtilities.invokeLater(() -> {
            // Verificar si el mensaje es mío (protegido contra null)
            String myUsername = ClientSocket.getInstance().getMyUsername();
            int myUserId = ClientSocket.getInstance().getMyUserId();
            boolean esMio = msg.getSenderName() != null && msg.getSenderName().equals(myUsername);

            // DEBUG
            System.out.println("📨 recibirMensaje: tipo=" + msg.getType() +
                             ", senderId=" + msg.getSenderId() +
                             ", senderName=" + msg.getSenderName() +
                             ", receiverId=" + msg.getReceiverId() +
                             ", isGroupChat=" + msg.isGroupChat() +
                             ", esMio=" + esMio);

            // Determinar si el mensaje corresponde al chat actual
            boolean mensajeDelChatActual = false;

            if (contactoActual != null) {
                System.out.println("📨 Estado actual: contactoActual.getId()=" + contactoActual.getId() +
                                 ", chatActualEsGrupo=" + chatActualEsGrupo);

                if (msg.isGroupChat() && chatActualEsGrupo) {
                    // Mensaje de grupo: comparar receiverId del mensaje con el ID del grupo actual
                    mensajeDelChatActual = (msg.getReceiverId() == contactoActual.getId());
                    System.out.println("📨 Comparación GRUPO: msg.receiverId=" + msg.getReceiverId() +
                                     " vs contactoActual.getId()=" + contactoActual.getId() +
                                     " -> match=" + mensajeDelChatActual);
                } else if (!msg.isGroupChat() && !chatActualEsGrupo) {
                    // Mensaje privado: verificar que es entre yo y el contacto actual
                    boolean delContactoHaciaMi = (msg.getSenderId() == contactoActual.getId() && msg.getReceiverId() == myUserId);
                    boolean mioHaciaContacto = (esMio && msg.getReceiverId() == contactoActual.getId());
                    mensajeDelChatActual = delContactoHaciaMi || mioHaciaContacto;
                    System.out.println("📨 Comparación PRIVADO: delContactoHaciaMi=" + delContactoHaciaMi +
                                     ", mioHaciaContacto=" + mioHaciaContacto +
                                     " -> match=" + mensajeDelChatActual);
                }
            }

            // Si el mensaje corresponde al chat actual, mostrarlo
            if (mensajeDelChatActual) {
                System.out.println("✅ Mostrando mensaje en chat actual");

                // Verificar si necesitamos añadir separador de día
                if (msg.getTimestamp() != null) {
                    java.time.LocalDate fechaMensaje = msg.getTimestamp().toLocalDate();
                    if (ultimaFechaMostrada == null || !fechaMensaje.equals(ultimaFechaMostrada)) {
                        agregarSeparadorDia(fechaMensaje);
                        ultimaFechaMostrada = fechaMensaje;
                    }
                }

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
                              "<b>[Archivo]</b> " +
                              "<a href='#download-" + fileId + "' style='color: #4FC3F7; text-decoration: none; font-weight: bold;'>" +
                              fileName + "</a><br/>" +
                              "<span style='color: #999; font-size: 11px;'>Tamaño: " + fileSizeStr + " - Clic para descargar</span>" +
                              "</div>";
                } else if (msg.getType() == MessageType.TEXT_MESSAGE) {
                    // Desencriptar el contenido si es un mensaje de texto
                    contenido = EncryptionUtil.decrypt(contenido);
                }

                // Obtener el nombre del remitente (protegido contra null)
                String senderName = msg.getSenderName() != null ? msg.getSenderName() : "Usuario";

                // Obtener timestamp del mensaje
                String timestamp = msg.getTimestamp() != null
                    ? msg.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                // Si es mensaje mío del historial, mostrar con estado
                if (esMio) {
                    agregarBurbujaConHora(senderName, contenido, true, msg.getMessageId(), msg.isDelivered(), msg.isRead(), timestamp);
                } else {
                    agregarBurbujaConHora(senderName, contenido, false, 0, false, false, timestamp);

                    // Enviar confirmación de lectura al servidor si el chat está abierto (solo para mensajes privados)
                    if (!msg.isGroupChat() && msg.getMessageId() > 0 && !msg.isRead()) {
                        enviarConfirmacionLectura(msg.getMessageId());
                    }
                }
            }
            // Si NO es del chat actual, es un mensaje nuevo de otro chat
            else if (!esMio && !mensajeDelChatActual) {
                if (msg.isGroupChat()) {
                    // Mensaje de grupo - mostrar notificación
                    mostrarNotificacionMensaje("Grupo: " + msg.getReceiverId());
                } else {
                    // Mensaje privado - verificar si el emisor ya está en la lista
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
        if (!tituloActual.contains("*")) {
            setTitle("* " + tituloActual + " - Mensaje de " + remitente);
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
        // Obtener fecha y hora actual en formato dd/MM/yyyy HH:mm
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        String html;
        if (esMio) {
            // Mensaje mío: mostrar "Tú" + contenido + hora + indicador de estado
            String statusIcon = getStatusIcon(delivered, read);
            String statusId = messageId > 0 ? "msg-status-" + messageId : "";

            html = "<div class='msg-container'>" +
                   "<div class='bubble-me'>" +
                   "<div class='msg-header'><span class='sender'>Tú</span></div>" +
                   "<div class='msg-content'>" + texto + "</div>" +
                   "<div class='msg-footer'>" +
                   "<span class='timestamp'>" + timestamp + "</span>" +
                   "<span class='status' id='" + statusId + "'>" + statusIcon + "</span>" +
                   "</div>" +
                   "</div></div>";

            // Guardar en el mapa para futuras actualizaciones
            if (messageId > 0) {
                messageSentMap.put(messageId, statusId);
            }
        } else {
            // Mensaje de otro: mostrar nombre del emisor + contenido + hora
            html = "<div class='msg-container'>" +
                   "<div class='bubble-other'>" +
                   "<div class='msg-header'><span class='sender-other'>" + usuario + "</span></div>" +
                   "<div class='msg-content'>" + texto + "</div>" +
                   "<div class='msg-footer'>" +
                   "<span class='timestamp'>" + timestamp + "</span>" +
                   "</div>" +
                   "</div></div>";
        }
        try {
            kit.insertHTML(doc, doc.getLength(), html, 0, 0, null);
            areaChat.setCaretPosition(doc.getLength()); // Auto-scroll al fondo
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Versión del método agregarBurbuja que acepta un timestamp específico
     */
    private void agregarBurbujaConHora(String usuario, String texto, boolean esMio, int messageId, boolean delivered, boolean read, String timestamp) {
        String html;
        if (esMio) {
            String statusIcon = getStatusIcon(delivered, read);
            String statusId = messageId > 0 ? "msg-status-" + messageId : "";

            html = "<div class='msg-container'>" +
                   "<div class='bubble-me'>" +
                   "<div class='msg-header'><span class='sender'>Tú</span></div>" +
                   "<div class='msg-content'>" + texto + "</div>" +
                   "<div class='msg-footer'>" +
                   "<span class='timestamp'>" + timestamp + "</span>" +
                   "<span class='status' id='" + statusId + "'>" + statusIcon + "</span>" +
                   "</div>" +
                   "</div></div>";

            if (messageId > 0) {
                messageSentMap.put(messageId, statusId);
            }
        } else {
            html = "<div class='msg-container'>" +
                   "<div class='bubble-other'>" +
                   "<div class='msg-header'><span class='sender-other'>" + usuario + "</span></div>" +
                   "<div class='msg-content'>" + texto + "</div>" +
                   "<div class='msg-footer'>" +
                   "<span class='timestamp'>" + timestamp + "</span>" +
                   "</div>" +
                   "</div></div>";
        }
        try {
            kit.insertHTML(doc, doc.getLength(), html, 0, 0, null);
            areaChat.setCaretPosition(doc.getLength());
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Añade un separador de día en el chat
     */
    private void agregarSeparadorDia(java.time.LocalDate fecha) {
        String textoFecha = formatearFechaSeparador(fecha);
        String html = "<div class='day-separator'>" +
                     "<span class='day-separator-label'>" + textoFecha + "</span>" +
                     "</div>";
        try {
            kit.insertHTML(doc, doc.getLength(), html, 0, 0, null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Formatea la fecha para el separador de día
     */
    private String formatearFechaSeparador(java.time.LocalDate fecha) {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.LocalDate ayer = hoy.minusDays(1);

        if (fecha.equals(hoy)) {
            return "Hoy";
        } else if (fecha.equals(ayer)) {
            return "Ayer";
        } else if (fecha.getYear() == hoy.getYear()) {
            // Mismo año: mostrar día y mes
            return fecha.format(java.time.format.DateTimeFormatter.ofPattern("d 'de' MMMM", new java.util.Locale("es", "ES")));
        } else {
            // Año diferente: mostrar fecha completa
            return fecha.format(java.time.format.DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new java.util.Locale("es", "ES")));
        }
    }

    /**
     * Obtiene el icono de estado según el estado del mensaje
     */
    private String getStatusIcon(boolean delivered, boolean read) {
        if (read) {
            return "<span class='status-read'>vv</span>"; // Azul: leído
        } else if (delivered) {
            return "<span class='status-delivered'>vv</span>"; // Gris claro: entregado
        } else {
            return "<span class='status-sent'>v</span>"; // Gris: enviado
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

    // Helper para crear botones con emoji (más confiable que imágenes)
    private JButton crearBotonImagen(String path, String textoAlt) {
        JButton btn = new JButton(textoAlt);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        btn.setForeground(StyleUtil.TEXT_PRIMARY);
        btn.setBackground(StyleUtil.BG_LIGHT);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Efecto hover
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(StyleUtil.BG_HOVER);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(StyleUtil.BG_LIGHT);
            }
        });

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
                senderName + " te ha enviado un mensaje.\n\n¿Deseas aceptar el chat y añadirlo a tus contactos?",
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
        // Si no hay resultados, primero buscar
        if (indicesResultados.isEmpty()) {
            buscarMensajes();
            return;
        }

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
        // Si no hay resultados, primero buscar
        if (indicesResultados.isEmpty()) {
            buscarMensajes();
            return;
        }

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
                    contenido = "[Archivo] " + msg.getFileName();
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
                    contenido = "[Archivo] " + msg.getFileName();
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
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String bordeExtra = esResultadoActual ? "border: 2px solid #FF9800;" : "";

        String html;
        if (esMio) {
            html = "<div class='msg-container'>" +
                   "<div class='bubble-me' style='" + bordeExtra + "'>" +
                   "<div class='msg-header'><span class='sender'>Tú</span></div>" +
                   "<div class='msg-content'>" + texto + "</div>" +
                   "<div class='msg-footer'><span class='timestamp'>" + timestamp + "</span></div>" +
                   "</div></div>";
        } else {
            html = "<div class='msg-container'>" +
                   "<div class='bubble-other' style='" + bordeExtra + "'>" +
                   "<div class='msg-header'><span class='sender-other'>" + usuario + "</span></div>" +
                   "<div class='msg-content'>" + texto + "</div>" +
                   "<div class='msg-footer'><span class='timestamp'>" + timestamp + "</span></div>" +
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
    // BÚSQUEDA/FILTRADO DE CHATS (Panel Izquierdo)
    // =================================================================

    /**
     * Muestra u oculta el panel de búsqueda de chats
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
     * Cierra el panel de búsqueda y restaura la lista completa
     */
    private void cerrarBusquedaGlobal() {
        panelBusquedaGlobal.setVisible(false);
        txtBusquedaGlobal.setText("");
        // Restaurar la lista completa de contactos
        restaurarListaCompleta();
    }

    /**
     * Filtra la lista de chats según el texto ingresado
     */
    private void filtrarListaChats() {
        String filtro = txtBusquedaGlobal.getText().trim().toLowerCase();

        if (filtro.isEmpty()) {
            // Si no hay filtro, mostrar todos
            restaurarListaCompleta();
            return;
        }

        // Filtrar la lista
        modeloContactos.clear();
        for (User u : listaCompletaContactos) {
            String nombre = u.getUsername().toLowerCase();
            if (nombre.contains(filtro)) {
                modeloContactos.addElement(u);
            }
        }
    }

    /**
     * Restaura la lista completa de contactos sin filtro
     */
    private void restaurarListaCompleta() {
        modeloContactos.clear();
        for (User u : listaCompletaContactos) {
            modeloContactos.addElement(u);
        }
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

    // =================================================================
    // GESTIÓN DE GRUPOS
    // =================================================================

    // Referencia a la ventana de grupos
    private GroupsFrame groupsFrame;

    /**
     * Abre la ventana de gestión de grupos
     */
    private void abrirVentanaGrupos() {
        if (groupsFrame != null && groupsFrame.isVisible()) {
            groupsFrame.toFront();
        } else {
            groupsFrame = new GroupsFrame();
            groupsFrame.setVisible(true);
        }
    }

    /**
     * Abre un chat de grupo (llamado desde GroupsFrame o desde la lista de contactos)
     * NOTA: El grupo.getId() debe ser el ID real (positivo) del grupo
     */
    public void abrirChatGrupo(Group grupo) {
        System.out.println("🔄 abrirChatGrupo: Abriendo grupo ID=" + grupo.getId() + " (" + grupo.getNombre() + ")");

        this.grupoActual = grupo;
        this.chatActualEsGrupo = true;

        // Mostrar botón de configuración de grupo
        if (btnConfigGrupo != null) {
            btnConfigGrupo.setVisible(true);
        }

        // Crear un User virtual para representar al grupo
        // IMPORTANTE: Usar el ID real del grupo (positivo) para que coincida con el historial
        User grupoComoUsuario = new User();
        grupoComoUsuario.setId(grupo.getId()); // ID real (positivo)
        grupoComoUsuario.setUsername(grupo.getNombre());
        grupoComoUsuario.setActivityStatus("grupo");

        this.contactoActual = grupoComoUsuario;

        System.out.println("🔄 abrirChatGrupo: contactoActual.getId()=" + contactoActual.getId() + ", chatActualEsGrupo=" + chatActualEsGrupo);

        // Actualizar título (mostrará "?" si no tenemos los miembros cargados)
        int memberCount = grupo.getMiembros() != null ? grupo.getMiembros().size() : 0;
        String memberText = memberCount > 0 ? String.valueOf(memberCount) : "?";
        lblTituloChat.setText(grupo.getNombre() + " (" + memberText + " miembros)");

        // Limpiar chat
        areaChat.setText("");
        mensajesActuales.clear();

        // Pedir historial del grupo
        Message msgHistory = new Message();
        msgHistory.setType(MessageType.GET_HISTORY);
        msgHistory.setReceiverId(grupo.getId()); // ID real del grupo
        msgHistory.setGroupChat(true);
        ClientSocket.getInstance().send(msgHistory);

        System.out.println("🔄 abrirChatGrupo: Solicitado historial para grupo ID=" + grupo.getId() + ", isGroupChat=true");

        // Siempre solicitar info actualizada del grupo (roles de admin, miembros, etc.)
        Message msgInfo = new Message();
        msgInfo.setType(MessageType.GET_GROUP_INFO);
        msgInfo.setReceiverId(grupo.getId());
        ClientSocket.getInstance().send(msgInfo);

        txtMensaje.requestFocus();
    }

    /**
     * Muestra el diálogo de configuración del grupo actual
     */
    private void mostrarConfiguracionGrupo() {
        if (!chatActualEsGrupo || grupoActual == null) {
            return;
        }

        // Crear diálogo
        JDialog dialogo = new JDialog(this, "Configuración del Grupo", true);
        dialogo.setSize(450, 580);
        dialogo.setLocationRelativeTo(this);

        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBackground(new Color(30, 30, 30));
        panelPrincipal.setBorder(new EmptyBorder(15, 15, 15, 15));

        int myId = ClientSocket.getInstance().getMyUserId();

        // Verificar si soy admin usando el nuevo campo isGroupAdmin de User
        boolean soyAdmin = false;
        if (grupoActual.getMiembros() != null) {
            for (User u : grupoActual.getMiembros()) {
                if (u.getId() == myId && u.isGroupAdmin()) {
                    soyAdmin = true;
                    break;
                }
            }
        }

        // Header con nombre del grupo
        JPanel headerPanel = new JPanel(new BorderLayout(5, 5));
        headerPanel.setBackground(new Color(30, 30, 30));

        JLabel lblNombre = new JLabel(grupoActual.getNombre());
        lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblNombre.setForeground(StyleUtil.TEXT_PRIMARY);
        headerPanel.add(lblNombre, BorderLayout.NORTH);

        int memberCount = grupoActual.getMiembros() != null ? grupoActual.getMiembros().size() : 0;

        // Contar admins
        int adminCount = 0;
        StringBuilder adminNames = new StringBuilder();
        if (grupoActual.getMiembros() != null) {
            for (User u : grupoActual.getMiembros()) {
                if (u.isGroupAdmin()) {
                    adminCount++;
                    if (adminNames.length() > 0) adminNames.append(", ");
                    adminNames.append(u.getUsername());
                    if (u.getId() == myId) adminNames.append(" (Tú)");
                }
            }
        }

        JLabel lblInfo = new JLabel("<html>" + memberCount + " miembros<br/>" +
                                   "<span style='color: #FFD700;'>[Admin] Admins (" + adminCount + "): " + adminNames + "</span>" +
                                   "</html>");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblInfo.setForeground(new Color(180, 180, 180));
        headerPanel.add(lblInfo, BorderLayout.CENTER);

        panelPrincipal.add(headerPanel, BorderLayout.NORTH);

        // Lista de miembros
        JPanel membersPanel = new JPanel(new BorderLayout(5, 5));
        membersPanel.setBackground(new Color(30, 30, 30));

        JLabel lblMiembros = new JLabel("Miembros:");
        lblMiembros.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblMiembros.setForeground(StyleUtil.TEXT_PRIMARY);
        membersPanel.add(lblMiembros, BorderLayout.NORTH);

        // Usar un modelo que guarde tanto el nombre mostrado como el User original
        DefaultListModel<String> memberModel = new DefaultListModel<>();
        java.util.Map<String, User> userMap = new java.util.HashMap<>();

        if (grupoActual.getMiembros() != null) {
            for (User u : grupoActual.getMiembros()) {
                String displayName;
                if (u.isGroupAdmin()) {
                    displayName = "* " + u.getUsername() + " (Admin)";
                } else {
                    displayName = "     " + u.getUsername();
                }
                String status = "activo".equals(u.getActivityStatus()) ? " 🟢" : " ⚫";
                String fullDisplay = displayName + status;
                memberModel.addElement(fullDisplay);
                userMap.put(fullDisplay, u);
            }
        }

        JList<String> listaMiembros = new JList<>(memberModel);
        listaMiembros.setBackground(new Color(40, 40, 40));
        listaMiembros.setForeground(StyleUtil.TEXT_PRIMARY);
        listaMiembros.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        listaMiembros.setFixedCellHeight(35);
        listaMiembros.setSelectionBackground(new Color(0, 120, 200));

        JScrollPane scrollMiembros = new JScrollPane(listaMiembros);
        scrollMiembros.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        membersPanel.add(scrollMiembros, BorderLayout.CENTER);

        panelPrincipal.add(membersPanel, BorderLayout.CENTER);

        // Panel de botones
        JPanel botonesPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        botonesPanel.setBackground(new Color(30, 30, 30));
        botonesPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        final boolean esSoyAdmin = soyAdmin;
        final int finalAdminCount = adminCount;

        if (soyAdmin) {
            // Botón añadir miembro
            JButton btnAddMember = new JButton("➕ Añadir Miembro");
            btnAddMember.setBackground(new Color(0, 120, 200));
            btnAddMember.setForeground(StyleUtil.TEXT_PRIMARY);
            btnAddMember.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnAddMember.addActionListener(e -> {
                String username = JOptionPane.showInputDialog(dialogo,
                    "Nombre del usuario a añadir:",
                    "Añadir Miembro",
                    JOptionPane.PLAIN_MESSAGE);
                if (username != null && !username.trim().isEmpty()) {
                    Message msg = new Message();
                    msg.setType(MessageType.ADD_GROUP_MEMBER);
                    msg.setReceiverId(grupoActual.getId());
                    msg.setContent(username.trim());
                    ClientSocket.getInstance().send(msg);
                    dialogo.dispose();
                }
            });
            botonesPanel.add(btnAddMember);

            // Botón eliminar miembro
            JButton btnRemoveMember = new JButton("➖ Eliminar Miembro");
            btnRemoveMember.setBackground(new Color(200, 80, 80));
            btnRemoveMember.setForeground(StyleUtil.TEXT_PRIMARY);
            btnRemoveMember.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnRemoveMember.addActionListener(e -> {
                String selected = listaMiembros.getSelectedValue();
                if (selected == null) {
                    JOptionPane.showMessageDialog(dialogo, "Selecciona un miembro primero");
                    return;
                }
                User selectedUser = userMap.get(selected);
                if (selectedUser == null) return;

                // No permitir eliminar si es admin único
                if (selectedUser.isGroupAdmin() && finalAdminCount <= 1) {
                    JOptionPane.showMessageDialog(dialogo,
                        "No puedes eliminar al único administrador.\nPromueve a otro admin primero.",
                        "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(dialogo,
                    "¿Eliminar a " + selectedUser.getUsername() + " del grupo?",
                    "Confirmar",
                    JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    Message msg = new Message();
                    msg.setType(MessageType.REMOVE_GROUP_MEMBER);
                    msg.setReceiverId(grupoActual.getId());
                    msg.setContent(selectedUser.getUsername());
                    ClientSocket.getInstance().send(msg);
                    dialogo.dispose();
                }
            });
            botonesPanel.add(btnRemoveMember);

            // Botón promover a admin
            JButton btnPromoteAdmin = new JButton("Hacer Admin");
            btnPromoteAdmin.setBackground(new Color(255, 193, 7));
            btnPromoteAdmin.setForeground(Color.BLACK);
            btnPromoteAdmin.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnPromoteAdmin.addActionListener(e -> {
                String selected = listaMiembros.getSelectedValue();
                if (selected == null) {
                    JOptionPane.showMessageDialog(dialogo, "Selecciona un miembro primero");
                    return;
                }
                User selectedUser = userMap.get(selected);
                if (selectedUser == null) return;

                if (selectedUser.isGroupAdmin()) {
                    JOptionPane.showMessageDialog(dialogo, "Este usuario ya es administrador");
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(dialogo,
                    "¿Hacer administrador a " + selectedUser.getUsername() + "?",
                    "Confirmar",
                    JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    Message msg = new Message();
                    msg.setType(MessageType.PROMOTE_TO_ADMIN);
                    msg.setReceiverId(grupoActual.getId());
                    msg.setContent(selectedUser.getUsername());
                    ClientSocket.getInstance().send(msg);
                    dialogo.dispose();
                }
            });
            botonesPanel.add(btnPromoteAdmin);

            // Botón quitar admin
            JButton btnDemoteAdmin = new JButton("⬇️ Quitar Admin");
            btnDemoteAdmin.setBackground(new Color(255, 152, 0));
            btnDemoteAdmin.setForeground(Color.BLACK);
            btnDemoteAdmin.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnDemoteAdmin.addActionListener(e -> {
                String selected = listaMiembros.getSelectedValue();
                if (selected == null) {
                    JOptionPane.showMessageDialog(dialogo, "Selecciona un miembro primero");
                    return;
                }
                User selectedUser = userMap.get(selected);
                if (selectedUser == null) return;

                if (!selectedUser.isGroupAdmin()) {
                    JOptionPane.showMessageDialog(dialogo, "Este usuario no es administrador");
                    return;
                }

                if (finalAdminCount <= 1) {
                    JOptionPane.showMessageDialog(dialogo,
                        "No puedes quitar el rol de admin.\nDebe haber al menos un administrador.",
                        "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(dialogo,
                    "¿Quitar rol de administrador a " + selectedUser.getUsername() + "?",
                    "Confirmar",
                    JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    Message msg = new Message();
                    msg.setType(MessageType.DEMOTE_FROM_ADMIN);
                    msg.setReceiverId(grupoActual.getId());
                    msg.setContent(selectedUser.getUsername());
                    ClientSocket.getInstance().send(msg);
                    dialogo.dispose();
                }
            });
            botonesPanel.add(btnDemoteAdmin);
        }

        // Botón abandonar grupo
        JButton btnLeave = new JButton("Abandonar Grupo");
        btnLeave.setBackground(new Color(100, 100, 100));
        btnLeave.setForeground(StyleUtil.TEXT_PRIMARY);
        btnLeave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLeave.addActionListener(e -> {
            String mensaje;
            if (esSoyAdmin && finalAdminCount <= 1) {
                mensaje = "⚠️ Eres el único administrador.\n\n" +
                          "Debes promover a otro admin antes de salir, o el grupo será eliminado.\n\n¿Continuar de todas formas?";
            } else {
                mensaje = "¿Estás seguro de que quieres abandonar este grupo?";
            }
            int confirm = JOptionPane.showConfirmDialog(dialogo,
                mensaje,
                "Abandonar Grupo",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                Message msg = new Message();
                msg.setType(MessageType.LEAVE_GROUP);
                msg.setReceiverId(grupoActual.getId());
                ClientSocket.getInstance().send(msg);
                dialogo.dispose();
            }
        });
        botonesPanel.add(btnLeave);

        // Botón cerrar
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setBackground(new Color(60, 60, 60));
        btnCerrar.setForeground(StyleUtil.TEXT_PRIMARY);
        btnCerrar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCerrar.addActionListener(e -> dialogo.dispose());
        botonesPanel.add(btnCerrar);

        panelPrincipal.add(botonesPanel, BorderLayout.SOUTH);

        dialogo.setContentPane(panelPrincipal);
        dialogo.setVisible(true);
    }

    /**
     * Actualiza la información del grupo actual (llamado cuando llega GROUP_INFO_RESPONSE o cambios de miembros)
     */
    public void actualizarInfoGrupoActual(Group grupo) {
        SwingUtilities.invokeLater(() -> {
            if (chatActualEsGrupo && grupoActual != null && grupoActual.getId() == grupo.getId()) {
                this.grupoActual = grupo;
                int memberCount = grupo.getMiembros() != null ? grupo.getMiembros().size() : 0;
                lblTituloChat.setText(grupo.getNombre() + " (" + memberCount + " miembros)");
                System.out.println("✅ Actualizado título del grupo: " + memberCount + " miembros");
            }
        });
    }

    /**
     * Actualiza la lista de grupos (llamado desde ClientSocket)
     * También añade los grupos a la lista de contactos para acceso rápido
     * NOTA: Los grupos se almacenan con ID negativo (-groupId) para evitar colisiones con IDs de usuarios
     */
    public void actualizarGrupos(List<Group> grupos) {
        // Notificamos a GroupsFrame si está abierto
        if (groupsFrame != null && groupsFrame.isVisible()) {
            groupsFrame.actualizarGrupos(grupos);
        }

        // Añadir grupos a la lista de contactos (al principio)
        SwingUtilities.invokeLater(() -> {
            // Guardar usuarios actuales (no grupos)
            java.util.List<User> usuariosActuales = new java.util.ArrayList<>();
            for (int i = 0; i < modeloContactos.size(); i++) {
                User u = modeloContactos.get(i);
                if (!"grupo".equals(u.getActivityStatus())) {
                    usuariosActuales.add(u);
                }
            }

            // Limpiar y reconstruir la lista
            modeloContactos.clear();

            // Primero añadir los grupos (con ID negativo para evitar colisiones)
            if (grupos != null) {
                for (Group g : grupos) {
                    User grupoComoUsuario = new User();
                    // Usar ID negativo para grupos: -groupId
                    grupoComoUsuario.setId(-g.getId());
                    grupoComoUsuario.setUsername(g.getNombre());
                    grupoComoUsuario.setActivityStatus("grupo");
                    modeloContactos.addElement(grupoComoUsuario);
                }
            }

            // Luego añadir los usuarios
            for (User u : usuariosActuales) {
                modeloContactos.addElement(u);
            }
        });
    }

    /**
     * Muestra un menú contextual según el tipo de elemento (usuario o grupo)
     */
    private void mostrarMenuContextual(User elemento, Component invoker, int x, int y) {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBackground(new Color(40, 40, 40));

        boolean esGrupo = "grupo".equals(elemento.getActivityStatus());

        if (esGrupo) {
            // Para grupos, el ID está negativo, convertir a positivo
            int realGroupId = -elemento.getId();
            String nombreGrupo = elemento.getUsername();

            // Menú para grupos
            JMenuItem itemAbrir = new JMenuItem("Abrir Chat");
            itemAbrir.setBackground(new Color(40, 40, 40));
            itemAbrir.setForeground(StyleUtil.TEXT_PRIMARY);
            itemAbrir.addActionListener(e -> {
                Group grupo = new Group(realGroupId, nombreGrupo, 0);
                abrirChatGrupo(grupo);
            });

            JMenuItem itemAdmin = new JMenuItem("Administrar Grupo");
            itemAdmin.setBackground(new Color(40, 40, 40));
            itemAdmin.setForeground(StyleUtil.TEXT_PRIMARY);
            itemAdmin.addActionListener(e -> {
                // Abrir ventana de grupos y seleccionar este grupo
                abrirVentanaGrupos();
            });

            JMenuItem itemAbandonar = new JMenuItem("Abandonar Grupo");
            itemAbandonar.setBackground(new Color(40, 40, 40));
            itemAbandonar.setForeground(new Color(255, 100, 100));
            itemAbandonar.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Seguro que quieres abandonar este grupo?",
                    "Abandonar Grupo",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    Message msg = new Message();
                    msg.setType(MessageType.LEAVE_GROUP);
                    msg.setReceiverId(realGroupId); // Usar el ID real del grupo
                    ClientSocket.getInstance().send(msg);
                }
            });

            popupMenu.add(itemAbrir);
            popupMenu.add(itemAdmin);
            popupMenu.addSeparator();
            popupMenu.add(itemAbandonar);

        } else {
            // Menú para usuarios
            JMenuItem itemAbrir = new JMenuItem("Abrir Chat");
            itemAbrir.setBackground(new Color(40, 40, 40));
            itemAbrir.setForeground(StyleUtil.TEXT_PRIMARY);
            itemAbrir.addActionListener(e -> cambiarChat(elemento));

            JMenuItem itemEliminar = new JMenuItem("Eliminar Contacto");
            itemEliminar.setBackground(new Color(40, 40, 40));
            itemEliminar.setForeground(new Color(255, 100, 100));
            itemEliminar.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar a " + elemento.getUsername() + " de tus contactos?",
                    "Eliminar Contacto",
                    JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    Message msg = new Message();
                    msg.setType(MessageType.REMOVE_CONTACT);
                    msg.setContent(elemento.getUsername());
                    ClientSocket.getInstance().send(msg);
                }
            });

            popupMenu.add(itemAbrir);
            popupMenu.addSeparator();
            popupMenu.add(itemEliminar);
        }

        popupMenu.show(invoker, x, y);
    }
}
