package com.griddynamics.forge.market_presence_service.dto;

import jakarta.validation.constraints.NotBlank;

public record StatusUpdateRequest(
        @NotBlank(message = "Status is required")
        String status
) {}
