package com.example.CampingHub.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Finca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFinca;

    private String nombre;
    private String ubicacion;
    private int capacidad;

    @Column(nullable = false)
    private double precioNoche = 0.0;

    /** Glamping, Cabaña, Finca, Domo, Refugio, Camping... */
    private String tipo;

    @Column(length = 1000)
    private String descripcion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "finca_imagenes", joinColumns = @JoinColumn(name = "id_finca"))
    @OrderColumn(name = "orden")
    @Column(name = "url", length = 500)
    private List<String> imagenes = new ArrayList<>();

    /** Lugares de ejemplo de la demostración: no se pueden editar ni eliminar. */
    @Column(nullable = false)
    private boolean base = false;

    @OneToMany(mappedBy = "finca", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Reserva> reservas;

    public Finca() { }

    public Finca(Long idFinca, int capacidad, String ubicacion, String nombre, double precioNoche) {
        this.idFinca = idFinca;
        this.capacidad = capacidad;
        this.ubicacion = ubicacion;
        this.nombre = nombre;
        this.precioNoche = precioNoche;
    }

    public Long getIdFinca() { return idFinca; }
    public void setIdFinca(Long idFinca) { this.idFinca = idFinca; }
    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public double getPrecioNoche() { return precioNoche; }
    public void setPrecioNoche(double precioNoche) { this.precioNoche = precioNoche; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public List<String> getImagenes() { return imagenes; }
    public void setImagenes(List<String> imagenes) { this.imagenes = imagenes; }
    public boolean isBase() { return base; }
    public void setBase(boolean base) { this.base = base; }
    public List<Reserva> getReservas() { return reservas; }
    public void setReservas(List<Reserva> reservas) { this.reservas = reservas; }

    public void mostrarDisponibilidad() {
        System.out.println("La finca " + nombre + " tiene capacidad de " + capacidad + " personas");
    }
}
