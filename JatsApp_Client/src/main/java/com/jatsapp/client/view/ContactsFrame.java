package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.client.util.StyleUtil;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;
import com.jatsapp.common.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ContactsFrame extends JFrame {

    private DefaultListModel<String> contactListModel;
    private JList<String> contactList;

    public ContactsFrame() {
        setTitle("Contactos - JatsApp");
        setSize(420, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(350, 450));

        // Registrarse en ClientSocket
        ClientSocket.getInstance().setContactsFrame(this);

        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(StyleUtil.BG_DARK);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(StyleUtil.BG_DARK);
        headerPanel.setBorder(new EmptyBorder(20, 25, 15, 25));

        JLabel lblTitle = new JLabel("📋 Mis Contactos");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(StyleUtil.TEXT_PRIMARY);
        headerPanel.add(lblTitle, BorderLayout.WEST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Lista de contactos
        contactListModel = new DefaultListModel<>();
        contactList = new JList<>(contactListModel);
        contactList.setBackground(StyleUtil.BG_MEDIUM);
        contactList.setForeground(StyleUtil.TEXT_PRIMARY);
        contactList.setFont(StyleUtil.FONT_BODY);
        contactList.setSelectionBackground(StyleUtil.BG_SELECTED);
        contactList.setSelectionForeground(StyleUtil.TEXT_PRIMARY);
        contactList.setFixedCellHeight(50);
        contactList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(new EmptyBorder(12, 20, 12, 20));
                label.setFont(StyleUtil.FONT_BODY);
                label.setText("👤 " + value.toString());

                if (isSelected) {
                    label.setBackground(StyleUtil.BG_SELECTED);
                } else {
                    label.setBackground(StyleUtil.BG_MEDIUM);
                }
                label.setForeground(StyleUtil.TEXT_PRIMARY);
                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(contactList);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, StyleUtil.BORDER_DARK));
        StyleUtil.styleScrollPane(scrollPane);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(StyleUtil.BG_DARK);
        buttonPanel.setBorder(new EmptyBorder(10, 20, 15, 20));

        JButton addButton = StyleUtil.createPrimaryButton("+ Añadir");
        JButton removeButton = StyleUtil.createDangerButton("Eliminar");

        addButton.addActionListener(e -> {
            String newContact = JOptionPane.showInputDialog(ContactsFrame.this,
                "Introduce el nombre del contacto:",
                "Añadir Contacto",
                JOptionPane.PLAIN_MESSAGE);
            if (newContact != null && !newContact.trim().isEmpty()) {
                Message msg = new Message();
                msg.setType(MessageType.ADD_CONTACT);
                msg.setContent(newContact.trim());
                ClientSocket.getInstance().send(msg);
            }
        });

        removeButton.addActionListener(e -> {
            String selectedContact = contactList.getSelectedValue();
            if (selectedContact != null) {
                int confirm = JOptionPane.showConfirmDialog(ContactsFrame.this,
                    "¿Eliminar a " + selectedContact + " de tus contactos?",
                    "Confirmar",
                    JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    Message msg = new Message();
                    msg.setType(MessageType.REMOVE_CONTACT);
                    msg.setContent(selectedContact);
                    ClientSocket.getInstance().send(msg);
                }
            } else {
                JOptionPane.showMessageDialog(ContactsFrame.this,
                    "Selecciona un contacto primero.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            }
        });

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Desregistrarse al cerrar
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                ClientSocket.getInstance().setContactsFrame(null);
            }
        });

        // Cargar contactos
        cargarContactos();
    }

    private void cargarContactos() {
        Message msg = new Message();
        msg.setType(MessageType.GET_CONTACTS);
        ClientSocket.getInstance().send(msg);
    }

    public void actualizarContactos(List<User> contactos) {
        SwingUtilities.invokeLater(() -> {
            contactListModel.clear();
            for (User u : contactos) {
                contactListModel.addElement(u.getUsername());
            }
        });
    }
}
