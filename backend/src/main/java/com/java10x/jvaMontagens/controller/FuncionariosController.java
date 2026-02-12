package com.java10x.jvaMontagens.controller;

import com.java10x.jvaMontagens.model.FuncionariosModel;
import com.java10x.jvaMontagens.model.JobRole;
import com.java10x.jvaMontagens.service.FuncionariosService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/funcionarios")
public class FuncionariosController {
    private final FuncionariosService funcionariosService;

    public FuncionariosController(FuncionariosService funcionariosService) {
        this.funcionariosService = funcionariosService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FuncionariosModel create(@RequestBody CreateFuncionarioRequest request) {
        try {
            FuncionariosModel funcionario = new FuncionariosModel();
            funcionario.setName(request.name());
            funcionario.setPixKey(request.pixKey());
            funcionario.setRole(request.role());
            funcionario.setDailyRate(request.dailyRate());
            funcionario.setPricePerMeter(request.pricePerMeter());
            funcionario.setActive(request.active() == null || request.active());
            return funcionariosService.createFuncionario(funcionario, request.userCpf());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping
    public List<FuncionariosModel> list(@RequestParam(required = false) JobRole role) {
        if (role != null) {
            return funcionariosService.listByRole(role);
        }
        return funcionariosService.listAll();
    }

    @GetMapping("/{id}")
    public FuncionariosModel getById(@PathVariable Long id) {
        try {
            return funcionariosService.getById(id);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    public record CreateFuncionarioRequest(
            String name,
            String pixKey,
            JobRole role,
            BigDecimal dailyRate,
            BigDecimal pricePerMeter,
            String userCpf,
            Boolean active
    ) {}
}
