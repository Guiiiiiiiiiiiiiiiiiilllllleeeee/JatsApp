package com.jatsapp.server.ui;

import com.jatsapp.server.ServerCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ServerGUI extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(ServerGUI.class);
    private static final Logger activityLogger = LoggerFactory.getLogger("com.jatsapp.server.activity");

    // Componentes principales
    private JTextArea logsArea;
    private JTextArea activityArea;
    private JTextArea errorsArea;
    private JLabel statusLabel;
    private JLabel clientsLabel;
    private JButton startButton;
    private JButton stopButton;
    private JButton clearLogsButton;
    private JTabbedPane tabbedPane;

    // Servidor
    private ServerCore serverCore;
    private Thread serverThread;
    private boolean isServerRunning = false;

    // Timer para actualizar logs
    private Timer logUpdateTimer;

    public ServerGUI() {
        initializeUI();
        startLogUpdateTimer();
    }

    private void initializeUI() {
        setTitle("JatsApp Server - Panel de Control");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Layout principal
        setLayout(new BorderLayout(10, 10));

        // Panel superior: Control y estado
        add(createControlPanel(), BorderLayout.NORTH);

        // Panel central: Tabs con logs y base de datos
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("📋 Logs Generales", createLogsPanel());
        tabbedPane.addTab("👥 Actividad Usuarios", createActivityPanel());
        tabbedPane.addTab("❌ Errores", createErrorsPanel());
        tabbedPane.addTab("💾 Base de Datos", createDatabasePanel());

        add(tabbedPane, BorderLayout.CENTER);

        // Panel inferior: Información
        add(createStatusBar(), BorderLayout.SOUTH);

        // Configurar shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (isServerRunning) {
                stopServer();
            }
        }));
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(45, 45, 48));

        // Panel izquierdo: Botones de control
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonsPanel.setBackground(new Color(45, 45, 48));

        startButton = new JButton("▶️ Iniciar Servidor");
        startButton.setPreferredSize(new Dimension(160, 35));
        startButton.setBackground(new Color(0, 122, 204));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.addActionListener(e -> startServer());

        stopButton = new JButton("⏹️ Detener Servidor");
        stopButton.setPreferredSize(new Dimension(160, 35));
        stopButton.setBackground(new Color(232, 17, 35));
        stopButton.setForeground(Color.WHITE);
        stopButton.setFocusPainted(false);
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopServer());

        clearLogsButton = new JButton("🗑️ Limpiar Logs");
        clearLogsButton.setPreferredSize(new Dimension(140, 35));
        clearLogsButton.setBackground(new Color(100, 100, 100));
        clearLogsButton.setForeground(Color.WHITE);
        clearLogsButton.setFocusPainted(false);
        clearLogsButton.addActionListener(e -> clearLogs());

        buttonsPanel.add(startButton);
        buttonsPanel.add(stopButton);
        buttonsPanel.add(clearLogsButton);

        // Panel derecho: Estado
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        statusPanel.setBackground(new Color(45, 45, 48));

        statusLabel = new JLabel("⚫ Servidor Detenido");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(Color.LIGHT_GRAY);

        clientsLabel = new JLabel("👥 Clientes: 0");
        clientsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        clientsLabel.setForeground(Color.LIGHT_GRAY);

        statusPanel.add(statusLabel);
        statusPanel.add(new JSeparator(SwingConstants.VERTICAL));
        statusPanel.add(clientsLabel);

        panel.add(buttonsPanel, BorderLayout.WEST);
        panel.add(statusPanel, BorderLayout.EAST);

        return panel;
    }

    private JScrollPane createLogsPanel() {
        logsArea = new JTextArea();
        logsArea.setEditable(false);
        logsArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logsArea.setBackground(new Color(30, 30, 30));
        logsArea.setForeground(new Color(220, 220, 220));
        logsArea.setCaretColor(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(logsArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        return scrollPane;
    }

    private JScrollPane createActivityPanel() {
        activityArea = new JTextArea();
        activityArea.setEditable(false);
        activityArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        activityArea.setBackground(new Color(30, 30, 30));
        activityArea.setForeground(new Color(173, 216, 230));
        activityArea.setCaretColor(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(activityArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        return scrollPane;
    }

    private JScrollPane createErrorsPanel() {
        errorsArea = new JTextArea();
        errorsArea.setEditable(false);
        errorsArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        errorsArea.setBackground(new Color(30, 30, 30));
        errorsArea.setForeground(new Color(255, 100, 100));
        errorsArea.setCaretColor(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(errorsArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        return scrollPane;
    }

    private JPanel createDatabasePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(new Color(40, 40, 40));

        // Panel superior con tabs para diferentes tablas
        JTabbedPane dbTabs = new JTabbedPane();
        dbTabs.setBackground(new Color(40, 40, 40));

        dbTabs.addTab("👤 Usuarios", createUsersTablePanel());
        dbTabs.addTab("💬 Mensajes", createMessagesTablePanel());
        dbTabs.addTab("👥 Grupos", createGroupsTablePanel());
        dbTabs.addTab("📞 Contactos", createContactsTablePanel());

        panel.add(dbTabs, BorderLayout.CENTER);

        // Botón para refrescar datos
        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshPanel.setBackground(new Color(40, 40, 40));

        JButton refreshButton = new JButton("🔄 Refrescar Datos");
        refreshButton.setPreferredSize(new Dimension(150, 30));
        refreshButton.setBackground(new Color(0, 122, 204));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> refreshDatabaseTables());

        refreshPanel.add(refreshButton);
        panel.add(refreshPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JScrollPane createUsersTablePanel() {
        String[] columns = {"ID", "Usuario", "Email", "Estado", "Último Acceso"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setName("usersTable");
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        return new JScrollPane(table);
    }

    private JScrollPane createMessagesTablePanel() {
        String[] columns = {"ID", "Emisor", "Receptor", "Tipo", "Contenido", "Fecha"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setName("messagesTable");
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        return new JScrollPane(table);
    }

    private JScrollPane createGroupsTablePanel() {
        String[] columns = {"ID Grupo", "Nombre", "Admin ID", "Fecha Creación"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setName("groupsTable");
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        return new JScrollPane(table);
    }

    private JScrollPane createContactsTablePanel() {
        String[] columns = {"Propietario ID", "Contacto ID", "Fecha Agregado"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setName("contactsTable");
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        return new JScrollPane(table);
    }

    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBackground(new Color(45, 45, 48));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel timeLabel = new JLabel();
        timeLabel.setForeground(Color.LIGHT_GRAY);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        // Actualizar hora cada segundo
        Timer timeTimer = new Timer(1000, e -> {
            timeLabel.setText("🕐 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        });
        timeTimer.start();
        timeLabel.setText("🕐 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        panel.add(timeLabel);

        return panel;
    }

    private void startServer() {
        if (isServerRunning) {
            JOptionPane.showMessageDialog(this, "El servidor ya está ejecutándose", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            serverCore = new ServerCore();
            serverThread = new Thread(() -> {
                logger.info("Iniciando servidor desde GUI...");
                activityLogger.info("SERVIDOR INICIADO DESDE GUI");
                serverCore.startServer();
            }, "ServerCore-Thread");

            serverThread.start();
            isServerRunning = true;

            // Actualizar UI
            SwingUtilities.invokeLater(() -> {
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                statusLabel.setText("🟢 Servidor Activo");
                statusLabel.setForeground(new Color(0, 255, 0));
                appendToLogsArea("===========================================\n");
                appendToLogsArea("✅ Servidor iniciado en puerto 5555\n");
                appendToLogsArea("===========================================\n");
            });

            // Iniciar actualización de clientes
            startClientsUpdateTimer();

        } catch (Exception e) {
            logger.error("Error iniciando servidor desde GUI", e);
            JOptionPane.showMessageDialog(this, "Error al iniciar servidor: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        if (!isServerRunning) {
            JOptionPane.showMessageDialog(this, "El servidor no está ejecutándose", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de que desea detener el servidor?",
            "Confirmar",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                logger.info("Deteniendo servidor desde GUI...");
                activityLogger.info("SERVIDOR DETENIDO DESDE GUI");

                if (serverCore != null) {
                    serverCore.stopServer();
                }

                isServerRunning = false;

                // Actualizar UI
                SwingUtilities.invokeLater(() -> {
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    statusLabel.setText("⚫ Servidor Detenido");
                    statusLabel.setForeground(Color.LIGHT_GRAY);
                    clientsLabel.setText("👥 Clientes: 0");
                    appendToLogsArea("===========================================\n");
                    appendToLogsArea("⏹️ Servidor detenido correctamente\n");
                    appendToLogsArea("===========================================\n");
                });

            } catch (Exception e) {
                logger.error("Error deteniendo servidor desde GUI", e);
                JOptionPane.showMessageDialog(this, "Error al detener servidor: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearLogs() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Limpiar todas las áreas de logs?",
            "Confirmar",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            logsArea.setText("");
            activityArea.setText("");
            errorsArea.setText("");
            logger.info("Logs de GUI limpiados");
        }
    }

    private void startLogUpdateTimer() {
        logUpdateTimer = new Timer(2000, e -> updateLogsFromFiles());
        logUpdateTimer.start();
    }

    private void startClientsUpdateTimer() {
        Timer clientsTimer = new Timer(1000, e -> {
            if (isServerRunning && serverCore != null) {
                int clients = serverCore.getConnectedClientsCount();
                SwingUtilities.invokeLater(() ->
                    clientsLabel.setText("👥 Clientes: " + clients)
                );
            }
        });
        clientsTimer.start();
    }

    private void updateLogsFromFiles() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Actualizar logs generales
                if (Files.exists(Paths.get("logs/jatsapp-server.log"))) {
                    List<String> lines = Files.readAllLines(Paths.get("logs/jatsapp-server.log"));
                    int start = Math.max(0, lines.size() - 200); // Últimas 200 líneas
                    StringBuilder sb = new StringBuilder();
                    for (int i = start; i < lines.size(); i++) {
                        sb.append(lines.get(i)).append("\n");
                    }
                    logsArea.setText(sb.toString());
                    logsArea.setCaretPosition(logsArea.getDocument().getLength());
                }

                // Actualizar logs de actividad
                if (Files.exists(Paths.get("logs/jatsapp-activity.log"))) {
                    List<String> lines = Files.readAllLines(Paths.get("logs/jatsapp-activity.log"));
                    int start = Math.max(0, lines.size() - 200);
                    StringBuilder sb = new StringBuilder();
                    for (int i = start; i < lines.size(); i++) {
                        sb.append(lines.get(i)).append("\n");
                    }
                    activityArea.setText(sb.toString());
                    activityArea.setCaretPosition(activityArea.getDocument().getLength());
                }

                // Actualizar logs de errores
                if (Files.exists(Paths.get("logs/jatsapp-server-errors.log"))) {
                    List<String> lines = Files.readAllLines(Paths.get("logs/jatsapp-server-errors.log"));
                    int start = Math.max(0, lines.size() - 200);
                    StringBuilder sb = new StringBuilder();
                    for (int i = start; i < lines.size(); i++) {
                        sb.append(lines.get(i)).append("\n");
                    }
                    errorsArea.setText(sb.toString());
                    errorsArea.setCaretPosition(errorsArea.getDocument().getLength());
                }

            } catch (IOException ex) {
                // Logs aún no creados, ignorar silenciosamente
            }
        });
    }

    private void appendToLogsArea(String text) {
        logsArea.append(text);
        logsArea.setCaretPosition(logsArea.getDocument().getLength());
    }

    private void refreshDatabaseTables() {
        // Esta funcionalidad se implementará en DatabaseManager
        DatabaseViewerPanel dbPanel = new DatabaseViewerPanel();
        dbPanel.refreshAllTables();

        // Buscar y actualizar las tablas en el panel de base de datos
        Component dbComponent = tabbedPane.getComponentAt(3); // Tab de BD
        if (dbComponent instanceof JPanel) {
            updateTablesInPanel((JPanel) dbComponent);
        }
    }

    private void updateTablesInPanel(JPanel panel) {
        // Buscar recursivamente las tablas y actualizarlas
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JTabbedPane) {
                JTabbedPane tabs = (JTabbedPane) comp;
                for (int i = 0; i < tabs.getTabCount(); i++) {
                    Component tabComp = tabs.getComponentAt(i);
                    if (tabComp instanceof JScrollPane) {
                        JScrollPane scroll = (JScrollPane) tabComp;
                        if (scroll.getViewport().getView() instanceof JTable) {
                            JTable table = (JTable) scroll.getViewport().getView();
                            refreshTable(table);
                        }
                    }
                }
            }
        }
    }

    private void refreshTable(JTable table) {
        DatabaseViewerPanel.refreshTableData(table);
    }

    public static void main(String[] args) {
        // Configurar Look and Feel del sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Crear y mostrar GUI
        SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI();
            gui.setVisible(true);
        });
    }
}

