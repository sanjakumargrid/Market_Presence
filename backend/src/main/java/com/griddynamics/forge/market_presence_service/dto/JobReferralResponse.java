package com.griddynamics.forge.market_presence_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Referral record with shareable URL (REQ-JP-11)")
public record JobReferralResponse(
        Long id,
        Long jobPostingId,
        Long referrerId,
        String referredCandidateName,
        String referredCandidateEmail,
        String referralCode,
        String status,
        String notes,
        Instant referredAt,

        @Schema(description = "Shareable URL for the referral — candidates who visit this link " +
                "and apply will be tracked with source=REFERRAL",
                example = "http://localhost:5173/careers/react-frontend-engineer-bangalore?ref=ABC123XY")
        String referralUrl
) {}
