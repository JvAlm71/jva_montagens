package com.java10x.jvaMontagens.repository;

import com.java10x.jvaMontagens.model.ServiceEntryModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceEntryRepository extends JpaRepository<ServiceEntryModel, Long> {
    List<ServiceEntryModel> findByFinancialId(Long financialId);
}
