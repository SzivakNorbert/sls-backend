package com.project.sls.service;

import com.project.sls.dto.delivery.DeliveryDto;
import com.project.sls.dto.delivery.UpdateStatusRequestDto;
import com.project.sls.dto.package_.AssignCourierRequestDto;
import com.project.sls.dto.package_.CreatePackageRequestDto;
import com.project.sls.dto.package_.PackageDto;
import com.project.sls.entity.Courier;
import com.project.sls.entity.Delivery;
import com.project.sls.entity.Package;
import com.project.sls.entity.StatusHistory;
import com.project.sls.entity.User;
import com.project.sls.repository.CourierRepository;
import com.project.sls.repository.DeliveryRepository;
import com.project.sls.repository.PackageRepository;
import com.project.sls.repository.StatusHistoryRepository;
import com.project.sls.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class LogisticsWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(LogisticsWorkflowService.class);

    private final PackageRepository packageRepository;
    private final CourierRepository courierRepository;
    private final DeliveryRepository deliveryRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;

    public LogisticsWorkflowService(
            PackageRepository packageRepository,
            CourierRepository courierRepository,
            DeliveryRepository deliveryRepository,
            StatusHistoryRepository statusHistoryRepository,
            UserRepository userRepository
    ) {
        this.packageRepository = packageRepository;
        this.courierRepository = courierRepository;
        this.deliveryRepository = deliveryRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PackageDto createPackage(CreatePackageRequestDto request, User actor) {
        User requiredActor = requireActor(actor);
        Package pkg = Package.builder()
                .trackingNumber(generateTrackingNumber())
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

        Package saved = packageRepository.save(pkg);
        saveStatusHistory(saved, null, saved.getStatus(), requiredActor);

        log.info("Package created id={} tracking={} actorUserId={}",
                saved.getId(), saved.getTrackingNumber(), requiredActor.getId());
        return toPackageDto(saved);
    }

    @Transactional
    public DeliveryDto assignCourier(AssignCourierRequestDto request, User actor) {
        User requiredActor = requireActor(actor);
        if (request.packageId() == null || request.courierId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "packageId and courierId are required");
        }

        Package pkg = packageRepository.findById(request.packageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));

        if (pkg.getStatus() == Package.Status.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delivered package cannot be reassigned");
        }

        Courier courier = courierRepository.findById(request.courierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Courier not found"));

        Optional<Delivery> existingDelivery = deliveryRepository.findByPkg(pkg);
        if (existingDelivery.isPresent() && existingDelivery.get().getStatus() == Delivery.Status.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delivered package cannot receive new delivery");
        }

        Package.Status oldPackageStatus = pkg.getStatus();
        pkg.setCourier(courier);
        pkg.setStatus(Package.Status.ASSIGNED);
        packageRepository.save(pkg);

        Delivery delivery = existingDelivery.orElseGet(Delivery::new);
        delivery.setPkg(pkg);
        delivery.setCourier(courier);
        delivery.setStatus(Delivery.Status.ASSIGNED);
        delivery.setAssignedAt(LocalDateTime.now());
        delivery.setDeliveredAt(null);
        delivery.setDeliveryNotes(null);

        Delivery savedDelivery = deliveryRepository.save(delivery);
        saveStatusHistory(pkg, oldPackageStatus, pkg.getStatus(), requiredActor);

        log.info("Courier assigned packageId={} courierId={} deliveryId={} actorUserId={}",
                pkg.getId(), courier.getId(), savedDelivery.getId(), requiredActor.getId());
        return toDeliveryDto(savedDelivery);
    }

    @Transactional
    public DeliveryDto updateDeliveryStatus(Integer deliveryId, UpdateStatusRequestDto request, User actor) {
        User requiredActor = requireActor(actor);
        if (request.newStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "newStatus is required");
        }

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));

        validateActorCanUpdate(delivery, requiredActor);
        validateTransition(delivery.getStatus(), request.newStatus());
        validateStatusNotes(request);

        delivery.setStatus(request.newStatus());
        delivery.setDeliveryNotes(request.deliveryNotes());
        if (request.newStatus() == Delivery.Status.DELIVERED) {
            delivery.setDeliveredAt(LocalDateTime.now());
        }

        Package pkg = delivery.getPkg();
        Package.Status oldPackageStatus = pkg != null ? pkg.getStatus() : null;
        if (pkg != null) {
            Package.Status newPackageStatus = mapDeliveryStatusToPackageStatus(request.newStatus());
            if (newPackageStatus != null) {
                pkg.setStatus(newPackageStatus);
                packageRepository.save(pkg);
            }
            if (oldPackageStatus != pkg.getStatus()) {
                saveStatusHistory(pkg, oldPackageStatus, pkg.getStatus(), requiredActor);
            }
        }

        Delivery saved = deliveryRepository.save(delivery);
        log.info("Delivery status updated deliveryId={} newStatus={} actorUserId={}",
                saved.getId(), saved.getStatus(), requiredActor.getId());
        return toDeliveryDto(saved);
    }

    private void validateActorCanUpdate(Delivery delivery, User actor) {
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (actor.getRole() != User.Role.COURIER) {
            return;
        }
        Integer deliveryCourierUserId = delivery.getCourier() != null && delivery.getCourier().getUser() != null
                ? delivery.getCourier().getUser().getId()
                : null;
        if (deliveryCourierUserId == null || !deliveryCourierUserId.equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own deliveries");
        }
    }

    private void validateTransition(Delivery.Status current, Delivery.Status next) {
        boolean validTransition = switch (current) {
            case ASSIGNED -> next == Delivery.Status.IN_TRANSIT;
            case IN_TRANSIT -> next == Delivery.Status.DELIVERED || next == Delivery.Status.FAILED;
            case DELIVERED, FAILED -> false;
        };
        if (!validTransition) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status transition: " + current + " -> " + next);
        }
    }

    private void validateStatusNotes(UpdateStatusRequestDto request) {
        if (request.newStatus() == Delivery.Status.FAILED
                && (request.deliveryNotes() == null || request.deliveryNotes().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deliveryNotes is required for FAILED");
        }
    }

    private Package.Status mapDeliveryStatusToPackageStatus(Delivery.Status deliveryStatus) {
        return switch (deliveryStatus) {
            case ASSIGNED -> Package.Status.ASSIGNED;
            case IN_TRANSIT -> Package.Status.IN_TRANSIT;
            case DELIVERED -> Package.Status.DELIVERED;
            case FAILED -> Package.Status.FAILED;
        };
    }

    private String generateTrackingNumber() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "PKG-" + token;
    }

    private void saveStatusHistory(Package pkg, Package.Status oldStatus, Package.Status newStatus, User actor) {
        StatusHistory history = StatusHistory.builder()
                .pkg(pkg)
                .oldStatus(oldStatus != null ? oldStatus.name() : null)
                .newStatus(newStatus.name())
                .changedBy(resolveActor(actor))
                .build();
        statusHistoryRepository.save(history);
    }

    private User resolveActor(User actor) {
        if (actor == null || actor.getId() == null) {
            return null;
        }
        return userRepository.findById(actor.getId()).orElse(null);
    }

    private User requireActor(User actor) {
        if (actor == null || actor.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return actor;
    }

    public static PackageDto toPackageDto(Package p) {
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

    public static DeliveryDto toDeliveryDto(Delivery d) {
        Package p = d.getPkg();
        Courier c = d.getCourier();

        Integer packageId = p != null ? p.getId() : null;
        String trackingNumber = p != null ? p.getTrackingNumber() : null;
        String receiverName = p != null ? p.getReceiverName() : null;
        String address = p != null ? p.getAddress() : null;
        String city = p != null ? p.getCity() : null;

        Integer courierId = c != null ? c.getId() : null;
        String courierName = c != null && c.getUser() != null ? c.getUser().getName() : null;

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
