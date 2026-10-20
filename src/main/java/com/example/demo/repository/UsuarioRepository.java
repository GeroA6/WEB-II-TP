package com.example.demo.repository;

import java.util.Optional;

import com.example.demo.domain.Usuario;

public interface UsuarioRepository {
    Usuario save(Usuario usuario);

    Optional<Usuario> findById(Long id);

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
