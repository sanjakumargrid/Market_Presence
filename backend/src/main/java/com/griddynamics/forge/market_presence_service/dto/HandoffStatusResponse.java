package com.griddynamics.forge.market_presence_service.dto;

import com.griddynamics.forge.market_presence_service.entity.HandoffRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Admin-facing view of a HandoffRecord — shown at GET /api/admin/handoffs.
 */
@Schema(description = "Handoff status record — shows whether a careers-portal application was forwarded to Team 2")
public record HandoffStatusResponse(

        @Schema(description = "Handoff record ID") Long id,
        @Schema(description = "Team 3 application intake ID") Long applicationIntakeId,
        @Schema(description = "Candidate email") String candidateEmail,
        @Schema(description = "Job slug") String jobSlug,
        @Schema(description = "Job title") String jobTitle,
        @Schema(description = "Source — always CAREERS_PORTAL") String source,

        @Schema(description = "PENDING | SENT | FAILED") String status,

        @Schema(description = "Error details if status = FAILED") String errorMessage,
        @Schema(description = "Team 2's assigned application ID (populated when SENT)") String team2ResponseId,

        @Schema(description = "When the HTTP call was last attempted") Instant attemptedAt,
        @Schema(description = "When this record was created") Instant createdAt
) {
    public static HandoffStatusResponse from(HandoffRecord r) {
        return new HandoffStatusResponse(
                r.getId(),
                r.getApplicationIntakeId(),
                r.getCandidateEmail(),
                r.getJobSlug(),
                r.getJobTitle(),
                r.getSource(),
                r.getStatus(),
                r.getErrorMessage(),
                r.getTeam2ResponseId(),
                r.getAttemptedAt(),
                r.getCreatedAt()
        );
    }
}
