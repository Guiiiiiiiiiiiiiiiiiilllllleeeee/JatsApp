package com.jatsapp.common;

import java.io.Serializable;

public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String nombre;
    private int idAdmin; // ID del creador

    public Group() {}

    public Group(int id, String nombre, int idAdmin) {
        this.id = id;
        this.nombre = nombre;
        this.idAdmin = idAdmin;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getIdAdmin() { return idAdmin; }
    public void setIdAdmin(int idAdmin) { this.idAdmin = idAdmin; }

    @Override
    public String toString() {
        return nombre; // Para mostrar en el JList de grupos
    }
}