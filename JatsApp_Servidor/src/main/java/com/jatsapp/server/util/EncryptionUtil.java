package com.jatsapp.server.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Utilidad para encriptar y desencriptar mensajes usando AES
 */
public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    // Clave de 16 bytes (128 bits) - Debe ser la misma que en el cliente
    private static final String SECRET_KEY = "JatsApp2026Key!!"; // 16 caracteres

    private static SecretKey getSecretKey() {
        return new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
    }

    /**
     * Encripta un texto usando AES
     * @param plainText Texto plano a encriptar
     * @return Texto encriptado en Base64
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            System.err.println("Error encriptando mensaje: " + e.getMessage());
            return plainText; // Devolver texto plano en caso de error
        }
    }

    /**
     * Desencripta un texto usando AES
     * @param encryptedText Texto encriptado en Base64
     * @return Texto plano desencriptado
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            System.err.println("Error desencriptando mensaje: " + e.getMessage());
            return encryptedText; // Devolver texto encriptado en caso de error
        }
    }
}

