package com.example.demo.domain;

public record Usuario(
        Long id,
        String email,
        String passwordHash,
        Rol rol) {
}
