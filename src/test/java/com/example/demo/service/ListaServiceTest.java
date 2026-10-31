package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.domain.Lista;
import com.example.demo.dto.lista.ListaRequest;
import com.example.demo.dto.lista.ListaResponse;
import com.example.demo.exception.AccesoDenegadoException;
import com.example.demo.repository.FavoritoJpaRepository;
import com.example.demo.repository.ListaJpaRespository;
import com.example.demo.repository.ListaRepository;
import com.example.demo.security.ContextoSeguridad;

@ExtendWith(MockitoExtension.class)
public class ListaServiceTest {

    @Mock
    private ListaRepository listaRepository;

    @Mock
    private FavoritoJpaRepository favoritoJpaRepository;

    @Mock
    private ListaJpaRespository listaJpaRespository;

    @Mock
    private ContextoSeguridad contextoSeguridad;

    @InjectMocks
    private ListaService listaService;

    @Captor
    private ArgumentCaptor<Lista> listaCaptor;

    @Test
    @DisplayName("Al crear una lista, debe quedar asociada al ausuario autenticado obtenido del ContextoSeguridad")
    void crear_deberiaAsocialListaAlUsuarioAutenticado() {
        // Arrange (Preparacion)
        Long usuarioAutenticadoId = 42L;
        ListaRequest request = new ListaRequest("A usted le gustan las listas caballero?");

        when(contextoSeguridad.usuarioActualId()).thenReturn(usuarioAutenticadoId);
        when(listaRepository.save(any(Lista.class))).thenAnswer(invocation -> {
            Lista lista = invocation.getArgument(0);
            return new Lista(10L, lista.nombre(), lista.usuarioId());
        });

        // Act (Ejecucion)
        ListaResponse response = listaService.crear(request);

        // Assert (Verificacion)
        verify(listaRepository).save(listaCaptor.capture());
        Lista listaGuardada = listaCaptor.getValue();

        // Verificar que el usuario autenticado sea el dueño de la lista:
        assertThat(listaGuardada.usuarioId())
                .as("La lista debe tener el usuarioId del usuario autenticado")
                .isEqualTo(usuarioAutenticadoId);

        // Verificacion del nombre y respuesta
        assertThat(listaGuardada.nombre()).isEqualTo("A usted le gustan las listas caballero?");
        assertThat(listaGuardada.id()).isEqualTo(10L);
        assertThat(response.usuarioId()).isEqualTo(usuarioAutenticadoId);
        assertThat(response.nombre()).isEqualTo("A usted le gustan las listas caballero?");
    }

    @Test 
    @DisplayName("Si un usuario intenta acceder a una lista ajena y no es ADMIN, debe lanzar AccesoDenegadoException")
    void obtenerPorId_deberiaLanzarAccesoDenegadoExceptionSiNoEsDuenioNiAdmin() {
        // Arrange
        Long listaId = 5L;
        Long duenioId = 99L;
        Long usuarioActualId = 42L;
        Lista listaAjena = new Lista(listaId, "Lista de otro Caballero", duenioId);

        when(listaRepository.findById(listaId)).thenReturn(Optional.of(listaAjena));
        when(contextoSeguridad.esAdmin()).thenReturn(false);
        when(contextoSeguridad.usuarioActualId()).thenReturn(usuarioActualId);

        // Act & Assert
        assertThrows(AccesoDenegadoException.class, () -> listaService.obtenerPorId(listaId));
    }

    @Test 
    @DisplayName("Si un usuario ADMIN accede a una lista ajena, debe permitir el acceso con exito")
    void obtenerPorId_deberiaPermitirAccesoSiEsAdminAunqueSeaAjena() {
        // Arrange
        Long listaId = 5L;
        Long duenioId = 99L;
        Lista listaAjena = new Lista(listaId, "Lista de otro Caballero", duenioId);

        when(listaRepository.findById(listaId)).thenReturn(Optional.of(listaAjena));
        when(contextoSeguridad.esAdmin()).thenReturn(true);

        // Act
        ListaResponse response = listaService.obtenerPorId(listaId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(listaId);
        assertThat(response.usuarioId()).isEqualTo(duenioId);
    }

}
