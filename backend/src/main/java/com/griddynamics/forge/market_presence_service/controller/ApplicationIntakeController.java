package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.ApplicationIntakeRequest;
import com.griddynamics.forge.market_presence_service.dto.ApplicationIntakeResponse;
import com.griddynamics.forge.market_presence_service.service.ApplicationIntakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/public/jobs")
@Tag(name = "Job Applications", description = "Public endpoint for candidates to apply (REQ-JP-07)")
public class ApplicationIntakeController {

    private final ApplicationIntakeService service;

    public ApplicationIntakeController(ApplicationIntakeService service) {
        this.service = service;
    }

    /**
     * REQ-JP-07 — Submit a job application.
     *
     * Accepts multipart/form-data with two parts:
     *   "application" — JSON blob: { candidateName, candidateEmail, candidatePhone, coverLetter }
     *   "resume"      — optional PDF or DOCX file (max 10 MB, validated server-side)
     *
     * Returns 201 on success, 409 if the candidate already applied, 400 for validation errors.
     */
    @PostMapping(value = "/{slug}/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Submit an application for a job posting (REQ-JP-07)",
            description = "Multipart request. Part 'application' is a JSON blob; part 'resume' is optional PDF/DOCX (≤10 MB). " +
                    "Returns 409 with a friendly message if the candidate has already applied. " +
                    "Returns 400 if the resume file type is not PDF or DOCX.")
    public ApplicationIntakeResponse apply(
            @PathVariable String slug,

            @RequestPart("application")
            @Valid
            ApplicationIntakeRequest request,

            @RequestPart(value = "resume", required = false)
            MultipartFile resume) {

        return service.apply(slug, request, resume);
    }
}
