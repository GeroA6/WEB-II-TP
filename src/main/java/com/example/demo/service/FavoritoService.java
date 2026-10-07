package com.example.demo.service;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.demo.domain.Favorito;
import com.example.demo.dto.favorito.FavoritoRequest;
import com.example.demo.dto.favorito.FavoritoResponse;
import com.example.demo.exception.RecursoNoEncontradoException;
import com.example.demo.repository.FavoritoRepository;

@Service
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;

    public FavoritoService(FavoritoRepository favoritoRepository) {
        this.favoritoRepository = favoritoRepository;
    }

    public List<FavoritoResponse> obtenerTodos() {
        return favoritoRepository.findAll()
                .stream()
                .map(this::aResponse)
                .toList();
    }

    public FavoritoResponse obtenerPorId(Long id) {
        Favorito favorito = buscarOFallar(id);
        return aResponse(favorito);
    }

    public FavoritoResponse crear(FavoritoRequest request) {
        Favorito nuevoFavorito = new Favorito(
                null,
                request.productoId(),
                request.nota(),
                LocalDateTime.now(),
                request.listaId());
        Favorito guardado = favoritoRepository.save(nuevoFavorito);
        return aResponse(guardado);
    }

    public FavoritoResponse actualizar(Long id, FavoritoRequest request) {
        Favorito existente = buscarOFallar(id);

        // se crea una nueva instancia manteniendo id y fechaAgregado
        Favorito actualizado = new Favorito(
                existente.id(),
                request.productoId(),
                request.nota(),
                existente.fechaAgregado(),
                request.listaId());
        Favorito guardado = favoritoRepository.save(actualizado);
        return aResponse(guardado);
    }

    public void eliminar(Long id) {
        buscarOFallar(id); // si no existe, lanza 404 antes de intentar borrar
        favoritoRepository.deleteById(id);
    }

    // metodos auxiliares
    private Favorito buscarOFallar(Long id) {
        return favoritoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el favorito con id: " + id));
    }

    private FavoritoResponse aResponse(Favorito favorito) {
        return new FavoritoResponse(
                favorito.id(),
                favorito.productoId(),
                favorito.nota(),
                favorito.fechaAgregado(),
                favorito.listaId());
    }

}
