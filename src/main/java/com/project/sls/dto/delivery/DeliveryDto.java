package com.project.sls.dto.delivery;

import com.project.sls.entity.Delivery;

import java.time.LocalDateTime;

public record DeliveryDto(
        Integer id,
        Integer packageId,
        String trackingNumber,
        String receiverName,
        String address,
        String city,
        Integer courierId,
        String courierName,
        Delivery.Status status,
        String deliveryNotes,
        LocalDateTime assignedAt,
        LocalDateTime deliveredAt
) {}