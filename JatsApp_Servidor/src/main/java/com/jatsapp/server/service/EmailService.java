package com.jatsapp.server.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailService {

    private final Properties configProps;

    public EmailService() {
        this.configProps = new Properties();
        loadConfig();
    }

    private void loadConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                configProps.load(input);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void sendEmail(String toEmail, String subject, String body) {
        final String username = configProps.getProperty("mail.user");
        final String password = configProps.getProperty("mail.password");

        // Validar que las credenciales estén configuradas
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            System.err.println("ERROR: Credenciales de email no configuradas en config.properties. No se enviará correo.");
            System.err.println("Código 2FA (solo para desarrollo): Ver en la base de datos");
            return;
        }

        // Configuración SMTP (Ejemplo para Gmail)
        // Estos valores deberían estar en tu config.properties preferiblemente
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", configProps.getProperty("mail.host", "smtp.gmail.com"));
        props.put("mail.smtp.port", configProps.getProperty("mail.port", "587"));


        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println("Correo enviado exitosamente a: " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Error enviando correo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}