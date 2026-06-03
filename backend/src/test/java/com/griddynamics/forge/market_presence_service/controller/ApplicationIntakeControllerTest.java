package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.ApplicationIntakeResponse;
import com.griddynamics.forge.market_presence_service.exception.ConflictException;
import com.griddynamics.forge.market_presence_service.exception.GlobalExceptionHandler;
import com.griddynamics.forge.market_presence_service.exception.ResourceNotFoundException;
import com.griddynamics.forge.market_presence_service.service.ApplicationIntakeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ApplicationIntakeControllerTest {

    @Mock
    private ApplicationIntakeService service;

    @InjectMocks
    private ApplicationIntakeController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── Helper parts ─────────────────────────────────────────────────────────

    private MockMultipartFile validAppPart(String name, String email, String phone) {
        String json = """
                {"candidateName":"%s","candidateEmail":"%s","candidatePhone":"%s","coverLetter":""}
                """.formatted(name, email, phone != null ? phone : "");
        return new MockMultipartFile(
                "application", "application.json",
                MediaType.APPLICATION_JSON_VALUE, json.getBytes());
    }

    private MockMultipartFile pdfResume() {
        return new MockMultipartFile(
                "resume", "my_cv.pdf",
                "application/pdf", "fake pdf content".getBytes());
    }

    private MockMultipartFile docxResume() {
        return new MockMultipartFile(
                "resume", "my_cv.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "fake docx content".getBytes());
    }

    private ApplicationIntakeResponse sampleResponse() {
        return new ApplicationIntakeResponse(
                1L, 10L,
                "Priya Sharma", "priya.sharma@example.com", "+91 98765 43210",
                "resumes/some-slug/my_cv.pdf", "SUBMITTED", "CAREERS_PORTAL", Instant.now());
    }

    // ── POST /{slug}/apply — happy paths ─────────────────────────────────────

    @Test
    void POST_apply_returns201_withPdfResume() throws Exception {
        when(service.apply(eq("react-frontend-engineer-bangalore"), any(), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(multipart("/api/public/jobs/react-frontend-engineer-bangalore/apply")
                        .file(validAppPart("Priya Sharma", "priya.sharma@example.com", "+91 98765 43210"))
                        .file(pdfResume()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidateEmail").value("priya.sharma@example.com"))
                .andExpect(jsonPath("$.candidateName").value("Priya Sharma"))
                .andExpect(jsonPath("$.candidatePhone").value("+91 98765 43210"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void POST_apply_returns201_withDocxResume() throws Exception {
        when(service.apply(any(), any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(multipart("/api/public/jobs/some-slug/apply")
                        .file(validAppPart("Priya Sharma", "priya.sharma@example.com", null))
                        .file(docxResume()))
                .andExpect(status().isCreated());
    }

    @Test
    void POST_apply_returns201_withoutResume() throws Exception {
        when(service.apply(any(), any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(multipart("/api/public/jobs/some-slug/apply")
                        .file(validAppPart("Priya Sharma", "priya.sharma@example.com", null)))
                .andExpect(status().isCreated());
    }

    // ── 409 — duplicate application ──────────────────────────────────────────

    @Test
    void POST_apply_returns409_withFriendlyMessage_whenDuplicate() throws Exception {
        when(service.apply(any(), any(), any()))
                .thenThrow(new ConflictException(
                        "You have already applied for this position. " +
                        "Check your application status in My Applications."));

        mockMvc.perform(multipart("/api/public/jobs/some-slug/apply")
                        .file(validAppPart("Priya Sharma", "priya.sharma@example.com", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already applied")));
    }

    // ── 400 — invalid file type (validated in FileStorageService, re-thrown as IAE) ──

    @Test
    void POST_apply_returns400_whenResumeIsWrongType() throws Exception {
        MockMultipartFile badFile = new MockMultipartFile(
                "resume", "malware.exe", "application/octet-stream", "bad content".getBytes());

        when(service.apply(any(), any(), any()))
                .thenThrow(new IllegalArgumentException(
                        "Unsupported file type 'malware.exe'. Only PDF and DOCX resumes are accepted."));

        mockMvc.perform(multipart("/api/public/jobs/some-slug/apply")
                        .file(validAppPart("Test User", "test@example.com", null))
                        .file(badFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unsupported file type")));
    }

    // ── 400 — validation errors on JSON part ────────────────────────────────

    @Test
    void POST_apply_returns400_whenCandidateNameBlank() throws Exception {
        MockMultipartFile badApp = new MockMultipartFile(
                "application", "application.json",
                MediaType.APPLICATION_JSON_VALUE,
                """
                {"candidateName":"","candidateEmail":"valid@example.com","coverLetter":""}
                """.getBytes());

        mockMvc.perform(multipart("/api/public/jobs/some-slug/apply")
                        .file(badApp))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details[0]").value(
                        org.hamcrest.Matchers.containsString("candidateName")));
    }

    @Test
    void POST_apply_returns400_whenEmailInvalid() throws Exception {
        MockMultipartFile badApp = new MockMultipartFile(
                "application", "application.json",
                MediaType.APPLICATION_JSON_VALUE,
                """
                {"candidateName":"Valid Name","candidateEmail":"not-an-email","coverLetter":""}
                """.getBytes());

        mockMvc.perform(multipart("/api/public/jobs/some-slug/apply")
                        .file(badApp))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details[0]").value(
                        org.hamcrest.Matchers.containsString("candidateEmail")));
    }

    // ── 404 — unknown slug ───────────────────────────────────────────────────

    @Test
    void POST_apply_returns404_whenJobNotFound() throws Exception {
        when(service.apply(eq("unknown-job"), any(), any()))
                .thenThrow(new ResourceNotFoundException("Job posting not found with slug: unknown-job"));

        mockMvc.perform(multipart("/api/public/jobs/unknown-job/apply")
                        .file(validAppPart("Test User", "test@example.com", null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job posting not found with slug: unknown-job"));
    }
}
