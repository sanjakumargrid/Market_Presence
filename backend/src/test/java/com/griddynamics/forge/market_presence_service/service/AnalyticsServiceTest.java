package com.griddynamics.forge.market_presence_service.service;

import com.griddynamics.forge.market_presence_service.dto.ChannelAnalyticsDto;
import com.griddynamics.forge.market_presence_service.dto.JobPostingAnalyticsResponse;
import com.griddynamics.forge.market_presence_service.entity.JobPosting;
import com.griddynamics.forge.market_presence_service.entity.JobPostingAnalyticEvent;
import com.griddynamics.forge.market_presence_service.exception.ResourceNotFoundException;
import com.griddynamics.forge.market_presence_service.repository.JobPostingAnalyticEventRepository;
import com.griddynamics.forge.market_presence_service.repository.JobPostingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.griddynamics.forge.market_presence_service.entity.JobPostingAnalyticEvent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyticsServiceTest {

    @Mock private JobPostingAnalyticEventRepository eventRepo;
    @Mock private JobPostingRepository              jobRepo;

    @InjectMocks private AnalyticsService service;

    // ── record() ─────────────────────────────────────────────────────────────

    @Test
    void record_savesEvent_withKnownTypeAndChannel() {
        when(eventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.record(1L, VIEW, CH_CAREERS);

        ArgumentCaptor<JobPostingAnalyticEvent> cap = ArgumentCaptor.forClass(JobPostingAnalyticEvent.class);
        verify(eventRepo).save(cap.capture());
        assertThat(cap.getValue().getEventType()).isEqualTo(VIEW);
        assertThat(cap.getValue().getChannelName()).isEqualTo(CH_CAREERS);
        assertThat(cap.getValue().getJobPostingId()).isEqualTo(1L);
    }

    @Test
    void record_defaultsToCareerPortal_forUnknownChannel() {
        when(eventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.record(1L, VIEW, "UNKNOWN_CHANNEL");

        ArgumentCaptor<JobPostingAnalyticEvent> cap = ArgumentCaptor.forClass(JobPostingAnalyticEvent.class);
        verify(eventRepo).save(cap.capture());
        assertThat(cap.getValue().getChannelName()).isEqualTo(CH_CAREERS);
    }

    @Test
    void record_ignoresUnknownEventType_andDoesNotSave() {
        service.record(1L, "BAD_EVENT", CH_CAREERS);
        verify(eventRepo, never()).save(any());
    }

    @Test
    void record_neverThrows_whenRepoFails() {
        when(eventRepo.save(any())).thenThrow(new RuntimeException("DB error"));
        assertDoesNotThrow(() -> service.record(1L, VIEW, CH_CAREERS));
    }

    // ── getAnalytics() ────────────────────────────────────────────────────────

    @Test
    void getAnalytics_returnsAggregatedCountsPerChannel() {
        JobPosting job = makeJob(1L, "Senior Java Engineer", "senior-java");
        when(jobRepo.findById(1L)).thenReturn(Optional.of(job));

        when(eventRepo.countByJobPostingIdAndEventTypeAndChannelName(1L, VIEW,            CH_CAREERS)).thenReturn(10L);
        when(eventRepo.countByJobPostingIdAndEventTypeAndChannelName(1L, CLICK,           CH_CAREERS)).thenReturn(5L);
        when(eventRepo.countByJobPostingIdAndEventTypeAndChannelName(1L, APPLY_START,    CH_CAREERS)).thenReturn(3L);
        when(eventRepo.countByJobPostingIdAndEventTypeAndChannelName(1L, APPLY_COMPLETE, CH_CAREERS)).thenReturn(2L);

        JobPostingAnalyticsResponse resp = service.getAnalytics(1L);

        assertThat(resp.totalViews()).isEqualTo(10);
        assertThat(resp.totalClicks()).isEqualTo(5);
        assertThat(resp.totalApplyStarts()).isEqualTo(3);
        assertThat(resp.totalApplyCompletions()).isEqualTo(2);
        assertThat(resp.channels()).hasSize(3); // CAREERS_PORTAL, LINKEDIN, INDEED

        ChannelAnalyticsDto careers = resp.channels().stream()
                .filter(c -> CH_CAREERS.equals(c.channelName()))
                .findFirst().orElseThrow();
        assertThat(careers.views()).isEqualTo(10);
        assertThat(careers.clicks()).isEqualTo(5);
        assertThat(careers.applyStarts()).isEqualTo(3);
        assertThat(careers.applyCompletions()).isEqualTo(2);
    }

    @Test
    void getAnalytics_throws404_whenJobNotFound() {
        when(jobRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getAnalytics(99L));
    }

    @Test
    void getAllAnalytics_returnsOneEntryPerJob() {
        JobPosting job = makeJob(1L, "Dev", "dev");
        when(jobRepo.findAll()).thenReturn(List.of(job));

        List<JobPostingAnalyticsResponse> all = service.getAllAnalytics();

        assertThat(all).hasSize(1);
        assertThat(all.get(0).jobPostingId()).isEqualTo(1L);
        assertThat(all.get(0).jobTitle()).isEqualTo("Dev");
        assertThat(all.get(0).channels()).hasSize(3);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private JobPosting makeJob(Long id, String title, String slug) {
        JobPosting j = new JobPosting();
        ReflectionTestUtils.setField(j, "id", id);
        j.setTitle(title);
        j.setSlug(slug);
        return j;
    }
}
