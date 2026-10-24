package com.example.CampingHub.dto;

import com.example.CampingHub.entities.Finca;
import com.example.CampingHub.entities.Reserva;
import com.example.CampingHub.entities.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Lo que el front ve de una reserva (sin datos sensibles del cliente). */
public record ReservaDTO(Long idReserva, Long idFinca, String finca, String ubicacion, String imagen,
                         LocalDate fechaInicio, LocalDate fechaFin, long noches, int huespedes,
                         double total, String estado, LocalDateTime creada,
                         String cliente, String correoCliente) {

    public static ReservaDTO de(Reserva r, boolean incluirCliente) {
        Finca f = r.getFinca();
        Usuario u = r.getUsuario();
        String imagen = (f.getImagenes() == null || f.getImagenes().isEmpty()) ? null : f.getImagenes().get(0);
        return new ReservaDTO(r.getIdReserva(), f.getIdFinca(), f.getNombre(), f.getUbicacion(), imagen,
                r.getFechaInicio(), r.getFechaFin(), r.getNoches(), r.getHuespedes(), r.getTotal(),
                r.getEstado(), r.getCreada(),
                incluirCliente ? u.getNombre() : null,
                incluirCliente ? ocultarCorreo(u.getCorreo()) : null);
    }

    /** luis@gmail.com -> lu***@gmail.com */
    static String ocultarCorreo(String c) {
        if (c == null || !c.contains("@")) return c;
        int at = c.indexOf('@');
        return c.substring(0, Math.min(2, at)) + "***" + c.substring(at);
    }
}
