package com.griddynamics.forge.market_presence_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Full admin response for a job posting — returned by all admin CRUD endpoints.
 * Contains every editable field so an admin editor can display and round-trip the
 * complete posting without a second request.
 */
@Schema(description = "Full job posting response for admin operations")
public record JobPostingResponse(

        // ── Identity ─────────────────────────────────────────────────────────
        @Schema(description = "Database ID", example = "1")
        Long id,

        @Schema(description = "ID of the originating Demand record (REQ-JP-01)", example = "42")
        Long demandId,

        @Schema(description = "Posting title", example = "Senior Java Developer")
        String title,

        @Schema(description = "URL-safe unique slug", example = "senior-java-developer-pune")
        String slug,

        @Schema(description = "Posting status: DRAFT | PUBLISHED | CLOSED", example = "DRAFT")
        String status,

        // ── Content ───────────────────────────────────────────────────────────
        @Schema(description = "Role summary / overview")
        String description,

        @Schema(description = "Required skills and qualifications")
        String requirements,

        @Schema(description = "Key responsibilities")
        String responsibilities,

        @Schema(description = "Perks and benefits")
        String benefits,

        // ── Type / level ──────────────────────────────────────────────────────
        @Schema(description = "Employment type: FULL_TIME | PART_TIME | CONTRACT | INTERNSHIP",
                example = "FULL_TIME")
        String employmentType,

        @Schema(description = "Work mode: REMOTE | HYBRID | ONSITE", example = "HYBRID")
        String workMode,

        @Schema(description = "Seniority: JUNIOR | MID | SENIOR | LEAD | EXECUTIVE", example = "SENIOR")
        String seniority,

        // ── Location ──────────────────────────────────────────────────────────
        @Schema(description = "Human-readable location string", example = "Bangalore, KA")
        String location,

        @Schema(description = "City", example = "Bangalore")
        String locationCity,

        @Schema(description = "State/region code", example = "KA")
        String locationState,

        @Schema(description = "ISO country code", example = "IN")
        String locationCountry,

        // ── Categorisation ────────────────────────────────────────────────────
        @Schema(description = "Business unit / department", example = "Engineering")
        String department,

        @Schema(description = "Job category", example = "Backend Development")
        String jobCategory,

        // ── Compensation ──────────────────────────────────────────────────────
        @Schema(description = "Minimum salary", example = "1800000")
        Integer salaryMin,

        @Schema(description = "Maximum salary", example = "2500000")
        Integer salaryMax,

        @Schema(description = "Currency code", example = "INR")
        String currency,

        @Schema(description = "Whether to show salary on the public portal", example = "true")
        Boolean showSalary,

        // ── SEO ───────────────────────────────────────────────────────────────
        @Schema(description = "Page title for search engines")
        String metaTitle,

        @Schema(description = "Meta description for search engines")
        String metaDescription,

        // ── Timing ────────────────────────────────────────────────────────────
        @Schema(description = "Application deadline", example = "2027-03-31")
        LocalDate applicationDeadline,

        // ── Stats ─────────────────────────────────────────────────────────────
        @Schema(description = "Number of applications received", example = "7")
        int applicationsCount,

        // ── Timestamps ───────────────────────────────────────────────────────
        @Schema(description = "ISO-8601 timestamp when the posting was published")
        String publishedAt,

        @Schema(description = "ISO-8601 creation timestamp")
        String createdAt,

        @Schema(description = "ISO-8601 last-update timestamp")
        String updatedAt
) {}
