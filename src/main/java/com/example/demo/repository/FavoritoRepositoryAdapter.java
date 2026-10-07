package com.example.demo.repository;

import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import com.example.demo.domain.Favorito;

@Repository
public class FavoritoRepositoryAdapter implements FavoritoRepository {

    private final FavoritoJpaRepository jpaRepository;

    public FavoritoRepositoryAdapter(FavoritoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Favorito> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(this::aDominio)
                .toList();
    }

    @Override
    public Optional<Favorito> findById(Long id) {
        return jpaRepository.findById(id)
                .map(this::aDominio);
    }

    @Override
    public Favorito save(Favorito favorito) {
        FavoritoEntity entity = aEntity(favorito);
        FavoritoEntity guardada = jpaRepository.save(entity);
        return aDominio(guardada);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    // Métodos privados de converción (Adapter)

    private Favorito aDominio(FavoritoEntity entity) {
        return new Favorito(
                entity.getId(),
                entity.getProductoId(),
                entity.getNota(),
                entity.getFechaAlta());
    }

    private FavoritoEntity aEntity(Favorito dominio) {
        return new FavoritoEntity(
                dominio.id(),
                dominio.productoId(),
                dominio.nota(),
                dominio.fechaAgregado());
    }
}
