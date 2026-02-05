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
        // Configuración SMTP (Ejemplo para Gmail)
        // Estos valores deberían estar en tu config.properties preferiblemente
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", configProps.getProperty("mail.host", "smtp.gmail.com"));
        props.put("mail.smtp.port", configProps.getProperty("mail.port", "587"));

        final String username = configProps.getProperty("mail.user"); // Tu email
        final String password = configProps.getProperty("mail.password"); // Tu contraseña de aplicación

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