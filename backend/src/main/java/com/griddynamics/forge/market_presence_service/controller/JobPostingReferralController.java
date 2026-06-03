package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.JobReferralResponse;
import com.griddynamics.forge.market_presence_service.service.JobReferralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REQ-JP-11 — Generate and list referral links per job posting.
 *
 * Mounted at /api/job-postings/{id} alongside the channel controller.
 * Generates shareable URLs of the form:
 *   http://localhost:5173/careers/{slug}?ref={CODE}
 *
 * Candidates who visit such a URL and apply will have their application
 * tagged with source = REFERRAL.
 */
@RestController
@RequestMapping("/api/job-postings/{id}")
@Tag(name = "Referral Links", description = "Generate and list referral links for job postings (REQ-JP-11)")
public class JobPostingReferralController {

    private final JobReferralService referralService;

    public JobPostingReferralController(JobReferralService referralService) {
        this.referralService = referralService;
    }

    @PostMapping("/referrals")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Generate a shareable referral URL for this job posting (REQ-JP-11)",
            description = "Creates a unique referral code and returns the full shareable URL. " +
                    "Candidates who apply via this URL will be tagged with source=REFERRAL. " +
                    "Body: { referrerId: Long } — referrer's employee ID (optional for demo).")
    public JobReferralResponse generateReferralLink(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Long> body) {
        Long referrerId = (body != null) ? body.get("referrerId") : null;
        return referralService.generateLink(id, referrerId);
    }

    @GetMapping("/referrals")
    @Operation(summary = "List all referrals generated for this job posting")
    public List<JobReferralResponse> listReferrals(@PathVariable Long id) {
        return referralService.listByJob(id);
    }
}
