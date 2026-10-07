package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.favorito.FavoritoResponse;
import com.example.demo.dto.lista.ListaRequest;
import com.example.demo.dto.lista.ListaResponse;
import com.example.demo.service.ListaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/listas")
@Tag(name = "listas", description = "Gestion de listas de favoritos")
public class ListaController {

    private final ListaService listaService;

    public ListaController(ListaService listaService) {
        this.listaService = listaService;
    }

    @Operation(summary = "Crear una nueva lista")
    @PostMapping
    public ResponseEntity<ListaResponse> crear(@Valid @RequestBody ListaRequest request) {
        ListaResponse creada = listaService.crear(request);
        URI location = URI.create("/api/listas/" + creada.id());
        return ResponseEntity.created(location).body(creada);
    }

    @Operation(summary = "Listar todas las listas")
    @GetMapping
    public List<ListaResponse> obtenerTodas() {
        return listaService.obtenerTodas();
    }

    @Operation(summary = "Obtener una lista por su ID")
    @GetMapping("/{id}")
    public ListaResponse obtenerPorId(@PathVariable Long id) {
        return listaService.obtenerPorId(id);
    }

    @Operation(summary = "Obtener todos los favoritos que pertenecen a una lista")
    @GetMapping("/{id}/favoritos")
    public List<FavoritoResponse> obtenerFavoritosDeLista(@PathVariable Long id) {
        return listaService.obtenerFavoritosDeLista(id);
    }

    @Operation(summary = "Eliminar una lista por su ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        listaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
