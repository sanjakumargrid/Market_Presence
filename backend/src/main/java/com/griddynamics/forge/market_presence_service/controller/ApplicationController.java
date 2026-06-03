package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.ApiResponse;
import com.griddynamics.forge.market_presence_service.dto.ApplicationDto;
import com.griddynamics.forge.market_presence_service.entity.User;
import com.griddynamics.forge.market_presence_service.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping(value = "/jobs/{slug}/apply", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApplicationDto.ApplicationResponse> apply(
            @PathVariable String slug,
            @RequestPart("application") ApplicationDto.SubmitRequest request,
            @RequestPart(value = "resume", required = false) MultipartFile resume,
            @RequestPart(value = "coverLetter", required = false) MultipartFile coverLetter,
            @RequestPart(value = "certifications", required = false) MultipartFile certifications,
            @RequestPart(value = "transcripts", required = false) MultipartFile transcripts,
            @RequestPart(value = "portfolio", required = false) MultipartFile portfolio,
            @AuthenticationPrincipal User currentUser) {

        return ApiResponse.ok("Application submitted successfully",
                applicationService.submitApplication(
                    slug, request, resume, coverLetter, certifications, transcripts, portfolio, currentUser));
    }

    @GetMapping("/mine")
    public ApiResponse<List<ApplicationDto.ApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(applicationService.getMyApplications(currentUser));
    }
}
