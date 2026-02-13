package com.jatsapp.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int MAX_MEMBERS = 10; // Límite máximo de participantes

    private int id;
    private String nombre;
    private int idAdmin; // ID del creador/administrador
    private List<User> miembros; // Lista de miembros del grupo
    private java.util.Date fechaCreacion;

    public Group() {
        this.miembros = new ArrayList<>();
    }

    public Group(int id, String nombre, int idAdmin) {
        this.id = id;
        this.nombre = nombre;
        this.idAdmin = idAdmin;
        this.miembros = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getIdAdmin() { return idAdmin; }
    public void setIdAdmin(int idAdmin) { this.idAdmin = idAdmin; }

    public List<User> getMiembros() { return miembros; }
    public void setMiembros(List<User> miembros) { this.miembros = miembros; }

    public java.util.Date getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(java.util.Date fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    /**
     * Añade un miembro al grupo si no se ha alcanzado el límite
     * @return true si se añadió correctamente, false si está lleno o ya existe
     */
    public boolean addMiembro(User user) {
        if (miembros.size() >= MAX_MEMBERS) {
            return false;
        }
        // Verificar que no esté ya en el grupo
        for (User m : miembros) {
            if (m.getId() == user.getId()) {
                return false;
            }
        }
        miembros.add(user);
        return true;
    }

    /**
     * Elimina un miembro del grupo
     * @return true si se eliminó correctamente
     */
    public boolean removeMiembro(int userId) {
        return miembros.removeIf(u -> u.getId() == userId);
    }

    /**
     * Verifica si un usuario es el administrador
     */
    public boolean isAdmin(int userId) {
        return this.idAdmin == userId;
    }

    /**
     * Obtiene el número actual de miembros
     */
    public int getMemberCount() {
        return miembros.size();
    }

    /**
     * Verifica si el grupo está lleno
     */
    public boolean isFull() {
        return miembros.size() >= MAX_MEMBERS;
    }

    @Override
    public String toString() {
        return nombre; // Para mostrar en el JList de grupos
    }
}