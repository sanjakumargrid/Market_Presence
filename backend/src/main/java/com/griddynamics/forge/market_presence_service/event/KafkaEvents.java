
package com.griddynamics.forge.market_presence_service.event;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

public class KafkaEvents {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApplicationSubmittedEvent {
        private UUID applicationId;
        private Long jobId;
        private String jobTitle;
        private String jobSlug;
        private String applicantName;
        private String applicantEmail;
        private String department;
        private String linkedInUrl;
        private String portfolioUrl;
        private String coverLetter;
        private String expectedSalary;
        private String currentCompany;
        private OffsetDateTime appliedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApplicationStatusUpdatedEvent {
        private UUID applicationId;
        private String applicantEmail;
        private String applicantName;
        private String jobTitle;
        private String oldStatus;
        private String newStatus;
        private String nextStep;
        private OffsetDateTime updatedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class JobViewedEvent {
        private Long jobId;
        private String jobSlug;
        private String viewerIp;
        private UUID userId;
        private OffsetDateTime viewedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EmailNotificationEvent {
        private String recipient;
        private String subject;
        private String type;
        private Object payload;
        private OffsetDateTime createdAt;
    }
}
