package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.domain.Rol;
import com.example.demo.domain.Usuario;
import com.example.demo.dto.auth.RegistoRequest;
import com.example.demo.dto.auth.UsuarioResponse;
import com.example.demo.exception.EmailDuplicadoException;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.security.JwtService;

@ExtendWith(MockitoExtension.class)
public class UsuarioServiceImplTest {
    
    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @Captor
    private ArgumentCaptor<Usuario> usuarioCaptor;

    @Test
    @DisplayName("Al registar un usuario, nunca debe guardarse la contraseña en texto plano en la base de datos")
    void registar_deberiaGuardarPasswordHasheadaYNuncaEnTextoPlano() {
        // Arrange (Preparación)
        String email = "juan@example.com";
        String plainPassword = "PasswordPlano123";
        String hashedPassword = "$2a$10$hashSeguroGeneradoPorBCrypt";
        RegistoRequest request = new RegistoRequest(email, plainPassword);

        when(usuarioRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(plainPassword)).thenReturn(hashedPassword);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return new Usuario(1L, u.email(), u.passwordHash(), u.rol());
        });

        // Act (Ejecución)
        UsuarioResponse response = usuarioService.registrar(request);
        
        // Assert (Verificación)
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario usuarioGuardado = usuarioCaptor.getValue();

        // Verificación crucial de no guardar texto plano
        assertThat(usuarioGuardado.passwordHash())
                .as("La contraseña nunca debe guardarse en texto plano")
                .isNotEqualTo(plainPassword);

        // Verificación de que se almeceno el hash generado por PasswordEncoder:
        assertThat(usuarioGuardado.passwordHash())
                .as("Debe guardarse el hash devuelto por el PasswordEncoder")
                .isEqualTo(hashedPassword);

        // Verificación de rol por defecto
        assertThat(usuarioGuardado.rol())
                .as("El usuario nuevo debe crearse con el rol USER")
                .isEqualTo(Rol.USER);

        // Verificación del email
        assertThat(usuarioGuardado.email())
                .as("El email debe ser el proporcionado en el request")
                .isEqualTo(email);

        // Verificación de que se llamó al repositorio
        verify(usuarioRepository).save(usuarioGuardado);

        // Verificación del response
        assertThat(response)
                .as("El response debe contener el ID, email y rol")
                .isNotNull();
        assertThat(response.id())
                .as("El response debe contener el ID del usuario guardado")
                .isEqualTo(1L);
        assertThat(response.email())
                .as("El response debe contener el email del usuario guardado")
                .isEqualTo(email);
        assertThat(response.rol())
                .as("El response debe contener el rol del usuario guardado")
                .isEqualTo(Rol.USER);
    }

    @Test
    @DisplayName("Si el email ya existe, debe lanzar EmailDuplicadoException y no persistir nada")
    void registrar_debeLanzarEmailDuplicadoExceptionSiElEmailYaExiste() {
        // Arrange (Preparación)
        String email = "existente@example.com";
        RegistoRequest request = new RegistoRequest(email, "password123");
        when(usuarioRepository.existsByEmail(email)).thenReturn(true);

        // Act (Ejecución) y Assert (Verificación)
        assertThrows(EmailDuplicadoException.class, () -> usuarioService.registrar(request));
        verify(usuarioRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
        
    }
}
