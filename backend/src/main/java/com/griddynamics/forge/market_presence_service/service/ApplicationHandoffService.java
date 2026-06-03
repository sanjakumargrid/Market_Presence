package com.griddynamics.forge.market_presence_service.service;

import com.griddynamics.forge.market_presence_service.dto.HandoffStatusResponse;
import com.griddynamics.forge.market_presence_service.dto.Team2ApplicationPayload;
import com.griddynamics.forge.market_presence_service.entity.ApplicationIntake;
import com.griddynamics.forge.market_presence_service.entity.HandoffRecord;
import com.griddynamics.forge.market_presence_service.entity.JobPosting;
import com.griddynamics.forge.market_presence_service.exception.ResourceNotFoundException;
import com.griddynamics.forge.market_presence_service.repository.HandoffRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the REQ-JP-08 handoff: forwarding a careers-portal application to
 * Chennai Team 2's Talent Acquisition Engine.
 *
 * Flow:
 *  1. createAndAttempt() — called from ApplicationIntakeService.apply() at the end
 *     of the same @Transactional scope.
 *  2. A HandoffRecord is saved with status = PENDING.
 *  3. If Team 2 URL is configured, an HTTP call is attempted immediately:
 *       success → SENT (+ team2ResponseId stored)
 *       failure → FAILED (+ errorMessage stored)
 *  4. If the URL is not configured, the record stays PENDING and a warning is logged.
 *     The admin endpoint POST /api/admin/handoffs/{id}/retry can attempt the send later.
 */
