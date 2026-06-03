package com.griddynamics.forge.market_presence_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JobReferralRequest(
        @NotNull(message = "Job posting ID is required")
        Long jobPostingId,

        Long referrerId,

        @NotBlank(message = "Referred candidate name is required")
        String referredCandidateName,

        @NotBlank(message = "Referred candidate email is required")
        @Email(message = "Referred candidate email must be a valid email address")
        String referredCandidateEmail,

        String referralCode,

        String notes
) {}
