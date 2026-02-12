package com.java10x.jvaMontagens.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "service_entries")
public class ServiceEntryModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_id", nullable = false)
    private FinancialModel financial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType = ServiceType.ASSEMBLY;

    @Column(nullable = false)
    private String teamType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id")
    private FuncionariosModel leader;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal meters;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal grossValue;

    @Column(length = 500)
    private String notes;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    private Integer days;

    @OneToMany(mappedBy = "serviceEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceHelperModel> helpers = new ArrayList<>();
}
