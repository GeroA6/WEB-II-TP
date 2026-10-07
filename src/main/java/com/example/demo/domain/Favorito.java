package com.example.demo.domain;

import java.time.LocalDateTime;

public record Favorito(
        Long id,
        Long productoId,
        String nota,
        LocalDateTime fechaAgregado,
        Long listaId) {
}
