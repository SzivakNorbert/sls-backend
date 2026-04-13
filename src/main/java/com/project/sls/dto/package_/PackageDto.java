package com.project.sls.dto.package_;

import com.project.sls.entity.Package;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PackageDto(
        Integer id,
        String trackingNumber,
        String senderName,
        String receiverName,
        String address,
        String city,
        String postalCode,
        BigDecimal weightKg,
        String dimensions,
        Package.Priority priority,
        Package.Status status,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer courierId,
        String courierName,
        String courierPhone
) {}