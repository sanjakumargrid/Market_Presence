package com.griddynamics.forge.market_presence_service.service;

import com.griddynamics.forge.market_presence_service.dto.HandoffStatusResponse;
import com.griddynamics.forge.market_presence_service.entity.ApplicationIntake;
import com.griddynamics.forge.market_presence_service.entity.HandoffRecord;
import com.griddynamics.forge.market_presence_service.entity.JobPosting;
import com.griddynamics.forge.market_presence_service.repository.HandoffRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
// ReflectionTestUtils used only to set the private id field on ApplicationIntake for assertions

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationHandoffServiceTest {

    @Mock private HandoffRecordRepository handoffRepository;
    @Mock private Team2Client             team2Client;

    private ApplicationHandoffService service;

    private ApplicationIntake intake;
    private JobPosting        job;

    @BeforeEach
    void setUp() {
        // Instantiate manually so we can set @Value fields via ReflectionTestUtils
        service = new ApplicationHandoffService(handoffRepository, team2Client, "");

        intake = new ApplicationIntake();
        ReflectionTestUtils.setField(intake, "id", 1L);
        intake.setCandidateEmail("priya.sharma@example.com");
        intake.setCandidatePhone("+91 98765 43210");
        intake.setResumeUrl("resumes/some-job/cv.pdf");
        intake.setSource("CAREERS_PORTAL");
        intake.setAppliedAt(Instant.now());

        job = new JobPosting();
        job.setSlug("react-frontend-engineer-bangalore");
        job.setTitle("React Frontend Engineer");

        // Default: save() returns the record it receives unchanged
        when(handoffRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Case 1: no Team 2 URL configured → PENDING, no HTTP call ────────────

    @Test
    void createAndAttempt_leavesPending_whenTeam2UrlNotConfigured() {
        // service already created with empty URL in setUp()

        service.createAndAttempt(intake, job);

        ArgumentCaptor<HandoffRecord> captor = ArgumentCaptor.forClass(HandoffRecord.class);
        // save() called once: for the PENDING record (no update after HTTP call)
        verify(handoffRepository, times(1)).save(captor.capture());

        HandoffRecord saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(HandoffRecord.PENDING);
        assertThat(saved.getSource()).isEqualTo("CAREERS_PORTAL");
        assertThat(saved.getJobSlug()).isEqualTo("react-frontend-engineer-bangalore");
        assertThat(saved.getCandidateEmail()).isEqualTo("priya.sharma@example.com");

        // Team2Client must NOT be called
        verifyNoInteractions(team2Client);
    }

    // ── Case 2: URL configured + Team 2 responds 201 → SENT ─────────────────

    @Test
    void createAndAttempt_transitionsToSent_whenTeam2CallSucceeds() {
        service = new ApplicationHandoffService(
                handoffRepository, team2Client,
                "http://team2-host:8080/api");

        when(team2Client.send(any())).thenReturn(Optional.of("T2-APP-42"));

        service.createAndAttempt(intake, job);

        ArgumentCaptor<HandoffRecord> captor = ArgumentCaptor.forClass(HandoffRecord.class);
        // save() called twice: PENDING creation + SENT update
        verify(handoffRepository, times(2)).save(captor.capture());

        HandoffRecord sentRecord = captor.getAllValues().get(1);
        assertThat(sentRecord.getStatus()).isEqualTo(HandoffRecord.SENT);
        assertThat(sentRecord.getTeam2ResponseId()).isEqualTo("T2-APP-42");
        assertThat(sentRecord.getErrorMessage()).isNull();
    }

    @Test
    void createAndAttempt_setsSourceCarriersPortalOnPayload_whenUrlConfigured() {
        service = new ApplicationHandoffService(
                handoffRepository, team2Client,
                "http://team2-host:8080/api");

        when(team2Client.send(any())).thenReturn(Optional.empty());

        service.createAndAttempt(intake, job);

        var payloadCaptor = ArgumentCaptor.forClass(
                com.griddynamics.forge.market_presence_service.dto.Team2ApplicationPayload.class);
        verify(team2Client).send(payloadCaptor.capture());

        assertThat(payloadCaptor.getValue().source()).isEqualTo("CAREERS_PORTAL");
        assertThat(payloadCaptor.getValue().email()).isEqualTo("priya.sharma@example.com");
        assertThat(payloadCaptor.getValue().jobSlug()).isEqualTo("react-frontend-engineer-bangalore");
        assertThat(payloadCaptor.getValue().applicationIntakeId()).isEqualTo(1L);
    }

    // ── Case 3: URL configured + Team 2 throws → FAILED ─────────────────────

    @Test
    void createAndAttempt_transitionsToFailed_whenTeam2CallThrows() {
        service = new ApplicationHandoffService(
                handoffRepository, team2Client,
                "http://team2-host:8080/api");

        when(team2Client.send(any())).thenThrow(
                new RuntimeException("Connection refused: team2-host:8080"));

        service.createAndAttempt(intake, job);

        ArgumentCaptor<HandoffRecord> captor = ArgumentCaptor.forClass(HandoffRecord.class);
        verify(handoffRepository, times(2)).save(captor.capture());

        HandoffRecord failedRecord = captor.getAllValues().get(1);
        assertThat(failedRecord.getStatus()).isEqualTo(HandoffRecord.FAILED);
        assertThat(failedRecord.getErrorMessage())
                .contains("Connection refused");
        assertThat(failedRecord.getTeam2ResponseId()).isNull();
    }

    // ── Verify: exception in HTTP call never propagates out ──────────────────

    @Test
    void createAndAttempt_neverThrows_evenWhenTeam2AndRepoFail() {
        service = new ApplicationHandoffService(
                handoffRepository, team2Client,
                "http://team2-host:8080/api");

        when(team2Client.send(any())).thenThrow(new RuntimeException("network down"));
        // First save (PENDING creation) succeeds; second save (FAILED update) throws
        when(handoffRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0))
                .thenThrow(new RuntimeException("DB write failed"));

        // Must not throw
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> service.createAndAttempt(intake, job));
    }
}
