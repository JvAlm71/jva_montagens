package com.java10x.jvaMontagens.service;

import com.java10x.jvaMontagens.model.ClientModel;
import com.java10x.jvaMontagens.repository.ClientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ClientService {
    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public ClientModel createClient(ClientModel client) {
        if (client.getName() == null || client.getName().isBlank()) {
            throw new IllegalArgumentException("Client name is required.");
        }

        client.setCnpj(DocumentUtils.normalizeCnpj(client.getCnpj()));
        client.setName(client.getName().trim());

        if (clientRepository.existsById(client.getCnpj())) {
            throw new IllegalArgumentException("Client with CNPJ " + client.getCnpj() + " already exists.");
        }
        return clientRepository.save(client);
    }

    public List<ClientModel> listClients() {
        return clientRepository.findAll();
    }

    public ClientModel getByCnpj(String cnpj) {
        String normalizedCnpj = DocumentUtils.normalizeCnpj(cnpj);
        return clientRepository.findById(normalizedCnpj)
                .orElseThrow(() -> new NoSuchElementException("Client not found for CNPJ " + normalizedCnpj));
    }

    public ClientModel updateClient(String cnpj, ClientModel updates) {
        String normalizedCnpj = DocumentUtils.normalizeCnpj(cnpj);
        ClientModel existing = clientRepository.findById(normalizedCnpj)
                .orElseThrow(() -> new NoSuchElementException("Client not found for CNPJ " + normalizedCnpj));

        if (updates.getName() != null && !updates.getName().isBlank()) {
            existing.setName(updates.getName().trim());
        }
        if (updates.getContactPhone() != null) {
            existing.setContactPhone(updates.getContactPhone());
        }
        if (updates.getEmail() != null) {
            existing.setEmail(updates.getEmail());
        }
        return clientRepository.save(existing);
    }

    public void deleteClient(String cnpj) {
        String normalizedCnpj = DocumentUtils.normalizeCnpj(cnpj);
        ClientModel existing = clientRepository.findById(normalizedCnpj)
                .orElseThrow(() -> new NoSuchElementException("Client not found for CNPJ " + normalizedCnpj));
        clientRepository.delete(existing);
    }
}
