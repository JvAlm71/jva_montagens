package com.java10x.jvaMontagens.security;

import com.java10x.jvaMontagens.model.JobRole;
import com.java10x.jvaMontagens.model.UserModel;
import com.java10x.jvaMontagens.repository.FuncionarioRepository;
import com.java10x.jvaMontagens.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class SecurityFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final FuncionarioRepository funcionarioRepository;

    public SecurityFilter(
            TokenService tokenService,
            UserRepository userRepository,
            FuncionarioRepository funcionarioRepository
    ) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            authenticateFromToken(token, request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateFromToken(String token, HttpServletRequest request) {
        try {
            TokenService.TokenData tokenData = tokenService.parseAndValidate(token);
            if (!JobRole.ADMINISTRATOR.name().equals(tokenData.role())) {
                throw new SecurityException("Invalid role.");
            }

            UserModel user = userRepository.findById(tokenData.cpf())
                    .orElseThrow(() -> new SecurityException("User not found for token."));

            funcionarioRepository.findByUserCpfAndRoleAndActiveTrue(tokenData.cpf(), JobRole.ADMINISTRATOR)
                    .orElseThrow(() -> new SecurityException("Inactive administrator."));

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                AdminPrincipal principal = new AdminPrincipal(
                        user.getCpf(),
                        user.getEmail(),
                        user.getFullName(),
                        JobRole.ADMINISTRATOR.name()
                );

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + JobRole.ADMINISTRATOR.name()))
                );
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }
    }

    public record AdminPrincipal(
            String cpf,
            String email,
            String fullName,
            String role
    ) {}
}
