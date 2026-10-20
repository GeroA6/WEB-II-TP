package com.example.demo.service;

import com.example.demo.dto.auth.RegistoRequest;
import com.example.demo.dto.auth.UsuarioResponse;

public interface UsuarioService {
    UsuarioResponse registrar(RegistoRequest request);
}
