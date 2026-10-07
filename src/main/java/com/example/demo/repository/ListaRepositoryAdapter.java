package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import com.example.demo.domain.Lista;

@Repository
public class ListaRepositoryAdapter implements ListaRepository {

    private final ListaJpaRespository jpaRepository;

    public ListaRepositoryAdapter(ListaJpaRespository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Lista> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(this::aDominio)
                .toList();
    }

    @Override
    public Optional<Lista> findById(Long id) {
        return jpaRepository.findById(id)
                .map(this::aDominio);
    }

    @Override
    public Lista save(Lista lista) {
        ListaEntity entity = aEntity(lista);
        ListaEntity guardada = jpaRepository.save(entity);
        return aDominio(guardada);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private Lista aDominio(ListaEntity entity) {
        return new Lista(entity.getId(), entity.getNombre());
    }

    private ListaEntity aEntity(Lista dominio) {
        return new ListaEntity(dominio.id(), dominio.nombre());
    }

}
