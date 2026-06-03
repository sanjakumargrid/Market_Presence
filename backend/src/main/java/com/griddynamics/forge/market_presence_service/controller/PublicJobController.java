package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.PublicJobResponse;
import com.griddynamics.forge.market_presence_service.entity.JobPostingAnalyticEvent;
import com.griddynamics.forge.market_presence_service.service.AnalyticsService;
import com.griddynamics.forge.market_presence_service.service.JobPostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/jobs")
@Tag(name = "Public Jobs", description = "Public candidate-facing endpoints for browsing job postings")
public class PublicJobController {

    private final JobPostingService service;
    private final AnalyticsService  analyticsService;

    public PublicJobController(JobPostingService service, AnalyticsService analyticsService) {
        this.service          = service;
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @Operation(summary = "List all published job postings")
    public List<PublicJobResponse> getPublishedJobs() {
        return service.getPublishedJobsPublic();
    }

    /**
     * REQ-JP-05: records a VIEW event for the given channel on every detail page load.
     * ?channel defaults to CAREERS_PORTAL; LinkedIn/Indeed can append ?channel=LINKEDIN etc.
     */
    @GetMapping("/{slug}")
    @Operation(summary = "Get a published job posting by slug")
    public PublicJobResponse getBySlug(
            @PathVariable String slug,
            @RequestParam(name = "channel", defaultValue = "CAREERS_PORTAL") String channel) {
        PublicJobResponse response = service.getPublicJobBySlug(slug);
        analyticsService.record(response.id(), JobPostingAnalyticEvent.VIEW, channel);
        return response;
    }
}
