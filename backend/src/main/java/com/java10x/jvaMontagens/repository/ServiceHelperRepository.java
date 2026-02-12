package com.java10x.jvaMontagens.repository;

import com.java10x.jvaMontagens.model.ServiceHelperModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceHelperRepository extends JpaRepository<ServiceHelperModel, Long> {
}
