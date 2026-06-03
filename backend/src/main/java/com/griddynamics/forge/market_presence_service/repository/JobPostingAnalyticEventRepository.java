package com.griddynamics.forge.market_presence_service.repository;

import com.griddynamics.forge.market_presence_service.entity.JobPostingAnalyticEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostingAnalyticEventRepository extends JpaRepository<JobPostingAnalyticEvent, Long> {

    long countByJobPostingIdAndEventTypeAndChannelName(
            Long jobPostingId, String eventType, String channelName);
}
