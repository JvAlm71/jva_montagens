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
@Table(name = "clients")
public class ClientModel {

    @Id
    @Column(name = "cnpj", nullable = false, unique = true, length = 14)
    private String cnpj;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 20)
    private String contactPhone;

    @Column(length = 150)
    private String email;

    @JsonIgnore
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParkModel> parks = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "client")
    private List<PaymentEntryModel> payments = new ArrayList<>();
}
