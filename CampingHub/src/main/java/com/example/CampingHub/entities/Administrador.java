package com.example.CampingHub.entities;

import jakarta.persistence.Entity;

@Entity
public class Administrador extends Usuario {

    public Administrador() { }

    public Administrador(String nombre, String correo, String contraseña) {
        setNombre(nombre);
        setCorreo(correo);
        setContraseña(contraseña);
    }

    public void gestionarFinca() {
        System.out.println("El administrador está gestionando una finca");
    }

    public void gestionarReserva() {
        System.out.println("El administrador está gestionando una reserva");
    }
}
