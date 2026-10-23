package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ListaJpaRespository extends JpaRepository<ListaEntity, Long> {

    List<ListaEntity> findByUsuarioId(Long usuarioId);

}
