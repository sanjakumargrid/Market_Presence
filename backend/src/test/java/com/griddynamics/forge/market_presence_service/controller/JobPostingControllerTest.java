package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.*;
import com.griddynamics.forge.market_presence_service.exception.GlobalExceptionHandler;
import com.griddynamics.forge.market_presence_service.exception.ResourceNotFoundException;
import com.griddynamics.forge.market_presence_service.service.JobPostingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class JobPostingControllerTest {

    @Mock
    private JobPostingService service;

    @InjectMocks
    private JobPostingController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /** Full-field factory — matches the 29-field JobPostingResponse record. */
    private JobPostingResponse sampleResponse() {
        return new JobPostingResponse(
                1L, 10L,
                "Backend Engineer", "backend-engineer-new-york", "DRAFT",
                "desc", "Java, Spring", "Build APIs", "Health insurance",
                "FULL_TIME", "HYBRID", "SENIOR",
                "New York, NY", "New York", "NY", "US",
                "Engineering", "Backend Development",
                80000, 120000, "USD", true,
                "Backend Engineer | Forge", null,
                LocalDate.now().plusMonths(1),
                0, null, null, null);
    }

    private JobPostingResponse publishedResponse() {
        return new JobPostingResponse(
                1L, 10L,
                "Backend Engineer", "backend-engineer-new-york", "PUBLISHED",
                "desc", null, null, null,
                "FULL_TIME", "HYBRID", "SENIOR",
                "New York, NY", "New York", "NY", "US",
                "Engineering", null,
                80000, 120000, "USD", true,
                null, null,
                LocalDate.now().plusMonths(1),
                0, null, null, null);
    }

    // ── POST /api/job-postings ────────────────────────────────────────────────

    @Test
    void POST_jobPostings_returns201_whenValid() throws Exception {
        String json = """
                {
                  "demandId": 10,
                  "title": "Backend Engineer",
                  "description": "desc",
                  "seniority": "Senior",
                  "applicationDeadline": "%s"
                }
                """.formatted(LocalDate.now().plusMonths(1));

        when(service.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/job-postings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Backend Engineer"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.salary_min").doesNotExist())   // camelCase in admin response
                .andExpect(jsonPath("$.salaryMin").value(80000));
    }

    @Test
    void POST_jobPostings_returns400_whenTitleBlank() throws Exception {
        String json = """
                {
                  "title": "",
                  "seniority": "Senior",
                  "applicationDeadline": "%s"
                }
                """.formatted(LocalDate.now().plusMonths(1));

        mockMvc.perform(post("/api/job-postings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void POST_jobPostings_returns400_whenSalaryNegative() throws Exception {
        String json = """
                {
                  "title": "Engineer",
                  "seniority": "MID",
                  "applicationDeadline": "%s",
                  "salaryMin": -1
                }
                """.formatted(LocalDate.now().plusMonths(1));

        mockMvc.perform(post("/api/job-postings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // ── POST /api/job-postings/from-demand ───────────────────────────────────

    @Test
    void POST_fromDemand_returns201_withPrefilledPosting() throws Exception {
        String json = """
                {
                  "demandId": 42,
                  "title": "Senior Java Developer",
                  "level": "SENIOR",
                  "skills": ["Java 17", "Spring Boot", "Kafka"],
                  "locationCity": "Bangalore",
                  "locationState": "KA",
                  "locationCountry": "IN",
                  "department": "Engineering",
                  "targetDate": "%s"
                }
                """.formatted(LocalDate.now().plusMonths(3));

        JobPostingResponse demandResponse = new JobPostingResponse(
                5L, 42L,
                "Senior Java Developer", "senior-java-developer-bangalore", "DRAFT",
                null, "Java 17, Spring Boot, Kafka", null, null,
                null, null, "SENIOR",
                "Bangalore, KA", "Bangalore", "KA", "IN",
                "Engineering", null,
                null, null, null, null,
                "Senior Java Developer | Forge AI Careers", null,
                LocalDate.now().plusMonths(3),
                0, null, null, null);

        when(service.createFromDemand(any())).thenReturn(demandResponse);

        mockMvc.perform(post("/api/job-postings/from-demand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Senior Java Developer"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.demandId").value(42))
                .andExpect(jsonPath("$.requirements").value("Java 17, Spring Boot, Kafka"))
                .andExpect(jsonPath("$.seniority").value("SENIOR"))
                .andExpect(jsonPath("$.locationCity").value("Bangalore"));
    }

    @Test
    void POST_fromDemand_returns400_whenTitleBlank() throws Exception {
        String json = """
                {
                  "title": "",
                  "targetDate": "%s"
                }
                """.formatted(LocalDate.now().plusMonths(1));

        mockMvc.perform(post("/api/job-postings/from-demand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void POST_fromDemand_returns400_whenDeadlineInPast() throws Exception {
        String json = """
                {
                  "title": "Engineer",
                  "targetDate": "2020-01-01"
                }
                """;

        mockMvc.perform(post("/api/job-postings/from-demand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // ── GET /api/job-postings ─────────────────────────────────────────────────

    @Test
    void GET_jobPostings_returns200_withPagedResponse() throws Exception {
        PagedResponse<JobPostingResponse> paged = new PagedResponse<>(
                List.of(sampleResponse()), 0, 20, 1, 1, true);

        when(service.getAll(any(), any(), any(), any(), any(Pageable.class))).thenReturn(paged);

        mockMvc.perform(get("/api/job-postings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Backend Engineer"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void GET_jobPostings_id_returns200_withAllRichFields() throws Exception {
        when(service.getById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/job-postings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.requirements").value("Java, Spring"))
                .andExpect(jsonPath("$.responsibilities").value("Build APIs"))
                .andExpect(jsonPath("$.salaryMin").value(80000))
                .andExpect(jsonPath("$.salaryMax").value(120000))
                .andExpect(jsonPath("$.workMode").value("HYBRID"))
                .andExpect(jsonPath("$.department").value("Engineering"));
    }

    @Test
    void GET_jobPostings_id_returns404_whenNotFound() throws Exception {
        when(service.getById(99L)).thenThrow(new ResourceNotFoundException("Job posting not found with id: 99"));

        mockMvc.perform(get("/api/job-postings/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job posting not found with id: 99"));
    }

    @Test
    void GET_jobPostings_slug_returns200_whenFound() throws Exception {
        when(service.getBySlug("backend-engineer-new-york")).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/job-postings/slug/backend-engineer-new-york"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("backend-engineer-new-york"));
    }

    // ── PATCH /api/job-postings/{id}/status ──────────────────────────────────

    @Test
    void PATCH_status_returns200_whenValid() throws Exception {
        when(service.updateStatus(eq(1L), any())).thenReturn(publishedResponse());

        mockMvc.perform(patch("/api/job-postings/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    // ── DELETE /api/job-postings/{id} ─────────────────────────────────────────

    @Test
    void DELETE_jobPostings_id_returns204_whenFound() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/api/job-postings/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void DELETE_jobPostings_id_returns404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Job posting not found with id: 99"))
                .when(service).delete(99L);

        mockMvc.perform(delete("/api/job-postings/99"))
                .andExpect(status().isNotFound());
    }
}
