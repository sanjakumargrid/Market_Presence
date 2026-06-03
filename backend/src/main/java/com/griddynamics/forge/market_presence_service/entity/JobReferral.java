package com.griddynamics.forge.market_presence_service.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job_referrals")
public class JobReferral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long jobPostingId;

    private Long referrerId;

    @Column(nullable = false)
    private String referredCandidateName;

    @Column(nullable = false)
    private String referredCandidateEmail;

    @Column(unique = true, nullable = false)
    private String referralCode;

    private String status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Instant referredAt;

    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    public void beforeCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.referredAt == null) {
            this.referredAt = Instant.now();
        }
        if (this.status == null) {
            this.status = "PENDING";
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

    public Long getReferrerId() {
        return referrerId;
    }

    public void setReferrerId(Long referrerId) {
        this.referrerId = referrerId;
    }

    public String getReferredCandidateName() {
        return referredCandidateName;
    }

    public void setReferredCandidateName(String referredCandidateName) {
        this.referredCandidateName = referredCandidateName;
    }

    public String getReferredCandidateEmail() {
        return referredCandidateEmail;
    }

    public void setReferredCandidateEmail(String referredCandidateEmail) {
        this.referredCandidateEmail = referredCandidateEmail;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
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

    public Instant getReferredAt() {
        return referredAt;
    }

    public void setReferredAt(Instant referredAt) {
        this.referredAt = referredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
