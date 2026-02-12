package com.java10x.jvaMontagens.service;

import com.java10x.jvaMontagens.model.UserModel;
import com.java10x.jvaMontagens.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public UserModel createUser(UserModel user) {
        user.setCpf(DocumentUtils.normalizeCpf(user.getCpf()));
        user.setPassword(normalizePassword(user.getPassword()));
        return userRepository.save(user);
    }
    
    public Optional<UserModel> getUserByCpf(String cpf) {
        return userRepository.findById(DocumentUtils.normalizeCpf(cpf));
    }
    
    public List<UserModel> getAllUsers() {
        return userRepository.findAll();
    }
    
    public UserModel updateUser(UserModel user) {
        user.setCpf(DocumentUtils.normalizeCpf(user.getCpf()));
        user.setPassword(normalizePassword(user.getPassword()));
        return userRepository.save(user);
    }
    
    public void deleteUserByCpf(String cpf) {
        userRepository.deleteById(DocumentUtils.normalizeCpf(cpf));
    }

    private String normalizePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$")) {
            return password;
        }
        return passwordEncoder.encode(password);
    }
}
