package com.example.demo.service;

import com.example.demo.dto.auth.LoginResponse;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.LoginResponse;
import com.example.demo.dto.auth.RegistoRequest;
import com.example.demo.dto.auth.UsuarioResponse;

public interface UsuarioService {
    UsuarioResponse registrar(RegistoRequest request);
    LoginResponse login(LoginRequest request);
}
