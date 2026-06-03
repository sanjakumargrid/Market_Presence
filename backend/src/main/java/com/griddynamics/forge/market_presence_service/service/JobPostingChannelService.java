package com.griddynamics.forge.market_presence_service.service;

import com.griddynamics.forge.market_presence_service.dto.JobPostingChannelResponse;
import com.griddynamics.forge.market_presence_service.entity.JobPostingChannel;
import com.griddynamics.forge.market_presence_service.exception.ResourceNotFoundException;
import com.griddynamics.forge.market_presence_service.repository.JobPostingChannelRepository;
import com.griddynamics.forge.market_presence_service.repository.JobPostingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Manages job posting channels (REQ-JP-03, REQ-JP-04).
 *
 * Status transitions:
 *   CAREERS_PORTAL publish  → always LIVE  (we ARE the portal)
 *   LINKEDIN publish        → LIVE if API configured; PENDING with copy-URL message if not
 *   INDEED publish          → LIVE (XML feed always available at /api/public/jobs/feed.xml)
 *   unpublish (any channel) → UNPUBLISHED
 *   auto-expiry (scheduler) → UNPUBLISHED on all LIVE channels
 */
@Service
public class JobPostingChannelService {

    private static final Logger log = LoggerFactory.getLogger(JobPostingChannelService.class);

    /** Statuses that count as "currently live" — catches both old seeds and new canonical values. */
    private static final Set<String> LIVE_STATUSES = Set.of(
            JobPostingChannel.LIVE, "ACTIVE", "PUBLISHED");

    private final JobPostingChannelRepository channelRepository;
    private final JobPostingRepository        jobPostingRepository;

    private final boolean linkedInApiConfigured;
    private final String  careersPortalBaseUrl;

