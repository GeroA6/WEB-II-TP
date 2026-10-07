package com.example.demo.dto.lista;

import jakarta.validation.constraints.NotBlank;

public record ListaRequest(
    @NotBlank(message = "nombre no puede ser vacío") String nombre) {

}
