package com.java10x.jvaMontagens.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.java10x.jvaMontagens.model.FuncionariosModel;


@Repository
public interface FuncionarioRepository extends JpaRepository<FuncionariosModel, Long> {
}