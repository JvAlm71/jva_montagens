package com.java10x.jvaMontagens.controller;

import com.java10x.jvaMontagens.security.SecurityFilter;
import com.java10x.jvaMontagens.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthService.LoginResponse login(@RequestBody AuthService.LoginRequest request) {
        try {
            return authService.login(request);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        } catch (SecurityException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        }
    }

    @GetMapping("/me")
    public AuthService.AuthUser me(Authentication authentication) {
        try {
            SecurityFilter.AdminPrincipal principal = (SecurityFilter.AdminPrincipal) authentication.getPrincipal();
            return authService.getCurrentAdmin(principal.cpf());
        } catch (ClassCastException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication not found.");
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (SecurityException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        }
    }
}
