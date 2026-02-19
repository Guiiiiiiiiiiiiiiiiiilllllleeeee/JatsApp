package com.jatsapp.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final Properties configProps;

    public EmailService() {
        this.configProps = new Properties();
        loadConfig();
    }

    private void loadConfig() {
        // 1. Intentar cargar desde archivo externo (junto al JAR)
        java.io.File externalConfig = new java.io.File("config.properties");
        if (externalConfig.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(externalConfig)) {
                configProps.load(fis);
                logger.debug("Configuración de email cargada desde archivo externo");
                return;
            } catch (Exception e) {
                logger.warn("Error cargando config externo para email", e);
            }
        }

        // 2. Si no existe externo, cargar desde recursos internos
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                configProps.load(input);
                logger.debug("Configuración de email cargada desde recursos internos");
            } else {
                logger.warn("No se encontró config.properties para EmailService");
            }
        } catch (IOException ex) {
            logger.error("Error cargando configuración de email", ex);
        }
    }

    public void sendEmail(String toEmail, String subject, String body) {
        final String username = configProps.getProperty("mail.user");
        final String password = configProps.getProperty("mail.password");

        // Validar que las credenciales estén configuradas
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            logger.warn("⚠️ Credenciales de email no configuradas. Email no enviado a: {}", toEmail);
            logger.info("📧 Para desarrollo: Verifica el código 2FA en la base de datos");
            return;
        }

        logger.debug("Enviando email a: {} con asunto: {}", toEmail, subject);

        // Configuración SMTP (Ejemplo para Gmail)
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
            logger.info("✉️ Correo enviado exitosamente a: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Error enviando correo a {}: {}", toEmail, e.getMessage());
        }
    }
}