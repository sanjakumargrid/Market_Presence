package com.griddynamics.forge.market_presence_service.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record JobPostingAnalyticsResponse(
        Long jobPostingId,
        String jobTitle,
        String slug,
        List<ChannelAnalyticsDto> channels,
        long totalViews,
        long totalClicks,
        long totalApplyStarts,
        long totalApplyCompletions
) {}
