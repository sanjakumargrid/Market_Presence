package com.griddynamics.forge.market_presence_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Payload posted to Chennai Team 2's Talent Acquisition Engine (REQ-JP-08).
 *
 * Field names match the ExternalCandidate + Application data model from PES Section 7.
 * Team 2 stores each submission as an ExternalCandidate + Application record,
 * visible in their UI within 30 seconds.
 */
@Schema(description = "Payload sent to Chennai Team 2 when a candidate applies via the careers portal")
public record Team2ApplicationPayload(

        @Schema(description = "Candidate first name") String firstName,
        @Schema(description = "Candidate last name")  String lastName,

        @Schema(description = "Candidate email — unique key in Team 2's ExternalCandidate table")
        String email,

        @Schema(description = "Candidate phone number")
        String phone,

        @Schema(description = "Always CAREERS_PORTAL for applications originating here (REQ-JP-08)")
        String source,

        @Schema(description = "Path or URL of the uploaded resume")
        String resumeUrl,

        @Schema(description = "URL slug of the job the candidate applied for")
        String jobSlug,

        @Schema(description = "Human-readable job title")
        String jobTitle,

        @Schema(description = "Timestamp of application submission")
        Instant appliedAt,

        @Schema(description = "Team 3's internal application intake ID — allows Team 2 to correlate records")
        Long applicationIntakeId
) {}
