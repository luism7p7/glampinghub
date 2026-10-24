package com.example.CampingHub.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    @JsonIgnore
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_finca", nullable = false)
    @JsonIgnore
    private Finca finca;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    private int huespedes = 1;

    private double total;

    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    private LocalDateTime creada;

    public Reserva() { }

    public Reserva(Usuario usuario, Finca finca, LocalDate fechaInicio, LocalDate fechaFin) {
        this.usuario = usuario;
        this.finca = finca;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    @PrePersist
    void alCrear() {
        if (creada == null) creada = LocalDateTime.now();
    }

    public long getNoches() {
        return (fechaInicio == null || fechaFin == null) ? 0 : ChronoUnit.DAYS.between(fechaInicio, fechaFin);
    }

    public Long getIdReserva() { return idReserva; }
    public void setIdReserva(Long idReserva) { this.idReserva = idReserva; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Finca getFinca() { return finca; }
    public void setFinca(Finca finca) { this.finca = finca; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public int getHuespedes() { return huespedes; }
    public void setHuespedes(int huespedes) { this.huespedes = huespedes; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getCreada() { return creada; }

    @Override
    public String toString() {
        return "Reserva{idReserva=" + idReserva + ", estado='" + estado + "'}";
    }
}
