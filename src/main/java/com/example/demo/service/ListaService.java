package com.example.demo.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.Lista;
import com.example.demo.dto.favorito.FavoritoResponse;
import com.example.demo.dto.lista.ListaRequest;
import com.example.demo.dto.lista.ListaResponse;
import com.example.demo.exception.AccesoDenegadoException;
import com.example.demo.exception.ConflictoEliminacionException;
import com.example.demo.exception.RecursoNoEncontradoException;
import com.example.demo.repository.FavoritoEntity;
import com.example.demo.repository.FavoritoJpaRepository;
import com.example.demo.repository.ListaEntity;
import com.example.demo.repository.ListaJpaRespository;
import com.example.demo.repository.ListaRepository;
import com.example.demo.security.ContextoSeguridad;

@Service
public class ListaService {

    private final ListaRepository listaRepository;
    private final FavoritoJpaRepository favoritoJpaRepository;
    private final ListaJpaRespository listaJpaRespository;
    private final ContextoSeguridad contextoSeguridad;

    public ListaService(
            ListaRepository listaRepository,
            FavoritoJpaRepository favoritoJpaRepository,
            ListaJpaRespository listaJpaRespository,
            ContextoSeguridad contextoSeguridad) {
        this.listaRepository = listaRepository;
        this.favoritoJpaRepository = favoritoJpaRepository;
        this.listaJpaRespository = listaJpaRespository;
        this.contextoSeguridad = contextoSeguridad;
    }

    public List<ListaResponse> obtenerTodas() {
        List<Lista> listas;
        if (contextoSeguridad.esAdmin()) {
            listas = listaRepository.findAll();
        } else {
            Long usuarioId = contextoSeguridad.usuarioActualId();
            listas = (usuarioId != null) ? listaRepository.findByUsuarioId(usuarioId) : List.of();
        }
        return listas.stream()
                .map(this::aResponse)
                .toList();
    }

    public ListaResponse obtenerPorId(Long id) {
        Lista lista = buscarPropiaOFallar(id);
        return aResponse(lista);
    }

    public ListaResponse crear(ListaRequest request) {
        Long usuarioId = contextoSeguridad.usuarioActualId();
        Lista nuevaLista = new Lista(null, request.nombre(), usuarioId);
        Lista guardada = listaRepository.save(nuevaLista);
        return aResponse(guardada);
    }

    public List<FavoritoResponse> obtenerFavoritosDeLista(Long listaId) {
        buscarPropiaOFallar(listaId); // Valida que la lista exista (403 si no)
        return favoritoJpaRepository.findByListaId(listaId)
                .stream()
                .map(this::aFavoritoResponse)
                .toList();
    }

    public void eliminar(Long id) {
        buscarPropiaOFallar(id); // Valida que la lista exista (403 si no)
        List<FavoritoEntity> favoritos = favoritoJpaRepository.findByListaId(id);
        if (!favoritos.isEmpty()) {
            throw new ConflictoEliminacionException("No se puede eliminar la lista porque todavía contiene favoritos");
        }
        listaRepository.deleteById(id);
    }

    @Transactional
    public void moverFavoritosYEliminarOrigen(Long origenId, Long destinoId) {
        if (Objects.equals(origenId, destinoId)) {
            throw new IllegalArgumentException(
                    "La lista de destino debe ser distinta de la lista de origen");
        }

        // Valida que ambas listas existan y pertenezcan al usuario (o sea ADMIN)
        buscarPropiaOFallar(origenId);
        buscarPropiaOFallar(destinoId);

        ListaEntity destino = listaJpaRespository.findById(destinoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la lista con id: " + destinoId));

        List<FavoritoEntity> favoritosOrigen = favoritoJpaRepository.findByListaId(origenId);

        favoritosOrigen.forEach(favorito -> favorito.setLista(destino));
        favoritoJpaRepository.saveAll(favoritosOrigen);

        listaRepository.deleteById(origenId);
    }

    // Métodos auxiliares
    private Lista buscarPropiaOFallar(Long id) {
        Lista lista = listaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la lista con id: " + id));

        boolean esDueno = lista.usuarioId() != null && lista.usuarioId().equals(contextoSeguridad.usuarioActualId());

        if (!esDueno && !contextoSeguridad.esAdmin()) {
            throw new AccesoDenegadoException("La lista con id " + id + " no te pertenece");
        }
        return lista;
    }

    private ListaResponse aResponse(Lista lista) {
        return new ListaResponse(lista.id(), lista.nombre(), lista.usuarioId());
    }

    private FavoritoResponse aFavoritoResponse(FavoritoEntity entity) {
        Long listaId = entity.getLista() != null ? entity.getLista().getId() : null;
        return new FavoritoResponse(
                entity.getId(),
                entity.getProductoId(),
                entity.getNota(),
                entity.getFechaAlta(),
                listaId);
    }

}
