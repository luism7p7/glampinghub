package com.example.CampingHub.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

@Entity
public class Usuario {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    private String nombre;

    @Column(unique = true)
    private String correo;

    // Nunca se devuelve en las respuestas JSON; se guarda cifrada con BCrypt
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String contraseña;

    private LocalDateTime creado;

    public Usuario() {
    }

    public Usuario(String correo, String nombre, Long idUsuario, String contraseña) {
        this.correo = correo;
        this.nombre = nombre;
        this.idUsuario = idUsuario;
        this.contraseña = contraseña;
    }

    /** Cifra la contraseña antes de guardar si todavía está en texto plano. */
    @PrePersist
    @PreUpdate
    void prepararGuardado() {
        if (creado == null) creado = LocalDateTime.now();
        if (correo != null) correo = correo.trim().toLowerCase();
        if (contraseña != null && !contraseña.startsWith("$2")) {
            contraseña = ENCODER.encode(contraseña);
        }
    }

    public boolean contraseñaCoincide(String claveEnTextoPlano) {
        return claveEnTextoPlano != null && contraseña != null && ENCODER.matches(claveEnTextoPlano, contraseña);
    }

    /** "ADMIN" o "CLIENTE"; el front lo usa para mostrar el panel de administración. */
    public String getRol() {
        return (this instanceof Administrador) ? "ADMIN" : "CLIENTE";
    }

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
    public String getContraseña() { return contraseña; }
    public void setContraseña(String contraseña) { this.contraseña = contraseña; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public LocalDateTime getCreado() { return creado; }

    public void iniciarSccion() {
        System.out.println(nombre + " ha iniciado sesión");
    }

    public void cerrarSeccion() {
        System.out.println(nombre + " ha cerrado sesión");
    }
}
