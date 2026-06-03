package com.griddynamics.forge.market_presence_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "candidate_projects")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private ExternalCandidate candidate;

    @Column(name = "project_name")
    private String projectName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "technologies_used")
    private String technologiesUsed;

    @Column(name = "project_url")
    private String projectUrl;
}
