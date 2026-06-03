package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.JobPostingChannelResponse;
import com.griddynamics.forge.market_presence_service.service.JobPostingChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-postings/{id}")
@Tag(name = "Job Posting Channels", description = "Publish job postings to channels (REQ-JP-03, REQ-JP-04)")
public class JobPostingChannelController {

    private final JobPostingChannelService service;

    public JobPostingChannelController(JobPostingChannelService service) {
        this.service = service;
    }

    // ── Generic endpoints ────────────────────────────────────────────────────

    @PostMapping("/channels/{channel}/publish")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Publish to a specific channel (REQ-JP-03)",
            description = "channel: CAREERS_PORTAL | LINKEDIN | INDEED. " +
                    "CAREERS_PORTAL → always LIVE. " +
                    "LINKEDIN → LIVE if API configured; PENDING with copy-URL message if not. " +
                    "INDEED → LIVE (XML feed always available at /api/public/jobs/feed.xml).")
    public JobPostingChannelResponse publishToChannel(
            @PathVariable Long id,
            @Parameter(description = "CAREERS_PORTAL | LINKEDIN | INDEED")
            @PathVariable String channel) {
        return service.publishChannel(id, channel);
    }

    @PostMapping("/channels/{channel}/unpublish")
    @Operation(
            summary = "Unpublish from a specific channel",
            description = "Sets channel status to UNPUBLISHED. Use to manually retract a posting.")
    public JobPostingChannelResponse unpublishFromChannel(
            @PathVariable Long id,
            @PathVariable String channel) {
        return service.unpublishChannel(id, channel);
    }

    @GetMapping("/channels")
    @Operation(summary = "List all channels with current status and last-updated timestamp")
    public List<JobPostingChannelResponse> getChannels(@PathVariable Long id) {
        return service.getChannels(id);
    }

    // ── Legacy endpoints (kept for backward compatibility) ───────────────────

    @PostMapping("/publish/linkedin")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publish to LinkedIn (delegates to /channels/LINKEDIN/publish)")
    public JobPostingChannelResponse publishToLinkedIn(@PathVariable Long id) {
        return service.publishToLinkedIn(id);
    }

    @PostMapping("/publish/indeed")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publish to Indeed (delegates to /channels/INDEED/publish)")
    public JobPostingChannelResponse publishToIndeed(@PathVariable Long id) {
        return service.publishToIndeed(id);
    }
}
