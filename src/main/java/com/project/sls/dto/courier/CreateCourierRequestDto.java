package com.project.sls.dto.courier;

import com.project.sls.entity.Courier;

import java.math.BigDecimal;

public record CreateCourierRequestDto(
        Integer userId,
        Courier.VehicleType vehicleType,
        String licensePlate,
        String phone,
        BigDecimal maxWeightKg
) {}