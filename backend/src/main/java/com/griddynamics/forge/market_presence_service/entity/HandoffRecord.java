package com.griddynamics.forge.market_presence_service.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Tracks every attempt to forward an application to Chennai Team 2 (REQ-JP-08).
 *
 * One record is created per application intake with status = PENDING immediately
 * after the intake is saved.  The status then transitions to SENT (HTTP call
 * succeeded) or FAILED (HTTP call threw).  If Team 2's URL is not configured the
 * record stays PENDING indefinitely and can be retried via the admin endpoint.
 */
@Entity
@Table(name = "application_handoffs")
public class HandoffRecord {

    /** PENDING → the handoff has not been attempted yet (or Team 2 URL is not set). */
    public static final String PENDING = "PENDING";

    /** SENT → the payload was accepted by Team 2 (HTTP 2xx received). */
    public static final String SENT    = "SENT";

    /** FAILED → the HTTP call to Team 2 threw or returned a non-2xx status. */
    public static final String FAILED  = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationIntakeId;

    private String candidateEmail;
    private String candidatePhone;
    private String jobSlug;
    private String jobTitle;

    @Column(nullable = false)
    private String source = "CAREERS_PORTAL";

    @Column(nullable = false)
    private String status = PENDING;

    /** Populated when status = FAILED. */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /** The application ID returned by Team 2 after a successful SENT. */
    private String team2ResponseId;

    private Instant attemptedAt;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void beforeCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void beforeUpdate() {
        this.updatedAt = Instant.now();
    }

    // ── Getters and setters ──────────────────────────────────────────────────

    public Long getId() { return id; }

    public Long getApplicationIntakeId() { return applicationIntakeId; }
    public void setApplicationIntakeId(Long v) { this.applicationIntakeId = v; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String v) { this.candidateEmail = v; }

    public String getCandidatePhone() { return candidatePhone; }
    public void setCandidatePhone(String v) { this.candidatePhone = v; }

    public String getJobSlug() { return jobSlug; }
    public void setJobSlug(String v) { this.jobSlug = v; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String v) { this.jobTitle = v; }

    public String getSource() { return source; }
    public void setSource(String v) { this.source = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }

    public String getTeam2ResponseId() { return team2ResponseId; }
    public void setTeam2ResponseId(String v) { this.team2ResponseId = v; }

    public Instant getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(Instant v) { this.attemptedAt = v; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
