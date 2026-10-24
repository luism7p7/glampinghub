package com.example.CampingHub.services;

import com.example.CampingHub.dto.NuevaReserva;
import com.example.CampingHub.dto.Rango;
import com.example.CampingHub.entities.Reserva;
import com.example.CampingHub.entities.Usuario;

import java.util.List;

public interface ReservaService {
    Reserva crearReserva(NuevaReserva datos, Usuario cliente);
    Reserva obtenerReservaPorId(Long id);
    List<Reserva> obtenerTodasLasReservas();
    List<Reserva> obtenerReservasDe(Usuario usuario);
    List<Rango> fechasOcupadas(Long idFinca);
    Reserva confirmarReserva(Long id);
    Reserva cancelarReserva(Long id, Usuario quien);
}
