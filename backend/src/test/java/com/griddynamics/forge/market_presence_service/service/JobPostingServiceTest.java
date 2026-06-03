package com.griddynamics.forge.market_presence_service.service;

import com.griddynamics.forge.market_presence_service.dto.*;
import com.griddynamics.forge.market_presence_service.entity.JobPosting;
import com.griddynamics.forge.market_presence_service.exception.ResourceNotFoundException;
import com.griddynamics.forge.market_presence_service.repository.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobPostingServiceTest {

    @Mock
    private JobPostingRepository repository;

    @Mock
    private JobPostingChannelService channelService;

    @InjectMocks
    private JobPostingService service;

    private JobPosting sampleJob;

    @BeforeEach
    void setUp() {
        sampleJob = new JobPosting();
        sampleJob.setTitle("Backend Engineer");
        sampleJob.setLocation("New York");
        sampleJob.setSeniority("SENIOR");
        sampleJob.setSlug("backend-engineer-new-york");
        sampleJob.setApplicationDeadline(LocalDate.now().plusMonths(1));
        sampleJob.beforeCreate();
    }

    private JobPostingRequest sampleRequest() {
        return new JobPostingRequest(
                1L,
                "Backend Engineer",
                "desc",
                "New York",
                "SENIOR",
                LocalDate.now().plusMonths(1),
                null, null, null,           // requirements, responsibilities, benefits
                null, null,                 // employmentType, workMode
                null, null, null,           // locationCity, locationState, locationCountry
                null, null,                 // department, jobCategory
                null, null,                 // salaryMin, salaryMax
                null, null,                 // currency, showSalary
                null, null);                // metaTitle, metaDescription
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_savesJobAndReturnsResponse() {
        when(repository.existsBySlug("backend-engineer-new-york")).thenReturn(false);
        when(repository.save(any())).thenReturn(sampleJob);

        JobPostingResponse response = service.create(sampleRequest());

        assertThat(response.title()).isEqualTo("Backend Engineer");
        assertThat(response.location()).isEqualTo("New York");
        verify(repository).save(any(JobPosting.class));
    }

    @Test
    void create_generatesUniqueSlugOnCollision() {
        when(repository.existsBySlug("backend-engineer-new-york")).thenReturn(true);
        when(repository.existsBySlug("backend-engineer-new-york-2")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> {
            JobPosting j = inv.getArgument(0);
            j.beforeCreate();
            return j;
        });

        JobPostingResponse response = service.create(sampleRequest());

        assertThat(response.slug()).isEqualTo("backend-engineer-new-york-2");
    }

    @Test
    void create_autoBuildsLocation_whenLocationOmitted() {
        JobPostingRequest req = new JobPostingRequest(
                null, "Engineer", null, null, "MID",
                LocalDate.now().plusMonths(1),
                null, null, null,
                null, null,
                "Bangalore", "KA", "IN",
                null, null,
                null, null, null, null, null, null);

        when(repository.existsBySlug(anyString())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> {
            JobPosting j = inv.getArgument(0);
            j.beforeCreate();
            return j;
        });

        JobPostingResponse response = service.create(req);

        assertThat(response.location()).isEqualTo("Bangalore, KA");
    }

    @Test
    void create_throwsIllegalArgument_whenSalaryMinExceedsMax() {
        JobPostingRequest req = new JobPostingRequest(
                null, "Engineer", null, "NYC", "MID",
                LocalDate.now().plusMonths(1),
                null, null, null, null, null, null, null, null,
                null, null,
                200000, 100000,  // min > max
                null, null, null, null);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaryMin");
    }

    // ── createFromDemand (REQ-JP-01) ─────────────────────────────────────────

    @Test
    void createFromDemand_createsPostingWithDraftStatus() {
        DemandSnapshot snapshot = new DemandSnapshot(
                42L, "Senior Java Developer", "SENIOR",
                List.of("Java 17", "Spring Boot", "Kafka"),
                null, "Bangalore", "KA", "IN",
                "Engineering", LocalDate.now().plusMonths(3));

        when(repository.existsBySlug(anyString())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> {
            JobPosting j = inv.getArgument(0);
            j.beforeCreate();
            return j;
        });

        JobPostingResponse response = service.createFromDemand(snapshot);

        assertThat(response.status()).isEqualTo("DRAFT");
        assertThat(response.title()).isEqualTo("Senior Java Developer");
        assertThat(response.seniority()).isEqualTo("SENIOR");
        assertThat(response.demandId()).isEqualTo(42L);
        verify(repository).save(any(JobPosting.class));
    }

    @Test
    void createFromDemand_joinsSkillsIntoRequirements() {
        DemandSnapshot snapshot = new DemandSnapshot(
                1L, "DevOps Engineer", "MID",
                List.of("Kubernetes", "Terraform", "AWS"),
                null, "Hyderabad", "TS", "IN",
                "Infrastructure", LocalDate.now().plusMonths(2));

        when(repository.existsBySlug(anyString())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> {
            JobPosting j = inv.getArgument(0);
            j.beforeCreate();
            return j;
        });

        JobPostingResponse response = service.createFromDemand(snapshot);

        assertThat(response.requirements()).isEqualTo("Kubernetes, Terraform, AWS");
    }

    @Test
    void createFromDemand_autoBuildsLocationFromCityState() {
        DemandSnapshot snapshot = new DemandSnapshot(
                7L, "Data Analyst", "JUNIOR",
                null, null, "Chennai", "TN", "IN",
                "Analytics", LocalDate.now().plusMonths(4));

        when(repository.existsBySlug(anyString())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> {
            JobPosting j = inv.getArgument(0);
            j.beforeCreate();
            return j;
        });

        JobPostingResponse response = service.createFromDemand(snapshot);

        assertThat(response.locationCity()).isEqualTo("Chennai");
    }

    @Test
    void createFromDemand_normalisesLevelAliases() {
        DemandSnapshot snapshot = new DemandSnapshot(
                3L, "Staff Engineer", "STAFF",
                null, null, "Pune", "MH", "IN",
                "Platform", LocalDate.now().plusMonths(2));

        when(repository.existsBySlug(anyString())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> {
            JobPosting j = inv.getArgument(0);
            j.beforeCreate();
            return j;
        });

        JobPostingResponse response = service.createFromDemand(snapshot);

        assertThat(response.seniority()).isEqualTo("LEAD");
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_returnsJob_whenFound() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleJob));

        JobPostingResponse response = service.getById(1L);

        assertThat(response.title()).isEqualTo("Backend Engineer");
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── getBySlug ─────────────────────────────────────────────────────────────

    @Test
    void getBySlug_returnsJob_whenFound() {
        when(repository.findBySlug("backend-engineer-new-york")).thenReturn(Optional.of(sampleJob));

        JobPostingResponse response = service.getBySlug("backend-engineer-new-york");

        assertThat(response.slug()).isEqualTo("backend-engineer-new-york");
    }

    @Test
    void getBySlug_throwsNotFound_whenMissing() {
        when(repository.findBySlug("unknown-slug")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySlug("unknown-slug"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getPublishedJobsPublic ────────────────────────────────────────────────

    @Test
    void getPublishedJobsPublic_returnsPublicJobList() {
        Pageable pageable = PageRequest.of(0, 1000);
        Page<JobPosting> page = new PageImpl<>(List.of(sampleJob), pageable, 1);
        when(repository.findByFilters(eq("PUBLISHED"), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        List<PublicJobResponse> result = service.getPublishedJobsPublic();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Backend Engineer");
        assertThat(result.get(0).postingStatus()).isEqualTo("DRAFT");
    }

    @Test
    void getPublicJobBySlug_returnsPublicJob_whenFound() {
        sampleJob.setDepartment("Engineering");
        sampleJob.setWorkMode("REMOTE");
        when(repository.findBySlug("backend-engineer-new-york")).thenReturn(Optional.of(sampleJob));

        PublicJobResponse response = service.getPublicJobBySlug("backend-engineer-new-york");

        assertThat(response.slug()).isEqualTo("backend-engineer-new-york");
        assertThat(response.experienceLevel()).isEqualTo("SENIOR");
        assertThat(response.workMode()).isEqualTo("REMOTE");
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    void getAll_returnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<JobPosting> page = new PageImpl<>(List.of(sampleJob), pageable, 1);
        when(repository.findByFilters(null, null, null, null, pageable)).thenReturn(page);

        PagedResponse<JobPostingResponse> result = service.getAll(null, null, null, null, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    void updateStatus_updatesStatus_whenValid() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(repository.save(any())).thenReturn(sampleJob);

        service.updateStatus(1L, new StatusUpdateRequest("PUBLISHED"));

        verify(repository).save(argThat(j -> "PUBLISHED".equals(j.getStatus())));
    }

    @Test
    void updateStatus_setsPublishedAt_whenTransitioningToPublished() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateStatus(1L, new StatusUpdateRequest("PUBLISHED"));

        verify(repository).save(argThat(j -> j.getPublishedAt() != null));
    }

    @Test
    void updateStatus_throwsBadRequest_whenInvalidStatus() {
        assertThatThrownBy(() -> service.updateStatus(1L, new StatusUpdateRequest("INVALID")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INVALID");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_deletesJob_whenFound() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleJob));

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
