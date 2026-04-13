package com.project.sls.controller;

import com.project.sls.dto.delivery.DeliveryDto;
import com.project.sls.dto.delivery.UpdateStatusRequestDto;
import com.project.sls.entity.Courier;
import com.project.sls.entity.Delivery;
import com.project.sls.repository.CourierRepository;
import com.project.sls.repository.DeliveryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryRepository deliveryRepository;
    private final CourierRepository courierRepository;

    public DeliveryController(DeliveryRepository deliveryRepository, CourierRepository courierRepository) {
        this.deliveryRepository = deliveryRepository;
        this.courierRepository = courierRepository;
    }

    // Admin: list all deliveries
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<DeliveryDto> getAll() {
        return deliveryRepository.findAll().stream().map(DeliveryController::toDto).toList();
    }

    // Admin or Courier: list deliveries for a courierId
    // (Stricter "courier can only access own courierId" can be added later.)
    @PreAuthorize("hasAnyRole('ADMIN','COURIER')")
    @GetMapping("/courier/{courierId}")
    public List<DeliveryDto> getByCourierId(@PathVariable Integer courierId) {
        Courier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Courier not found"));

        return deliveryRepository.findAllByCourier(courier).stream().map(DeliveryController::toDto).toList();
    }

    // Courier/Admin can update status
    @PreAuthorize("hasAnyRole('ADMIN','COURIER')")
    @PatchMapping("/{deliveryId}/status")
    public DeliveryDto updateStatus(@PathVariable Integer deliveryId, @RequestBody UpdateStatusRequestDto request) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));

        if (request.newStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "newStatus is required");
        }

        // minimal rules:
        // - if FAILED, notes required
        if (request.newStatus() == Delivery.Status.FAILED && (request.deliveryNotes() == null || request.deliveryNotes().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deliveryNotes is required for FAILED");
        }

        delivery.setStatus(request.newStatus());
        delivery.setDeliveryNotes(request.deliveryNotes());

        if (request.newStatus() == Delivery.Status.DELIVERED) {
            delivery.setDeliveredAt(LocalDateTime.now());
        }

        Delivery saved = deliveryRepository.save(delivery);
        return toDto(saved);
    }

    private static DeliveryDto toDto(Delivery d) {
        Integer packageId = d.getPkg() != null ? d.getPkg().getId() : null;
        String trackingNumber = d.getPkg() != null ? d.getPkg().getTrackingNumber() : null;
        String receiverName = d.getPkg() != null ? d.getPkg().getReceiverName() : null;
        String address = d.getPkg() != null ? d.getPkg().getAddress() : null;
        String city = d.getPkg() != null ? d.getPkg().getCity() : null;

        Integer courierId = d.getCourier() != null ? d.getCourier().getId() : null;
        String courierName = (d.getCourier() != null && d.getCourier().getUser() != null)
                ? d.getCourier().getUser().getName()
                : null;

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
}