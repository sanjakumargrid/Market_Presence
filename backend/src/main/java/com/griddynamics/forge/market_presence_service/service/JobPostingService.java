package com.griddynamics.forge.market_presence_service.service;

import com.griddynamics.forge.market_presence_service.dto.*;
import com.griddynamics.forge.market_presence_service.entity.JobPosting;
import com.griddynamics.forge.market_presence_service.exception.ResourceNotFoundException;
import com.griddynamics.forge.market_presence_service.repository.JobPostingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JobPostingService {

    private static final Set<String> VALID_STATUSES = Set.of("DRAFT", "PUBLISHED", "CLOSED");

    private final JobPostingRepository      repository;
    private final JobPostingChannelService  channelService;

    public JobPostingService(JobPostingRepository repository,
                             JobPostingChannelService channelService) {
        this.repository     = repository;
        this.channelService = channelService;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public JobPostingResponse create(JobPostingRequest request) {
        validateSalaryRange(request.salaryMin(), request.salaryMax());

        String resolvedLocation = resolveLocation(request.location(),
                request.locationCity(), request.locationState(), request.locationCountry());
        String slug = uniqueSlug(request.title(), resolvedLocation);

        JobPosting job = new JobPosting();
        job.setDemandId(request.demandId());
        job.setTitle(request.title());
        job.setDescription(request.description());
        job.setLocation(resolvedLocation);
        job.setSeniority(request.seniority());
        job.setApplicationDeadline(request.applicationDeadline());
        job.setSlug(slug);
        applyRichFields(job,
                request.requirements(), request.responsibilities(), request.benefits(),
                request.employmentType(), request.workMode(),
                request.locationCity(), request.locationState(), request.locationCountry(),
                request.department(), request.jobCategory(),
                request.salaryMin(), request.salaryMax(), request.currency(), request.showSalary(),
                request.metaTitle(), request.metaDescription());

        return toResponse(repository.save(job));
    }

    /**
     * REQ-JP-01 — Pre-fill a DRAFT posting from demand data.
     *
     * Accepts a DemandSnapshot that mirrors the shape of the Kafka event that
     * Chennai Team 1 will publish when a Demand transitions to OPEN_EXTERNAL.
     * This endpoint lets the flow be tested and demonstrated without Team 1 running.
     * Replace the HTTP call with a @KafkaListener when the real event arrives.
     */
    public JobPostingResponse createFromDemand(DemandSnapshot snapshot) {
        String resolvedLocation = resolveLocation(snapshot.location(),
                snapshot.locationCity(), snapshot.locationState(), snapshot.locationCountry());
        String slug = uniqueSlug(snapshot.title(), resolvedLocation);

        String requirementsText = buildRequirementsFromSkills(snapshot.skills());

        JobPosting job = new JobPosting();
        job.setDemandId(snapshot.demandId());
        job.setTitle(snapshot.title());
        job.setSeniority(normaliseSeniority(snapshot.level()));
        job.setRequirements(requirementsText);
        job.setLocation(resolvedLocation);
        job.setLocationCity(snapshot.locationCity());
        job.setLocationState(snapshot.locationState());
        job.setLocationCountry(snapshot.locationCountry());
        job.setDepartment(snapshot.department());
        job.setApplicationDeadline(snapshot.targetDate());
        job.setSlug(slug);
        job.setMetaTitle(snapshot.title() + " | Forge AI Careers");

        return toResponse(repository.save(job));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public PagedResponse<JobPostingResponse> getAll(
            String status, String location, String seniority, String title, Pageable pageable) {
        Page<JobPosting> page = repository.findByFilters(status, location, seniority, title, pageable);
        return new PagedResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    public List<PublicJobResponse> getPublishedJobsPublic() {
        Pageable pageable = PageRequest.of(0, 1000, Sort.by("createdAt").descending());
        return repository.findByFilters("PUBLISHED", null, null, null, pageable)
                .getContent().stream().map(this::toPublicResponse).toList();
    }

    public JobPostingResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public JobPostingResponse getBySlug(String slug) {
        JobPosting job = repository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found with slug: " + slug));
        return toResponse(job);
    }

    public PublicJobResponse getPublicJobBySlug(String slug) {
        JobPosting job = repository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found with slug: " + slug));
        return toPublicResponse(job);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public JobPostingResponse update(Long id, JobPostingUpdateRequest request) {
        validateSalaryRange(request.salaryMin(), request.salaryMax());

        JobPosting job = findOrThrow(id);

        String resolvedLocation = resolveLocation(request.location(),
                request.locationCity(), request.locationState(), request.locationCountry());
        String newSlug = uniqueSlug(request.title(), resolvedLocation);
        if (!newSlug.equals(job.getSlug()) && repository.existsBySlug(newSlug)) {
            newSlug = newSlug + "-" + id;
        }

        job.setTitle(request.title());
        job.setDescription(request.description());
        job.setLocation(resolvedLocation);
        job.setSeniority(request.seniority());
        job.setApplicationDeadline(request.applicationDeadline());
        job.setSlug(newSlug);
        applyRichFields(job,
                request.requirements(), request.responsibilities(), request.benefits(),
                request.employmentType(), request.workMode(),
                request.locationCity(), request.locationState(), request.locationCountry(),
                request.department(), request.jobCategory(),
                request.salaryMin(), request.salaryMax(), request.currency(), request.showSalary(),
                request.metaTitle(), request.metaDescription());

        return toResponse(repository.save(job));
    }

    @Transactional
    public JobPostingResponse updateStatus(Long id, StatusUpdateRequest request) {
        String newStatus = request.status().toUpperCase();
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status '" + request.status() + "'. Allowed values: " + VALID_STATUSES);
        }

        JobPosting job = findOrThrow(id);
        job.setStatus(newStatus);
        if ("PUBLISHED".equals(newStatus) && job.getPublishedAt() == null) {
            job.setPublishedAt(Instant.now());
        }
        JobPosting saved = repository.save(job);

        // REQ-JP-04 — auto-publish to careers portal when posting is approved
        if ("PUBLISHED".equals(newStatus)) {
            channelService.upsertCareersPortalChannel(saved.getId(), saved.getSlug());
        }
        // REQ-JP-03 — auto-unpublish all live channels when posting is closed
        if ("CLOSED".equals(newStatus)) {
            channelService.unpublishAllLiveChannels(saved.getId());
        }
        return toResponse(saved);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(Long id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JobPosting findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found with id: " + id));
    }

    private void applyRichFields(JobPosting job,
                                  String requirements, String responsibilities, String benefits,
                                  String employmentType, String workMode,
                                  String locationCity, String locationState, String locationCountry,
                                  String department, String jobCategory,
                                  Integer salaryMin, Integer salaryMax, String currency, Boolean showSalary,
                                  String metaTitle, String metaDescription) {
        job.setRequirements(requirements);
        job.setResponsibilities(responsibilities);
        job.setBenefits(benefits);
        job.setEmploymentType(employmentType);
        job.setWorkMode(workMode);
        job.setLocationCity(locationCity);
        job.setLocationState(locationState);
        job.setLocationCountry(locationCountry);
        job.setDepartment(department);
        job.setJobCategory(jobCategory);
        job.setSalaryMin(salaryMin);
        job.setSalaryMax(salaryMax);
        job.setCurrency(currency);
        job.setShowSalary(showSalary);
        job.setMetaTitle(metaTitle);
        job.setMetaDescription(metaDescription);
    }

    /**
     * If the caller supplied a free-form location string, use it as-is.
     * Otherwise auto-build "City, State" (or "City" if state is absent).
     */
    private String resolveLocation(String location, String city, String state, String country) {
        if (location != null && !location.isBlank()) {
            return location;
        }
        if (city != null && !city.isBlank()) {
            if (state != null && !state.isBlank()) {
                return city + ", " + state;
            }
            return city;
        }
        return country != null ? country : "";
    }

    private String uniqueSlug(String title, String location) {
        String base = (title + "-" + location)
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (!repository.existsBySlug(base)) {
            return base;
        }
        int suffix = 2;
        while (repository.existsBySlug(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }

    /** Maps PES seniority level strings to the values already in the system. */
    private String normaliseSeniority(String level) {
        if (level == null) return null;
        return switch (level.toUpperCase()) {
            case "ENTRY", "JUNIOR" -> "JUNIOR";
            case "MID", "INTERMEDIATE" -> "MID";
            case "SENIOR" -> "SENIOR";
            case "LEAD", "STAFF" -> "LEAD";
            case "EXECUTIVE", "PRINCIPAL" -> "EXECUTIVE";
            default -> level;
        };
    }

    private String buildRequirementsFromSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) return null;
        return skills.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
    }

    /** Cross-field salary guard — called at service layer, not annotation, for clear error messages. */
    private void validateSalaryRange(Integer min, Integer max) {
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException(
                    "salaryMin (" + min + ") must not exceed salaryMax (" + max + ")");
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private JobPostingResponse toResponse(JobPosting job) {
        return new JobPostingResponse(
                job.getId(),
                job.getDemandId(),
                job.getTitle(),
                job.getSlug(),
                job.getStatus(),
                job.getDescription(),
                job.getRequirements(),
                job.getResponsibilities(),
                job.getBenefits(),
                job.getEmploymentType(),
                job.getWorkMode(),
                job.getSeniority(),
                job.getLocation(),
                job.getLocationCity(),
                job.getLocationState(),
                job.getLocationCountry(),
                job.getDepartment(),
                job.getJobCategory(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getCurrency(),
                job.getShowSalary(),
                job.getMetaTitle(),
                job.getMetaDescription(),
                job.getApplicationDeadline(),
                job.getApplicationsCount(),
                job.getPublishedAt() != null ? job.getPublishedAt().toString() : null,
                job.getCreatedAt() != null ? job.getCreatedAt().toString() : null,
                job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : null
        );
    }

    private PublicJobResponse toPublicResponse(JobPosting job) {
        return new PublicJobResponse(
                job.getId(),
                job.getDemandId(),
                job.getTitle(),
                job.getSlug(),
                job.getDescription(),
                job.getRequirements(),
                job.getResponsibilities(),
                job.getBenefits(),
                job.getEmploymentType(),
                job.getSeniority(),
                job.getWorkMode(),
                job.getLocationCity(),
                job.getLocationState(),
                job.getLocationCountry(),
                job.getDepartment(),
                job.getJobCategory(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getCurrency(),
                job.getShowSalary(),
                job.getStatus(),
                job.getMetaTitle(),
                job.getMetaDescription(),
                job.getPublishedAt() != null ? job.getPublishedAt().toString() : null,
                job.getApplicationDeadline() != null ? job.getApplicationDeadline().toString() : null,
                job.getCreatedAt() != null ? job.getCreatedAt().toString() : null,
                job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : null
        );
    }
}
