package com.griddynamics.forge.market_presence_service.service;

import com.griddynamics.forge.market_presence_service.dto.JobPostingChannelResponse;
import com.griddynamics.forge.market_presence_service.entity.JobPostingChannel;
import com.griddynamics.forge.market_presence_service.entity.JobPosting;
import com.griddynamics.forge.market_presence_service.exception.ResourceNotFoundException;
import com.griddynamics.forge.market_presence_service.repository.JobPostingChannelRepository;
import com.griddynamics.forge.market_presence_service.repository.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobPostingChannelServiceTest {

    @Mock private JobPostingChannelRepository channelRepository;
    @Mock private JobPostingRepository        jobPostingRepository;

    private JobPostingChannelService service;

    private JobPosting sampleJob;

    @BeforeEach
    void setUp() {
        // LinkedIn API not configured (default fallback mode)
        service = new JobPostingChannelService(
                channelRepository, jobPostingRepository,
                false, "http://localhost:5173");

        sampleJob = new JobPosting();
        sampleJob.setSlug("react-frontend-engineer-bangalore");
        sampleJob.setTitle("React Frontend Engineer");

        when(jobPostingRepository.existsById(1L)).thenReturn(true);
        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(channelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(channelRepository.findByJobPostingIdAndChannelName(anyLong(), anyString()))
                .thenReturn(Optional.empty());
    }

    // ── CAREERS_PORTAL → always LIVE ────────────────────────────────────────

    @Test
    void publishChannel_careersPortal_setsLive() {
        JobPostingChannelResponse result = service.publishChannel(1L, "CAREERS_PORTAL");

        assertThat(result.status()).isEqualTo(JobPostingChannel.LIVE);
        assertThat(result.channelName()).isEqualTo(JobPostingChannel.CAREERS_PORTAL);
        assertThat(result.channelUrl()).isEqualTo("http://localhost:5173/jobs/react-frontend-engineer-bangalore");
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void publishChannel_careersPortal_isIdempotent_onRepublish() {
        // Simulate existing LIVE record
        JobPostingChannel existing = new JobPostingChannel();
        existing.setStatus(JobPostingChannel.LIVE);
        when(channelRepository.findByJobPostingIdAndChannelName(1L, JobPostingChannel.CAREERS_PORTAL))
                .thenReturn(Optional.of(existing));

        JobPostingChannelResponse result = service.publishChannel(1L, "CAREERS_PORTAL");

        assertThat(result.status()).isEqualTo(JobPostingChannel.LIVE);
        verify(channelRepository, times(1)).save(any());
    }

    // ── LINKEDIN no API → PENDING with message ────────────────────────────────

    @Test
    void publishChannel_linkedin_setsPending_whenApiNotConfigured() {
        JobPostingChannelResponse result = service.publishChannel(1L, "LINKEDIN");

        assertThat(result.status()).isEqualTo(JobPostingChannel.PENDING);
        assertThat(result.errorMessage()).contains("LinkedIn API is not configured");
        assertThat(result.errorMessage()).contains("http://localhost:5173/jobs/react-frontend-engineer-bangalore");
        assertThat(result.channelUrl()).isEqualTo("http://localhost:5173/jobs/react-frontend-engineer-bangalore");
    }

    @Test
    void publishChannel_linkedin_setsLive_whenApiConfigured() {
        service = new JobPostingChannelService(
                channelRepository, jobPostingRepository,
                true, "http://localhost:5173");

        JobPostingChannelResponse result = service.publishChannel(1L, "LINKEDIN");

        assertThat(result.status()).isEqualTo(JobPostingChannel.LIVE);
        assertThat(result.errorMessage()).isNull();
    }

    // ── INDEED → LIVE with feed URL in message ───────────────────────────────

    @Test
    void publishChannel_indeed_setsLive() {
        JobPostingChannelResponse result = service.publishChannel(1L, "INDEED");

        assertThat(result.status()).isEqualTo(JobPostingChannel.LIVE);
        assertThat(result.errorMessage()).contains("feed.xml");
        assertThat(result.errorMessage()).contains("Job Distributor");
    }

    // ── Unknown channel → 400 ────────────────────────────────────────────────

    @Test
    void publishChannel_throwsIllegalArgument_forUnknownChannel() {
        assertThatThrownBy(() -> service.publishChannel(1L, "NAUKRI"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown channel");
    }

    // ── unpublish ────────────────────────────────────────────────────────────

    @Test
    void unpublishChannel_setsUnpublished() {
        JobPostingChannel existing = new JobPostingChannel();
        existing.setChannelName(JobPostingChannel.LINKEDIN);
        existing.setStatus(JobPostingChannel.LIVE);
        when(channelRepository.findByJobPostingIdAndChannelName(1L, JobPostingChannel.LINKEDIN))
                .thenReturn(Optional.of(existing));

        JobPostingChannelResponse result = service.unpublishChannel(1L, "LINKEDIN");

        assertThat(result.status()).isEqualTo(JobPostingChannel.UNPUBLISHED);
        assertThat(result.unpublishedAt()).isNotNull();
    }

    @Test
    void unpublishChannel_throws404_whenChannelDoesNotExist() {
        assertThatThrownBy(() -> service.unpublishChannel(1L, "LINKEDIN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── 404 on missing job ────────────────────────────────────────────────────

    @Test
    void publishChannel_throws404_whenJobNotFound() {
        when(jobPostingRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.publishChannel(99L, "CAREERS_PORTAL"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── unpublishAllLiveChannels ──────────────────────────────────────────────

    @Test
    void unpublishAllLiveChannels_unpublishesLiveAndActiveLegacy() {
        JobPostingChannel live = new JobPostingChannel();
        live.setStatus(JobPostingChannel.LIVE);
        JobPostingChannel active = new JobPostingChannel();
        active.setStatus("ACTIVE"); // legacy

        when(channelRepository.findByJobPostingId(1L)).thenReturn(List.of(live, active));

        service.unpublishAllLiveChannels(1L);

        ArgumentCaptor<JobPostingChannel> captor = ArgumentCaptor.forClass(JobPostingChannel.class);
        verify(channelRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .allMatch(ch -> JobPostingChannel.UNPUBLISHED.equals(ch.getStatus()));
    }
}
