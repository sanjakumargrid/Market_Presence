package com.griddynamics.forge.market_presence_service.service;

import com.griddynamics.forge.market_presence_service.entity.JobPosting;
import com.griddynamics.forge.market_presence_service.repository.JobPostingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * REQ-JP-03 — Auto-unpublish expired job postings and their channels.
 *
 * Runs every hour. Finds PUBLISHED jobs whose applicationDeadline has passed,
 * closes them, and unpublishes all their LIVE channels.
 *
 * For the demo: trigger manually by calling expireJobs() directly, or set
 * app.scheduler.expiry.enabled=false to disable and control timing manually.
 */
@Component
public class ChannelExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ChannelExpiryScheduler.class);

    private final JobPostingRepository     jobPostingRepository;
    private final JobPostingChannelService channelService;

    public ChannelExpiryScheduler(JobPostingRepository jobPostingRepository,
                                  JobPostingChannelService channelService) {
        this.jobPostingRepository = jobPostingRepository;
        this.channelService       = channelService;
    }

    /**
     * Runs every hour on the hour.
     * Cron: second=0, minute=0, every hour, every day, every month, every weekday.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireJobs() {
        LocalDate today = LocalDate.now();

        List<JobPosting> expired = jobPostingRepository.findAll(
                PageRequest.of(0, 200)).getContent().stream()
                .filter(j -> "PUBLISHED".equals(j.getStatus()))
                .filter(j -> j.getApplicationDeadline() != null && j.getApplicationDeadline().isBefore(today))
                .toList();

        if (expired.isEmpty()) {
            log.debug("[EXPIRY] No expired job postings found.");
            return;
        }

        log.info("[EXPIRY] Found {} expired job posting(s) — closing and unpublishing channels.", expired.size());

        for (JobPosting job : expired) {
            log.info("[EXPIRY] Closing job {} '{}' (deadline: {})",
                    job.getId(), job.getTitle(), job.getApplicationDeadline());

            job.setStatus("CLOSED");
            jobPostingRepository.save(job);

            channelService.unpublishAllLiveChannels(job.getId());
        }
    }
}
