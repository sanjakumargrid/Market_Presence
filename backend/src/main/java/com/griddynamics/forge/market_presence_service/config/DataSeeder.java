package com.griddynamics.forge.market_presence_service.config;

import com.griddynamics.forge.market_presence_service.entity.ApplicationIntake;
import com.griddynamics.forge.market_presence_service.entity.JobPosting;
import com.griddynamics.forge.market_presence_service.entity.JobPostingChannel;
import com.griddynamics.forge.market_presence_service.entity.JobReferral;
import com.griddynamics.forge.market_presence_service.repository.ApplicationIntakeRepository;
import com.griddynamics.forge.market_presence_service.repository.JobPostingChannelRepository;
import com.griddynamics.forge.market_presence_service.repository.JobPostingRepository;
import com.griddynamics.forge.market_presence_service.repository.JobReferralRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class DataSeeder implements CommandLineRunner {

    private final JobPostingRepository jobPostingRepo;
    private final ApplicationIntakeRepository intakeRepo;
    private final JobPostingChannelRepository channelRepo;
    private final JobReferralRepository referralRepo;

    public DataSeeder(JobPostingRepository jobPostingRepo,
                      ApplicationIntakeRepository intakeRepo,
                      JobPostingChannelRepository channelRepo,
                      JobReferralRepository referralRepo) {
        this.jobPostingRepo = jobPostingRepo;
        this.intakeRepo = intakeRepo;
        this.channelRepo = channelRepo;
        this.referralRepo = referralRepo;
    }

    @Override
    public void run(String... args) {
        patchLegacyJobs();
        patchLegacyChannelNames();
        List<JobPosting> seeded = seedJobPostings();
        if (!seeded.isEmpty()) {
            seedApplicationIntakes(seeded);
            seedChannels(seeded);
            seedReferrals(seeded);
        }
        seedCareersPortalChannels();
    }

    // ── Patch pre-Phase-2 rows that have null new-column values ───────────────

    private void patchLegacyJobs() {
        jobPostingRepo.findAll().stream()
            .filter(j -> j.getDepartment() == null || j.getWorkMode() == null)
            .forEach(j -> {
                if (j.getDepartment() == null)     j.setDepartment("Engineering");
                if (j.getJobCategory() == null)    j.setJobCategory("Backend Development");
                if (j.getWorkMode() == null)       j.setWorkMode("HYBRID");
                if (j.getEmploymentType() == null) j.setEmploymentType("FULL_TIME");
                if (j.getLocationCity() == null)   j.setLocationCity("Bangalore");
                if (j.getLocationState() == null)  j.setLocationState("KA");
                if (j.getLocationCountry() == null) j.setLocationCountry("IN");
                if (j.getSalaryMin() == null)      j.setSalaryMin(1800000);
                if (j.getSalaryMax() == null)      j.setSalaryMax(2600000);
                if (j.getCurrency() == null)       j.setCurrency("INR");
                if (j.getShowSalary() == null)     j.setShowSalary(true);
                if (j.getPublishedAt() == null && "PUBLISHED".equals(j.getStatus())) {
                    j.setPublishedAt(Instant.now().minus(30, ChronoUnit.DAYS));
                }
                jobPostingRepo.save(j);
            });
    }

    /**
     * Normalise legacy mixed-case channel names ("LinkedIn" → "LINKEDIN") added before Phase 5.
     * Safe to run repeatedly — only modifies rows whose channelName is not already canonical.
     */
    private void patchLegacyChannelNames() {
        java.util.Map<String, String> renames = java.util.Map.of(
                "LinkedIn", "LINKEDIN",
                "Indeed",   "INDEED",
                "Naukri",   "NAUKRI"
        );
        channelRepo.findAll().stream()
                .filter(ch -> renames.containsKey(ch.getChannelName()))
                .forEach(ch -> {
                    String canonical = renames.get(ch.getChannelName());
                    // Only rename if no canonical entry already exists for this posting
                    if (channelRepo.findByJobPostingIdAndChannelName(ch.getJobPostingId(), canonical).isEmpty()) {
                        ch.setChannelName(canonical);
                        // Normalise status to LIVE if it was ACTIVE or PUBLISHED
                        if ("ACTIVE".equals(ch.getStatus()) || "PUBLISHED".equals(ch.getStatus())) {
                            ch.setStatus(com.griddynamics.forge.market_presence_service.entity.JobPostingChannel.LIVE);
                        }
                        channelRepo.save(ch);
                    }
                });
    }

    /**
     * REQ-JP-04 — Ensure every PUBLISHED job has a CAREERS_PORTAL channel record with status LIVE.
     * Idempotent: skips jobs that already have the channel.
     */
    private void seedCareersPortalChannels() {
        jobPostingRepo.findAll().stream()
                .filter(j -> "PUBLISHED".equals(j.getStatus()))
                .forEach(j -> {
                    String canonical = com.griddynamics.forge.market_presence_service.entity.JobPostingChannel.CAREERS_PORTAL;
                    if (channelRepo.findByJobPostingIdAndChannelName(j.getId(), canonical).isEmpty()) {
                        com.griddynamics.forge.market_presence_service.entity.JobPostingChannel ch =
                                new com.griddynamics.forge.market_presence_service.entity.JobPostingChannel();
                        ch.setJobPostingId(j.getId());
                        ch.setChannelName(canonical);
                        ch.setChannelUrl("http://localhost:5173/jobs/" + j.getSlug());
                        ch.setStatus(com.griddynamics.forge.market_presence_service.entity.JobPostingChannel.LIVE);
                        ch.setPublishedAt(j.getPublishedAt() != null ? j.getPublishedAt() : java.time.Instant.now());
                        channelRepo.save(ch);
                    }
                });
    }

    // ── Job Postings ──────────────────────────────────────────────────────────

    private List<JobPosting> seedJobPostings() {
        List<JobPosting> saved = new ArrayList<>();

        saved.add(saveJob(job(
            "react-frontend-engineer-bangalore",
            "React Frontend Engineer",
            "PUBLISHED",
            "We are seeking a Senior React Frontend Engineer to join our platform team in Bangalore. You will be responsible for building high-performance, accessible web UIs that serve millions of users. You will work closely with product designers and backend engineers to deliver seamless digital experiences.",
            "React 18+, TypeScript, Redux Toolkit, React Query, Jest, Cypress, REST APIs, GraphQL",
            "Design and implement complex React components and micro-frontends\nLead frontend architecture decisions and code reviews\nOptimise bundle size, rendering performance, and accessibility\nCollaborate with UX designers on design-system tokens\nMentor junior engineers on best practices",
            "Competitive salary (₹18–25 LPA)\nRemote-friendly hybrid work model\nAnnual performance bonus\nHealth insurance for employee and family\n₹50,000 annual learning & conference budget",
            101L, "SENIOR", "Engineering", "Frontend Development",
            "FULL_TIME", "HYBRID",
            "Bangalore", "KA", "IN", "Bangalore, KA",
            1800000, 2500000, "INR", true,
            LocalDate.of(2026, 12, 31), 7
        )));

        saved.add(saveJob(job(
            "devops-engineer-hyderabad",
            "DevOps Engineer",
            "PUBLISHED",
            "Join our Infrastructure team as a DevOps Engineer and help us build and maintain the CI/CD pipelines, cloud infrastructure, and observability stack that powers our AI-driven workforce platform. You will drive automation and reliability improvements across our entire delivery pipeline.",
            "Kubernetes, Helm, Terraform, AWS/GCP, Docker, Jenkins, GitHub Actions, Prometheus, Grafana, ELK Stack",
            "Design and maintain Kubernetes clusters on AWS EKS\nBuild and optimise CI/CD pipelines using GitHub Actions and Jenkins\nProvision infrastructure-as-code using Terraform\nImplement monitoring, alerting, and incident-response runbooks\nCoordinate with developers on deployment strategy and SLOs",
            "Competitive salary (₹15–20 LPA)\nHybrid work model (3 days office)\nHealth and wellness benefits\nCertification sponsorship (AWS, GCP, CKA)\nAnnual team offsite",
            102L, "MID", "Infrastructure", "DevOps & Cloud",
            "FULL_TIME", "HYBRID",
            "Hyderabad", "TS", "IN", "Hyderabad, TS",
            1500000, 2000000, "INR", true,
            LocalDate.of(2026, 11, 30), 14
        )));

        saved.add(saveJob(job(
            "qa-automation-engineer-pune",
            "QA Automation Engineer",
            "PUBLISHED",
            "We are looking for a QA Automation Engineer to join our Quality team in Pune. In this role you will design and maintain test automation frameworks, write end-to-end tests, and partner with developers to shift quality left. You will play a key role in maintaining our 99.9% uptime SLA.",
            "Selenium, Playwright, TestNG, JUnit, REST Assured, Postman, JIRA, SQL, CI/CD pipelines",
            "Build and maintain end-to-end test automation suites using Playwright\nDesign API test frameworks using REST Assured\nIntegrate automation into CI/CD pipelines (GitHub Actions)\nPerform regression, smoke, and performance testing\nReport and track bugs with detailed reproduction steps in JIRA",
            "Salary: ₹8–12 LPA\nFull remote work option\nFlexible working hours\nLaptop and home-office allowance\nMonthly tech meetup reimbursement",
            103L, "JUNIOR", "Quality", "Quality Assurance",
            "FULL_TIME", "REMOTE",
            "Pune", "MH", "IN", "Pune, MH",
            800000, 1200000, "INR", true,
            LocalDate.of(2026, 10, 15), 21
        )));

        saved.add(saveJob(job(
            "data-engineer-bangalore",
            "Data Engineer",
            "PUBLISHED",
            "As a Senior Data Engineer at Forge AI, you will design, build, and maintain the data pipelines and lakehouse architecture that power our ML models and executive dashboards. You will work with petabyte-scale datasets and make data a first-class product within the organisation.",
            "Apache Spark, Kafka, dbt, Airflow, BigQuery, Snowflake, Python, SQL, Delta Lake, Terraform",
            "Design and maintain real-time and batch ETL pipelines using Kafka and Spark\nBuild data models and transformations using dbt on Snowflake\nOrchestrate workflows using Apache Airflow\nCollaborate with ML engineers on feature engineering\nEstablish data quality SLAs and monitoring dashboards",
            "Salary: ₹20–28 LPA\nHybrid work (Bangalore)\nAnnual stock-option plan\nConference speaking budget\nFull health cover + gym membership",
            104L, "SENIOR", "Data", "Data Engineering",
            "FULL_TIME", "HYBRID",
            "Bangalore", "KA", "IN", "Bangalore, KA",
            2000000, 2800000, "INR", true,
            LocalDate.of(2027, 1, 31), 5
        )));

        saved.add(saveJob(job(
            "product-manager-chennai",
            "Product Manager",
            "PUBLISHED",
            "We are hiring an experienced Product Manager to lead our Candidate Experience product vertical from our Chennai office. You will define the product roadmap, work closely with engineering and design, and ensure every feature ships with measurable business impact. This is a high-visibility role reporting to the VP of Product.",
            "Product roadmap, OKR frameworks, Figma, JIRA, SQL, A/B testing, stakeholder management, user research",
            "Own the full product lifecycle for the Candidate Portal product\nConduct user interviews and synthesise insights into actionable requirements\nDrive quarterly roadmap planning with engineering and design\nDefine success metrics and run A/B experiments\nPresent roadmap and results to C-suite stakeholders",
            "Salary: ₹30–40 LPA\nLeadership role with direct impact on company strategy\nFlexible hybrid policy\nComprehensive ESOP plan\nRelocation assistance for Chennai",
            105L, "LEAD", "Product", "Product Management",
            "FULL_TIME", "ONSITE",
            "Chennai", "TN", "IN", "Chennai, TN",
            3000000, 4000000, "INR", true,
            LocalDate.of(2027, 2, 28), 10
        )));

        saved.add(saveJob(job(
            "business-analyst-hyderabad",
            "Business Analyst",
            "DRAFT",
            "We are looking for a Business Analyst to join our Enterprise Solutions team in Hyderabad. In this role you will bridge the gap between business stakeholders and the technical teams, translating requirements into clear user stories and supporting the delivery of digital transformation initiatives.",
            "Business process modelling, BPMN, SQL, Confluence, JIRA, data analysis, stakeholder workshops",
            "Facilitate requirements-gathering workshops with business stakeholders\nDocument user stories, acceptance criteria, and process flows\nAnalyse business data to identify improvement opportunities\nSupport UAT and go-live activities\nProduce executive reports and presentations",
            "Salary: ₹10–15 LPA\nHybrid work model\nFast-track career path to Senior BA\nLearning & development budget\nTeam events and annual company retreat",
            106L, "MID", "Business", "Business Analysis",
            "FULL_TIME", "HYBRID",
            "Hyderabad", "TS", "IN", "Hyderabad, TS",
            1000000, 1500000, "INR", false,
            LocalDate.of(2027, 3, 31), 0
        )));

        saved.add(saveJob(job(
            "full-stack-engineer-remote",
            "Full Stack Engineer",
            "PUBLISHED",
            "Forge AI is hiring a Full Stack Engineer for our fully-remote global team. You will contribute to both the React frontend and the Spring Boot backend, shipping features end-to-end. We value engineers who care deeply about code quality, performance, and user experience.",
            "React, TypeScript, Spring Boot, Java 17+, PostgreSQL, Docker, REST APIs, Git, CI/CD",
            "Build and maintain React and Spring Boot features end-to-end\nDesign RESTful APIs and data models\nWrite unit and integration tests (JUnit, Playwright)\nParticipate in architecture discussions and ADR process\nContribute to internal developer tooling and documentation",
            "$80,000–$120,000 USD per year\n100% remote — work from anywhere\nEquity participation\n$2,000 annual home-office stipend\nUnlimited PTO with minimum 15 days",
            107L, "MID", "Engineering", "Full Stack Development",
            "FULL_TIME", "REMOTE",
            "Remote", null, null, "Remote",
            80000, 120000, "USD", true,
            LocalDate.of(2027, 3, 31), 3
        )));

        saved.add(saveJob(job(
            "senior-java-developer-pune",
            "Senior Java Developer",
            "CLOSED",
            "This position has been filled. We were looking for a Senior Java Developer to join our core platform team in Pune to help scale our microservices architecture to handle 10x current load. Thank you to all applicants.",
            "Java 17+, Spring Boot 3, Hibernate, PostgreSQL, Kafka, Docker, Kubernetes, JUnit 5, Mockito",
            "Design and implement high-throughput microservices in Java/Spring Boot\nOptimise database queries and Hibernate mappings\nLead technical design discussions and document ADRs\nContribute to platform reliability and SLO targets\nMentor mid-level engineers",
            "Salary: ₹20–30 LPA\nOnsite role in Pune\nPerformance-linked bonus\nHealth, dental and vision insurance\nPaid certifications",
            108L, "SENIOR", "Engineering", "Backend Development",
            "FULL_TIME", "ONSITE",
            "Pune", "MH", "IN", "Pune, MH",
            2000000, 3000000, "INR", false,
            LocalDate.of(2026, 6, 30), 0
        )));

        return saved.stream().filter(j -> j != null).toList();
    }

    private JobPosting saveJob(JobPosting job) {
        if (jobPostingRepo.existsBySlug(job.getSlug())) {
            return null;
        }
        return jobPostingRepo.save(job);
    }

    private JobPosting job(String slug, String title, String status,
                           String description, String requirements,
                           String responsibilities, String benefits,
                           Long demandId, String seniority, String department,
                           String jobCategory, String employmentType, String workMode,
                           String locationCity, String locationState, String locationCountry,
                           String location, Integer salaryMin, Integer salaryMax,
                           String currency, boolean showSalary,
                           LocalDate deadline, int daysAgo) {
        JobPosting j = new JobPosting();
        j.setSlug(slug);
        j.setTitle(title);
        j.setDescription(description);
        j.setRequirements(requirements);
        j.setResponsibilities(responsibilities);
        j.setBenefits(benefits);
        j.setDemandId(demandId);
        j.setSeniority(seniority);
        j.setDepartment(department);
        j.setJobCategory(jobCategory);
        j.setEmploymentType(employmentType);
        j.setWorkMode(workMode);
        j.setLocationCity(locationCity);
        j.setLocationState(locationState);
        j.setLocationCountry(locationCountry);
        j.setLocation(location);
        j.setSalaryMin(salaryMin);
        j.setSalaryMax(salaryMax);
        j.setCurrency(currency);
        j.setShowSalary(showSalary);
        j.setApplicationDeadline(deadline);
        j.setStatus(status);
        j.setMetaTitle(title + " | Forge AI Careers");
        j.setMetaDescription("Join Forge AI as a " + title + ". " + description.substring(0, Math.min(120, description.length())) + "...");

        if ("PUBLISHED".equals(status)) {
            j.setPublishedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        }
        return j;
    }

    // ── Application Intakes ───────────────────────────────────────────────────

    private void seedApplicationIntakes(List<JobPosting> jobs) {
        findBySlug(jobs, "react-frontend-engineer-bangalore").ifPresent(job -> {
            saveIntake(job.getId(), "Priya Mehta", "priya.mehta@example.com",
                "resume_priya_mehta.pdf",
                "Experience: 4 years React\nSkills: React, TypeScript, Redux\nPhone: +91 98765 43210");
            saveIntake(job.getId(), "Rohan Verma", "rohan.verma@example.com",
                "resume_rohan_verma.pdf",
                "Experience: 5 years frontend\nSkills: React, Next.js, GraphQL\nPhone: +91 91234 56789");
        });

        findBySlug(jobs, "devops-engineer-hyderabad").ifPresent(job ->
            saveIntake(job.getId(), "Kiran Reddy", "kiran.reddy@example.com",
                "resume_kiran_reddy.pdf",
                "Experience: 3 years DevOps\nSkills: Kubernetes, Terraform, AWS\nPhone: +91 87654 32109"));

        findBySlug(jobs, "full-stack-engineer-remote").ifPresent(job ->
            saveIntake(job.getId(), "Sara Nguyen", "sara.nguyen@example.com",
                "resume_sara_nguyen.pdf",
                "Experience: 4 years full-stack\nSkills: React, Spring Boot, PostgreSQL\nPhone: +1 415 555 0182"));
    }

    private void saveIntake(Long jobPostingId, String name, String email,
                             String resumeUrl, String coverLetter) {
        boolean exists = intakeRepo.existsByJobPostingIdAndCandidateEmail(jobPostingId, email);
        if (exists) return;

        ApplicationIntake intake = new ApplicationIntake();
        intake.setJobPostingId(jobPostingId);
        intake.setCandidateName(name);
        intake.setCandidateEmail(email);
        intake.setResumeUrl(resumeUrl);
        intake.setCoverLetter(coverLetter);
        intakeRepo.save(intake);

        // Keep applications_count in sync
        jobPostingRepo.findById(jobPostingId).ifPresent(job -> {
            job.setApplicationsCount(job.getApplicationsCount() + 1);
            jobPostingRepo.save(job);
        });
    }

    // ── Job Posting Channels ──────────────────────────────────────────────────

    private void seedChannels(List<JobPosting> jobs) {
        findBySlug(jobs, "react-frontend-engineer-bangalore").ifPresent(job -> {
            saveChannel(job.getId(), "LinkedIn",
                "https://linkedin.com/jobs/react-frontend-engineer-bangalore");
            saveChannel(job.getId(), "Naukri",
                "https://naukri.com/react-frontend-engineer-bangalore");
        });

        findBySlug(jobs, "devops-engineer-hyderabad").ifPresent(job ->
            saveChannel(job.getId(), "LinkedIn",
                "https://linkedin.com/jobs/devops-engineer-hyderabad"));

        findBySlug(jobs, "full-stack-engineer-remote").ifPresent(job -> {
            saveChannel(job.getId(), "LinkedIn",
                "https://linkedin.com/jobs/full-stack-engineer-remote");
            saveChannel(job.getId(), "Indeed",
                "https://indeed.com/viewjob?jk=full-stack-engineer-remote");
        });

        findBySlug(jobs, "data-engineer-bangalore").ifPresent(job ->
            saveChannel(job.getId(), "Naukri",
                "https://naukri.com/data-engineer-bangalore"));
    }

    private void saveChannel(Long jobPostingId, String channelName, String channelUrl) {
        if (channelRepo.findByJobPostingIdAndChannelName(jobPostingId, channelName).isPresent()) {
            return;
        }
        JobPostingChannel ch = new JobPostingChannel();
        ch.setJobPostingId(jobPostingId);
        ch.setChannelName(channelName);
        ch.setChannelUrl(channelUrl);
        channelRepo.save(ch);
    }

    // ── Job Referrals ─────────────────────────────────────────────────────────

    private void seedReferrals(List<JobPosting> jobs) {
        findBySlug(jobs, "react-frontend-engineer-bangalore").ifPresent(job -> {
            saveReferral(job.getId(), "Amit Singh", "amit.singh@example.com",
                "REF-FORGE-REACT-001", 1001L,
                "Former colleague from previous company, 4 years React experience.");
        });

        findBySlug(jobs, "data-engineer-bangalore").ifPresent(job -> {
            saveReferral(job.getId(), "Divya Nair", "divya.nair@example.com",
                "REF-FORGE-DATA-001", 1002L,
                "IIT Bombay batch-mate, strong in Spark and Python.");
        });
    }

    private void saveReferral(Long jobPostingId, String candidateName, String candidateEmail,
                               String referralCode, Long referrerId, String notes) {
        if (referralRepo.existsByReferralCode(referralCode)) return;

        JobReferral ref = new JobReferral();
        ref.setJobPostingId(jobPostingId);
        ref.setReferredCandidateName(candidateName);
        ref.setReferredCandidateEmail(candidateEmail);
        ref.setReferralCode(referralCode);
        ref.setReferrerId(referrerId);
        ref.setNotes(notes);
        referralRepo.save(ref);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Optional<JobPosting> findBySlug(List<JobPosting> jobs, String slug) {
        return jobs.stream().filter(j -> slug.equals(j.getSlug())).findFirst();
    }
}
