package com.project.sls.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.sls.entity.Courier;
import com.project.sls.entity.Delivery;
import com.project.sls.entity.Package;
import com.project.sls.entity.User;
import com.project.sls.repository.CourierRepository;
import com.project.sls.repository.DeliveryRepository;
import com.project.sls.repository.PackageRepository;
import com.project.sls.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AutomatedBackupService {

    private static final Logger log = LoggerFactory.getLogger(AutomatedBackupService.class);
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final PackageRepository packageRepository;
    private final DeliveryRepository deliveryRepository;
    private final CourierRepository courierRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final boolean backupEnabled;
    private final String backupDirectory;

    public AutomatedBackupService(
            PackageRepository packageRepository,
            DeliveryRepository deliveryRepository,
            CourierRepository courierRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            @Value("${app.backup.enabled:true}") boolean backupEnabled,
            @Value("${app.backup.directory:./backups}") String backupDirectory
    ) {
        this.packageRepository = packageRepository;
        this.deliveryRepository = deliveryRepository;
        this.courierRepository = courierRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.backupEnabled = backupEnabled;
        this.backupDirectory = backupDirectory;
    }

    @Scheduled(cron = "${app.backup.cron:0 0 */6 * * *}")
    @Transactional(readOnly = true)
    public void createScheduledBackup() {
        if (!backupEnabled) {
            return;
        }

        Path targetDirectory = Path.of(backupDirectory);
        String filename = "sls-backup-" + LocalDateTime.now().format(FILE_TS) + ".json";
        Path targetPath = targetDirectory.resolve(filename);
        Path tempPath = targetDirectory.resolve(filename + ".tmp");

        Map<String, Object> payload = Map.of(
                "generatedAt", toIsoString(LocalDateTime.now()),
                "packages", mapPackages(packageRepository.findAll()),
                "deliveries", mapDeliveries(deliveryRepository.findAll()),
                "couriers", mapCouriers(courierRepository.findAll()),
                "users", mapUsers(userRepository.findAll())
        );

        try {
            Files.createDirectories(targetDirectory);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempPath.toFile(), payload);
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException cleanupEx) {
                log.warn("Backup temp file cleanup failed file={}", tempPath.toAbsolutePath(), cleanupEx);
            }
            log.error("Backup creation failed target={}", targetPath.toAbsolutePath(), ex);
            throw new IllegalStateException("Failed to create backup file at " + targetPath.toAbsolutePath(), ex);
        }

        log.info("Backup created file={} packages={} deliveries={} couriers={} users={}",
                targetPath.toAbsolutePath(),
                ((List<?>) payload.get("packages")).size(),
                ((List<?>) payload.get("deliveries")).size(),
                ((List<?>) payload.get("couriers")).size(),
                ((List<?>) payload.get("users")).size());
    }

    private List<Map<String, Object>> mapPackages(List<Package> packages) {
        return packages.stream().map(pkg -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", pkg.getId());
            row.put("trackingNumber", pkg.getTrackingNumber());
            row.put("status", pkg.getStatus().name());
            row.put("priority", pkg.getPriority().name());
            row.put("city", pkg.getCity());
            row.put("createdAt", toIsoString(pkg.getCreatedAt()));
            row.put("updatedAt", toIsoString(pkg.getUpdatedAt()));
            row.put("courierId", pkg.getCourier() != null ? pkg.getCourier().getId() : null);
            return row;
        }).toList();
    }

    private List<Map<String, Object>> mapDeliveries(List<Delivery> deliveries) {
        return deliveries.stream().map(delivery -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", delivery.getId());
            row.put("packageId", delivery.getPkg() != null ? delivery.getPkg().getId() : null);
            row.put("courierId", delivery.getCourier() != null ? delivery.getCourier().getId() : null);
            row.put("status", delivery.getStatus().name());
            row.put("assignedAt", toIsoString(delivery.getAssignedAt()));
            row.put("deliveredAt", toIsoString(delivery.getDeliveredAt()));
            row.put("deliveryNotes", delivery.getDeliveryNotes());
            return row;
        }).toList();
    }

    private List<Map<String, Object>> mapCouriers(List<Courier> couriers) {
        return couriers.stream().map(courier -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", courier.getId());
            row.put("userId", courier.getUser() != null ? courier.getUser().getId() : null);
            row.put("vehicleType", courier.getVehicleType() != null ? courier.getVehicleType().name() : null);
            row.put("licensePlate", courier.getLicensePlate());
            row.put("phone", courier.getPhone());
            row.put("maxWeightKg", courier.getMaxWeightKg());
            row.put("isActive", courier.getIsActive());
            return row;
        }).toList();
    }

    private List<Map<String, Object>> mapUsers(List<User> users) {
        return users.stream().map(user -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", user.getId());
            row.put("name", user.getName());
            row.put("email", user.getEmail());
            row.put("role", user.getRole().name());
            row.put("createdAt", toIsoString(user.getCreatedAt()));
            return row;
        }).toList();
    }

    private String toIsoString(LocalDateTime value) {
        return value != null ? value.toString() : null;
    }
}
