package com.java10x.jvaMontagens.repository;

import com.java10x.jvaMontagens.model.ParkModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkRepository extends JpaRepository<ParkModel, Long> {
    List<ParkModel> findByClientCnpj(String clientCnpj);
}
