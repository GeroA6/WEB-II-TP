package com.example.demo.domain;

public record Lista(
        Long id,
        String nombre,
        Long usuarioId) {

        // Constructor de conveniencia por si se instancia sin usuarioId
        public Lista(Long id, String nombre) {
            this(id, nombre, null);
        }
}
