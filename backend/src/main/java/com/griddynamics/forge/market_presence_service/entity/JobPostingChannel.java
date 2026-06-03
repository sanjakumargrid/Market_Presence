package com.griddynamics.forge.market_presence_service.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "job_posting_channels",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_channel_posting_name",
                columnNames = {"job_posting_id", "channel_name"}
        )
)
public class JobPostingChannel {

    // ── Status constants (REQ-JP-03) ─────────────────────────────────────────
    public static final String DRAFT       = "DRAFT";
    /** Publish has been requested but not yet confirmed (e.g. LinkedIn approval pending). */
    public static final String PENDING     = "PENDING";
    /** The posting is live and visible on the channel. */
    public static final String LIVE        = "LIVE";
    /** The publish attempt failed — errorMessage contains details. */
    public static final String FAILED      = "FAILED";
    /** The posting was explicitly unpublished or expired. */
    public static final String UNPUBLISHED = "UNPUBLISHED";

    // ── Channel name constants ────────────────────────────────────────────────
    public static final String CAREERS_PORTAL = "CAREERS_PORTAL";
    public static final String LINKEDIN       = "LINKEDIN";
    public static final String INDEED         = "INDEED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_posting_id", nullable = false)
    private Long jobPostingId;

    @Column(name = "channel_name", nullable = false)
    private String channelName;

    private String channelUrl;

    private String status;

    /** Actionable message for the recruiter — set when status = PENDING or FAILED. */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /** When the channel record was first published / set to LIVE. */
    @Column(name = "posted_at")
    private Instant publishedAt;

    /** When the channel record was set to UNPUBLISHED. */
    private Instant unpublishedAt;

    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void beforeCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = DRAFT;
        }
    }

    @PreUpdate
    public void beforeUpdate() {
        this.updatedAt = Instant.now();
    }

    // ── Getters and setters ──────────────────────────────────────────────────

    public Long getId() { return id; }

    public Long getJobPostingId() { return jobPostingId; }
    public void setJobPostingId(Long v) { this.jobPostingId = v; }

    public String getChannelName() { return channelName; }
    public void setChannelName(String v) { this.channelName = v; }

    public String getChannelUrl() { return channelUrl; }
    public void setChannelUrl(String v) { this.channelUrl = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant v) { this.publishedAt = v; }

    public Instant getUnpublishedAt() { return unpublishedAt; }
    public void setUnpublishedAt(Instant v) { this.unpublishedAt = v; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant v) { this.expiresAt = v; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
