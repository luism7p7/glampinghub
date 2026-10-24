package com.example.CampingHub.controllers;

import com.example.CampingHub.dto.NuevaReserva;
import com.example.CampingHub.dto.ReservaDTO;
import com.example.CampingHub.entities.Usuario;
import com.example.CampingHub.seguridad.AuthInterceptor;
import com.example.CampingHub.services.ReservaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    // POST /reservas/crear  {idFinca, fechaInicio, fechaFin, huespedes}
    @PostMapping("/crear")
    public ResponseEntity<ReservaDTO> crear(@RequestBody NuevaReserva datos,
                                            @RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
        var r = reservaService.crearReserva(datos, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaDTO.de(r, false));
    }

    // GET /reservas/mias
    @GetMapping("/mias")
    public List<ReservaDTO> mias(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
        return reservaService.obtenerReservasDe(usuario).stream().map(r -> ReservaDTO.de(r, false)).toList();
    }

    // PUT /reservas/cancelar/{id}  (dueño de la reserva o administrador)
    @PutMapping("/cancelar/{id}")
    public ReservaDTO cancelar(@PathVariable Long id,
                               @RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario usuario) {
        return ReservaDTO.de(reservaService.cancelarReserva(id, usuario), "ADMIN".equals(usuario.getRol()));
    }

    // PUT /reservas/confirmar/{id}  (administrador)
    @PutMapping("/confirmar/{id}")
    public ReservaDTO confirmar(@PathVariable Long id) {
        return ReservaDTO.de(reservaService.confirmarReserva(id), true);
    }

    // GET /reservas  (administrador)
    @GetMapping
    public List<ReservaDTO> todas() {
        return reservaService.obtenerTodasLasReservas().stream().map(r -> ReservaDTO.de(r, true)).toList();
    }

    // GET /reservas/{id}  (administrador)
    @GetMapping("/{id}")
    public ReservaDTO porId(@PathVariable Long id) {
        return ReservaDTO.de(reservaService.obtenerReservaPorId(id), true);
    }
}
