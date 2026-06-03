package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.HandoffStatusResponse;
import com.griddynamics.forge.market_presence_service.dto.PagedResponse;
import com.griddynamics.forge.market_presence_service.service.ApplicationHandoffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-only (no auth in dev) endpoint for inspecting and retrying Team 2 handoffs.
 *
 * Designed for the Week 8 demo:
 *   GET  /api/admin/handoffs?status=PENDING   → see what hasn't reached Team 2 yet
 *   POST /api/admin/handoffs/{id}/retry       → manually trigger the HTTP call
 *
 * REQ-JP-08: verifies that every careers-portal application is forwarded with
 * source=CAREERS_PORTAL within 30 seconds.
 */
@RestController
@RequestMapping("/api/admin/handoffs")
@Tag(name = "Handoff Status", description = "Inspect and retry Team 2 application handoffs (REQ-JP-08)")
public class HandoffStatusController {

    private final ApplicationHandoffService handoffService;

    public HandoffStatusController(ApplicationHandoffService handoffService) {
        this.handoffService = handoffService;
    }

    @GetMapping
    @Operation(
            summary = "List handoff records",
            description = "Returns all handoff records ordered by creation date descending. " +
                    "Filter by ?status=PENDING|SENT|FAILED for targeted queries.")
    public PagedResponse<HandoffStatusResponse> list(
            @Parameter(description = "Filter by status: PENDING, SENT, or FAILED (omit for all)")
            @RequestParam(required = false) String status,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<HandoffStatusResponse> result = (status != null && !status.isBlank())
                ? handoffService.listByStatus(status, pageable)
                : handoffService.listAll(pageable);

        return new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single handoff record by ID")
    public HandoffStatusResponse getById(@PathVariable Long id) {
        return handoffService.getById(id);
    }

    @PostMapping("/{id}/retry")
    @Operation(
            summary = "Retry a PENDING or FAILED handoff",
            description = "Re-attempts the HTTP call to Team 2. " +
                    "Useful during demos when Team 2 comes online after the initial attempt failed.")
    public HandoffStatusResponse retry(@PathVariable Long id) {
        return handoffService.retry(id);
    }

    @PostMapping("/retry-pending")
    @Operation(
            summary = "Retry all PENDING handoffs in one call (REQ-JP-08)",
            description = "Bulk-retries every record with status=PENDING. " +
                    "Useful when Team 2's URL is configured after applications have already been submitted.")
    public Map<String, Integer> retryAllPending() {
        int count = handoffService.retryAllPending();
        return Map.of("retriedCount", count);
    }
}
