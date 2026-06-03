package com.griddynamics.forge.market_presence_service.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job_posting_analytic_events", indexes = {
        @Index(name = "idx_analytic_job_id",        columnList = "job_posting_id"),
        @Index(name = "idx_analytic_job_event_chan", columnList = "job_posting_id, event_type, channel_name")
})
public class JobPostingAnalyticEvent {

    public static final String VIEW            = "VIEW";
    public static final String CLICK          = "CLICK";
    public static final String APPLY_START    = "APPLY_START";
    public static final String APPLY_COMPLETE = "APPLY_COMPLETE";

    public static final String CH_CAREERS  = "CAREERS_PORTAL";
    public static final String CH_LINKEDIN = "LINKEDIN";
    public static final String CH_INDEED   = "INDEED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long jobPostingId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String channelName;

    @Column(nullable = false)
    private Instant occurredAt;

    @PrePersist
    public void beforeCreate() {
        this.occurredAt = Instant.now();
    }

    public Long getId() { return id; }

    public Long getJobPostingId() { return jobPostingId; }
    public void setJobPostingId(Long v) { this.jobPostingId = v; }

    public String getEventType() { return eventType; }
    public void setEventType(String v) { this.eventType = v; }

    public String getChannelName() { return channelName; }
    public void setChannelName(String v) { this.channelName = v; }

    public Instant getOccurredAt() { return occurredAt; }
}
