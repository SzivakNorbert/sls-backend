package com.project.sls.dto.delivery;

import com.project.sls.entity.Delivery;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequestDto(
        @NotNull Delivery.Status newStatus,
        @Size(max = 1000) String deliveryNotes
) {} 
