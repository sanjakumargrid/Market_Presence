package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.PublicJobResponse;
import com.griddynamics.forge.market_presence_service.exception.GlobalExceptionHandler;
import com.griddynamics.forge.market_presence_service.service.JobPostingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class IndeedFeedControllerTest {

    @Mock  private JobPostingService jobPostingService;
    @InjectMocks private IndeedFeedController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "careersPortalBaseUrl", "http://localhost:5173");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private PublicJobResponse sampleJob() {
        return new PublicJobResponse(
                1L, 10L, "React Frontend Engineer", "react-frontend-engineer-bangalore",
                "We are looking for a senior engineer.", "React, TypeScript",
                "Build UI components", "Health insurance",
                "FULL_TIME", "SENIOR", "REMOTE",
                "Bangalore", "KA", "IN",
                "Engineering", "Frontend",
                1800000, 2500000, "INR", true,
                "PUBLISHED", "React Frontend Engineer | Forge", null,
                null, "2027-03-31", null, null);
    }

    @Test
    void GET_feedXml_returns200_withXmlContentType() throws Exception {
        when(jobPostingService.getPublishedJobsPublic()).thenReturn(List.of(sampleJob()));

        mockMvc.perform(get("/api/public/jobs/feed.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"));
    }

    @Test
    void GET_feedXml_containsJobTitle_andSlug() throws Exception {
        when(jobPostingService.getPublishedJobsPublic()).thenReturn(List.of(sampleJob()));

        mockMvc.perform(get("/api/public/jobs/feed.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("React Frontend Engineer")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("react-frontend-engineer-bangalore")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<source>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Forge AI Careers")));
    }

    @Test
    void GET_feedXml_returnsEmptyFeed_whenNoPublishedJobs() throws Exception {
        when(jobPostingService.getPublishedJobsPublic()).thenReturn(List.of());

        mockMvc.perform(get("/api/public/jobs/feed.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<source>")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("<job>"))));
    }
}
