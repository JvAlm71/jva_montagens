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
            funcionario.setGovEmail(request.govEmail());
            funcionario.setGovPassword(request.govPassword());
            funcionario.setCpf(request.cpf());
            funcionario.setRole(request.role());
            funcionario.setDailyRate(request.dailyRate());
            funcionario.setPricePerMeter(request.pricePerMeter());
            funcionario.setActive(request.active() == null || request.active());
            return funcionariosService.createFuncionario(funcionario);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping
    public List<FuncionariosModel> list(
            @RequestParam(required = false) JobRole role,
            @RequestParam(required = false) Boolean onlyActive
    ) {
        if (role != null) {
            return funcionariosService.listByRole(role, onlyActive);
        }
        return funcionariosService.listAll(onlyActive);
    }

    @GetMapping("/{id}")
    public FuncionariosModel getById(@PathVariable Long id) {
        try {
            return funcionariosService.getById(id);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public FuncionariosModel update(@PathVariable Long id, @RequestBody UpdateFuncionarioRequest request) {
        try {
            FuncionariosModel updates = new FuncionariosModel();
            updates.setName(request.name());
            updates.setPixKey(request.pixKey());
            updates.setGovEmail(request.govEmail());
            updates.setGovPassword(request.govPassword());
            updates.setCpf(request.cpf());
            updates.setRole(request.role());
            updates.setDailyRate(request.dailyRate());
            updates.setPricePerMeter(request.pricePerMeter());
            updates.setActive(request.active());
            return funcionariosService.updateFuncionario(id, updates);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    public record CreateFuncionarioRequest(
            String name,
            String pixKey,
            String govEmail,
            String govPassword,
            String cpf,
            JobRole role,
            BigDecimal dailyRate,
            BigDecimal pricePerMeter,
            Boolean active
    ) {}

    public record UpdateFuncionarioRequest(
            String name,
            String pixKey,
            String govEmail,
            String govPassword,
            String cpf,
            JobRole role,
            BigDecimal dailyRate,
            BigDecimal pricePerMeter,
            Boolean active
    ) {}
}
