package com.griddynamics.forge.market_presence_service.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp,
        List<String> details
) {
    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, Instant.now(), null);
    }

    public ErrorResponse(int status, String error, String message, List<String> details) {
        this(status, error, message, Instant.now(), details);
    }
}
