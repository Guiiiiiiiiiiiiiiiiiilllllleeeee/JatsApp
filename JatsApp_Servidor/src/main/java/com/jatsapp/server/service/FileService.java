package com.jatsapp.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

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
            boolean created = directory.mkdirs();
            if (created) {
                logger.info("✓ Carpeta {} creada", STORAGE_DIR);
            } else {
                logger.error("No se pudo crear carpeta {}", STORAGE_DIR);
            }
        } else {
            logger.debug("Carpeta {} ya existe", STORAGE_DIR);
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
            logger.warn("Intento de guardar archivo vacío");
            return null;
        }

        // Validar tamaño
        if (fileData.length > MAX_FILE_SIZE) {
            logger.warn("Archivo demasiado grande rechazado: {} bytes (máx: {})", fileData.length, MAX_FILE_SIZE);
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

        logger.info("✓ Archivo guardado: {} (original: {}, {} bytes)", uniqueFileName, originalName, fileData.length);

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
            byte[] data = Files.readAllBytes(path);
            logger.debug("Archivo cargado: {} ({} bytes)", fileName, data.length);
            return data;
        }
        logger.warn("Archivo no encontrado: {}", fileName);
        return null;
    }

    public void deleteFile(String fileName) {
        try {
            Path path = Paths.get(STORAGE_DIR, fileName);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                logger.info("Archivo eliminado: {}", fileName);
            } else {
                logger.debug("Archivo no existía para eliminar: {}", fileName);
            }
        } catch (IOException e) {
            logger.error("Error eliminando archivo: {}", fileName, e);
        }
    }
}