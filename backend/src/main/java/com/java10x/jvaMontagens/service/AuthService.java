package com.java10x.jvaMontagens.service;

import com.java10x.jvaMontagens.model.FuncionariosModel;
import com.java10x.jvaMontagens.model.JobRole;
import com.java10x.jvaMontagens.model.UserModel;
import com.java10x.jvaMontagens.repository.FuncionarioRepository;
import com.java10x.jvaMontagens.repository.UserRepository;
import com.java10x.jvaMontagens.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(
            UserRepository userRepository,
            FuncionarioRepository funcionarioRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public LoginResponse login(LoginRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        UserModel user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new NoSuchElementException("Invalid email or password."));

        if (!passwordMatches(request.password(), user.getPassword())) {
            throw new NoSuchElementException("Invalid email or password.");
        }

        FuncionariosModel administrator = funcionarioRepository
                .findByUserCpfAndRoleAndActiveTrue(user.getCpf(), JobRole.ADMINISTRATOR)
                .orElseThrow(() -> new SecurityException("User is not an active administrator."));

        String token = tokenService.generateToken(user.getCpf(), JobRole.ADMINISTRATOR.name());

        AuthUser authUser = new AuthUser(
                user.getCpf(),
                user.getEmail(),
                user.getFullName(),
                administrator.getId(),
                administrator.getName(),
                administrator.getRole().name()
        );

        return new LoginResponse(token, "Bearer", authUser);
    }

    public AuthUser getCurrentAdmin(String cpf) {
        String normalizedCpf = DocumentUtils.normalizeCpf(cpf);

        UserModel user = userRepository.findById(normalizedCpf)
                .orElseThrow(() -> new NoSuchElementException("Authenticated user not found."));

        FuncionariosModel administrator = funcionarioRepository
                .findByUserCpfAndRoleAndActiveTrue(normalizedCpf, JobRole.ADMINISTRATOR)
                .orElseThrow(() -> new SecurityException("Authenticated user is not an active administrator."));

        return new AuthUser(
                user.getCpf(),
                administrator.getGovEmail() != null ? administrator.getGovEmail() : user.getEmail(),
                user.getFullName(),
                administrator.getId(),
                administrator.getName(),
                administrator.getRole().name()
        );
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword);
    }

    public record LoginRequest(
            String email,
            String password
    ) {}

    public record AuthUser(
            String cpf,
            String email,
            String fullName,
            Long employeeId,
            String employeeName,
            String role
    ) {}

    public record LoginResponse(
            String accessToken,
            String tokenType,
            AuthUser user
    ) {}
}
