package com.java10x.jvaMontagens.service;

import com.java10x.jvaMontagens.model.FuncionariosModel;
import com.java10x.jvaMontagens.model.JobRole;
import com.java10x.jvaMontagens.model.UserModel;
import com.java10x.jvaMontagens.repository.FuncionarioRepository;
import com.java10x.jvaMontagens.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class FuncionariosService {
    private final FuncionarioRepository funcionarioRepository;
    private final UserRepository userRepository;

    public FuncionariosService(FuncionarioRepository funcionarioRepository, UserRepository userRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.userRepository = userRepository;
    }

    public FuncionariosModel createFuncionario(FuncionariosModel funcionario, String userCpf) {
        if (funcionario.getName() == null || funcionario.getName().isBlank()) {
            throw new IllegalArgumentException("Employee name is required.");
        }
        if (funcionario.getRole() == null) {
            throw new IllegalArgumentException("Employee role is required.");
        }

        funcionario.setName(funcionario.getName().trim());

        if (funcionario.getRole() == JobRole.ASSEMBLER && funcionario.getDailyRate() == null) {
            throw new IllegalArgumentException("dailyRate is required for ASSEMBLER.");
        }
        if (funcionario.getRole() == JobRole.LEADER && funcionario.getPricePerMeter() == null) {
            throw new IllegalArgumentException("pricePerMeter is required for LEADER.");
        }

        if (funcionario.getActive() == null) {
            funcionario.setActive(true);
        }

        if (userCpf != null) {
            String normalizedCpf = DocumentUtils.normalizeCpf(userCpf);
            UserModel user = userRepository.findById(normalizedCpf)
                    .orElseThrow(() -> new NoSuchElementException("User not found for CPF " + normalizedCpf));
            funcionario.setUser(user);
        }

        return funcionarioRepository.save(funcionario);
    }

    public List<FuncionariosModel> listAll() {
        return funcionarioRepository.findAll();
    }

    public List<FuncionariosModel> listByRole(JobRole role) {
        return funcionarioRepository.findByRole(role);
    }

    public FuncionariosModel getById(Long id) {
        return funcionarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Employee not found for id " + id));
    }
}
