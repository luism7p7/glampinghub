package com.example.CampingHub.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Sesión iniciada: el front envía el token en la cabecera Authorization: Bearer <token>. */
@Entity
@Table(name = "sesiones", indexes = @Index(name = "idx_sesion_token", columnList = "token", unique = true))
public class Sesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String token;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime expira;

    public Sesion() { }

    public Sesion(String token, Usuario usuario, LocalDateTime expira) {
        this.token = token;
        this.usuario = usuario;
        this.expira = expira;
    }

    public Long getId() { return id; }
    public String getToken() { return token; }
    public Usuario getUsuario() { return usuario; }
    public LocalDateTime getExpira() { return expira; }
}
