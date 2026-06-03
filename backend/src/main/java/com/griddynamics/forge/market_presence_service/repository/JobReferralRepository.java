package com.griddynamics.forge.market_presence_service.repository;

import com.griddynamics.forge.market_presence_service.entity.JobReferral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobReferralRepository extends JpaRepository<JobReferral, Long> {

    Optional<JobReferral> findByReferralCode(String referralCode);

    boolean existsByReferralCode(String referralCode);

    List<JobReferral> findByJobPostingId(Long jobPostingId);
}
