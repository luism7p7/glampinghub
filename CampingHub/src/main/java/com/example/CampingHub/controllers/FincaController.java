package com.example.CampingHub.controllers;

import com.example.CampingHub.dto.Rango;
import com.example.CampingHub.entities.Finca;
import com.example.CampingHub.services.FincaService;
import com.example.CampingHub.services.ReservaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fincas")
public class FincaController {

    private final FincaService fincaService;
    private final ReservaService reservaService;

    public FincaController(FincaService fincaService, ReservaService reservaService) {
        this.fincaService = fincaService;
        this.reservaService = reservaService;
    }

    @GetMapping
    public List<Finca> obtenerTodas() {
        return fincaService.obtenerTodasLasFincas();
    }

    @GetMapping("/{id}")
    public Finca obtenerPorId(@PathVariable Long id) {
        return fincaService.obtenerFincaPorId(id);
    }

    // Rangos de fechas ya reservados, para bloquearlos en el calendario
    @GetMapping("/{id}/ocupadas")
    public List<Rango> ocupadas(@PathVariable Long id) {
        fincaService.obtenerFincaPorId(id);
        return reservaService.fechasOcupadas(id);
    }

    @GetMapping("/disponibilidad/{id}")
    public Finca mostrarDisponibilidad(@PathVariable Long id) {
        return fincaService.mostrarDisponibilidad(id);
    }

    @PostMapping
    public ResponseEntity<Finca> crear(@RequestBody Finca finca) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fincaService.crearFinca(finca));
    }

    @PutMapping("/{id}")
    public Finca actualizar(@RequestBody Finca finca, @PathVariable Long id) {
        finca.setIdFinca(id);
        return fincaService.actualizarFinca(finca);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        fincaService.eliminarFinca(id);
        return ResponseEntity.noContent().build();
    }
}
