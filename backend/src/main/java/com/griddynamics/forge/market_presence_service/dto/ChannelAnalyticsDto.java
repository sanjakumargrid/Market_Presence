package com.griddynamics.forge.market_presence_service.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ChannelAnalyticsDto(
        String channelName,
        long views,
        long clicks,
        long applyStarts,
        long applyCompletions
) {}
