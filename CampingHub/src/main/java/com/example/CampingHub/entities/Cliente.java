package com.example.CampingHub.entities;

import jakarta.persistence.Entity;

@Entity
public class Cliente extends Usuario {

    public Cliente() { }

    public Cliente(String nombre, String correo, String contraseña) {
        setNombre(nombre);
        setCorreo(correo);
        setContraseña(contraseña);
    }

    public void realizarReserva() {
        System.out.println("El cliente " + getNombre() + " ha realizado una reserva");
    }
}
