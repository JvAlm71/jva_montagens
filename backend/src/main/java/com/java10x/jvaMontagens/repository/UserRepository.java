package com.java10x.jvaMontagens.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.java10x.jvaMontagens.model.UserModel;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserModel, String> {
    Optional<UserModel> findByEmailIgnoreCase(String email);
}
