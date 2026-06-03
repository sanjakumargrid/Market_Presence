package com.griddynamics.forge.market_presence_service.repository;

import com.griddynamics.forge.market_presence_service.entity.JobPostingChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobPostingChannelRepository extends JpaRepository<JobPostingChannel, Long> {

    List<JobPostingChannel> findByJobPostingId(Long jobPostingId);

    Optional<JobPostingChannel> findByJobPostingIdAndChannelName(Long jobPostingId, String channelName);
}
