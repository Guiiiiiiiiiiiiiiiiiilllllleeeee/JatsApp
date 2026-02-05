package com.jatsapp.common;

import java.io.Serializable;

public class User implements Serializable {
    private int id;
    private String username;
    private String email;
    // La contraseña NO se guarda aquí por seguridad

    public User() {}

    public User(int id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return username; // Para que en las listas de Swing salga solo el nombre
    }
}