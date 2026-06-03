package com.griddynamics.forge.market_presence_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ApplicationDto {

    @Data
    public static class SubmitRequest {
        @JsonProperty("first_name") private String firstName;
        @JsonProperty("middle_name") private String middleName;
        @JsonProperty("last_name") private String lastName;
        private String email;
        private String phone;
        @JsonProperty("current_location") private String currentLocation;
        @JsonProperty("linkedin_url") private String linkedinUrl;
        @JsonProperty("portfolio_url") private String portfolioUrl;
        @JsonProperty("github_url") private String githubUrl;
        
        private List<ExperienceDto> experiences;
        private List<EducationDto> educations;
        private List<SkillDto> skills;
        private List<CertificationDto> certifications;
        private List<ProjectDto> projects;
        
        @JsonProperty("screening_answers")
        private Map<String, String> screeningAnswers;
        
        @JsonProperty("gdpr_consent") private boolean gdprConsent;
        
        private String gender;
        @JsonProperty("veteran_status") private String veteranStatus;
        @JsonProperty("disability_status") private String disabilityStatus;
    }
    
    @Data
    public static class ExperienceDto {
        @JsonProperty("company_name") private String companyName;
        private String designation;
        @JsonProperty("start_date") private LocalDate startDate;
        @JsonProperty("end_date") private LocalDate endDate;
        @JsonProperty("current_employer") private Boolean currentEmployer;
        private String responsibilities;
    }
    
    @Data
    public static class EducationDto {
        private String institution;
        private String degree;
        private String specialization;
        @JsonProperty("start_year") private Integer startYear;
        @JsonProperty("end_year") private Integer endYear;
        private String cgpa;
    }
    
    @Data
    public static class SkillDto {
        @JsonProperty("skill_name") private String skillName;
        private String proficiency;
        @JsonProperty("years_of_experience") private Float yearsOfExperience;
    }

    @Data
    public static class CertificationDto {
        @JsonProperty("certification_name") private String certificationName;
        @JsonProperty("issuing_organization") private String issuingOrganization;
        @JsonProperty("issue_date") private LocalDate issueDate;
        @JsonProperty("expiry_date") private LocalDate expiryDate;
    }

    @Data
    public static class ProjectDto {
        @JsonProperty("project_name") private String projectName;
        private String description;
        @JsonProperty("technologies_used") private String technologiesUsed;
        @JsonProperty("project_url") private String projectUrl;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApplicationResponse {
        private UUID id;
        @JsonProperty("job_id") private Long jobId;
        @JsonProperty("job_title") private String jobTitle;
        @JsonProperty("job_slug") private String jobSlug;
        private String department;
        private String location;
        @JsonProperty("applicant_name") private String applicantName;
        @JsonProperty("applicant_email") private String applicantEmail;
        private String status;
        @JsonProperty("next_step") private String nextStep;
        @JsonProperty("applied_at") private OffsetDateTime appliedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApplicationStats {
        private long total;
        private long applied;
        private long interviewing;
        private long offers;
        private long rejected;
    }
}
