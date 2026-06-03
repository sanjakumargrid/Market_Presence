package com.griddynamics.forge.market_presence_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * JSON part of the multipart apply request.
 * Sent as @RequestPart("application") with Content-Type: application/json.
 * The resume file is sent as a separate @RequestPart("resume").
 */
@Schema(description = "Application metadata — the JSON part of a multipart/form-data apply request")
public record ApplicationIntakeRequest(

        @NotBlank(message = "Candidate name is required")
        @Schema(description = "Full name of the applicant", example = "Priya Sharma")
        String candidateName,

        @NotBlank(message = "Candidate email is required")
        @Email(message = "Candidate email must be a valid email address")
        @Schema(description = "Email address", example = "priya.sharma@example.com")
        String candidateEmail,

        @Pattern(
                regexp = "^(\\+?[0-9 .\\-()]{7,20})?$",
                message = "Phone number must be 7–20 digits and may contain +, spaces, hyphens, or parentheses"
        )
        @Schema(description = "Contact phone number (REQ-JP-07)", example = "+91 98765 43210")
        String candidatePhone,

        @Schema(description = "Cover letter or plain-text notes about the candidate")
        String coverLetter,

        @Schema(description = "Referral code from a shared referral link (REQ-JP-11). " +
                "When present and valid, sets application source = REFERRAL.",
                example = "REF-FORGE-REACT-001")
        String referralCode
) {}
