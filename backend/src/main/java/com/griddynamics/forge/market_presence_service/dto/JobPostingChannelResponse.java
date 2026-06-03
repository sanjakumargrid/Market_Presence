package com.griddynamics.forge.market_presence_service.dto;

import com.griddynamics.forge.market_presence_service.entity.JobPostingChannel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Channel status for a job posting (REQ-JP-03)")
public record JobPostingChannelResponse(
        @Schema(description = "Record ID") Long id,
        @Schema(description = "Job posting ID") Long jobPostingId,
        @Schema(description = "CAREERS_PORTAL | LINKEDIN | INDEED") String channelName,
        @Schema(description = "Public URL of the posting on this channel") String channelUrl,

        @Schema(description = "DRAFT | PENDING | LIVE | FAILED | UNPUBLISHED") String status,

        @Schema(description = "Recruiter-actionable message — populated on PENDING or FAILED")
        String errorMessage,

        @Schema(description = "When status was first set to LIVE") Instant publishedAt,
        @Schema(description = "When status was set to UNPUBLISHED") Instant unpublishedAt,
        @Schema(description = "Last status change timestamp") Instant lastUpdatedAt,
        @Schema(description = "Channel expiry date") Instant expiresAt
) {
    public static JobPostingChannelResponse from(JobPostingChannel ch) {
        return new JobPostingChannelResponse(
                ch.getId(),
                ch.getJobPostingId(),
                ch.getChannelName(),
                ch.getChannelUrl(),
                ch.getStatus(),
                ch.getErrorMessage(),
                ch.getPublishedAt(),
                ch.getUnpublishedAt(),
                ch.getUpdatedAt(),
                ch.getExpiresAt()
        );
    }
}
