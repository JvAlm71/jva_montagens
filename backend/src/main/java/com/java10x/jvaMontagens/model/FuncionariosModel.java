package com.java10x.jvaMontagens.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "funcionarios")
@Entity
public class FuncionariosModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 120)
    private String pixKey;

    @Column(name = "gov_email", length = 150)
    private String govEmail;

    @Column(name = "gov_password", length = 200)
    private String govPassword;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobRole role;

    @Column(precision = 12, scale = 2)
    private BigDecimal dailyRate; 

    @Column(precision = 12, scale = 2)
    private BigDecimal pricePerMeter;

    @Column(nullable = false)
    private Boolean active = true;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "user_cpf", referencedColumnName = "cpf", unique = true)
    private UserModel user;

    @JsonIgnore
    @OneToMany(mappedBy = "leader")
    private List<ServiceEntryModel> servicesAsLeader = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "employee")
    private List<ServiceHelperModel> serviceHelpers = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "employee")
    private List<PaymentEntryModel> payments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "administrator")
    private List<FinancialModel> administeredPeriods = new ArrayList<>();
}
