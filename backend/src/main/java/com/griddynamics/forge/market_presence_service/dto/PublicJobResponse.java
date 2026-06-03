package com.griddynamics.forge.market_presence_service.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PublicJobResponse(
        Long id,
        Long demandId,
        String title,
        String slug,
        String description,
        String requirements,
        String responsibilities,
        String benefits,
        String employmentType,
        String experienceLevel,
        String workMode,
        String locationCity,
        String locationState,
        String locationCountry,
        String department,
        String jobCategory,
        Integer salaryMin,
        Integer salaryMax,
        String currency,
        Boolean showSalary,
        String postingStatus,
        String metaTitle,
        String metaDescription,
        String publishedAt,
        String expiresAt,
        String createdAt,
        String updatedAt
) {}
