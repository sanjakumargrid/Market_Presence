package com.griddynamics.forge.market_presence_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {
    private String fullName;
    private String email;
    private String phone;
    private String bio;
    private String professionalTitle;
    private String resumeFileName;
    private String resumeFilePath;
    private String salaryExpectation;
    private String workModePreference;
    private List<String> skills;
    private List<String> preferredLocations;
    private boolean smartJobAlerts;
    private boolean appStatusUpdates;
    private boolean employerMessaging;
}
