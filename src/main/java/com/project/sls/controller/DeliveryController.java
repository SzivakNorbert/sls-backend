package com.project.sls.controller;

import com.project.sls.dto.delivery.DeliveryDto;
import com.project.sls.dto.delivery.UpdateStatusRequestDto;
import com.project.sls.entity.Courier;
import com.project.sls.entity.Delivery;
import com.project.sls.entity.User;
import com.project.sls.repository.CourierRepository;
import com.project.sls.repository.DeliveryRepository;
import com.project.sls.service.LogisticsWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryRepository deliveryRepository;
    private final CourierRepository courierRepository;
    private final LogisticsWorkflowService workflowService;

    public DeliveryController(
            DeliveryRepository deliveryRepository,
            CourierRepository courierRepository,
            LogisticsWorkflowService workflowService
    ) {
        this.deliveryRepository = deliveryRepository;
        this.courierRepository = courierRepository;
        this.workflowService = workflowService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<DeliveryDto> getAll() {
        return deliveryRepository.findAll().stream().map(LogisticsWorkflowService::toDeliveryDto).toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','COURIER')")
    @GetMapping("/courier/{courierId}")
    public List<DeliveryDto> getByCourierId(@PathVariable Integer courierId, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User currentUser
                && currentUser.getRole() == User.Role.COURIER) {
            Courier authenticatedCourier = courierRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Courier profile not found"));

            if (!authenticatedCourier.getId().equals(courierId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own deliveries");
            }
        }

        Courier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Courier not found"));

        return deliveryRepository.findAllByCourier(courier).stream().map(LogisticsWorkflowService::toDeliveryDto).toList();
    }

    @PreAuthorize("hasRole('COURIER')")
    @GetMapping("/my")
    public List<DeliveryDto> getMyDeliveries(Authentication authentication) {
        Courier courier = getAuthenticatedCourier(authentication);
        return deliveryRepository.findAllByCourier(courier).stream().map(LogisticsWorkflowService::toDeliveryDto).toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','COURIER')")
    @GetMapping("/{id}")
    public DeliveryDto getById(@PathVariable Integer id, Authentication authentication) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));
        validateAccess(delivery, authentication);
        return LogisticsWorkflowService.toDeliveryDto(delivery);
    }

    @PreAuthorize("hasAnyRole('ADMIN','COURIER')")
    @PatchMapping("/{deliveryId}/status")
    public DeliveryDto updateStatus(
            @PathVariable Integer deliveryId,
            @Valid @RequestBody UpdateStatusRequestDto request,
            Authentication authentication
    ) {
        return workflowService.updateDeliveryStatus(deliveryId, request, getCurrentUser(authentication));
    }

    private Courier getAuthenticatedCourier(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return courierRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Courier profile not found"));
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User currentUser)) {
            return null;
        }
        return currentUser;
    }

    private void validateAccess(Delivery delivery, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (currentUser.getRole() == User.Role.ADMIN) {
            return;
        }
        Integer deliveryCourierUserId = delivery.getCourier() != null && delivery.getCourier().getUser() != null
                ? delivery.getCourier().getUser().getId()
                : null;
        if (deliveryCourierUserId == null || !deliveryCourierUserId.equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own deliveries");
        }
    }
}
