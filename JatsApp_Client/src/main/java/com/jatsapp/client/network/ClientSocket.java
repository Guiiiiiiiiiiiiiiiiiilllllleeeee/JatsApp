package com.jatsapp.client.network;

import com.google.gson.Gson;
import com.jatsapp.client.view.ChatFrame;
import com.jatsapp.client.view.LoginFrame;
import com.jatsapp.common.Message;
import com.jatsapp.common.MessageType;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientSocket {

    private static ClientSocket instance;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private final Gson gson = new Gson();

    // Referencias a las ventanas para poder actualizarlas desde aquí
    private LoginFrame loginFrame;
    private ChatFrame chatFrame;

    private String myUsername;

    // MODO OFFLINE: Para probar la interfaz sin servidor
    private boolean isOfflineMode = false;

    // Singleton: Para tener una única conexión en toda la app
    public static ClientSocket getInstance() {
        if (instance == null) instance = new ClientSocket();
        return instance;
    }

    /**
     * Intenta conectar al servidor. Si falla, activa el modo Offline.
     */
    public void connect(String ip, int port) {
        try {
            this.socket = new Socket(ip, port);
            this.out = new DataOutputStream(socket.getOutputStream());
            this.in = new DataInputStream(socket.getInputStream());

            System.out.println("✅ CONECTADO AL SERVIDOR (" + ip + ":" + port + ")");
            this.isOfflineMode = false;

            // Arrancamos el hilo que escucha mensajes
            new Thread(this::listen).start();

        } catch (IOException e) {
            System.err.println("⚠️ NO SE PUDO CONECTAR AL SERVIDOR.");
            System.err.println("👉 Activando MODO OFFLINE para pruebas de interfaz.");
            this.isOfflineMode = true;
        }
    }

    // Setters para que el Socket sepa qué ventanas están abiertas
    public void setLoginFrame(LoginFrame frame) { this.loginFrame = frame; }
    public void setChatFrame(ChatFrame frame) { this.chatFrame = frame; }

    public void setMyUsername(String user) { this.myUsername = user; }
    public String getMyUsername() { return myUsername; }

    /**
     * Envía un mensaje al servidor (o simula enviarlo si estamos offline)
     */
    public void send(Message msg) {
        // --- LOGICA MODO OFFLINE (SIMULACIÓN) ---
        if (isOfflineMode) {
            System.out.println("[OFFLINE] Simulando envío: " + msg.getType());

            // Si intentas hacer Login, te digo que sí automáticamente
            if (msg.getType() == MessageType.LOGIN || msg.getType() == MessageType.REGISTER) {
                // Simulamos un pequeño retraso de red
                new Thread(() -> {
                    try { Thread.sleep(500); } catch (InterruptedException e) {}
                    if (loginFrame != null) loginFrame.onLoginSuccess();
                }).start();
            }

            // Si envías un mensaje, te lo devuelvo para que veas que funciona el chat
            if (msg.getType() == MessageType.TEXT_MESSAGE) {
                if (chatFrame != null) chatFrame.recibirMensaje(msg);
            }
            return;
        }

        // --- LOGICA REAL (ONLINE) ---
        try {
            String json = gson.toJson(msg);
            out.writeUTF(json);
            System.out.println("📤 Enviado: " + json);
        } catch (IOException e) {
            System.err.println("❌ Error enviando mensaje: " + e.getMessage());
        }
    }

    /**
     * Hilo que escucha lo que manda el servidor
     */
    private void listen() {
        try {
            while (true) {
                // 1. Recibir JSON
                String json = in.readUTF();
                System.out.println("📥 Recibido: " + json);

                // 2. Convertir a Objeto
                Message msg = gson.fromJson(json, Message.class);

                // 3. Procesar
                handleMessage(msg);
            }
        } catch (IOException e) {
            System.err.println("❌ Desconectado del servidor");
            if (!isOfflineMode) {
                JOptionPane.showMessageDialog(null, "Conexión perdida con el servidor.");
                System.exit(0);
            }
        }
    }

    /**
     * Decide qué hacer con el mensaje que acaba de llegar
     */
    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            case LOGIN_OK:
                if (loginFrame != null) loginFrame.onLoginSuccess();
                break;