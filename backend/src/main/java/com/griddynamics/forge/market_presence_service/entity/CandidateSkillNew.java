package com.griddynamics.forge.market_presence_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "candidate_skills_new")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateSkillNew {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private ExternalCandidate candidate;

    @Column(name = "skill_name")
    private String skillName;

    private String proficiency;

    @Column(name = "years_of_experience")
    private Float yearsOfExperience;
}
