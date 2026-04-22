package com.project.sls.controller;

import com.project.sls.dto.delivery.DeliveryDto;
import com.project.sls.dto.package_.AssignCourierRequestDto;
import com.project.sls.dto.package_.CreatePackageRequestDto;
import com.project.sls.dto.package_.PackageDto;
import com.project.sls.dto.package_.StatusHistoryDto;
import com.project.sls.entity.Courier;
import com.project.sls.entity.Package;
import com.project.sls.entity.StatusHistory;
import com.project.sls.entity.User;
import com.project.sls.repository.CourierRepository;
import com.project.sls.repository.PackageRepository;
import com.project.sls.repository.StatusHistoryRepository;
import com.project.sls.service.LogisticsWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
public class PackageController {

    private final PackageRepository packageRepository;
    private final CourierRepository courierRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final LogisticsWorkflowService workflowService;

    public PackageController(
            PackageRepository packageRepository,
            CourierRepository courierRepository,
            StatusHistoryRepository statusHistoryRepository,
            LogisticsWorkflowService workflowService
    ) {
        this.packageRepository = packageRepository;
        this.courierRepository = courierRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.workflowService = workflowService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<PackageDto> getAll() {
        return packageRepository.findAll().stream().map(LogisticsWorkflowService::toPackageDto).toList();
    }

    @PreAuthorize("hasRole('COURIER')")
    @GetMapping("/my")
    public List<PackageDto> getMyPackages(Authentication authentication) {
        Courier courier = getAuthenticatedCourier(authentication);
        return packageRepository.findAllByCourier(courier).stream().map(LogisticsWorkflowService::toPackageDto).toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','COURIER')")
    @GetMapping("/{packageId}")
    public PackageDto getById(@PathVariable Integer packageId, Authentication authentication) {
        Package pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        if (!canAccessPackage(authentication, pkg)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own packages");
        }
        return LogisticsWorkflowService.toPackageDto(pkg);
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<PackageDto> getByTrackingNumber(@PathVariable String trackingNumber) {
        return packageRepository.findByTrackingNumber(trackingNumber)
                .map(LogisticsWorkflowService::toPackageDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/tracking/{trackingNumber}/history")
    public List<StatusHistoryDto> getTrackingHistory(@PathVariable String trackingNumber) {
        Package pkg = packageRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        return statusHistoryRepository.findAllByPkgOrderByChangedAtAsc(pkg).stream()
                .map(this::toStatusHistoryDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','COURIER')")
    @GetMapping("/{packageId}/history")
    public List<StatusHistoryDto> getPackageHistory(@PathVariable Integer packageId, Authentication authentication) {
        Package pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        if (!canAccessPackage(authentication, pkg)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own packages");
        }
        return statusHistoryRepository.findAllByPkgOrderByChangedAtAsc(pkg).stream()
                .map(this::toStatusHistoryDto)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PackageDto create(@Valid @RequestBody CreatePackageRequestDto request, Authentication authentication) {
        return workflowService.createPackage(request, getCurrentUser(authentication));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assign")
    public DeliveryDto assignCourier(
            @Valid @RequestBody AssignCourierRequestDto request,
            Authentication authentication
    ) {
        return workflowService.assignCourier(request, getCurrentUser(authentication));
    }

    private StatusHistoryDto toStatusHistoryDto(StatusHistory history) {
        User changedBy = history.getChangedBy();
        return new StatusHistoryDto(
                history.getId(),
                history.getOldStatus(),
                history.getNewStatus(),
                history.getChangedAt(),
                changedBy != null ? changedBy.getId() : null,
                changedBy != null ? changedBy.getName() : null
        );
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

    private boolean canAccessPackage(Authentication authentication, Package pkg) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return false;
        }
        if (currentUser.getRole() == User.Role.ADMIN) {
            return true;
        }
        if (currentUser.getRole() == User.Role.COURIER && pkg.getCourier() != null && pkg.getCourier().getUser() != null) {
            return pkg.getCourier().getUser().getId().equals(currentUser.getId());
        }
        return false;
    }
}
