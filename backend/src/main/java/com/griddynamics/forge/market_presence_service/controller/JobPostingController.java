package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.*;
import com.griddynamics.forge.market_presence_service.service.JobPostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/job-postings")
@Tag(name = "Job Postings", description = "Manage job postings (REQ-JP-01, REQ-JP-02)")
public class JobPostingController {

    private final JobPostingService service;

    public JobPostingController(JobPostingService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new job posting (manual entry — REQ-JP-02)")
    public JobPostingResponse create(@Valid @RequestBody JobPostingRequest request) {
        return service.create(request);
    }

    /**
     * REQ-JP-01 — Pre-fill a DRAFT posting from a Demand snapshot.
     *
     * Accepts a DemandSnapshot matching the shape that Chennai Team 1 will publish
     * as a Kafka event when a Demand reaches OPEN_EXTERNAL status. This HTTP
     * endpoint is the local fallback: call it directly in dev/demo, or replace
     * the method body with a @KafkaListener when Team 1's Kafka cluster is live.
     */
    @PostMapping("/from-demand")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Pre-fill a DRAFT posting from demand data (REQ-JP-01)",
            description = "Creates a DRAFT JobPosting from a DemandSnapshot. " +
                    "In production this will be triggered by a Kafka event from Chennai Team 1 " +
                    "when the Demand transitions to OPEN_EXTERNAL. " +
                    "Call this endpoint directly to test the flow without Team 1 running.")
    public JobPostingResponse createFromDemand(@Valid @RequestBody DemandSnapshot snapshot) {
        return service.createFromDemand(snapshot);
    }

    @GetMapping
    @Operation(summary = "List job postings with optional filters and pagination")
    public PagedResponse<JobPostingResponse> getAll(
            @Parameter(description = "Filter by status (DRAFT, PUBLISHED, CLOSED)")
            @RequestParam(required = false) String status,

            @Parameter(description = "Filter by location (partial match)")
            @RequestParam(required = false) String location,

            @Parameter(description = "Filter by seniority")
            @RequestParam(required = false) String seniority,

            @Parameter(description = "Filter by title (partial match)")
            @RequestParam(required = false) String title,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return service.getAll(status, location, seniority, title, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a full job posting by ID (all REQ-JP-02 fields)")
    public JobPostingResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get a full job posting by slug")
    public JobPostingResponse getBySlug(@PathVariable String slug) {
        return service.getBySlug(slug);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace all fields of a job posting (REQ-JP-02)")
    public JobPostingResponse update(@PathVariable Long id,
                                     @Valid @RequestBody JobPostingUpdateRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change posting status: DRAFT → PUBLISHED → CLOSED")
    public JobPostingResponse updateStatus(@PathVariable Long id,
                                           @Valid @RequestBody StatusUpdateRequest request) {
        return service.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a job posting")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
