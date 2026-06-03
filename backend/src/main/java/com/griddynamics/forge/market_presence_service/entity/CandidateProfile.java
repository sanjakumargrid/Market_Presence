package com.griddynamics.forge.market_presence_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "candidate_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name")
    private String fullName;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "professional_title")
    private String professionalTitle;

    @Column(name = "resume_file_name")
    private String resumeFileName;

    @Column(name = "resume_file_path")
    private String resumeFilePath;

    @Column(name = "salary_expectation")
    private String salaryExpectation;

    @Column(name = "work_mode_preference")
    private String workModePreference = "REMOTE";

    @Column(name = "smart_job_alerts")
    private boolean smartJobAlerts = true;

    @Column(name = "app_status_updates")
    private boolean appStatusUpdates = true;

    @Column(name = "employer_messaging")
    private boolean employerMessaging = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "candidate_skills", joinColumns = @JoinColumn(name = "candidate_id"))
    @Column(name = "skill")
    @Builder.Default
    private List<String> skills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "candidate_preferred_locations", joinColumns = @JoinColumn(name = "candidate_id"))
    @Column(name = "location")
    @Builder.Default
    private List<String> preferredLocations = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
