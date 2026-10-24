package com.example.CampingHub.services.Impl;

import com.example.CampingHub.entities.Cliente;
import com.example.CampingHub.entities.Usuario;
import com.example.CampingHub.repositories.ReservaRepository;
import com.example.CampingHub.repositories.SesionRepository;
import com.example.CampingHub.repositories.UsuarioRepository;
import com.example.CampingHub.seguridad.ApiException;
import com.example.CampingHub.services.UsuarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private static final Pattern CORREO = Pattern.compile("^[^\\s@]{1,64}@[^\\s@]{1,190}\\.[^\\s@]{2,20}$");

    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final SesionRepository sesionRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, ReservaRepository reservaRepository,
                              SesionRepository sesionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.reservaRepository = reservaRepository;
        this.sesionRepository = sesionRepository;
    }

    @Override
    @Transactional
    public Usuario registrar(String nombre, String correo, String contraseña) {
        nombre = nombre == null ? "" : nombre.trim();
        correo = correo == null ? "" : correo.trim().toLowerCase();
        if (nombre.length() < 2 || nombre.length() > 60) throw ApiException.badRequest("Escribe tu nombre (entre 2 y 60 letras).");
        if (!CORREO.matcher(correo).matches()) throw ApiException.badRequest("Escribe un correo válido.");
        if (contraseña == null || contraseña.length() < 6 || contraseña.length() > 72)
            throw ApiException.badRequest("La contraseña debe tener al menos 6 caracteres.");
        if (usuarioRepository.existsByCorreoIgnoreCase(correo))
            throw ApiException.conflict("Ya existe una cuenta con ese correo. Inicia sesión.");
        return usuarioRepository.save(new Cliente(nombre, correo, contraseña));
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> ApiException.notFound("Usuario no encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> obtenerTodosLosUsuarios() {
        return usuarioRepository.findAll();
    }

    @Override
    @Transactional
    public Usuario actualizarUsuario(Usuario cambios) {
        Usuario u = obtenerUsuarioPorId(cambios.getIdUsuario());
        if (cambios.getNombre() != null && !cambios.getNombre().isBlank()) u.setNombre(cambios.getNombre().trim());
        if (cambios.getCorreo() != null && !cambios.getCorreo().isBlank()) u.setCorreo(cambios.getCorreo());
        if (cambios.getContraseña() != null && !cambios.getContraseña().isBlank()) u.setContraseña(cambios.getContraseña());
        return usuarioRepository.save(u);
    }

    @Override
    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario u = obtenerUsuarioPorId(id);
        sesionRepository.borrarDelUsuario(id);
        reservaRepository.borrarDelUsuario(id);
        usuarioRepository.delete(u);
    }

    @Override
    public Usuario iniciarSesion(Long id) {
        Usuario u = obtenerUsuarioPorId(id);
        u.iniciarSccion();
        return u;
    }

    @Override
    public Usuario cerrarSesion(Long id) {
        Usuario u = obtenerUsuarioPorId(id);
        u.cerrarSeccion();
        return u;
    }
}
