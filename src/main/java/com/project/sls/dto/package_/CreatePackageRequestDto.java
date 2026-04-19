package com.project.sls.dto.package_;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreatePackageRequestDto(
        @NotBlank @Size(max = 100)  String senderName,
        @NotBlank @Size(max = 100)  String receiverName,
        @NotBlank @Size(max = 255)  String address,
        @NotBlank @Size(max = 100)  String city,
        @NotBlank @Size(max = 10)   String postalCode,
        @NotNull @DecimalMin("0.01") @DecimalMax("9999.99") BigDecimal weightKg,
        @Size(max = 50)             String dimensions,
        @NotNull com.project.sls.entity.Package.Priority priority,
        @Size(max = 1000)           String notes
) {}