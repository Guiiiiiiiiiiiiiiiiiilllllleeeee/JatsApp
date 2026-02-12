package com.jatsapp.server.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class FileService {

    // Carpeta donde se guardarán los archivos dentro del proyecto del servidor
    private static final String STORAGE_DIR = "server_files";

    // Límite de tamaño de archivo: 10MB
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024;

    public FileService() {
        createStorageDirectory();
    }

    private void createStorageDirectory() {
        File directory = new File(STORAGE_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
            System.out.println("✓ Carpeta " + STORAGE_DIR + " creada");
        }
    }

    /**
     * Guarda un array de bytes en el disco.
     * @param fileData Los bytes del archivo.
     * @param originalName El nombre original (ej: "foto.jpg").
     * @return La ruta relativa donde se guardó (para guardar en la BD).
     */
    public String saveFile(byte[] fileData, String originalName) throws IOException {
        if (fileData == null || fileData.length == 0) {
            return null;
        }

        // Validar tamaño
        if (fileData.length > MAX_FILE_SIZE) {
            throw new IOException("Archivo demasiado grande. Máximo permitido: " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        // Generamos un nombre único para evitar sobrescribir archivos con el mismo nombre
        String extension = "";
        int i = originalName.lastIndexOf('.');
        if (i > 0) {
            extension = originalName.substring(i);
        }

        String uniqueFileName = UUID.randomUUID().toString() + extension;
        Path path = Paths.get(STORAGE_DIR, uniqueFileName);

        Files.write(path, fileData);

        return uniqueFileName; // Retornamos solo el nombre del archivo guardado
    }

    /**
     * Lee un archivo del disco para enviarlo al cliente.
     * @param fileName El nombre del archivo guardado en la BD.
     * @return Array de bytes del archivo.
     */
    public byte[] loadFile(String fileName) throws IOException {
        Path path = Paths.get(STORAGE_DIR, fileName);
        if (Files.exists(path)) {
            return Files.readAllBytes(path);
        }
        return null;
    }

    public void deleteFile(String fileName) {
        try {
            Path path = Paths.get(STORAGE_DIR, fileName);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}