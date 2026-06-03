package com.griddynamics.forge.market_presence_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "candidate_experience")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private ExternalCandidate candidate;

    @Column(name = "company_name")
    private String companyName;

    private String designation;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "current_employer")
    private boolean currentEmployer;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;
}
