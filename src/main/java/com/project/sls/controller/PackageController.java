package com.project.sls.controller;

import com.project.sls.dto.delivery.DeliveryDto;
import com.project.sls.dto.package_.AssignCourierRequestDto;
import com.project.sls.dto.package_.CreatePackageRequestDto;
import com.project.sls.dto.package_.PackageDto;
import com.project.sls.entity.Courier;
import com.project.sls.entity.Delivery;
import com.project.sls.entity.Package;
import com.project.sls.repository.CourierRepository;
import com.project.sls.repository.DeliveryRepository;
import com.project.sls.repository.PackageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/packages")
public class PackageController {

    private final PackageRepository packageRepository;
    private final CourierRepository courierRepository;
    private final DeliveryRepository deliveryRepository;

    public PackageController(
            PackageRepository packageRepository,
            CourierRepository courierRepository,
            DeliveryRepository deliveryRepository
    ) {
        this.packageRepository = packageRepository;
        this.courierRepository = courierRepository;
        this.deliveryRepository = deliveryRepository;
    }

    // Admin: list all packages
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<PackageDto> getAll() {
        return packageRepository.findAll().stream().map(PackageController::toDto).toList();
    }

    // Admin/Courier: view package by id (still needs "courier can only view assigned" rule later if you want)
    @PreAuthorize("hasAnyRole('ADMIN','COURIER')")
    @GetMapping("/{id}")
    public ResponseEntity<PackageDto> getById(@PathVariable Integer id) {
        return packageRepository.findById(id)
                .map(PackageController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Recommended: public tracking endpoint (no auth required)
    @PreAuthorize("permitAll()")
    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<PackageDto> getByTrackingNumber(@PathVariable String trackingNumber) {
        return packageRepository.findByTrackingNumber(trackingNumber)
                .map(PackageController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Admin: create package
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PackageDto create(@RequestBody CreatePackageRequestDto request) {
        if (request.priority() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "priority is required");
        }

        Package p = Package.builder()
                .trackingNumber("PKG-" + System.currentTimeMillis()) // TODO: generate properly
                .senderName(request.senderName())
                .receiverName(request.receiverName())
                .address(request.address())
                .city(request.city())
                .postalCode(request.postalCode())
                .weightKg(request.weightKg())
                .dimensions(request.dimensions())
                .priority(request.priority())
                .status(Package.Status.CREATED)
                .notes(request.notes())
                .build();

        Package saved = packageRepository.save(p);
        return toDto(saved);
    }

    // Admin: assign courier -> create Delivery
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assign")
    public DeliveryDto assignCourier(@RequestBody AssignCourierRequestDto request) {
        if (request.packageId() == null || request.courierId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "packageId and courierId are required");
        }

        Package p = packageRepository.findById(request.packageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));

        Courier c = courierRepository.findById(request.courierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Courier not found"));

        // Update package assignment
        p.setCourier(c);
        p.setStatus(Package.Status.ASSIGNED);
        packageRepository.save(p);

        // Create delivery record
        Delivery delivery = Delivery.builder()
                .pkg(p)
                .courier(c)
                .status(Delivery.Status.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .deliveredAt(null)
                .deliveryNotes(null)
                .build();

        Delivery saved = deliveryRepository.save(delivery);

        // Map to DeliveryDto (flat)
        return toDeliveryDto(saved);
    }

    private static DeliveryDto toDeliveryDto(Delivery d) {
        Package p = d.getPkg();
        Courier c = d.getCourier();

        Integer packageId = (p != null) ? p.getId() : null;
        String trackingNumber = (p != null) ? p.getTrackingNumber() : null;
        String receiverName = (p != null) ? p.getReceiverName() : null;
        String address = (p != null) ? p.getAddress() : null;
        String city = (p != null) ? p.getCity() : null;

        Integer courierId = (c != null) ? c.getId() : null;
        String courierName = (c != null && c.getUser() != null) ? c.getUser().getName() : null;

        return new DeliveryDto(
                d.getId(),
                packageId,
                trackingNumber,
                receiverName,
                address,
                city,
                courierId,
                courierName,
                d.getStatus(),
                d.getDeliveryNotes(),
                d.getAssignedAt(),
                d.getDeliveredAt()
        );
    }

    private static PackageDto toDto(Package p) {
        Courier c = p.getCourier();

        Integer courierId = null;
        String courierName = null;
        String courierPhone = null;

        if (c != null) {
            courierId = c.getId();
            courierPhone = c.getPhone();
            if (c.getUser() != null) {
                courierName = c.getUser().getName();
            }
        }

        return new PackageDto(
                p.getId(),
                p.getTrackingNumber(),
                p.getSenderName(),
                p.getReceiverName(),
                p.getAddress(),
                p.getCity(),
                p.getPostalCode(),
                p.getWeightKg(),
                p.getDimensions(),
                p.getPriority(),
                p.getStatus(),
                p.getNotes(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                courierId,
                courierName,
                courierPhone
        );
    }
}