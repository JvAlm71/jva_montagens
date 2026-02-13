package com.java10x.jvaMontagens.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.java10x.jvaMontagens.model.FuncionariosModel;
import com.java10x.jvaMontagens.model.JobRole;

import java.util.List;
import java.util.Optional;


@Repository
public interface FuncionarioRepository extends JpaRepository<FuncionariosModel, Long> {
    List<FuncionariosModel> findByRole(JobRole role);
    List<FuncionariosModel> findByActiveTrue();
    List<FuncionariosModel> findByRoleAndActiveTrue(JobRole role);
    Optional<FuncionariosModel> findByUserCpfAndRoleAndActiveTrue(String userCpf, JobRole role);
}
