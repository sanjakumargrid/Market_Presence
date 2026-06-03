package com.griddynamics.forge.market_presence_service.service;

import com.griddynamics.forge.market_presence_service.dto.ProfileDto;
import com.griddynamics.forge.market_presence_service.entity.CandidateProfile;
import com.griddynamics.forge.market_presence_service.entity.User;
import com.griddynamics.forge.market_presence_service.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CandidateProfileRepository profileRepository;
    private final FileStorageService fileStorageService;

    public ProfileDto getProfile(User user) {
        CandidateProfile profile = profileRepository.findByUser(user)
            .orElseThrow(() -> new RuntimeException("Profile not found"));

        return ProfileDto.builder()
            .fullName(profile.getFullName())
            .email(user.getEmail())
            .phone(profile.getPhone())
            .bio(profile.getBio())
            .professionalTitle(profile.getProfessionalTitle())
            .resumeFileName(profile.getResumeFileName())
            .resumeFilePath(profile.getResumeFilePath())
            .salaryExpectation(profile.getSalaryExpectation())
            .workModePreference(profile.getWorkModePreference())
            .skills(profile.getSkills())
            .preferredLocations(profile.getPreferredLocations())
            .smartJobAlerts(profile.isSmartJobAlerts())
            .appStatusUpdates(profile.isAppStatusUpdates())
            .employerMessaging(profile.isEmployerMessaging())
            .build();
    }

    @Transactional
    public ProfileDto updateProfile(ProfileDto dto, User user) {
        CandidateProfile profile = profileRepository.findByUser(user)
            .orElseThrow(() -> new RuntimeException("Profile not found"));

        profile.setFullName(dto.getFullName());
        profile.setPhone(dto.getPhone());
        profile.setBio(dto.getBio());
        profile.setProfessionalTitle(dto.getProfessionalTitle());
        profile.setSalaryExpectation(dto.getSalaryExpectation());
        profile.setWorkModePreference(dto.getWorkModePreference());
        profile.setSkills(dto.getSkills());
        profile.setPreferredLocations(dto.getPreferredLocations());
        profile.setSmartJobAlerts(dto.isSmartJobAlerts());
        profile.setAppStatusUpdates(dto.isAppStatusUpdates());
        profile.setEmployerMessaging(dto.isEmployerMessaging());

        profileRepository.save(profile);
        return getProfile(user);
    }

    @Transactional
    public ProfileDto uploadResume(MultipartFile file, User user) {
        CandidateProfile profile = profileRepository.findByUser(user)
            .orElseThrow(() -> new RuntimeException("Profile not found"));

        String filePath = fileStorageService.store(file, "profiles/" + user.getId().toString());
        profile.setResumeFileName(file.getOriginalFilename());
        profile.setResumeFilePath(filePath);

        profileRepository.save(profile);
        return getProfile(user);
    }
}
