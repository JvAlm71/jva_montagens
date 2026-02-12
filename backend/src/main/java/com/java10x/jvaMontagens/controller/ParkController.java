package com.java10x.jvaMontagens.controller;

import com.java10x.jvaMontagens.model.ParkModel;
import com.java10x.jvaMontagens.service.ParkService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/parks")
public class ParkController {
    private final ParkService parkService;

    public ParkController(ParkService parkService) {
        this.parkService = parkService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParkModel createPark(@RequestBody CreateParkRequest request) {
        try {
            ParkModel park = new ParkModel();
            park.setName(request.name());
            park.setCity(request.city());
            park.setState(request.state());
            return parkService.createPark(park, request.clientCnpj());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping
    public List<ParkModel> listParks(@RequestParam(required = false) String clientCnpj) {
        try {
            return parkService.listParks(clientCnpj);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PutMapping("/{parkId}")
    public ParkModel updatePark(@PathVariable Long parkId, @RequestBody CreateParkRequest request) {
        try {
            return parkService.updatePark(parkId, request.name(), request.city(), request.state(), request.clientCnpj());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @DeleteMapping("/{parkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePark(@PathVariable Long parkId) {
        try {
            parkService.deletePark(parkId);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    public record CreateParkRequest(
            String name,
            String city,
            String state,
            String clientCnpj
    ) {}
}
