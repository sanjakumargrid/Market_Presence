package com.griddynamics.forge.market_presence_service.service;

import com.griddynamics.forge.market_presence_service.dto.ApplicationIntakeRequest;
import com.griddynamics.forge.market_presence_service.dto.ApplicationIntakeResponse;
import com.griddynamics.forge.market_presence_service.entity.ApplicationIntake;
import com.griddynamics.forge.market_presence_service.entity.JobPosting;
import com.griddynamics.forge.market_presence_service.entity.JobPostingAnalyticEvent;
import com.griddynamics.forge.market_presence_service.exception.ConflictException;
import com.griddynamics.forge.market_presence_service.exception.ResourceNotFoundException;
import com.griddynamics.forge.market_presence_service.repository.ApplicationIntakeRepository;
import com.griddynamics.forge.market_presence_service.repository.JobPostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ApplicationIntakeService {

    private final ApplicationIntakeRepository intakeRepository;
    private final JobPostingRepository        jobPostingRepository;
    private final FileStorageService          fileStorageService;
    private final EmailService                emailService;
    private final ApplicationHandoffService   handoffService;
    private final JobReferralService          referralService;
    private final AnalyticsService            analyticsService;

    public ApplicationIntakeService(ApplicationIntakeRepository intakeRepository,
                                    JobPostingRepository jobPostingRepository,
                                    FileStorageService fileStorageService,
                                    EmailService emailService,
                                    ApplicationHandoffService handoffService,
                                    JobReferralService referralService,
                                    AnalyticsService analyticsService) {
        this.intakeRepository    = intakeRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.fileStorageService  = fileStorageService;
        this.emailService        = emailService;
        this.handoffService      = handoffService;
        this.referralService     = referralService;
        this.analyticsService    = analyticsService;
    }

    /**
     * Process a candidate application (REQ-JP-07, REQ-JP-08).
     *
     * Sets source = CAREERS_PORTAL on the intake record, then forwards the application
     * to Chennai Team 2 via ApplicationHandoffService after the DB save.
     *
     * @param slug    URL slug of the target job posting
     * @param request JSON metadata part (name, email, phone, coverLetter)
     * @param resume  Optional PDF/DOCX resume file (validated by FileStorageService)
     */
    @Transactional
    public ApplicationIntakeResponse apply(String slug,
                                           ApplicationIntakeRequest request,
                                           MultipartFile resume) {
        JobPosting job = jobPostingRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job posting not found with slug: " + slug));

        if (intakeRepository.existsByJobPostingIdAndCandidateEmail(
                job.getId(), request.candidateEmail())) {
            throw new ConflictException(
                    "You have already applied for this position. " +
                    "Check your application status in My Applications.");
        }

        String resumeUrl = null;
        if (resume != null && !resume.isEmpty()) {
            resumeUrl = fileStorageService.store(resume, slug);
        }

        ApplicationIntake intake = new ApplicationIntake();
        intake.setJobPostingId(job.getId());
        intake.setCandidateName(request.candidateName());
        intake.setCandidateEmail(request.candidateEmail());
        intake.setCandidatePhone(request.candidatePhone());
        intake.setResumeUrl(resumeUrl);
        intake.setCoverLetter(request.coverLetter());
        // REQ-JP-11: source = REFERRAL when a valid referral code is present; else CAREERS_PORTAL
        String source = referralService.resolveSource(request.referralCode(), job.getId());
        intake.setSource(source);

        ApplicationIntake saved = intakeRepository.save(intake);

        job.setApplicationsCount(job.getApplicationsCount() + 1);
        jobPostingRepository.save(job);

        // Confirmation email — never throws (LoggingEmailService)
        emailService.sendApplicationConfirmation(
                request.candidateEmail(), request.candidateName(), job.getTitle());

        // REQ-JP-11: mark referral as APPLIED and update candidate details
        if ("REFERRAL".equals(source)) {
            referralService.markApplied(request.referralCode(), request.candidateName(), request.candidateEmail());
        }

        // REQ-JP-08 handoff to Team 2 — never throws (all errors caught internally)
        handoffService.createAndAttempt(saved, job);

        // REQ-JP-05 analytics — record completion event; never throws
        analyticsService.record(job.getId(),
                JobPostingAnalyticEvent.APPLY_COMPLETE,
                JobPostingAnalyticEvent.CH_CAREERS);

        return toResponse(saved);
    }

    private ApplicationIntakeResponse toResponse(ApplicationIntake intake) {
        return new ApplicationIntakeResponse(
                intake.getId(),
                intake.getJobPostingId(),
                intake.getCandidateName(),
                intake.getCandidateEmail(),
                intake.getCandidatePhone(),
                intake.getResumeUrl(),
                intake.getStatus(),
                intake.getSource(),
                intake.getAppliedAt()
        );
    }
}
