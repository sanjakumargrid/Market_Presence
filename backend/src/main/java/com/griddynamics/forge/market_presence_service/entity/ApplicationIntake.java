package com.griddynamics.forge.market_presence_service.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "application_intakes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_intake_posting_email",
                columnNames = {"job_posting_id", "candidate_email"}
        )
)
public class ApplicationIntake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_posting_id", nullable = false)
    private Long jobPostingId;

    @Column(nullable = false)
    private String candidateName;

    @Column(nullable = false)
    private String candidateEmail;

    private String candidatePhone;

    private String resumeUrl;

    /** Application source channel — always CAREERS_PORTAL for new intakes (REQ-JP-08). Nullable for legacy rows. */
    private String source = "CAREERS_PORTAL";

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    private String status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Instant appliedAt;

    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    public void beforeCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.appliedAt == null) {
            this.appliedAt = Instant.now();
        }
        if (this.status == null) {
            this.status = "SUBMITTED";
        }
    }

    @PreUpdate
    public void beforeUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public Long getJobPostingId() {
        return jobPostingId;
    }

    public void setJobPostingId(Long jobPostingId) {
        this.jobPostingId = jobPostingId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;
    }

    public String getCandidatePhone() {
        return candidatePhone;
    }

    public void setCandidatePhone(String candidatePhone) {
        this.candidatePhone = candidatePhone;
    }

    public String getResumeUrl() {
        return resumeUrl;
    }

    public void setResumeUrl(String resumeUrl) {
        this.resumeUrl = resumeUrl;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
