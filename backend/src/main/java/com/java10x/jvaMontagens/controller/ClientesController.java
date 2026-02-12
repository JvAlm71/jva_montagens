package com.java10x.jvaMontagens.controller;

import com.java10x.jvaMontagens.model.ClientModel;
import com.java10x.jvaMontagens.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/clientes")
public class ClientesController {
    private final ClientService clientService;

    public ClientesController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientModel createClient(@RequestBody CreateClientRequest request) {
        try {
            ClientModel client = new ClientModel();
            client.setCnpj(request.cnpj());
            client.setName(request.name());
            client.setContactPhone(request.contactPhone());
            client.setEmail(request.email());
            return clientService.createClient(client);
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("already exists")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping
    public List<ClientModel> listClients() {
        return clientService.listClients();
    }

    @GetMapping("/{cnpj}")
    public ClientModel getByCnpj(@PathVariable String cnpj) {
        try {
            return clientService.getByCnpj(cnpj);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PutMapping("/{cnpj}")
    public ClientModel updateClient(@PathVariable String cnpj, @RequestBody CreateClientRequest request) {
        try {
            ClientModel updates = new ClientModel();
            updates.setName(request.name());
            updates.setContactPhone(request.contactPhone());
            updates.setEmail(request.email());
            return clientService.updateClient(cnpj, updates);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @DeleteMapping("/{cnpj}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClient(@PathVariable String cnpj) {
        try {
            clientService.deleteClient(cnpj);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    public record CreateClientRequest(
            String cnpj,
            String name,
            String contactPhone,
            String email
    ) {}
}
