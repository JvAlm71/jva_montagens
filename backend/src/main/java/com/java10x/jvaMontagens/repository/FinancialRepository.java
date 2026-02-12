package com.java10x.jvaMontagens.repository;

import com.java10x.jvaMontagens.model.FinancialModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialRepository extends JpaRepository<FinancialModel, Long> {
    Optional<FinancialModel> findByParkIdAndYearAndMonth(Long parkId, Integer year, Integer month);
    boolean existsByParkIdAndYearAndMonth(Long parkId, Integer year, Integer month);
    List<FinancialModel> findByParkIdOrderByYearDescMonthDesc(Long parkId);
}
