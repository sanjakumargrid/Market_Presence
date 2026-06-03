package com.griddynamics.forge.market_presence_service.service;

import com.griddynamics.forge.market_presence_service.dto.JobReferralResponse;
import com.griddynamics.forge.market_presence_service.entity.JobPosting;
import com.griddynamics.forge.market_presence_service.entity.JobReferral;
import com.griddynamics.forge.market_presence_service.exception.ResourceNotFoundException;
import com.griddynamics.forge.market_presence_service.repository.JobPostingRepository;
import com.griddynamics.forge.market_presence_service.repository.JobReferralRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobReferralServiceTest {

    @Mock private JobReferralRepository  referralRepository;
    @Mock private JobPostingRepository   jobPostingRepository;

    private JobReferralService service;

    private JobPosting sampleJob;

    @BeforeEach
    void setUp() {
        service = new JobReferralService(
                referralRepository, jobPostingRepository,
                "http://localhost:5173");

        sampleJob = new JobPosting();
        sampleJob.setSlug("react-frontend-engineer-bangalore");
        sampleJob.setTitle("React Frontend Engineer");

        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(jobPostingRepository.existsById(1L)).thenReturn(true);
        when(referralRepository.existsByReferralCode(anyString())).thenReturn(false);
        when(referralRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── generateLink ──────────────────────────────────────────────────────────

    @Test
    void generateLink_returnsReferralWithShareableUrl() {
        JobReferralResponse result = service.generateLink(1L, 1001L);

        assertThat(result.referralCode()).isNotBlank();
        assertThat(result.referralUrl())
                .startsWith("http://localhost:5173/careers/react-frontend-engineer-bangalore?ref=");
        assertThat(result.referrerId()).isEqualTo(1001L);
        assertThat(result.status()).isEqualTo("PENDING");
    }

    @Test
    void generateLink_throws404_whenJobNotFound() {
        when(jobPostingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateLink(99L, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void generateLink_usesPlaceholderCandidate() {
        service.generateLink(1L, 1001L);

        ArgumentCaptor<JobReferral> captor = ArgumentCaptor.forClass(JobReferral.class);
        verify(referralRepository).save(captor.capture());

        assertThat(captor.getValue().getReferredCandidateName()).isEqualTo("PENDING");
        assertThat(captor.getValue().getReferredCandidateEmail()).isEqualTo("pending@referral.tbd");
    }

    // ── resolveSource ─────────────────────────────────────────────────────────

    @Test
    void resolveSource_returnsReferral_whenCodeIsValidForJob() {
        JobReferral referral = new JobReferral();
        referral.setJobPostingId(1L);
        referral.setReferralCode("VALIDCODE1");
        when(referralRepository.findByReferralCode("VALIDCODE1")).thenReturn(Optional.of(referral));

        String source = service.resolveSource("VALIDCODE1", 1L);

        assertThat(source).isEqualTo("REFERRAL");
    }

    @Test
    void resolveSource_returnsCareersPortal_whenCodeIsAbsent() {
        assertThat(service.resolveSource(null, 1L)).isEqualTo("CAREERS_PORTAL");
        assertThat(service.resolveSource("", 1L)).isEqualTo("CAREERS_PORTAL");
    }

    @Test
    void resolveSource_returnsCareersPortal_whenCodeUnknown() {
        when(referralRepository.findByReferralCode("BADCODE")).thenReturn(Optional.empty());

        assertThat(service.resolveSource("BADCODE", 1L)).isEqualTo("CAREERS_PORTAL");
    }

    @Test
    void resolveSource_returnsCareersPortal_whenCodeBelongsToDifferentJob() {
        JobReferral referral = new JobReferral();
        referral.setJobPostingId(99L); // different job
        referral.setReferralCode("WRONGJOB1");
        when(referralRepository.findByReferralCode("WRONGJOB1")).thenReturn(Optional.of(referral));

        assertThat(service.resolveSource("WRONGJOB1", 1L)).isEqualTo("CAREERS_PORTAL");
    }

    // ── markApplied ───────────────────────────────────────────────────────────

    @Test
    void markApplied_updatesReferralStatusAndCandidateDetails() {
        JobReferral referral = new JobReferral();
        referral.setReferralCode("VALIDCODE1");
        referral.setReferredCandidateName("PENDING");
        referral.setReferredCandidateEmail("pending@referral.tbd");
        when(referralRepository.findByReferralCode("VALIDCODE1")).thenReturn(Optional.of(referral));

        service.markApplied("VALIDCODE1", "Priya Sharma", "priya.sharma@example.com");

        ArgumentCaptor<JobReferral> captor = ArgumentCaptor.forClass(JobReferral.class);
        verify(referralRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo("APPLIED");
        assertThat(captor.getValue().getReferredCandidateName()).isEqualTo("Priya Sharma");
        assertThat(captor.getValue().getReferredCandidateEmail()).isEqualTo("priya.sharma@example.com");
    }

    @Test
    void markApplied_isNoOp_whenCodeIsNull() {
        service.markApplied(null, "Name", "email@example.com");

        verifyNoInteractions(referralRepository);
    }
}
