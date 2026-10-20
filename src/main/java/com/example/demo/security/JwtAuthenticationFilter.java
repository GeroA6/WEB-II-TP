package com.example.demo.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Quita "Bearer "
            try {
                Claims claims = jwtService.validarYObtenerClaims(token);
                Long usuarioId = Long.valueOf(claims.getSubject());
                String rol = claims.get("rol", String.class);

                // Asigna la autoridad con el prefijo estandar "ROLE_"
                var authority = new SimpleGrantedAuthority("ROLE_" + rol);
                var authentication = new UsernamePasswordAuthenticationToken(
                        usuarioId,
                        null,
                        List.of(authority));

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtException | IllegalArgumentException e) {
                // Token invalido, vencido o manipulado: no autentica
                SecurityContextHolder.clearContext();
            }
        }

        // Continua la cadena de filtros de Spring Security
        filterChain.doFilter(request, response);
    }
}
