package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoritoJpaRepository extends JpaRepository<FavoritoEntity, Long> {

    List<FavoritoEntity> findByListaId(Long listaId);
}
