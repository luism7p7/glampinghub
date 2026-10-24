package com.example.CampingHub.seguridad;

import com.example.CampingHub.entities.Sesion;
import com.example.CampingHub.entities.Usuario;
import com.example.CampingHub.repositories.SesionRepository;
import com.example.CampingHub.repositories.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DIAS_SESION = 7;

    private final UsuarioRepository usuarioRepository;
    private final SesionRepository sesionRepository;

    public AuthService(UsuarioRepository usuarioRepository, SesionRepository sesionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.sesionRepository = sesionRepository;
    }

    @Transactional
    public Sesion iniciarSesion(String correo, String contraseña) {
        Usuario u = usuarioRepository.findByCorreoIgnoreCase(correo == null ? "" : correo.trim())
                .filter(x -> x.contraseñaCoincide(contraseña))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos."));
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        return sesionRepository.save(new Sesion(token, u, LocalDateTime.now().plusDays(DIAS_SESION)));
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> usuarioDelToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return sesionRepository.findByToken(token)
                .filter(s -> s.getExpira().isAfter(LocalDateTime.now()))
                .map(Sesion::getUsuario);
    }

    @Transactional
    public void cerrarSesion(String token) {
        sesionRepository.findByToken(token).ifPresent(sesionRepository::delete);
    }

    @Transactional
    public int limpiarVencidas() {
        return sesionRepository.borrarVencidas(LocalDateTime.now());
    }
}
