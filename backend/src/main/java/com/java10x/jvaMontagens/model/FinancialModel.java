package com.java10x.jvaMontagens.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "financial_periods",
        uniqueConstraints = @UniqueConstraint(name = "uk_park_year_month", columnNames = {"park_id", "fiscal_year", "fiscal_month"})
)
public class FinancialModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_year", nullable = false)
    private Integer year;

    @Column(name = "fiscal_month", nullable = false)
    private Integer month;

    @Column(name = "jva_price_per_meter", precision = 12, scale = 2, nullable = false)
    private BigDecimal jvaPricePerMeter;

    @Column(name = "leader_price_per_meter", precision = 12, scale = 2, nullable = false)
    private BigDecimal leaderPricePerMeter;

    @Column(name = "tax_rate", precision = 6, scale = 4, nullable = false)
    private BigDecimal taxRate;

    @Column(name = "car_rental_value", precision = 12, scale = 2, nullable = false)
    private BigDecimal carRentalValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancialStatus status = FinancialStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "park_id", nullable = false)
    private ParkModel park;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrator_id")
    private FuncionariosModel administrator;

    @OneToMany(mappedBy = "financial", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceEntryModel> serviceEntries = new ArrayList<>();

    @OneToMany(mappedBy = "financial", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentEntryModel> paymentEntries = new ArrayList<>();
}
