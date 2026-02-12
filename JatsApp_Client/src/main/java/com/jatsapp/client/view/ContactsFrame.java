package com.jatsapp.client.view;

import com.jatsapp.client.network.ClientSocket;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;
import com.jatsapp.common.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ContactsFrame extends JFrame {

    private DefaultListModel<String> contactListModel;

    public ContactsFrame() {
        setTitle("Contactos");
        setSize(400, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Registrarse en ClientSocket
        ClientSocket.getInstance().setContactsFrame(this);

        // Panel principal
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // Lista de contactos
        contactListModel = new DefaultListModel<>();
        JList<String> contactList = new JList<>(contactListModel);
        JScrollPane scrollPane = new JScrollPane(contactList);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Botones para añadir y eliminar contactos
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        JButton addButton = new JButton("Añadir Contacto");
        JButton removeButton = new JButton("Eliminar Contacto");

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newContact = JOptionPane.showInputDialog(ContactsFrame.this, "Introduce el nombre del contacto:", "Añadir Contacto", JOptionPane.PLAIN_MESSAGE);
                if (newContact != null && !newContact.trim().isEmpty()) {
                    Message msg = new Message();
                    msg.setType(MessageType.ADD_CONTACT);
                    msg.setContent(newContact.trim());
                    ClientSocket.getInstance().send(msg);
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedContact = contactList.getSelectedValue();
                if (selectedContact != null) {
                    int confirm = JOptionPane.showConfirmDialog(ContactsFrame.this, "¿Estás seguro de que deseas eliminar a " + selectedContact + "?", "Eliminar Contacto", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        Message msg = new Message();
                        msg.setType(MessageType.REMOVE_CONTACT);
                        msg.setContent(selectedContact);
                        ClientSocket.getInstance().send(msg);
                    }
                } else {
                    JOptionPane.showMessageDialog(ContactsFrame.this, "Selecciona un contacto para eliminar.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        add(panel);

        // Agregar listener para desregistrarse al cerrar
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                // Desregistrarse al cerrar
                ClientSocket.getInstance().setContactsFrame(null);
            }
        });

        // Cargar contactos al abrir la ventana
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
            for (User user : contactos) {
                contactListModel.addElement(user.getUsername());
            }
        });
    }
}
