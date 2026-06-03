package com.griddynamics.forge.market_presence_service.service;

import com.griddynamics.forge.market_presence_service.dto.JobReferralRequest;
import com.griddynamics.forge.market_presence_service.dto.JobReferralResponse;
import com.griddynamics.forge.market_presence_service.entity.JobPosting;
import com.griddynamics.forge.market_presence_service.entity.JobReferral;
import com.griddynamics.forge.market_presence_service.exception.ResourceNotFoundException;
import com.griddynamics.forge.market_presence_service.repository.JobPostingRepository;
import com.griddynamics.forge.market_presence_service.repository.JobReferralRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobReferralService {

    private static final Logger log = LoggerFactory.getLogger(JobReferralService.class);

    private final JobReferralRepository referralRepository;
    private final JobPostingRepository  jobPostingRepository;
    private final String                careersPortalBaseUrl;

    public JobReferralService(
            JobReferralRepository referralRepository,
            JobPostingRepository jobPostingRepository,
            @Value("${app.channels.careers-portal.base-url:http://localhost:5173}") String careersPortalBaseUrl) {
        this.referralRepository  = referralRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.careersPortalBaseUrl = careersPortalBaseUrl;
    }

    // ── Existing endpoint (POST /api/referrals) ───────────────────────────────

    public JobReferralResponse create(JobReferralRequest request) {
        JobPosting job = jobPostingRepository.findById(request.jobPostingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job posting not found with id: " + request.jobPostingId()));

        String code = resolveUniqueCode(request.referralCode());

        JobReferral referral = new JobReferral();
        referral.setJobPostingId(request.jobPostingId());
        referral.setReferrerId(request.referrerId());
        referral.setReferredCandidateName(request.referredCandidateName());
        referral.setReferredCandidateEmail(request.referredCandidateEmail());
        referral.setReferralCode(code);
        referral.setNotes(request.notes());

        return toResponse(referralRepository.save(referral), job.getSlug());
    }

    public JobReferralResponse getByReferralCode(String referralCode) {
        JobReferral referral = referralRepository.findByReferralCode(referralCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Referral not found with code: " + referralCode));
        String slug = jobPostingRepository.findById(referral.getJobPostingId())
                .map(JobPosting::getSlug).orElse(String.valueOf(referral.getJobPostingId()));
        return toResponse(referral, slug);
    }

    // ── New: generate shareable link (POST /api/job-postings/{id}/referrals) ──

    /**
     * REQ-JP-11 — Generate a unique referral URL for a job posting.
     *
     * Creates a referral record without a specific candidate (placeholder values).
     * The candidate name/email are updated when the referral is used to apply.
     *
     * @param jobPostingId the posting to create the referral for
     * @param referrerId   the employee generating the link (null accepted in demo)
     * @return referral with a shareable URL: /careers/{slug}?ref={code}
     */
    public JobReferralResponse generateLink(Long jobPostingId, Long referrerId) {
        JobPosting job = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job posting not found with id: " + jobPostingId));

        String code = resolveUniqueCode(null);

        JobReferral referral = new JobReferral();
        referral.setJobPostingId(jobPostingId);
        referral.setReferrerId(referrerId);
        // Placeholder values — will be populated when the candidate applies
        referral.setReferredCandidateName("PENDING");
        referral.setReferredCandidateEmail("pending@referral.tbd");
        referral.setReferralCode(code);
        referral.setNotes("Generated link — no candidate yet");
        referral.setStatus("PENDING");

        JobReferralResponse response = toResponse(referralRepository.save(referral), job.getSlug());
        log.info("[REFERRAL] Generated link for job {} → {}", jobPostingId, response.referralUrl());
        return response;
    }

    /** List all referrals for a job posting. */
    public List<JobReferralResponse> listByJob(Long jobPostingId) {
        if (!jobPostingRepository.existsById(jobPostingId)) {
            throw new ResourceNotFoundException("Job posting not found with id: " + jobPostingId);
        }
        String slug = jobPostingRepository.findById(jobPostingId)
                .map(JobPosting::getSlug).orElse(String.valueOf(jobPostingId));
        return referralRepository.findByJobPostingId(jobPostingId)
                .stream().map(r -> toResponse(r, slug)).toList();
    }

    // ── Apply-flow helpers ────────────────────────────────────────────────────

    /**
     * REQ-JP-11 — Resolves the application source channel based on referral code.
     *
     * Returns "REFERRAL" if the code is valid and belongs to the given job posting.
     * Returns "CAREERS_PORTAL" if the code is absent, unknown, or for a different job.
     * Never throws — invalid codes are silently ignored so the apply flow is unaffected.
     */
    public String resolveSource(String referralCode, Long jobPostingId) {
        if (!StringUtils.hasText(referralCode)) return "CAREERS_PORTAL";

        Optional<JobReferral> referral = referralRepository.findByReferralCode(referralCode.trim());
        if (referral.isEmpty()) {
            log.warn("[REFERRAL] Unknown referral code '{}' — treating as CAREERS_PORTAL", referralCode);
            return "CAREERS_PORTAL";
        }
        if (!referral.get().getJobPostingId().equals(jobPostingId)) {
            log.warn("[REFERRAL] Code '{}' belongs to job {} but was used for job {} — ignoring",
                    referralCode, referral.get().getJobPostingId(), jobPostingId);
            return "CAREERS_PORTAL";
        }
        return "REFERRAL";
    }

    /**
     * Mark a referral as APPLIED once a candidate has used the link to submit an application.
     * Called by ApplicationIntakeService after a successful save.
     */
    public void markApplied(String referralCode, String candidateName, String candidateEmail) {
        if (!StringUtils.hasText(referralCode)) return;
        referralRepository.findByReferralCode(referralCode.trim()).ifPresent(r -> {
            if ("PENDING".equals(r.getReferredCandidateName())) {
                r.setReferredCandidateName(candidateName);
                r.setReferredCandidateEmail(candidateEmail);
            }
            r.setStatus("APPLIED");
            referralRepository.save(r);
            log.info("[REFERRAL] Marked code '{}' as APPLIED for candidate {}", referralCode, candidateEmail);
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveUniqueCode(String requested) {
        if (StringUtils.hasText(requested)) {
            if (referralRepository.existsByReferralCode(requested)) {
                throw new IllegalArgumentException("Referral code already in use: " + requested);
            }
            return requested;
        }
        String generated;
        do {
            generated = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        } while (referralRepository.existsByReferralCode(generated));
        return generated;
    }

    private String buildReferralUrl(String slug, String code) {
        return careersPortalBaseUrl + "/careers/" + slug + "?ref=" + code;
    }

    private JobReferralResponse toResponse(JobReferral referral, String jobSlug) {
        return new JobReferralResponse(
                referral.getId(),
                referral.getJobPostingId(),
                referral.getReferrerId(),
                referral.getReferredCandidateName(),
                referral.getReferredCandidateEmail(),
                referral.getReferralCode(),
                referral.getStatus(),
                referral.getNotes(),
                referral.getReferredAt(),
                buildReferralUrl(jobSlug, referral.getReferralCode())
        );
    }
}
