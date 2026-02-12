package com.java10x.jvaMontagens.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "funcionarios")
public class FuncionariosModel {
    @Id
    @Column(name = "cpf", length = 11, nullable = false, unique = true)
    private Long cpf;
}
