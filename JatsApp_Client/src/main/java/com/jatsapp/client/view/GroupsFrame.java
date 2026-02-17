package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.client.util.StyleUtil;
import com.jatsapp.common.Group;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;
import com.jatsapp.common.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Frame para gestionar grupos:
 * - Ver lista de grupos
 * - Crear nuevos grupos
 * - Ver/gestionar miembros
 */
public class GroupsFrame extends JFrame {

    private DefaultListModel<Group> groupListModel;
    private JList<Group> groupList;
    private JPanel detailPanel;

    // Panel de detalles del grupo seleccionado
    private JLabel lblGroupName;
    private JLabel lblGroupAdmin;
    private JLabel lblMemberCount;
    private DefaultListModel<User> memberListModel;
    private JList<User> memberList;

    private Group selectedGroup = null;

    public GroupsFrame() {
        setTitle("Mis Grupos - JatsApp");
        setSize(800, 550);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(650, 450));

        // Registrarse en ClientSocket
        ClientSocket.getInstance().setGroupsFrame(this);

        // Panel principal con BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(StyleUtil.BG_DARK);

        // ===== PANEL IZQUIERDO: Lista de grupos =====
        JPanel leftPanel = new JPanel(new BorderLayout(0, 0));
        leftPanel.setBackground(StyleUtil.BG_MEDIUM);
        leftPanel.setPreferredSize(new Dimension(280, 0));
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, StyleUtil.BORDER_DARK));

        JLabel lblTitulo = new JLabel("Mis Grupos");
        lblTitulo.setFont(StyleUtil.FONT_SUBTITLE);
        lblTitulo.setForeground(StyleUtil.TEXT_PRIMARY);
        lblTitulo.setBorder(new EmptyBorder(20, 20, 15, 20));
        leftPanel.add(lblTitulo, BorderLayout.NORTH);

        // Lista de grupos
        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setCellRenderer(new GroupListRenderer());
        groupList.setBackground(StyleUtil.BG_MEDIUM);
        groupList.setForeground(StyleUtil.TEXT_PRIMARY);
        groupList.setSelectionBackground(StyleUtil.BG_SELECTED);
        groupList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Group selected = groupList.getSelectedValue();
                if (selected != null) {
                    mostrarDetallesGrupo(selected);
                }
            }
        });

        JScrollPane scrollGroups = new JScrollPane(groupList);
        scrollGroups.setBorder(null);
        StyleUtil.styleScrollPane(scrollGroups);
        leftPanel.add(scrollGroups, BorderLayout.CENTER);

        // Botón crear grupo
        JButton btnCrearGrupo = StyleUtil.createPrimaryButton("+ Crear Grupo");
        btnCrearGrupo.addActionListener(e -> crearNuevoGrupo());

        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.setBackground(StyleUtil.BG_MEDIUM);
        btnPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        btnPanel.add(btnCrearGrupo, BorderLayout.CENTER);
        leftPanel.add(btnPanel, BorderLayout.SOUTH);

        mainPanel.add(leftPanel, BorderLayout.WEST);

        // ===== PANEL DERECHO: Detalles del grupo =====
        detailPanel = new JPanel(new BorderLayout(10, 10));
        detailPanel.setBackground(StyleUtil.BG_DARK);
        detailPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Header con información del grupo
        JPanel headerPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        headerPanel.setBackground(StyleUtil.BG_DARK);

        lblGroupName = new JLabel("Selecciona un grupo");
        lblGroupName.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblGroupName.setForeground(StyleUtil.TEXT_PRIMARY);

        lblGroupAdmin = new JLabel("");
        lblGroupAdmin.setFont(StyleUtil.FONT_BODY);
        lblGroupAdmin.setForeground(StyleUtil.PRIMARY);

        lblMemberCount = new JLabel("");
        lblMemberCount.setFont(StyleUtil.FONT_SMALL);
        lblMemberCount.setForeground(StyleUtil.TEXT_SECONDARY);

        headerPanel.add(lblGroupName);
        headerPanel.add(lblGroupAdmin);
        headerPanel.add(lblMemberCount);
        detailPanel.add(headerPanel, BorderLayout.NORTH);

        // Lista de miembros
        JPanel membersPanel = new JPanel(new BorderLayout(5, 10));
        membersPanel.setBackground(StyleUtil.BG_DARK);

        JLabel lblMiembros = new JLabel("Miembros del grupo:");
        lblMiembros.setFont(StyleUtil.FONT_HEADING);
        lblMiembros.setForeground(StyleUtil.TEXT_PRIMARY);
        membersPanel.add(lblMiembros, BorderLayout.NORTH);

        memberListModel = new DefaultListModel<>();
        memberList = new JList<>(memberListModel);
        memberList.setCellRenderer(new MemberListRenderer());
        memberList.setBackground(StyleUtil.BG_LIGHT);
        memberList.setForeground(StyleUtil.TEXT_PRIMARY);

        JScrollPane scrollMembers = new JScrollPane(memberList);
        scrollMembers.setBorder(BorderFactory.createLineBorder(StyleUtil.BORDER_LIGHT, 1, true));
        StyleUtil.styleScrollPane(scrollMembers);
        membersPanel.add(scrollMembers, BorderLayout.CENTER);

        detailPanel.add(membersPanel, BorderLayout.CENTER);

        // Botones de acción
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        actionPanel.setBackground(StyleUtil.BG_DARK);

        JButton btnAddMember = StyleUtil.createAccentButton("Añadir");
        btnAddMember.addActionListener(e -> añadirMiembro());

        JButton btnRemoveMember = StyleUtil.createDangerButton("Eliminar");
        btnRemoveMember.addActionListener(e -> eliminarMiembro());

        JButton btnLeaveGroup = StyleUtil.createSecondaryButton("Abandonar");
        btnLeaveGroup.addActionListener(e -> abandonarGrupo());

        JButton btnOpenChat = StyleUtil.createPrimaryButton("Abrir Chat");
        btnOpenChat.addActionListener(e -> abrirChatGrupo());

        actionPanel.add(btnAddMember);
        actionPanel.add(btnRemoveMember);
        actionPanel.add(btnLeaveGroup);
        actionPanel.add(btnOpenChat);

        detailPanel.add(actionPanel, BorderLayout.SOUTH);

        mainPanel.add(detailPanel, BorderLayout.CENTER);

        add(mainPanel);

        // Listener para limpiar referencia al cerrar
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                ClientSocket.getInstance().setGroupsFrame(null);
            }
        });

        // Cargar grupos al abrir
        cargarGrupos();
    }

    /**
     * Solicita la lista de grupos al servidor
     */
    private void cargarGrupos() {
        Message msg = new Message();
        msg.setType(MessageType.GET_GROUPS);
        ClientSocket.getInstance().send(msg);
    }

    /**
     * Actualiza la lista de grupos (llamado desde ClientSocket)
     */
    public void actualizarGrupos(List<Group> grupos) {
        SwingUtilities.invokeLater(() -> {
            groupListModel.clear();
            for (Group g : grupos) {
                groupListModel.addElement(g);
            }

            // Si teníamos un grupo seleccionado, actualizar sus detalles
            if (selectedGroup != null) {
                for (Group g : grupos) {
                    if (g.getId() == selectedGroup.getId()) {
                        mostrarDetallesGrupo(g);
                        return;
                    }
                }
                // El grupo ya no existe (fue eliminado)
                limpiarDetalles();
            }
        });
    }

    /**
     * Muestra los detalles de un grupo en el panel derecho
     */
    private void mostrarDetallesGrupo(Group group) {
        mostrarDetallesGrupo(group, true);
    }

    /**
     * Muestra los detalles de un grupo en el panel derecho
     * @param solicitarActualizacion si es true, solicita info actualizada al servidor
     */
    private void mostrarDetallesGrupo(Group group, boolean solicitarActualizacion) {
        this.selectedGroup = group;

        lblGroupName.setText(group.getNombre());
        lblMemberCount.setText("Miembros: " + (group.getMiembros() != null ? group.getMiembros().size() : "?")
                              + "/" + Group.MAX_MEMBERS);

        // Determinar si el usuario actual es admin y contar admins
        int myId = ClientSocket.getInstance().getMyUserId();
        boolean soyAdmin = false;
        StringBuilder adminNames = new StringBuilder();
        int adminCount = 0;

        if (group.getMiembros() != null) {
            for (User u : group.getMiembros()) {
                if (u.isGroupAdmin()) {
                    adminCount++;
                    if (adminNames.length() > 0) adminNames.append(", ");
                    adminNames.append(u.getUsername());
                    if (u.getId() == myId) {
                        soyAdmin = true;
                        adminNames.append(" (Tú)");
                    }
                }
            }
        }

        if (soyAdmin) {
            lblGroupAdmin.setText("* Eres administrador (" + adminCount + " admin" + (adminCount > 1 ? "s" : "") + ")");
        } else {
            lblGroupAdmin.setText("* Admins: " + (adminNames.length() > 0 ? adminNames.toString() : "Sin admins"));
        }

        // Actualizar lista de miembros
        memberListModel.clear();
        if (group.getMiembros() != null) {
            for (User u : group.getMiembros()) {
                memberListModel.addElement(u);
            }
        }

        // Solo solicitar información actualizada del grupo si se indica
        // (evita el loop infinito)
        if (solicitarActualizacion) {
            Message msg = new Message();
            msg.setType(MessageType.GET_GROUP_INFO);
            msg.setReceiverId(group.getId());
            ClientSocket.getInstance().send(msg);
        }
    }

    /**
     * Actualiza la información de un grupo específico (llamado desde ClientSocket)
     */
    public void actualizarInfoGrupo(Group group) {
        SwingUtilities.invokeLater(() -> {
            // Actualizar detalles SIN solicitar otra vez al servidor (false)
            if (selectedGroup != null && selectedGroup.getId() == group.getId()) {
                mostrarDetallesGrupo(group, false);
            }

            // Actualizar también en la lista
            for (int i = 0; i < groupListModel.size(); i++) {
                if (groupListModel.get(i).getId() == group.getId()) {
                    groupListModel.set(i, group);
                    break;
                }
            }
        });
    }

    /**
     * Limpia el panel de detalles
     */
    private void limpiarDetalles() {
        selectedGroup = null;
        lblGroupName.setText("Selecciona un grupo");
        lblGroupAdmin.setText("");
        lblMemberCount.setText("");
        memberListModel.clear();
    }

    /**
     * Abre diálogo para crear un nuevo grupo
     */
    private void crearNuevoGrupo() {
        String nombre = JOptionPane.showInputDialog(this,
            "Introduce el nombre del grupo:",
            "Crear Nuevo Grupo",
            JOptionPane.PLAIN_MESSAGE);

        if (nombre != null && !nombre.trim().isEmpty()) {
            Message msg = new Message();
            msg.setType(MessageType.CREATE_GROUP);
            msg.setContent(nombre.trim());
            ClientSocket.getInstance().send(msg);
        }
    }

    /**
     * Abre diálogo para añadir un miembro al grupo seleccionado
     */
    private void añadirMiembro() {
        if (selectedGroup == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un grupo primero");
            return;
        }

        // Verificar si soy admin (usando el nuevo campo isGroupAdmin)
        boolean soyAdmin = false;
        int myId = ClientSocket.getInstance().getMyUserId();
        if (selectedGroup.getMiembros() != null) {
            for (User u : selectedGroup.getMiembros()) {
                if (u.getId() == myId && u.isGroupAdmin()) {
                    soyAdmin = true;
                    break;
                }
            }
        }

        if (!soyAdmin) {
            JOptionPane.showMessageDialog(this,
                "Solo los administradores pueden añadir miembros",
                "Sin permisos",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Verificar límite
        if (selectedGroup.getMiembros() != null && selectedGroup.getMiembros().size() >= Group.MAX_MEMBERS) {
            JOptionPane.showMessageDialog(this,
                "El grupo ha alcanzado el límite de " + Group.MAX_MEMBERS + " miembros",
                "Grupo lleno",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String username = JOptionPane.showInputDialog(this,
            "Introduce el nombre del usuario a añadir:",
            "Añadir Miembro",
            JOptionPane.PLAIN_MESSAGE);

        if (username != null && !username.trim().isEmpty()) {
            Message msg = new Message();
            msg.setType(MessageType.ADD_GROUP_MEMBER);
            msg.setReceiverId(selectedGroup.getId());
            msg.setContent(username.trim());
            ClientSocket.getInstance().send(msg);
        }
    }

    /**
     * Elimina el miembro seleccionado del grupo
     */
    private void eliminarMiembro() {
        if (selectedGroup == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un grupo primero");
            return;
        }

        // Verificar si soy admin (usando el nuevo campo isGroupAdmin)
        boolean soyAdmin = false;
        int myId = ClientSocket.getInstance().getMyUserId();
        if (selectedGroup.getMiembros() != null) {
            for (User u : selectedGroup.getMiembros()) {
                if (u.getId() == myId && u.isGroupAdmin()) {
                    soyAdmin = true;
                    break;
                }
            }
        }

        if (!soyAdmin) {
            JOptionPane.showMessageDialog(this,
                "Solo los administradores pueden eliminar miembros",
                "Sin permisos",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        User selectedMember = memberList.getSelectedValue();
        if (selectedMember == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un miembro para eliminar");
            return;
        }

        // No permitir eliminar si es el único admin
        if (selectedMember.isGroupAdmin()) {
            // Contar cuántos admins hay
            int adminCount = 0;
            for (User u : selectedGroup.getMiembros()) {
                if (u.isGroupAdmin()) adminCount++;
            }
            if (adminCount <= 1) {
                JOptionPane.showMessageDialog(this,
                    "No puedes eliminar al único administrador del grupo.\nPromueve a otro admin primero.",
                    "Operación no permitida",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Eliminar a " + selectedMember.getUsername() + " del grupo?",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            Message msg = new Message();
            msg.setType(MessageType.REMOVE_GROUP_MEMBER);
            msg.setReceiverId(selectedGroup.getId());
            msg.setContent(selectedMember.getUsername());
            ClientSocket.getInstance().send(msg);
        }
    }

    /**
     * Abandona el grupo seleccionado
     */
    private void abandonarGrupo() {
        if (selectedGroup == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un grupo primero");
            return;
        }

        // Verificar si soy admin y contar admins
        boolean soyAdmin = false;
        int adminCount = 0;
        int myId = ClientSocket.getInstance().getMyUserId();
        if (selectedGroup.getMiembros() != null) {
            for (User u : selectedGroup.getMiembros()) {
                if (u.isGroupAdmin()) {
                    adminCount++;
                    if (u.getId() == myId) soyAdmin = true;
                }
            }
        }

        String mensaje;
        if (soyAdmin && adminCount <= 1 && selectedGroup.getMiembros().size() > 1) {
            mensaje = "⚠️ Eres el único administrador.\n\n" +
                      "Debes promover a otro admin antes de salir, o el grupo será eliminado.\n\n¿Continuar de todas formas?";
        } else if (soyAdmin && selectedGroup.getMiembros().size() <= 1) {
            mensaje = "Eres el único miembro. Si abandonas, el grupo será eliminado. ¿Continuar?";
        } else {
            mensaje = "¿Estás seguro de que quieres abandonar el grupo '" + selectedGroup.getNombre() + "'?";
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            mensaje,
            "Abandonar Grupo",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            Message msg = new Message();
            msg.setType(MessageType.LEAVE_GROUP);
            msg.setReceiverId(selectedGroup.getId());
            ClientSocket.getInstance().send(msg);
        }
    }

    /**
     * Abre la ventana de chat con el grupo seleccionado
     */
    private void abrirChatGrupo() {
        if (selectedGroup == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un grupo primero");
            return;
        }

        // Crear un User "virtual" para representar al grupo en la lista de chats
        User groupAsUser = new User();
        groupAsUser.setId(selectedGroup.getId());
        groupAsUser.setUsername(selectedGroup.getNombre());
        groupAsUser.setActivityStatus("grupo"); // Marcar como grupo

        // Notificar al ChatFrame para abrir el chat del grupo
        ChatFrame chatFrame = ClientSocket.getInstance().getChatFrame();
        if (chatFrame != null) {
            chatFrame.abrirChatGrupo(selectedGroup);
            this.dispose(); // Cerrar ventana de grupos
        }
    }

    /**
     * Maneja notificaciones del grupo (nuevo miembro, eliminado, etc.)
     */
    public void mostrarNotificacion(String mensaje, Group group) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, mensaje, "Notificación de Grupo", JOptionPane.INFORMATION_MESSAGE);

            // Recargar lista de grupos
            cargarGrupos();

            // Si el grupo afectado es el seleccionado, actualizar detalles
            if (group != null && selectedGroup != null && selectedGroup.getId() == group.getId()) {
                mostrarDetallesGrupo(group);
            }
        });
    }

    /**
     * Renderer para la lista de grupos
     */
    private class GroupListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setOpaque(true);

            if (value instanceof Group) {
                Group g = (Group) value;
                label.setText(g.getNombre());
                label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                label.setBorder(new EmptyBorder(12, 15, 12, 15));

                if (isSelected) {
                    label.setBackground(StyleUtil.BG_SELECTED);
                    label.setForeground(StyleUtil.TEXT_PRIMARY);
                } else {
                    label.setBackground(StyleUtil.BG_MEDIUM);
                    label.setForeground(StyleUtil.TEXT_PRIMARY);
                }
            }
            return label;
        }
    }

    /**
     * Renderer para la lista de miembros
     */
    private class MemberListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setOpaque(true);

            if (value instanceof User) {
                User u = (User) value;
                String prefix = "";

                // Marcar a los admins con corona
                if (u.isGroupAdmin()) {
                    prefix = "* ";
                }

                // Indicador de estado (sin emoji)
                String statusIcon = "activo".equals(u.getActivityStatus()) ? "[ON] " : "";

                label.setText(prefix + statusIcon + u.getUsername() + (u.isGroupAdmin() ? " (Admin)" : ""));
                label.setFont(StyleUtil.FONT_BODY);
                label.setBorder(new EmptyBorder(10, 15, 10, 15));

                if (isSelected) {
                    label.setBackground(StyleUtil.BG_SELECTED);
                    label.setForeground(StyleUtil.TEXT_PRIMARY);
                } else {
                    label.setBackground(StyleUtil.BG_LIGHT);
                    label.setForeground(StyleUtil.TEXT_PRIMARY);
                }
            }
            return label;
        }
    }
}

