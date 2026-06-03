package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.ChannelAnalyticsDto;
import com.griddynamics.forge.market_presence_service.dto.JobPostingAnalyticsResponse;
import com.griddynamics.forge.market_presence_service.entity.JobPosting;
import com.griddynamics.forge.market_presence_service.exception.GlobalExceptionHandler;
import com.griddynamics.forge.market_presence_service.exception.ResourceNotFoundException;
import com.griddynamics.forge.market_presence_service.repository.JobPostingRepository;
import com.griddynamics.forge.market_presence_service.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class JobAnalyticsControllerTest {

    @Mock private AnalyticsService     analyticsService;
    @Mock private JobPostingRepository jobPostingRepository;

    @InjectMocks private JobAnalyticsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── POST /api/public/jobs/{slug}/events ───────────────────────────────────

    @Test
    void POST_recordEvent_returns204_forClickEvent() throws Exception {
        JobPosting job = makeJob(1L);
        when(jobPostingRepository.findBySlug("test-job")).thenReturn(Optional.of(job));

        mockMvc.perform(post("/api/public/jobs/test-job/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"CLICK\",\"channel\":\"CAREERS_PORTAL\"}"))
                .andExpect(status().isNoContent());

        verify(analyticsService).record(1L, "CLICK", "CAREERS_PORTAL");
    }

    @Test
    void POST_recordEvent_returns204_forApplyStartEvent() throws Exception {
        JobPosting job = makeJob(1L);
        when(jobPostingRepository.findBySlug("test-job")).thenReturn(Optional.of(job));

        mockMvc.perform(post("/api/public/jobs/test-job/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"APPLY_START\",\"channel\":\"CAREERS_PORTAL\"}"))
                .andExpect(status().isNoContent());

        verify(analyticsService).record(1L, "APPLY_START", "CAREERS_PORTAL");
    }

    @Test
    void POST_recordEvent_returns204_evenWhenSlugNotFound() throws Exception {
        when(jobPostingRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/public/jobs/unknown/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"CLICK\",\"channel\":\"CAREERS_PORTAL\"}"))
                .andExpect(status().isNoContent());

        // analyticsService must NOT be called for unknown slugs
        verify(analyticsService, never()).record(any(), any(), any());
    }

    // ── GET /api/analytics/job-postings/{id} ──────────────────────────────────

    @Test
    void GET_analytics_single_returns200_withCorrectShape() throws Exception {
        JobPostingAnalyticsResponse resp = new JobPostingAnalyticsResponse(
                1L, "Senior Java Engineer", "senior-java-engineer",
                List.of(new ChannelAnalyticsDto("CAREERS_PORTAL", 10, 5, 3, 2)),
                10L, 5L, 3L, 2L
        );
        when(analyticsService.getAnalytics(1L)).thenReturn(resp);

        mockMvc.perform(get("/api/analytics/job-postings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job_posting_id").value(1))
                .andExpect(jsonPath("$.job_title").value("Senior Java Engineer"))
                .andExpect(jsonPath("$.total_views").value(10))
                .andExpect(jsonPath("$.total_clicks").value(5))
                .andExpect(jsonPath("$.total_apply_starts").value(3))
                .andExpect(jsonPath("$.total_apply_completions").value(2))
                .andExpect(jsonPath("$.channels[0].channel_name").value("CAREERS_PORTAL"))
                .andExpect(jsonPath("$.channels[0].views").value(10))
                .andExpect(jsonPath("$.channels[0].apply_starts").value(3));
    }

    @Test
    void GET_analytics_single_returns404_whenJobNotFound() throws Exception {
        when(analyticsService.getAnalytics(99L))
                .thenThrow(new ResourceNotFoundException("Job posting not found: 99"));

        mockMvc.perform(get("/api/analytics/job-postings/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job posting not found: 99"));
    }

    // ── GET /api/analytics/job-postings ───────────────────────────────────────

    @Test
    void GET_analytics_all_returns200_withList() throws Exception {
        JobPostingAnalyticsResponse resp = new JobPostingAnalyticsResponse(
                2L, "React Engineer", "react-engineer",
                List.of(), 0L, 0L, 0L, 0L
        );
        when(analyticsService.getAllAnalytics()).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/analytics/job-postings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].job_posting_id").value(2))
                .andExpect(jsonPath("$[0].job_title").value("React Engineer"))
                .andExpect(jsonPath("$[0].total_views").value(0));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private JobPosting makeJob(Long id) {
        JobPosting j = new JobPosting();
        ReflectionTestUtils.setField(j, "id", id);
        return j;
    }
}
