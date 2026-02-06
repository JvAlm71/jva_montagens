package com.java10x.jvaMontagens.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.java10x.jvaMontagens.model.UserModel;
import com.java10x.jvaMontagens.repository.UserRepository;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public UserModel createUser(UserModel user) {
        return userRepository.save(user);
    }
    
    public Optional<UserModel> getUserByCpf(Long cpf) {
        return userRepository.findById(cpf);
    }
    
    public List<UserModel> getAllUsers() {
        return userRepository.findAll();
    }
    
    public UserModel updateUser(UserModel user) {
        return userRepository.save(user);
    }
    
    public void deleteUserByCpf(Long cpf) {
        userRepository.deleteById(cpf);
    }
}