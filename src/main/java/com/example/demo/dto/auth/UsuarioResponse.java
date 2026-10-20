package com.example.demo.dto.auth;

import com.example.demo.domain.Rol;

public record UsuarioResponse(
    Long id,
    String email,
    Rol rol
) {
}
