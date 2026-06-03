package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.AnalyticEventRequest;
import com.griddynamics.forge.market_presence_service.dto.JobPostingAnalyticsResponse;
import com.griddynamics.forge.market_presence_service.repository.JobPostingRepository;
import com.griddynamics.forge.market_presence_service.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Analytics", description = "REQ-JP-05: event ingestion and Team 4 reporting API")
public class JobAnalyticsController {

    private final AnalyticsService      analyticsService;
    private final JobPostingRepository  jobPostingRepository;

    public JobAnalyticsController(AnalyticsService analyticsService,
                                  JobPostingRepository jobPostingRepository) {
        this.analyticsService     = analyticsService;
        this.jobPostingRepository = jobPostingRepository;
    }

    /**
     * Frontend calls this to record CLICK (Apply Now pressed) or APPLY_START (form opened).
     * Always returns 204 — unknown slug is silently ignored so the caller never sees an error.
     */
    @PostMapping("/api/public/jobs/{slug}/events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Record a candidate interaction event for a job posting")
    public void recordEvent(@PathVariable String slug,
                            @RequestBody @Valid AnalyticEventRequest request) {
        jobPostingRepository.findBySlug(slug).ifPresent(job ->
                analyticsService.record(
                        job.getId(),
                        request.eventType(),
                        request.channel() != null ? request.channel() : "CAREERS_PORTAL"
                )
        );
    }

    /** Team 4 analytics API — aggregated counts for all job postings. */
    @GetMapping("/api/analytics/job-postings")
    @Operation(summary = "Get analytics for all job postings (Team 4 API)")
    public List<JobPostingAnalyticsResponse> getAllAnalytics() {
        return analyticsService.getAllAnalytics();
    }

    /** Team 4 analytics API — aggregated counts for one job posting. */
    @GetMapping("/api/analytics/job-postings/{id}")
    @Operation(summary = "Get analytics for a single job posting (Team 4 API)")
    public JobPostingAnalyticsResponse getAnalytics(@PathVariable Long id) {
        return analyticsService.getAnalytics(id);
    }
}
