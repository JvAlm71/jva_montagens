package com.java10x.jvaMontagens.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "parks")
public class ParkModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 80)
    private String city;

    @Column(length = 2)
    private String state;

    @ManyToOne
    @JoinColumn(name = "client_cnpj", referencedColumnName = "cnpj", nullable = false)
    private ClientModel client;

    @JsonIgnore
    @OneToMany(mappedBy = "park", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinancialModel> financialPeriods = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "park", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParkMediaModel> mediaFiles = new ArrayList<>();
}