    public JobPostingChannelService(
            JobPostingChannelRepository channelRepository,
            JobPostingRepository jobPostingRepository,
            @Value("${app.channels.linkedin.api-configured:false}") boolean linkedInApiConfigured,
            @Value("${app.channels.careers-portal.base-url:http://localhost:5173}") String careersPortalBaseUrl) {

        this.channelRepository      = channelRepository;
        this.jobPostingRepository   = jobPostingRepository;
        this.linkedInApiConfigured  = linkedInApiConfigured;
        this.careersPortalBaseUrl   = careersPortalBaseUrl;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Unified publish — determines the correct status and fallback message based on channel.
     * Called both from the generic endpoint and the legacy /publish/linkedin|indeed endpoints.
     */
    @Transactional
    public JobPostingChannelResponse publishChannel(Long jobPostingId, String channel) {
        String canonical = canonical(channel);
        requireJobExists(jobPostingId);
        String jobSlug = jobPostingRepository.findById(jobPostingId)
                .map(j -> j.getSlug()).orElse(String.valueOf(jobPostingId));

        return switch (canonical) {
            case JobPostingChannel.CAREERS_PORTAL -> publishCareersPortal(jobPostingId, jobSlug);
            case JobPostingChannel.LINKEDIN        -> publishLinkedIn(jobPostingId, jobSlug);
            case JobPostingChannel.INDEED          -> publishIndeed(jobPostingId, jobSlug);
            default -> throw new IllegalArgumentException(
                    "Unknown channel '" + channel + "'. Allowed: CAREERS_PORTAL, LINKEDIN, INDEED");
        };
    }

    /**
     * Unified unpublish — sets the channel to UNPUBLISHED.
     */
    @Transactional
    public JobPostingChannelResponse unpublishChannel(Long jobPostingId, String channel) {
        String canonical = canonical(channel);
        requireJobExists(jobPostingId);

        JobPostingChannel ch = channelRepository
                .findByJobPostingIdAndChannelName(jobPostingId, canonical)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Channel " + canonical + " not found for job " + jobPostingId));

        ch.setStatus(JobPostingChannel.UNPUBLISHED);
        ch.setUnpublishedAt(Instant.now());
        ch.setErrorMessage(null);

        log.info("[CHANNEL] Unpublished {} for job {}", canonical, jobPostingId);
        return JobPostingChannelResponse.from(channelRepository.save(ch));
    }

    /** List all channel records for a job posting. */
    public List<JobPostingChannelResponse> getChannels(Long jobPostingId) {
        requireJobExists(jobPostingId);
        return channelRepository.findByJobPostingId(jobPostingId)
                .stream()
                .map(JobPostingChannelResponse::from)
                .toList();
    }

    // ── Internal triggers (called by JobPostingService) ──────────────────────

    /**
     * REQ-JP-04 — Auto-publish to careers portal when a job posting is set to PUBLISHED.
     * Creates or updates the CAREERS_PORTAL channel to LIVE.
     */
    @Transactional
    public void upsertCareersPortalChannel(Long jobPostingId, String jobSlug) {
        publishCareersPortal(jobPostingId, jobSlug);
    }

    /**
     * REQ-JP-03 — Auto-unpublish all currently live channels when a job is CLOSED.
     */
    @Transactional
    public void unpublishAllLiveChannels(Long jobPostingId) {
        List<JobPostingChannel> channels = channelRepository.findByJobPostingId(jobPostingId);
        for (JobPostingChannel ch : channels) {
            if (LIVE_STATUSES.contains(ch.getStatus())) {
                ch.setStatus(JobPostingChannel.UNPUBLISHED);
                ch.setUnpublishedAt(Instant.now());
                channelRepository.save(ch);
                log.info("[CHANNEL] Auto-unpublished {} for closed/expired job {}",
                        ch.getChannelName(), jobPostingId);
            }
        }
    }

    // ── Legacy endpoint delegates (backward-compatible) ──────────────────────

    @Transactional
    public JobPostingChannelResponse publishToLinkedIn(Long jobPostingId) {
        return publishChannel(jobPostingId, JobPostingChannel.LINKEDIN);
    }

    @Transactional
    public JobPostingChannelResponse publishToIndeed(Long jobPostingId) {
        return publishChannel(jobPostingId, JobPostingChannel.INDEED);
    }

    // ── Channel-specific publish logic ───────────────────────────────────────

    private JobPostingChannelResponse publishCareersPortal(Long jobPostingId, String jobSlug) {
        String url = careersPortalBaseUrl + "/jobs/" + jobSlug;
        JobPostingChannel ch = getOrCreate(jobPostingId, JobPostingChannel.CAREERS_PORTAL);
        ch.setChannelUrl(url);
        ch.setStatus(JobPostingChannel.LIVE);
        ch.setPublishedAt(ch.getPublishedAt() != null ? ch.getPublishedAt() : Instant.now());
        ch.setErrorMessage(null);
        log.info("[CHANNEL] Careers portal LIVE for job {} → {}", jobPostingId, url);
        return JobPostingChannelResponse.from(channelRepository.save(ch));
    }

    private JobPostingChannelResponse publishLinkedIn(Long jobPostingId, String jobSlug) {
        JobPostingChannel ch = getOrCreate(jobPostingId, JobPostingChannel.LINKEDIN);

        if (linkedInApiConfigured) {
            // Real LinkedIn Jobs API call would go here (Phase 6 scope)
            String url = "https://www.linkedin.com/jobs/view/" + jobSlug;
            ch.setChannelUrl(url);
            ch.setStatus(JobPostingChannel.LIVE);
            ch.setPublishedAt(ch.getPublishedAt() != null ? ch.getPublishedAt() : Instant.now());
            ch.setErrorMessage(null);
            log.info("[CHANNEL] LinkedIn LIVE for job {}", jobPostingId);
        } else {
            // Fallback: recruiter copies the careers-portal URL and posts to LinkedIn manually
            String jobUrl = careersPortalBaseUrl + "/jobs/" + jobSlug;
            ch.setChannelUrl(jobUrl);
            ch.setStatus(JobPostingChannel.PENDING);
            ch.setErrorMessage(
                    "LinkedIn API is not configured. To post manually: copy this URL and use it in LinkedIn's " +
                    "\"Create a Job Post\" flow: " + jobUrl);
            log.info("[CHANNEL] LinkedIn PENDING for job {} — manual copy-paste required", jobPostingId);
        }
        return JobPostingChannelResponse.from(channelRepository.save(ch));
    }

    private JobPostingChannelResponse publishIndeed(Long jobPostingId, String jobSlug) {
        // Indeed is fed via the XML endpoint — always available, no external API call needed
        String feedUrl = careersPortalBaseUrl.replace("5173", "8086") + "/api/public/jobs/feed.xml";
        String jobUrl  = careersPortalBaseUrl + "/jobs/" + jobSlug;

        JobPostingChannel ch = getOrCreate(jobPostingId, JobPostingChannel.INDEED);
        ch.setChannelUrl(jobUrl);
        ch.setStatus(JobPostingChannel.LIVE);
        ch.setPublishedAt(ch.getPublishedAt() != null ? ch.getPublishedAt() : Instant.now());
        ch.setErrorMessage(
                "Indeed feed is live at: " + feedUrl +
                " — submit this URL to Indeed's Job Distributor portal to activate automatic sync.");
        log.info("[CHANNEL] Indeed LIVE for job {} (feed: {})", jobPostingId, feedUrl);
        return JobPostingChannelResponse.from(channelRepository.save(ch));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private JobPostingChannel getOrCreate(Long jobPostingId, String channelName) {
        return channelRepository
                .findByJobPostingIdAndChannelName(jobPostingId, channelName)
                .orElseGet(() -> {
                    JobPostingChannel ch = new JobPostingChannel();
                    ch.setJobPostingId(jobPostingId);
                    ch.setChannelName(channelName);
                    return ch;
                });
    }

    private void requireJobExists(Long jobPostingId) {
        if (!jobPostingRepository.existsById(jobPostingId)) {
            throw new ResourceNotFoundException("Job posting not found with id: " + jobPostingId);
        }
    }

    /** Normalise "LinkedIn" → "LINKEDIN", "Indeed" → "INDEED", etc. */
    private String canonical(String name) {
        return name == null ? "" : name.toUpperCase();
    }
}
