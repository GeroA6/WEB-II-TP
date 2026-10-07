package com.example.demo.dto.lista;

import jakarta.validation.constraints.NotNull;

public record MoverFavoritosRequest(
    @NotNull(message = "destinoId es obligatorio") Long destinoId) {
}
