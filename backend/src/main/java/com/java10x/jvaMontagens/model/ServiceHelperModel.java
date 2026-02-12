package com.java10x.jvaMontagens.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "service_helpers")
public class ServiceHelperModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_entry_id", nullable = false)
    private ServiceEntryModel serviceEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private FuncionariosModel employee;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal dailyRateUsed;

    @Column(nullable = false)
    private Integer daysUsed;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal totalCost;
}
