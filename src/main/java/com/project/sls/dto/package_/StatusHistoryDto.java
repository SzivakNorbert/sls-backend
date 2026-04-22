package com.project.sls.dto.package_;

import java.time.LocalDateTime;

public record StatusHistoryDto(
        Integer id,
        String oldStatus,
        String newStatus,
        LocalDateTime changedAt,
        Integer changedByUserId,
        String changedByUserName
) {
}
