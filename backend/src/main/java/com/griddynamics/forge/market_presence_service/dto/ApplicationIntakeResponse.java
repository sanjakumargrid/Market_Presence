package com.griddynamics.forge.market_presence_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Confirmation returned after a successful application submission")
public record ApplicationIntakeResponse(
        @Schema(description = "Database ID of the application intake record") Long id,
        @Schema(description = "ID of the job posting applied to")              Long jobPostingId,
        @Schema(description = "Full name stored against this application")      String candidateName,
        @Schema(description = "Email address stored against this application")  String candidateEmail,
        @Schema(description = "Phone number stored against this application")   String candidatePhone,
        @Schema(description = "Resume storage path")                           String resumeUrl,
        @Schema(description = "Application status — always SUBMITTED on creation") String status,
        @Schema(description = "Source channel — always CAREERS_PORTAL for public submissions (REQ-JP-08)") String source,
        @Schema(description = "Timestamp of submission")                       Instant appliedAt
) {}
