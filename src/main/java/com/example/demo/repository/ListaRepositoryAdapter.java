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
    public List<Lista> findByUsuarioId(Long usuarioId) {
        return jpaRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(this::aDominio)
                .toList();
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
        Long usuarioId = entity.getUsuario() != null ? entity.getUsuario().getId() : null;
        return new Lista(entity.getId(), entity.getNombre(), usuarioId);
    }

    private ListaEntity aEntity(Lista dominio) {
        ListaEntity entity = new ListaEntity(dominio.id(), dominio.nombre());
        if (dominio.usuarioId() != null) {
            UsuarioEntity usuario = new UsuarioEntity();
            usuario.setId(dominio.usuarioId());
            entity.setUsuario(usuario);
        }
        return entity;
    }

}
