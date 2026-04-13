package com.project.sls.dto.courier;

import com.project.sls.entity.Courier;

import java.math.BigDecimal;

public record CourierDto(
        Integer id,
        Integer userId,
        String name,
        String email,
        Courier.VehicleType vehicleType,
        String licensePlate,
        String phone,
        BigDecimal maxWeightKg,
        Boolean isActive,
        long activeDeliveries,
        long totalDelivered
) {}