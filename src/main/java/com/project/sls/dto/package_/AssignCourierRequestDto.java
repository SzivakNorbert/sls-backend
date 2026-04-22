package com.project.sls.dto.package_;

import jakarta.validation.constraints.NotNull;

public record AssignCourierRequestDto(
        @NotNull Integer packageId,
        @NotNull Integer courierId
) {}
