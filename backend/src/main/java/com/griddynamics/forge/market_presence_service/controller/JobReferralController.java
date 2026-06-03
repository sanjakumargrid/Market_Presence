package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.JobReferralRequest;
import com.griddynamics.forge.market_presence_service.dto.JobReferralResponse;
import com.griddynamics.forge.market_presence_service.service.JobReferralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/referrals")
@Tag(name = "Job Referrals", description = "Create and look up job referrals (legacy endpoint — prefer /api/job-postings/{id}/referrals)")
public class JobReferralController {

    private final JobReferralService service;

    public JobReferralController(JobReferralService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a referral for a job posting")
    public JobReferralResponse create(@Valid @RequestBody JobReferralRequest request) {
        return service.create(request);
    }

    @GetMapping("/{referralCode}")
    @Operation(summary = "Look up a referral by its code")
    public JobReferralResponse getByReferralCode(@PathVariable String referralCode) {
        return service.getByReferralCode(referralCode);
    }
}
