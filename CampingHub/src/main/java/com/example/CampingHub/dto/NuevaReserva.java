package com.example.CampingHub.dto;

import java.time.LocalDate;

public record NuevaReserva(Long idFinca, LocalDate fechaInicio, LocalDate fechaFin, Integer huespedes) { }
