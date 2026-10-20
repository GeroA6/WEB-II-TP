package com.example.demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.domain.Rol;
import com.example.demo.domain.Usuario;
import com.example.demo.dto.auth.RegistoRequest;
import com.example.demo.dto.auth.UsuarioResponse;
import com.example.demo.exception.EmailDuplicadoException;
import com.example.demo.repository.UsuarioRepository;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UsuarioResponse registrar(RegistoRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new EmailDuplicadoException("Ya existe una cuenta con el email" + request.email());
        }

        Usuario nuevo = new Usuario(
                null,
                request.email(),
                passwordEncoder.encode(request.password()), // hash seguro, nunca texto plano
                Rol.USER // Rol por defecto
        );

        Usuario guardado = usuarioRepository.save(nuevo);

        return aResponse(guardado);
    }

    private UsuarioResponse aResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.id(),
                usuario.email(),
                usuario.rol());
    }

}
