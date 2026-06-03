package com.griddynamics.forge.market_presence_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "candidate_applications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "application_id")
    private UUID applicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobPosting job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private ExternalCandidate candidate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    private String source;

    @Column(name = "applied_date", updatable = false)
    @Builder.Default
    private OffsetDateTime appliedDate = OffsetDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApplicationQuestionAnswer> answers = new ArrayList<>();

    public enum ApplicationStatus {
        APPLIED, UNDER_REVIEW, TECHNICAL_INTERVIEW, HR_INTERVIEW, OFFER, REJECTED, WITHDRAWN
    }
}
