package com.griddynamics.forge.market_presence_service.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalyticEventRequest(
        @NotBlank String eventType,
        String channel
) {}
