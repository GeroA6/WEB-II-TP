package com.example.demo.dto.favorito;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FavoritoRequest(
        @NotNull(message = "productoId es obligatorio") Long productoId,

        @NotBlank(message = "nota no puede estar vacia") String nota,

        @NotNull(message = "listaId es obligatorio") Long listaId) {
}
