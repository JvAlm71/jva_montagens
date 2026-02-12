package com.java10x.jvaMontagens.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class UserModel {
        
    @Id
    @Column(name = "cpf", length = 11, nullable = false, unique = true)
    private String cpf;
    
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;
    
    @Column(name = "password", nullable = false, length = 200)
    @JsonIgnore
    private String password;
    
    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;
    
}
