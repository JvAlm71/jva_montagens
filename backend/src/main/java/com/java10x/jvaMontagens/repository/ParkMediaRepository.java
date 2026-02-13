package com.java10x.jvaMontagens.repository;

import com.java10x.jvaMontagens.model.ParkMediaModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkMediaRepository extends JpaRepository<ParkMediaModel, Long> {
    List<ParkMediaModel> findByParkIdOrderByUploadedAtDesc(Long parkId);
}
