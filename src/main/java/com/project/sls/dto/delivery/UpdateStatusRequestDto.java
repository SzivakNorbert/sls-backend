package com.project.sls.dto.delivery;

import com.project.sls.entity.Delivery;

public record UpdateStatusRequestDto(
        Delivery.Status newStatus,
        String deliveryNotes
) {}