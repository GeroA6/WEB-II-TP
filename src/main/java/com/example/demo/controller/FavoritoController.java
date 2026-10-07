package com.example.demo.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.favorito.FavoritoRequest;
import com.example.demo.dto.favorito.FavoritoResponse;
import com.example.demo.service.FavoritoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("api/favoritos")
@Tag(name = "favoritos", description = "CRUD de favoritos persistidos en PostgreSQL")
public class FavoritoController {

    private final FavoritoService favoritoService;

    public FavoritoController(FavoritoService favoritoService) {
        this.favoritoService = favoritoService;
    }

    @Operation(summary = "Crear un nuevo favorito")
    @PostMapping
    public ResponseEntity<FavoritoResponse> crear(@Valid @RequestBody FavoritoRequest request) {
        FavoritoResponse creado = favoritoService.crear(request);
        URI location = URI.create("/api/favoritos/" + creado.id());
        return ResponseEntity.created(location).body(creado);
    }

    @Operation(summary = "Listar todos los favoritos")
    @GetMapping
    public List<FavoritoResponse> obtenerTodos() {
        return favoritoService.obtenerTodos();
    }

    @Operation(summary = "Obtener un favorito por su ID")
    @GetMapping("/{id}")
    public FavoritoResponse obtenerPorId(@PathVariable Long id) {
        return favoritoService.obtenerPorId(id);
    }

    @Operation(summary = "Actualizar un favorito existente")
    @PutMapping("/{id}")
    public FavoritoResponse actualizar(@PathVariable Long id, @Valid @RequestBody FavoritoRequest request) {
        return favoritoService.actualizar(id, request);
    }

    @Operation(summary = "Eliminar un favorito por su ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        favoritoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

}
