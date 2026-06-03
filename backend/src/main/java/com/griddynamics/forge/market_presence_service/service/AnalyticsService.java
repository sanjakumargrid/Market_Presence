package com.griddynamics.forge.market_presence_service.service;

import com.griddynamics.forge.market_presence_service.dto.ChannelAnalyticsDto;
import com.griddynamics.forge.market_presence_service.dto.JobPostingAnalyticsResponse;
import com.griddynamics.forge.market_presence_service.entity.JobPosting;
import com.griddynamics.forge.market_presence_service.entity.JobPostingAnalyticEvent;
import com.griddynamics.forge.market_presence_service.exception.ResourceNotFoundException;
import com.griddynamics.forge.market_presence_service.repository.JobPostingAnalyticEventRepository;
import com.griddynamics.forge.market_presence_service.repository.JobPostingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.griddynamics.forge.market_presence_service.entity.JobPostingAnalyticEvent.*;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private static final List<String> VALID_EVENTS   = List.of(VIEW, CLICK, APPLY_START, APPLY_COMPLETE);
    private static final List<String> VALID_CHANNELS = List.of(CH_CAREERS, CH_LINKEDIN, CH_INDEED);

    private final JobPostingAnalyticEventRepository eventRepo;
    private final JobPostingRepository              jobRepo;

    public AnalyticsService(JobPostingAnalyticEventRepository eventRepo,
                            JobPostingRepository jobRepo) {
        this.eventRepo = eventRepo;
        this.jobRepo   = jobRepo;
    }

    /**
     * Record one analytic event.  Never throws — analytics must never break the caller's flow.
     * Unknown event types are silently ignored.  Unknown channels default to CAREERS_PORTAL.
     */
    public void record(Long jobPostingId, String eventType, String channelName) {
        try {
            if (!VALID_EVENTS.contains(eventType)) {
                log.warn("[ANALYTICS] Unknown event type '{}' for job {} — ignored", eventType, jobPostingId);
                return;
            }
            String safeChannel = VALID_CHANNELS.contains(channelName) ? channelName : CH_CAREERS;

            JobPostingAnalyticEvent event = new JobPostingAnalyticEvent();
            event.setJobPostingId(jobPostingId);
            event.setEventType(eventType);
            event.setChannelName(safeChannel);
            eventRepo.save(event);

            log.debug("[ANALYTICS] {} / {} / job={}", eventType, safeChannel, jobPostingId);
        } catch (Exception ex) {
            log.error("[ANALYTICS] Failed to record {} for job {}: {}", eventType, jobPostingId, ex.getMessage());
        }
    }

    /** Aggregate analytics for a single job posting (Team 4 API). */
    public JobPostingAnalyticsResponse getAnalytics(Long jobPostingId) {
        JobPosting job = jobRepo.findById(jobPostingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job posting not found: " + jobPostingId));
        return buildResponse(job);
    }

    /** Aggregate analytics for all job postings (Team 4 API). */
    public List<JobPostingAnalyticsResponse> getAllAnalytics() {
        return jobRepo.findAll().stream()
                .map(this::buildResponse)
                .toList();
    }

    private JobPostingAnalyticsResponse buildResponse(JobPosting job) {
        Long id = job.getId();

        List<ChannelAnalyticsDto> channels = VALID_CHANNELS.stream()
                .map(ch -> new ChannelAnalyticsDto(
                        ch,
                        eventRepo.countByJobPostingIdAndEventTypeAndChannelName(id, VIEW, ch),
                        eventRepo.countByJobPostingIdAndEventTypeAndChannelName(id, CLICK, ch),
                        eventRepo.countByJobPostingIdAndEventTypeAndChannelName(id, APPLY_START, ch),
                        eventRepo.countByJobPostingIdAndEventTypeAndChannelName(id, APPLY_COMPLETE, ch)
                ))
                .toList();

        return new JobPostingAnalyticsResponse(
                id,
                job.getTitle(),
                job.getSlug(),
                channels,
                channels.stream().mapToLong(ChannelAnalyticsDto::views).sum(),
                channels.stream().mapToLong(ChannelAnalyticsDto::clicks).sum(),
                channels.stream().mapToLong(ChannelAnalyticsDto::applyStarts).sum(),
                channels.stream().mapToLong(ChannelAnalyticsDto::applyCompletions).sum()
        );
    }
}
