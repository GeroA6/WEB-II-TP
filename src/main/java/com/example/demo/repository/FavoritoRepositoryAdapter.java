package com.example.demo.repository;

import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import com.example.demo.domain.Favorito;

@Repository
public class FavoritoRepositoryAdapter implements FavoritoRepository {

    private final FavoritoJpaRepository jpaRepository;
    private final ListaJpaRespository listaJpaRespository;

    public FavoritoRepositoryAdapter(FavoritoJpaRepository jpaRepository, ListaJpaRespository listaJpaRespository) {
        this.jpaRepository = jpaRepository;
        this.listaJpaRespository = listaJpaRespository;
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
        Long listaId = entity.getLista() != null ? entity.getLista().getId() : null;
        return new Favorito(
                entity.getId(),
                entity.getProductoId(),
                entity.getNota(),
                entity.getFechaAlta(),
                listaId);
    }

    private FavoritoEntity aEntity(Favorito dominio) {
        FavoritoEntity entity = new FavoritoEntity(
                dominio.id(),
                dominio.productoId(),
                dominio.nota(),
                dominio.fechaAgregado());
        if (dominio.listaId() != null) {
            entity.setLista(listaJpaRespository.getReferenceById(dominio.listaId()));
        }
        return entity;
    }
}
