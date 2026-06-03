package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.PublicJobResponse;
import com.griddynamics.forge.market_presence_service.exception.GlobalExceptionHandler;
import com.griddynamics.forge.market_presence_service.exception.ResourceNotFoundException;
import com.griddynamics.forge.market_presence_service.service.AnalyticsService;
import com.griddynamics.forge.market_presence_service.service.JobPostingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;


import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PublicJobControllerTest {

    @Mock
    private JobPostingService service;

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private PublicJobController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private PublicJobResponse publishedJob() {
        return new PublicJobResponse(
                1L, 10L, "Backend Engineer", "backend-engineer-new-york",
                "desc", "Java, Spring", "Build APIs", "Health insurance",
                "FULL_TIME", "Senior", "REMOTE",
                "New York", "NY", "US",
                "Engineering", "Software",
                80000, 120000, "USD", true,
                "PUBLISHED", "Backend Engineer | Forge", null,
                null, LocalDate.now().plusMonths(1).toString(),
                null, null);
    }

    @Test
    void GET_publicJobs_returns200_withPublishedJobList() throws Exception {
        when(service.getPublishedJobsPublic()).thenReturn(List.of(publishedJob()));

        mockMvc.perform(get("/api/public/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Backend Engineer"))
                .andExpect(jsonPath("$[0].posting_status").value("PUBLISHED"))
                .andExpect(jsonPath("$[0].experience_level").value("Senior"))
                .andExpect(jsonPath("$[0].work_mode").value("REMOTE"))
                .andExpect(jsonPath("$[0].salary_min").value(80000));
    }

    @Test
    void GET_publicJobs_slug_returns200_withSnakeCaseFields() throws Exception {
        when(service.getPublicJobBySlug("backend-engineer-new-york")).thenReturn(publishedJob());

        mockMvc.perform(get("/api/public/jobs/backend-engineer-new-york"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("backend-engineer-new-york"))
                .andExpect(jsonPath("$.posting_status").value("PUBLISHED"))
                .andExpect(jsonPath("$.location_city").value("New York"))
                .andExpect(jsonPath("$.show_salary").value(true));
    }

    @Test
    void GET_publicJobs_slug_returns404_whenNotFound() throws Exception {
        when(service.getPublicJobBySlug("unknown-slug"))
                .thenThrow(new ResourceNotFoundException("Job posting not found with slug: unknown-slug"));

        mockMvc.perform(get("/api/public/jobs/unknown-slug"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job posting not found with slug: unknown-slug"));
    }
}
