package com.example.CampingHub.controllers;

import com.example.CampingHub.dto.SesionDTO;
import com.example.CampingHub.entities.Sesion;
import com.example.CampingHub.entities.Usuario;
import com.example.CampingHub.seguridad.AuthInterceptor;
import com.example.CampingHub.seguridad.AuthService;
import com.example.CampingHub.services.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuthService authService;

    public UsuarioController(UsuarioService usuarioService, AuthService authService) {
        this.usuarioService = usuarioService;
        this.authService = authService;
    }

    // Registro público: crea un Cliente con la contraseña cifrada
    @PostMapping
    public ResponseEntity<Usuario> registrar(@RequestBody Map<String, String> datos) {
        Usuario nuevo = usuarioService.registrar(datos.get("nombre"), datos.get("correo"), datos.get("contraseña"));
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    // Inicio de sesión: devuelve el token que el front envía en Authorization: Bearer
    @PostMapping("/login")
    public SesionDTO login(@RequestBody Map<String, String> datos) {
        Sesion s = authService.iniciarSesion(datos.get("correo"), datos.get("contraseña"));
        return SesionDTO.de(s.getToken(), s.getUsuario());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String auth) {
        authService.cerrarSesion(auth.substring(7).trim());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/yo")
    public Usuario yo(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
        return usuario;
    }

    // ---- CRUD de administración interna (requiere X-Admin-Key) ----

    @GetMapping
    public List<Usuario> obtenerTodos() {
        return usuarioService.obtenerTodosLosUsuarios();
    }

    @GetMapping("/{id}")
    public Usuario obtenerPorId(@PathVariable Long id) {
        return usuarioService.obtenerUsuarioPorId(id);
    }

    @PutMapping("/{id}")
    public Usuario actualizar(@RequestBody Usuario usuario, @PathVariable Long id) {
        usuario.setIdUsuario(id);
        return usuarioService.actualizarUsuario(usuario);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/iniciar-sesion/{id}")
    public Usuario iniciarSesion(@PathVariable Long id) {
        return usuarioService.iniciarSesion(id);
    }

    @PutMapping("/cerrar-sesion/{id}")
    public Usuario cerrarSesion(@PathVariable Long id) {
        return usuarioService.cerrarSesion(id);
    }
}
