package com.java10x.jvaMontagens.service;

import com.java10x.jvaMontagens.model.ClientModel;
import com.java10x.jvaMontagens.model.ParkModel;
import com.java10x.jvaMontagens.repository.ClientRepository;
import com.java10x.jvaMontagens.repository.ParkRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ParkService {
    private final ParkRepository parkRepository;
    private final ClientRepository clientRepository;

    public ParkService(ParkRepository parkRepository, ClientRepository clientRepository) {
        this.parkRepository = parkRepository;
        this.clientRepository = clientRepository;
    }

    public ParkModel createPark(ParkModel park, String clientCnpj) {
        if (park.getName() == null || park.getName().isBlank()) {
            throw new IllegalArgumentException("Park name is required.");
        }

        String normalizedCnpj = DocumentUtils.normalizeCnpj(clientCnpj);
        ClientModel client = clientRepository.findById(normalizedCnpj)
                .orElseThrow(() -> new NoSuchElementException("Client not found for CNPJ " + normalizedCnpj));

        park.setName(park.getName().trim());
        park.setClient(client);
        return parkRepository.save(park);
    }

    public List<ParkModel> listParks(String clientCnpj) {
        if (clientCnpj != null) {
            return parkRepository.findByClientCnpj(DocumentUtils.normalizeCnpj(clientCnpj));
        }
        return parkRepository.findAll();
    }

    public ParkModel updatePark(Long parkId, String name, String city, String state, String clientCnpj) {
        ParkModel existing = parkRepository.findById(parkId)
                .orElseThrow(() -> new NoSuchElementException("Park not found for id " + parkId));

        if (name != null && !name.isBlank()) {
            existing.setName(name.trim());
        }
        if (city != null) {
            existing.setCity(city);
        }
        if (state != null) {
            existing.setState(state);
        }
        if (clientCnpj != null && !clientCnpj.isBlank()) {
            String normalizedCnpj = DocumentUtils.normalizeCnpj(clientCnpj);
            ClientModel client = clientRepository.findById(normalizedCnpj)
                    .orElseThrow(() -> new NoSuchElementException("Client not found for CNPJ " + normalizedCnpj));
            existing.setClient(client);
        }
        return parkRepository.save(existing);
    }

    public void deletePark(Long parkId) {
        ParkModel existing = parkRepository.findById(parkId)
                .orElseThrow(() -> new NoSuchElementException("Park not found for id " + parkId));
        parkRepository.delete(existing);
    }
}