@Service
public class ApplicationHandoffService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationHandoffService.class);

    private final HandoffRecordRepository handoffRepository;
    private final Team2Client             team2Client;
    private final String                  team2BaseUrl;

    public ApplicationHandoffService(
            HandoffRecordRepository handoffRepository,
            Team2Client team2Client,
            @Value("${app.team2.api-base-url:}") String team2BaseUrl) {

        this.handoffRepository = handoffRepository;
        this.team2Client       = team2Client;
        this.team2BaseUrl      = team2BaseUrl;
    }

    // ── Main entry point ────────────────────────────────────────────────────

    /**
     * Creates a PENDING handoff record, then attempts the HTTP call to Team 2.
     * All exceptions are caught — this method never propagates to the caller.
     */
    public void createAndAttempt(ApplicationIntake intake, JobPosting job) {
        HandoffRecord record = buildRecord(intake, job);
        try {
            handoffRepository.save(record);
        } catch (Exception e) {
            log.error("[HANDOFF] Could not persist handoff record for intake {}: {}",
                    intake.getId(), e.getMessage());
            return;
        }

        attemptSend(record, intake);
    }

    // ── Admin / demo operations ─────────────────────────────────────────────

    public Page<HandoffStatusResponse> listAll(Pageable pageable) {
        return handoffRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(HandoffStatusResponse::from);
    }

    public Page<HandoffStatusResponse> listByStatus(String status, Pageable pageable) {
        return handoffRepository.findByStatus(status.toUpperCase(), pageable)
                .map(HandoffStatusResponse::from);
    }

    public HandoffStatusResponse getById(Long id) {
        return HandoffStatusResponse.from(findOrThrow(id));
    }

    /**
     * Manual retry for a PENDING or FAILED handoff — called from the admin endpoint.
     * Useful during demos when Team 2 comes online after an application was submitted.
     */
    public HandoffStatusResponse retry(Long id) {
        HandoffRecord record = findOrThrow(id);

        // Re-build a minimal intake stub from what's stored on the record.
        // The ID is not needed here — buildPayload() reads applicationIntakeId from the record directly.
        ApplicationIntake stub = new ApplicationIntake();
        stub.setCandidateEmail(record.getCandidateEmail());
        stub.setCandidatePhone(record.getCandidatePhone());
        stub.setAppliedAt(record.getAttemptedAt() != null ? record.getAttemptedAt() : Instant.now());

        attemptSend(record, stub);
        return HandoffStatusResponse.from(handoffRepository.findById(id).orElseThrow());
    }

    /**
     * Retry every PENDING handoff record in one call.
     *
     * Useful after Team 2's URL becomes available post-deployment: one admin call
     * converts all accumulated PENDING records to SENT (or FAILED if Team 2 still
     * unreachable).
     *
     * @return number of records that were retried (regardless of outcome)
     */
    public int retryAllPending() {
        List<HandoffRecord> pending = handoffRepository.findByStatus(HandoffRecord.PENDING);
        for (HandoffRecord record : pending) {
            ApplicationIntake stub = new ApplicationIntake();
            stub.setCandidateEmail(record.getCandidateEmail());
            stub.setCandidatePhone(record.getCandidatePhone());
            stub.setAppliedAt(Instant.now());
            attemptSend(record, stub);
        }
        log.info("[HANDOFF] Bulk retry: {} PENDING record(s) processed", pending.size());
        return pending.size();
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private void attemptSend(HandoffRecord record, ApplicationIntake intake) {
        if (!StringUtils.hasText(team2BaseUrl)) {
            log.warn("[HANDOFF] Team 2 URL not configured (app.team2.api-base-url). " +
                     "Handoff record {} is PENDING. Set the URL and use POST /api/admin/handoffs/{}/retry " +
                     "to forward when Team 2 becomes available.",
                     record.getId(), record.getId());
            return; // remains PENDING
        }

        record.setAttemptedAt(Instant.now());

        try {
            Team2ApplicationPayload payload = buildPayload(record, intake);
            Optional<String> team2Id = team2Client.send(payload);

            record.setStatus(HandoffRecord.SENT);
            record.setTeam2ResponseId(team2Id.orElse(null));
            record.setErrorMessage(null);

            log.info("[HANDOFF] Application {} forwarded to Team 2 successfully. " +
                     "Team 2 application ID: {}",
                     record.getApplicationIntakeId(), team2Id.orElse("(none)"));

        } catch (Exception e) {
            record.setStatus(HandoffRecord.FAILED);
            record.setErrorMessage(truncate(e.getMessage(), 1000));

            log.error("[HANDOFF] Failed to forward application {} to Team 2: {}",
                      record.getApplicationIntakeId(), e.getMessage());
        } finally {
            try {
                handoffRepository.save(record);
            } catch (Exception e) {
                log.error("[HANDOFF] Could not persist SENT/FAILED status for record {}: {}",
                        record.getId(), e.getMessage());
            }
        }
    }

    private HandoffRecord buildRecord(ApplicationIntake intake, JobPosting job) {
        HandoffRecord r = new HandoffRecord();
        r.setApplicationIntakeId(intake.getId());
        r.setCandidateEmail(intake.getCandidateEmail());
        r.setCandidatePhone(intake.getCandidatePhone());
        r.setJobSlug(job.getSlug());
        r.setJobTitle(job.getTitle());
        r.setSource(intake.getSource());
        r.setStatus(HandoffRecord.PENDING);
        return r;
    }

    private Team2ApplicationPayload buildPayload(HandoffRecord record, ApplicationIntake intake) {
        String[] nameParts = splitName(record.getCandidateEmail());
        return new Team2ApplicationPayload(
                nameParts[0],
                nameParts[1],
                record.getCandidateEmail(),
                record.getCandidatePhone(),
                record.getSource(),
                intake.getResumeUrl(),
                record.getJobSlug(),
                record.getJobTitle(),
                intake.getAppliedAt() != null ? intake.getAppliedAt() : Instant.now(),
                record.getApplicationIntakeId()
        );
    }

    /** Splits an email prefix into first/last name parts for the payload. */
    private String[] splitName(String email) {
        if (email == null) return new String[]{"", ""};
        String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String[] parts = local.split("[._\\-]", 2);
        return parts.length == 2
                ? new String[]{ capitalise(parts[0]), capitalise(parts[1]) }
                : new String[]{ capitalise(local), "" };
    }

    private String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }

    private HandoffRecord findOrThrow(Long id) {
        return handoffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Handoff record not found with id: " + id));
    }
}
