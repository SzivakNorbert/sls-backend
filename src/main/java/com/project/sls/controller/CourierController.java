package com.project.sls.controller;

import com.project.sls.dto.courier.CourierDto;
import com.project.sls.entity.Courier;
import com.project.sls.repository.CourierRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/couriers")
public class CourierController {

    private final CourierRepository courierRepository;

    public CourierController(CourierRepository courierRepository) {
        this.courierRepository = courierRepository;
    }

    @GetMapping
    public List<CourierDto> getAll() {
        return courierRepository.findAll().stream()
                .map(CourierController::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourierDto> getById(@PathVariable Integer id) {
        return courierRepository.findById(id)
                .map(CourierController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private static CourierDto toDto(Courier c) {
        Integer userId = null;
        String name = null;
        String email = null;

        if (c.getUser() != null) {
            userId = c.getUser().getId();
            name = c.getUser().getName();
            email = c.getUser().getEmail();
        }

        long activeDeliveries = 0; // TODO implement
        long totalDelivered = 0;   // TODO implement

        return new CourierDto(
                c.getId(),
                userId,
                name,
                email,
                c.getVehicleType(),
                c.getLicensePlate(),
                c.getPhone(),
                c.getMaxWeightKg(),
                c.getIsActive(),
                activeDeliveries,
                totalDelivered
        );
    }
}