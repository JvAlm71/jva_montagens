package com.java10x.jvaMontagens.service;

import com.java10x.jvaMontagens.model.FuncionariosModel;
import com.java10x.jvaMontagens.model.JobRole;
import com.java10x.jvaMontagens.model.UserModel;
import com.java10x.jvaMontagens.repository.FuncionarioRepository;
import com.java10x.jvaMontagens.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
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
        sanitizeAndValidate(funcionario);
        if (funcionario.getActive() == null) funcionario.setActive(true);
        attachUserIfProvided(funcionario, userCpf);

        return funcionarioRepository.save(funcionario);
    }

    public FuncionariosModel updateFuncionario(Long id, FuncionariosModel updates, String userCpf) {
        FuncionariosModel existing = getById(id);

        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getPixKey() != null) existing.setPixKey(updates.getPixKey());
        if (updates.getGovEmail() != null) existing.setGovEmail(updates.getGovEmail());
        if (updates.getGovPassword() != null) existing.setGovPassword(updates.getGovPassword());
        if (updates.getRole() != null) existing.setRole(updates.getRole());
        if (updates.getDailyRate() != null) existing.setDailyRate(updates.getDailyRate());
        if (updates.getPricePerMeter() != null) existing.setPricePerMeter(updates.getPricePerMeter());
        if (updates.getActive() != null) existing.setActive(updates.getActive());

        if (userCpf != null) {
            if (userCpf.isBlank()) {
                existing.setUser(null);
            } else {
                attachUserIfProvided(existing, userCpf);
            }
        }

        sanitizeAndValidate(existing);
        if (existing.getActive() == null) existing.setActive(true);
        return funcionarioRepository.save(existing);
    }

    public List<FuncionariosModel> listAll(Boolean onlyActive) {
        if (Boolean.TRUE.equals(onlyActive)) {
            return funcionarioRepository.findByActiveTrue();
        }
        return funcionarioRepository.findAll();
    }

    public List<FuncionariosModel> listByRole(JobRole role, Boolean onlyActive) {
        if (Boolean.TRUE.equals(onlyActive)) {
            return funcionarioRepository.findByRoleAndActiveTrue(role);
        }
        return funcionarioRepository.findByRole(role);
    }

    public FuncionariosModel getById(Long id) {
        return funcionarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Employee not found for id " + id));
    }

    private void sanitizeAndValidate(FuncionariosModel funcionario) {
        if (funcionario.getName() == null || funcionario.getName().isBlank()) {
            throw new IllegalArgumentException("Employee name is required.");
        }
        if (funcionario.getRole() == null) {
            throw new IllegalArgumentException("Employee role is required.");
        }

        funcionario.setName(funcionario.getName().trim());
        funcionario.setPixKey(normalizeNullable(funcionario.getPixKey()));

        String govEmail = normalizeNullable(funcionario.getGovEmail());
        funcionario.setGovEmail(govEmail == null ? null : govEmail.toLowerCase(Locale.ROOT));
        funcionario.setGovPassword(normalizeNullable(funcionario.getGovPassword()));

        validateNonNegative(funcionario.getDailyRate(), "dailyRate");
        validateNonNegative(funcionario.getPricePerMeter(), "pricePerMeter");
        validateRoleCompensation(funcionario);
    }

    private void validateRoleCompensation(FuncionariosModel funcionario) {
        if (funcionario.getRole() == JobRole.ASSEMBLER && funcionario.getDailyRate() == null) {
            throw new IllegalArgumentException("dailyRate is required for ASSEMBLER.");
        }
        if (funcionario.getRole() == JobRole.LEADER && funcionario.getPricePerMeter() == null) {
            throw new IllegalArgumentException("pricePerMeter is required for LEADER.");
        }
    }

    private void validateNonNegative(BigDecimal value, String field) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(field + " cannot be negative.");
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void attachUserIfProvided(FuncionariosModel funcionario, String userCpf) {
        if (userCpf == null || userCpf.isBlank()) return;

        String normalizedCpf = DocumentUtils.normalizeCpf(userCpf);
        UserModel user = userRepository.findById(normalizedCpf)
                .orElseThrow(() -> new NoSuchElementException("User not found for CPF " + normalizedCpf));
        funcionario.setUser(user);
    }
}
