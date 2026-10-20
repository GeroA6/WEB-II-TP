package com.example.demo.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El formato del email es inválido")
    String email,

    @NotBlank(message = "La contraseña no puede estar vacía")
    String password
) {
}
