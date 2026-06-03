package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.HandoffStatusResponse;
import com.griddynamics.forge.market_presence_service.dto.PagedResponse;
import com.griddynamics.forge.market_presence_service.exception.GlobalExceptionHandler;
import com.griddynamics.forge.market_presence_service.exception.ResourceNotFoundException;
import com.griddynamics.forge.market_presence_service.service.ApplicationHandoffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HandoffStatusControllerTest {

    @Mock  private ApplicationHandoffService handoffService;
    @InjectMocks private HandoffStatusController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private HandoffStatusResponse sample(String status) {
        return new HandoffStatusResponse(
                1L, 10L, "priya.sharma@example.com",
                "react-frontend-engineer-bangalore", "React Frontend Engineer",
                "CAREERS_PORTAL", status,
                status.equals("FAILED") ? "Connection refused" : null,
                status.equals("SENT") ? "T2-APP-42" : null,
                Instant.now(), Instant.now());
    }

    // ── GET /api/admin/handoffs ───────────────────────────────────────────────

    @Test
    void GET_handoffs_returns200_withPagedList_noFilter() throws Exception {
        PagedResponse<HandoffStatusResponse> paged = new PagedResponse<>(
                List.of(sample("PENDING")), 0, 20, 1, 1, true);
        when(handoffService.listAll(any(Pageable.class))).thenReturn(
                org.springframework.data.support.PageableExecutionUtils.getPage(
                        List.of(sample("PENDING")),
                        Pageable.ofSize(20),
                        () -> 1L));

        mockMvc.perform(get("/api/admin/handoffs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].source").value("CAREERS_PORTAL"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void GET_handoffs_returns200_filteredByStatus() throws Exception {
        when(handoffService.listByStatus(eq("FAILED"), any(Pageable.class))).thenReturn(
                org.springframework.data.support.PageableExecutionUtils.getPage(
                        List.of(sample("FAILED")),
                        Pageable.ofSize(20),
                        () -> 1L));

        mockMvc.perform(get("/api/admin/handoffs").param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("FAILED"))
                .andExpect(jsonPath("$.content[0].errorMessage").value("Connection refused"));
    }

    // ── GET /api/admin/handoffs/{id} ─────────────────────────────────────────

    @Test
    void GET_handoffs_id_returns200_whenFound() throws Exception {
        when(handoffService.getById(1L)).thenReturn(sample("SENT"));

        mockMvc.perform(get("/api/admin/handoffs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.team2ResponseId").value("T2-APP-42"))
                .andExpect(jsonPath("$.source").value("CAREERS_PORTAL"));
    }

    @Test
    void GET_handoffs_id_returns404_whenNotFound() throws Exception {
        when(handoffService.getById(99L))
                .thenThrow(new ResourceNotFoundException("Handoff record not found with id: 99"));

        mockMvc.perform(get("/api/admin/handoffs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Handoff record not found with id: 99"));
    }

    // ── POST /api/admin/handoffs/{id}/retry ──────────────────────────────────

    @Test
    void POST_retry_returns200_withUpdatedRecord() throws Exception {
        // retry converts PENDING → SENT (or FAILED) — we return SENT for this test
        when(handoffService.retry(1L)).thenReturn(sample("SENT"));

        mockMvc.perform(post("/api/admin/handoffs/1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.team2ResponseId").value("T2-APP-42"));
    }

    @Test
    void POST_retry_returns404_whenHandoffNotFound() throws Exception {
        when(handoffService.retry(99L))
                .thenThrow(new ResourceNotFoundException("Handoff record not found with id: 99"));

        mockMvc.perform(post("/api/admin/handoffs/99/retry"))
                .andExpect(status().isNotFound());
    }
}
