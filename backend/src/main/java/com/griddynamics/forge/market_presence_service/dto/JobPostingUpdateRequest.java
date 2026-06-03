package com.griddynamics.forge.market_presence_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

@Schema(description = "Request body for updating an existing job posting (REQ-JP-02)")
public record JobPostingUpdateRequest(

        @NotBlank(message = "Title is required")
        @Schema(description = "Role title", example = "Senior Java Developer")
        String title,

        @Schema(description = "Role summary / overview")
        String description,

        @Schema(description = "Human-readable location (auto-built from city/state if omitted)",
                example = "Bangalore, KA")
        String location,

        @NotBlank(message = "Seniority is required")
        @Schema(description = "JUNIOR | MID | SENIOR | LEAD | EXECUTIVE", example = "SENIOR")
        String seniority,

        @NotNull(message = "Application deadline is required")
        @Future(message = "Application deadline must be a future date")
        @Schema(description = "Last date to accept applications", example = "2027-03-31")
        LocalDate applicationDeadline,

        @Schema(description = "Required skills and qualifications")
        String requirements,

        @Schema(description = "Key responsibilities")
        String responsibilities,

        @Schema(description = "Perks and benefits")
        String benefits,

        @Schema(description = "FULL_TIME | PART_TIME | CONTRACT | INTERNSHIP", example = "FULL_TIME")
        String employmentType,

        @Schema(description = "REMOTE | HYBRID | ONSITE", example = "HYBRID")
        String workMode,

        @Schema(description = "City", example = "Bangalore")
        String locationCity,

        @Schema(description = "State/region code", example = "KA")
        String locationState,

        @Schema(description = "ISO country code", example = "IN")
        String locationCountry,

        @Schema(description = "Business unit / department", example = "Engineering")
        String department,

        @Schema(description = "Job category", example = "Backend Development")
        String jobCategory,

        @PositiveOrZero(message = "Minimum salary must be zero or greater")
        @Schema(description = "Minimum salary", example = "1800000")
        Integer salaryMin,

        @PositiveOrZero(message = "Maximum salary must be zero or greater")
        @Schema(description = "Maximum salary", example = "2500000")
        Integer salaryMax,

        @Schema(description = "Currency code", example = "INR")
        String currency,

        @Schema(description = "Show salary on public portal")
        Boolean showSalary,

        @Schema(description = "SEO page title")
        String metaTitle,

        @Schema(description = "SEO meta description")
        String metaDescription
) {}
