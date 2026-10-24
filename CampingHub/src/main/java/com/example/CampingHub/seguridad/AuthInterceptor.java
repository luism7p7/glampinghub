package com.example.CampingHub.seguridad;

import com.example.CampingHub.entities.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Control de acceso de la API.
 *  - PÚBLICO: ver lugares, registrarse e iniciar sesión.
 *  - USUARIO: reservar, ver y cancelar sus reservas.
 *  - ADMIN:   gestionar lugares y reservas.
 *  - CLAVE:   el resto del CRUD heredado (usuarios, clientes, administradores, notificaciones)
 *             solo con la cabecera X-Admin-Key = variable de entorno ADMIN_KEY.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_USUARIO = "usuarioActual";

    private enum Nivel { PUBLICO, USUARIO, ADMIN, CLAVE }

    private record Regla(String metodo, String patron, Nivel nivel) { }

    private static final List<Regla> REGLAS = List.of(
            new Regla("GET", "/salud", Nivel.PUBLICO),
            new Regla("GET", "/fincas", Nivel.PUBLICO),
            new Regla("GET", "/fincas/{id}", Nivel.PUBLICO),
            new Regla("GET", "/fincas/{id}/ocupadas", Nivel.PUBLICO),
            new Regla("GET", "/fincas/disponibilidad/{id}", Nivel.PUBLICO),
            new Regla("POST", "/usuarios", Nivel.PUBLICO),
            new Regla("POST", "/usuarios/login", Nivel.PUBLICO),

            new Regla("GET", "/usuarios/yo", Nivel.USUARIO),
            new Regla("POST", "/usuarios/logout", Nivel.USUARIO),
            new Regla("POST", "/reservas/crear", Nivel.USUARIO),
            new Regla("GET", "/reservas/mias", Nivel.USUARIO),
            new Regla("PUT", "/reservas/cancelar/{id}", Nivel.USUARIO),

            new Regla("POST", "/fincas", Nivel.ADMIN),
            new Regla("PUT", "/fincas/{id}", Nivel.ADMIN),
            new Regla("DELETE", "/fincas/{id}", Nivel.ADMIN),
            new Regla("GET", "/reservas", Nivel.ADMIN),
            new Regla("GET", "/reservas/{id}", Nivel.ADMIN),
            new Regla("PUT", "/reservas/confirmar/{id}", Nivel.ADMIN),
            new Regla("GET", "/notificaciones", Nivel.ADMIN)
    );

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final AuthService authService;
    private final String adminKey;

    public AuthInterceptor(AuthService authService, @Value("${app.admin-key:}") String adminKey) {
        this.authService = authService;
        this.adminKey = adminKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String metodo = req.getMethod();
        if ("OPTIONS".equals(metodo)) return true;
        String ruta = req.getRequestURI().substring(req.getContextPath().length());
        if (ruta.length() > 1 && ruta.endsWith("/")) ruta = ruta.substring(0, ruta.length() - 1);

        Nivel nivel = Nivel.CLAVE;
        for (Regla r : REGLAS) {
            if (r.metodo().equals(metodo) && matcher.match(r.patron(), ruta)) { nivel = r.nivel(); break; }
        }

        String auth = req.getHeader("Authorization");
        String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7).trim() : null;
        Optional<Usuario> usuario = authService.usuarioDelToken(token);
        usuario.ifPresent(u -> req.setAttribute(ATRIBUTO_USUARIO, u));

        switch (nivel) {
            case PUBLICO:
                return true;
            case USUARIO:
                if (usuario.isPresent()) return true;
                return negar(res, 401, "Inicia sesión para continuar.");
            case ADMIN:
                if (usuario.isEmpty()) return negar(res, 401, "Inicia sesión para continuar.");
                if ("ADMIN".equals(usuario.get().getRol())) return true;
                return negar(res, 403, "Solo un administrador puede hacer esto.");
            default:
                if (!adminKey.isBlank() && adminKey.equals(req.getHeader("X-Admin-Key"))) return true;
                return negar(res, 403, "Esta operación no está disponible en la versión pública.");
        }
    }

    private boolean negar(HttpServletResponse res, int status, String mensaje) throws Exception {
        res.setStatus(status);
        res.setContentType("text/plain;charset=UTF-8");
        res.getOutputStream().write(mensaje.getBytes(StandardCharsets.UTF_8));
        return false;
    }
}
