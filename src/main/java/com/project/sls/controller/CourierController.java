package com.project.sls.controller;

import com.project.sls.dto.courier.CourierDto;
import com.project.sls.dto.courier.CreateCourierRequestDto;
import com.project.sls.entity.Courier;
import com.project.sls.entity.Delivery;
import com.project.sls.entity.User;
import com.project.sls.repository.CourierRepository;
import com.project.sls.repository.DeliveryRepository;
import com.project.sls.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/couriers")
public class CourierController {

    private final CourierRepository courierRepository;
    private final UserRepository userRepository;
    private final DeliveryRepository deliveryRepository;

    public CourierController(CourierRepository courierRepository, UserRepository userRepository, DeliveryRepository deliveryRepository) {
        this.courierRepository = courierRepository;
        this.userRepository = userRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN','COURIER')")
    @GetMapping
    public List<CourierDto> getAll() {
        return courierRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','COURIER')")
    @GetMapping("/{id}")
    public ResponseEntity<CourierDto> getById(@PathVariable Integer id) {
        return courierRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private CourierDto toDto(Courier c) {
        Integer userId = null;
        String name = null;
        String email = null;

        if (c.getUser() != null) {
            userId = c.getUser().getId();
            name = c.getUser().getName();
            email = c.getUser().getEmail();
        }

        long activeDeliveries = deliveryRepository.countByCourierAndStatus(c, Delivery.Status.ASSIGNED)
                + deliveryRepository.countByCourierAndStatus(c, Delivery.Status.IN_TRANSIT);
        long totalDelivered   = deliveryRepository.countByCourierAndStatus(c, Delivery.Status.DELIVERED);

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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourierDto create(@RequestBody CreateCourierRequestDto request) {
        if (request.userId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (courierRepository.findByUserId(request.userId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Courier already exists for this user");
        }

        Courier courier = Courier.builder()
                .user(user)
                .vehicleType(request.vehicleType())
                .licensePlate(request.licensePlate())
                .phone(request.phone())
                .maxWeightKg(request.maxWeightKg())
                .isActive(true)
                .build();

        return toDto(courierRepository.save(courier));
    }
}