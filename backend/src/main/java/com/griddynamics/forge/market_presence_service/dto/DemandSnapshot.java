package com.griddynamics.forge.market_presence_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

/**
 * Demand pre-fill stub (REQ-JP-01).
 *
 * Represents the subset of Demand fields that Team 3 needs to create a JobPosting.
 * In production this will be consumed from a Kafka event published by Chennai Team 1
 * when a Demand transitions to OPEN_EXTERNAL. For now it is accepted directly via
 * POST /api/job-postings/from-demand so the flow can be tested without Team 1.
 *
 * Field mapping → JobPosting:
 *   level      → seniority
 *   skills[]   → requirements (joined, comma-separated)
 *   targetDate → applicationDeadline
 *   title + locationCity → slug (auto-generated)
 */
@Schema(description = "Demand snapshot used to pre-fill a job posting (REQ-JP-01)")
public record DemandSnapshot(

        @Schema(description = "Unique ID of the originating Demand record in Chennai Team 1", example = "42")
        Long demandId,

        @NotBlank(message = "Title is required")
        @Schema(description = "Role title copied verbatim from the Demand", example = "Senior Java Developer")
        String title,

        @Schema(description = "Seniority level from Demand: JUNIOR, MID, SENIOR, LEAD, EXECUTIVE", example = "SENIOR")
        String level,

        @Schema(description = "Required skills from the Demand — becomes the requirements text",
                example = "[\"Java 17\", \"Spring Boot\", \"Kafka\"]")
        List<String> skills,

        @Schema(description = "Free-form location string (e.g. 'Bangalore, KA')", example = "Bangalore, KA")
        String location,

        @Schema(description = "City component of the location", example = "Bangalore")
        String locationCity,

        @Schema(description = "State/region code", example = "KA")
        String locationState,

        @Schema(description = "ISO country code", example = "IN")
        String locationCountry,

        @Schema(description = "Business unit / department from the Demand", example = "Engineering")
        String department,

        @Future(message = "Target date must be in the future")
        @Schema(description = "Demand target fill date — used as the posting application deadline",
                example = "2027-03-31")
        LocalDate targetDate
) {}
