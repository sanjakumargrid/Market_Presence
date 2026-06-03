package com.griddynamics.forge.market_presence_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.griddynamics.forge.market_presence_service.dto.ApplicationDto;
import com.griddynamics.forge.market_presence_service.entity.*;
import com.griddynamics.forge.market_presence_service.event.KafkaEvents;
import com.griddynamics.forge.market_presence_service.exception.ConflictException;
import com.griddynamics.forge.market_presence_service.exception.ResourceNotFoundException;
import com.griddynamics.forge.market_presence_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final CandidateApplicationRepository applicationRepository;
    private final ExternalCandidateRepository candidateRepository;
    private final JobPostingRepository jobRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ApplicationDto.ApplicationResponse submitApplication(
            String jobSlug, ApplicationDto.SubmitRequest request,
            MultipartFile resume, MultipartFile coverLetter, MultipartFile certifications,
            MultipartFile transcripts, MultipartFile portfolio, User currentUser) {

        JobPosting job = jobRepository.findBySlug(jobSlug)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobSlug));

        // Find or create external candidate
        ExternalCandidate candidate = candidateRepository.findByEmail(request.getEmail())
            .orElse(ExternalCandidate.builder()
                .email(request.getEmail())
                .createdBy(currentUser != null ? currentUser.getId().getMostSignificantBits() : 0L)
                .build());

        // Update candidate fields
        candidate.setFirstName(request.getFirstName());
        candidate.setMiddleName(request.getMiddleName());
        candidate.setLastName(request.getLastName());
        candidate.setPhone(request.getPhone());
        candidate.setAddress(request.getCurrentLocation());
        candidate.setLinkedinUrl(request.getLinkedinUrl());
        candidate.setGithubUrl(request.getGithubUrl());
        candidate.setPortfolioUrl(request.getPortfolioUrl());
        candidate.setGender(request.getGender());
        candidate.setConsentAccepted(request.isGdprConsent());
        if (request.isGdprConsent() && candidate.getConsentAcceptedAt() == null) {
            candidate.setConsentAcceptedAt(OffsetDateTime.now());
        }

        // Handle cascading collections
        updateExperiences(candidate, request.getExperiences());
        updateEducations(candidate, request.getEducations());
        updateSkills(candidate, request.getSkills());
        updateCertifications(candidate, request.getCertifications());
        updateProjects(candidate, request.getProjects());

        // Add documents
        addDocument(candidate, resume, "RESUME", jobSlug);
        addDocument(candidate, coverLetter, "COVER_LETTER", jobSlug);
        addDocument(candidate, certifications, "CERTIFICATIONS", jobSlug);
        addDocument(candidate, transcripts, "TRANSCRIPTS", jobSlug);
        addDocument(candidate, portfolio, "PORTFOLIO", jobSlug);

        candidate = candidateRepository.save(candidate);

        // Check if already applied
        if (applicationRepository.existsByJobIdAndCandidateId(job.getId(), candidate.getId())) {
            throw new ConflictException("Already applied for this position");
        }

        // Create Application
        CandidateApplication application = CandidateApplication.builder()
            .job(job)
            .candidate(candidate)
            .status(CandidateApplication.ApplicationStatus.APPLIED)
            .source("CAREER_PORTAL")
            .answers(new ArrayList<>())
            .build();

        if (request.getScreeningAnswers() != null) {
            for (Map.Entry<String, String> entry : request.getScreeningAnswers().entrySet()) {
                application.getAnswers().add(ApplicationQuestionAnswer.builder()
                    .application(application)
                    .question(entry.getKey())
                    .answer(entry.getValue())
                    .build());
            }
        }

        application = applicationRepository.save(application);

        // Increment job application count
        job.setApplicationsCount(job.getApplicationsCount() + 1);
        jobRepository.save(job);

        // Outbox Events
        publishOutboxEvents(application, job, candidate);

        return toResponse(application);
    }

    private void updateExperiences(ExternalCandidate candidate, List<ApplicationDto.ExperienceDto> dtos) {
        if (candidate.getExperiences() == null) candidate.setExperiences(new ArrayList<>());
        candidate.getExperiences().clear();
        if (dtos != null) {
            for (var e : dtos) {
                candidate.getExperiences().add(CandidateExperience.builder()
                    .candidate(candidate)
                    .companyName(e.getCompanyName())
                    .designation(e.getDesignation())
                    .startDate(e.getStartDate())
                    .endDate(e.getEndDate())
                    .currentEmployer(e.getCurrentEmployer() != null ? e.getCurrentEmployer() : false)
                    .responsibilities(e.getResponsibilities())
                    .build());
            }
        }
    }

    private void updateEducations(ExternalCandidate candidate, List<ApplicationDto.EducationDto> dtos) {
        if (candidate.getEducations() == null) candidate.setEducations(new ArrayList<>());
        candidate.getEducations().clear();
        if (dtos != null) {
            for (var e : dtos) {
                candidate.getEducations().add(CandidateEducation.builder()
                    .candidate(candidate)
                    .institution(e.getInstitution())
                    .degree(e.getDegree())
                    .specialization(e.getSpecialization())
                    .startYear(e.getStartYear())
                    .endYear(e.getEndYear())
                    .cgpa(e.getCgpa())
                    .build());
            }
        }
    }

    private void updateSkills(ExternalCandidate candidate, List<ApplicationDto.SkillDto> dtos) {
        if (candidate.getSkills() == null) candidate.setSkills(new ArrayList<>());
        candidate.getSkills().clear();
        if (dtos != null) {
            for (var s : dtos) {
                candidate.getSkills().add(CandidateSkillNew.builder()
                    .candidate(candidate)
                    .skillName(s.getSkillName())
                    .proficiency(s.getProficiency())
                    .yearsOfExperience(s.getYearsOfExperience())
                    .build());
            }
        }
    }

    private void updateCertifications(ExternalCandidate candidate, List<ApplicationDto.CertificationDto> dtos) {
        if (candidate.getCertifications() == null) candidate.setCertifications(new ArrayList<>());
        candidate.getCertifications().clear();
        if (dtos != null) {
            for (var c : dtos) {
                candidate.getCertifications().add(CandidateCertification.builder()
                    .candidate(candidate)
                    .certificationName(c.getCertificationName())
                    .issuingOrganization(c.getIssuingOrganization())
                    .issueDate(c.getIssueDate())
                    .expiryDate(c.getExpiryDate())
                    .build());
            }
        }
    }

    private void updateProjects(ExternalCandidate candidate, List<ApplicationDto.ProjectDto> dtos) {
        if (candidate.getProjects() == null) candidate.setProjects(new ArrayList<>());
        candidate.getProjects().clear();
        if (dtos != null) {
            for (var p : dtos) {
                candidate.getProjects().add(CandidateProject.builder()
                    .candidate(candidate)
                    .projectName(p.getProjectName())
                    .description(p.getDescription())
                    .technologiesUsed(p.getTechnologiesUsed())
                    .projectUrl(p.getProjectUrl())
                    .build());
            }
        }
    }

    private void addDocument(ExternalCandidate candidate, MultipartFile file, String docType, String slug) {
        if (file != null && !file.isEmpty()) {
            String path = fileStorageService.store(file, slug);
            candidate.getDocuments().add(CandidateDocument.builder()
                .candidate(candidate)
                .documentType(docType)
                .fileName(file.getOriginalFilename())
                .fileUrl(path)
                .build());
        }
    }

    private void publishOutboxEvents(CandidateApplication application, JobPosting job, ExternalCandidate candidate) {
        try {
            var event = KafkaEvents.ApplicationSubmittedEvent.builder()
                .applicationId(application.getApplicationId())
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .jobSlug(job.getSlug())
                .applicantName(candidate.getFirstName() + " " + candidate.getLastName())
                .applicantEmail(candidate.getEmail())
                .department(job.getDepartment())
                .appliedAt(application.getAppliedDate())
                .build();

            outboxEventRepository.save(OutboxEvent.builder()
                .topic("application-submitted")
                .payload(objectMapper.writeValueAsString(event))
                .status(OutboxEvent.EventStatus.PENDING)
                .build());

            var emailEvent = KafkaEvents.EmailNotificationEvent.builder()
                .recipient(candidate.getEmail())
                .subject("Application Received — " + job.getTitle())
                .type("APPLICATION_CONFIRMATION")
                .payload(application.getApplicationId())
                .createdAt(OffsetDateTime.now())
                .build();

            outboxEventRepository.save(OutboxEvent.builder()
                .topic("email-notification")
                .payload(objectMapper.writeValueAsString(emailEvent))
                .status(OutboxEvent.EventStatus.PENDING)
                .build());
        } catch (Exception e) {
            log.error("Failed to serialize outbox event", e);
        }
    }

    public List<ApplicationDto.ApplicationResponse> getMyApplications(User currentUser) {
        ExternalCandidate candidate = candidateRepository.findByEmail(currentUser.getEmail()).orElse(null);
        if (candidate == null) return List.of();
        return applicationRepository.findByCandidateIdOrderByAppliedDateDesc(candidate.getId())
            .stream().map(this::toResponse).toList();
    }

    private ApplicationDto.ApplicationResponse toResponse(CandidateApplication a) {
        JobPosting job = a.getJob();
        ExternalCandidate c = a.getCandidate();
        return ApplicationDto.ApplicationResponse.builder()
            .id(a.getApplicationId())
            .jobId(job.getId())
            .jobTitle(job.getTitle())
            .jobSlug(job.getSlug())
            .department(job.getDepartment())
            .location(job.getLocationCity() + " · " + job.getWorkMode())
            .applicantName(c.getFirstName() + " " + c.getLastName())
            .applicantEmail(c.getEmail())
            .status(a.getStatus().name())
            .appliedAt(a.getAppliedDate())
            .build();
    }
}
