package com.project.sls.dto.package_;

import com.project.sls.entity.Package;

import java.math.BigDecimal;

public record CreatePackageRequestDto(
        String senderName,
        String receiverName,
        String address,
        String city,
        String postalCode,
        BigDecimal weightKg,
        String dimensions,
        Package.Priority priority,
        String notes
) {}