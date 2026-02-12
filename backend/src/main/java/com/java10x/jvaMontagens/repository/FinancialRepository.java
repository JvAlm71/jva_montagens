package com.java10x.jvaMontagens.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.java10x.jvaMontagens.model.FinancialModel;

@Repository
public  interface FinancialRepository extends JpaRepository<FinancialModel, Long> {
}