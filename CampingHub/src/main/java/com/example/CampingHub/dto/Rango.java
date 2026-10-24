package com.example.CampingHub.dto;

import java.time.LocalDate;

/** Rango ocupado [desde, hasta) de una finca. */
public record Rango(LocalDate desde, LocalDate hasta) { }
