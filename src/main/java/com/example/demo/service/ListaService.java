package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.domain.Lista;
import com.example.demo.dto.favorito.FavoritoResponse;
import com.example.demo.dto.lista.ListaRequest;
import com.example.demo.dto.lista.ListaResponse;
import com.example.demo.exception.ConflictoEliminacionException;
import com.example.demo.exception.RecursoNoEncontradoException;
import com.example.demo.repository.FavoritoEntity;
import com.example.demo.repository.FavoritoJpaRepository;
import com.example.demo.repository.ListaRepository;

@Service
public class ListaService {

    private final ListaRepository listaRepository;
    private final FavoritoJpaRepository favoritoJpaRepository;

    public ListaService(ListaRepository listaRepository, FavoritoJpaRepository favoritoJpaRepository) {
        this.listaRepository = listaRepository;
        this.favoritoJpaRepository = favoritoJpaRepository;
    }

    public List<ListaResponse> obtenerTodas() {
        return listaRepository.findAll()
                .stream()
                .map(this::aResponse)
                .toList();
    }

    public ListaResponse obtenerPorId(Long id) {
        Lista lista = buscarOFallar(id);
        return aResponse(lista);
    }

    public ListaResponse crear(ListaRequest request) {
        Lista nuevaLista = new Lista(null, request.nombre());
        Lista guardada = listaRepository.save(nuevaLista);
        return aResponse(guardada);
    }

    public List<FavoritoResponse> obtenerFavoritosDeLista(Long listaId) {
        buscarOFallar(listaId); // Valida que la lista exista (404 si no)
        return favoritoJpaRepository.findByListaId(listaId)
                .stream()
                .map(this::aFavoritoResponse)
                .toList();
    }

    public void eliminar(Long id) {
        buscarOFallar(id); // Valida que la lista exista (404 si no)
        List<FavoritoEntity> favoritos = favoritoJpaRepository.findByListaId(id);
        if (!favoritos.isEmpty()) {
            throw new ConflictoEliminacionException("No se puede eliminar la lista porque todavía contiene favoritos");
        }
        listaRepository.deleteById(id);
    }

    // Métodos auxiliares
    private Lista buscarOFallar(Long id) {
        return listaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la lista con id: " + id));
    }

    private ListaResponse aResponse(Lista lista) {
        return new ListaResponse(lista.id(), lista.nombre());
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
