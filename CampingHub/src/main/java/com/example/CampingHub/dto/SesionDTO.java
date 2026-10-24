package com.example.CampingHub.dto;

import com.example.CampingHub.entities.Usuario;

public record SesionDTO(String token, Long idUsuario, String nombre, String correo, String rol) {
    public static SesionDTO de(String token, Usuario u) {
        return new SesionDTO(token, u.getIdUsuario(), u.getNombre(), u.getCorreo(), u.getRol());
    }
}
