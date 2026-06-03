package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.PublicJobResponse;
import com.griddynamics.forge.market_presence_service.service.JobPostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * REQ-JP-03 (Indeed fallback) — Generates an Indeed-compatible XML job feed.
 *
 * Recruiters submit the URL of this endpoint (GET /api/public/jobs/feed.xml)
 * to Indeed's Job Distributor portal to enable automatic synchronisation.
 * No real Indeed API key is required — Indeed pulls the XML from this URL.
 *
 * Feed spec: https://docs.indeed.com/en/job-feeds/xml-feed-specification
 */
@RestController
@RequestMapping("/api/public/jobs")
@Tag(name = "Indeed XML Feed", description = "Indeed-format XML job feed for REQ-JP-03")
public class IndeedFeedController {

    private static final DateTimeFormatter RFC822 =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z");

    private final JobPostingService jobPostingService;
    private final String            careersPortalBaseUrl;

    public IndeedFeedController(
            JobPostingService jobPostingService,
            @Value("${app.channels.careers-portal.base-url:http://localhost:5173}") String careersPortalBaseUrl) {
        this.jobPostingService   = jobPostingService;
        this.careersPortalBaseUrl = careersPortalBaseUrl;
    }

    @GetMapping(value = "/feed.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(
            summary = "Indeed-format XML job feed (REQ-JP-03 fallback)",
            description = "Returns all PUBLISHED jobs in Indeed XML feed format. " +
                    "Submit this URL to Indeed's Job Distributor portal: " +
                    "/api/public/jobs/feed.xml")
    public String indeedFeed() {
        List<PublicJobResponse> jobs = jobPostingService.getPublishedJobsPublic();

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<source>\n");
        sb.append("  <publisher>Forge AI Careers</publisher>\n");
        sb.append("  <publisherurl>").append(escape(careersPortalBaseUrl)).append("</publisherurl>\n");
        sb.append("  <lastBuildDate>")
                .append(ZonedDateTime.now(ZoneOffset.UTC).format(RFC822))
                .append("</lastBuildDate>\n");

        for (PublicJobResponse job : jobs) {
            sb.append("  <job>\n");
            sb.append("    <title><![CDATA[").append(job.title()).append("]]></title>\n");
            sb.append("    <date>")
                    .append(job.publishedAt() != null ? job.publishedAt() : ZonedDateTime.now(ZoneOffset.UTC).format(RFC822))
                    .append("</date>\n");
            sb.append("    <referencenumber>").append(escape(job.slug())).append("</referencenumber>\n");
            sb.append("    <url>").append(escape(careersPortalBaseUrl + "/jobs/" + job.slug())).append("</url>\n");
            sb.append("    <company><![CDATA[Forge AI]]></company>\n");
            sb.append("    <city><![CDATA[").append(nvl(job.locationCity())).append("]]></city>\n");
            sb.append("    <state><![CDATA[").append(nvl(job.locationState())).append("]]></state>\n");
            sb.append("    <country><![CDATA[").append(nvl(job.locationCountry())).append("]]></country>\n");
            sb.append("    <remotetype>").append(indeedRemoteType(job.workMode())).append("</remotetype>\n");
            sb.append("    <jobtype>").append(indeedJobType(job.employmentType())).append("</jobtype>\n");
            sb.append("    <experience>").append(nvl(job.experienceLevel())).append("</experience>\n");
            if (Boolean.TRUE.equals(job.showSalary()) && job.salaryMin() != null) {
                sb.append("    <salary><![CDATA[")
                        .append(job.currency()).append(" ")
                        .append(job.salaryMin()).append(" – ").append(job.salaryMax())
                        .append("]]></salary>\n");
            }
            sb.append("    <description><![CDATA[").append(nvl(job.description())).append("]]></description>\n");
            sb.append("  </job>\n");
        }

        sb.append("</source>");
        return sb.toString();
    }

    private String indeedRemoteType(String workMode) {
        if (workMode == null) return "";
        return switch (workMode.toUpperCase()) {
            case "REMOTE" -> "Remote";
            case "HYBRID" -> "Hybrid";
            default       -> "In-person";
        };
    }

    private String indeedJobType(String type) {
        if (type == null) return "fulltime";
        return switch (type.toUpperCase()) {
            case "FULL_TIME"  -> "fulltime";
            case "PART_TIME"  -> "parttime";
            case "CONTRACT"   -> "contract";
            case "INTERNSHIP" -> "internship";
            default           -> "fulltime";
        };
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
