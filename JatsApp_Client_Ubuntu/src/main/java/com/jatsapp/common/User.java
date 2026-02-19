package com.jatsapp.common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String email;
    private String password; // Necesario para transportar la pass en el Registro/Login
    private String activityStatus; // 'activo' o 'desconectado'
    private LocalDateTime lastAccess;
    private boolean groupAdmin; // Indica si es admin del grupo (usado en contexto de grupos)

    public User() {}

    // Constructor para Registro
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // Constructor para mostrar en listas (sin password)
    public User(int id, String username, String activityStatus) {
        this.id = id;
        this.username = username;
        this.activityStatus = activityStatus;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getActivityStatus() { return activityStatus; }
    public void setActivityStatus(String activityStatus) { this.activityStatus = activityStatus; }

    public LocalDateTime getLastAccess() { return lastAccess; }
    public void setLastAccess(LocalDateTime lastAccess) { this.lastAccess = lastAccess; }

    public boolean isGroupAdmin() { return groupAdmin; }
    public void setGroupAdmin(boolean groupAdmin) { this.groupAdmin = groupAdmin; }

    @Override
    public String toString() {
        return username; // Importante para que el JList muestre el nombre
    }
}