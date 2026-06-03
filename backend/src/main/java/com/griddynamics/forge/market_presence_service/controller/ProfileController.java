package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.ApiResponse;
import com.griddynamics.forge.market_presence_service.dto.ProfileDto;
import com.griddynamics.forge.market_presence_service.entity.User;
import com.griddynamics.forge.market_presence_service.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<ProfileDto> getProfile(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(profileService.getProfile(currentUser));
    }

    @PutMapping
    public ApiResponse<ProfileDto> updateProfile(
            @RequestBody ProfileDto dto,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok("Profile updated", profileService.updateProfile(dto, currentUser));
    }

    @PostMapping("/resume")
    public ApiResponse<ProfileDto> uploadResume(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok("Resume uploaded", profileService.uploadResume(file, currentUser));
    }
}
