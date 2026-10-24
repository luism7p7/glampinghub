package com.example.CampingHub.services;

import com.example.CampingHub.entities.Usuario;

import java.util.List;

public interface UsuarioService {
    Usuario registrar(String nombre, String correo, String contraseña);
    Usuario obtenerUsuarioPorId(Long id);
    List<Usuario> obtenerTodosLosUsuarios();
    Usuario actualizarUsuario(Usuario usuario);
    void eliminarUsuario(Long id);
    Usuario iniciarSesion(Long id);
    Usuario cerrarSesion(Long id);
}
