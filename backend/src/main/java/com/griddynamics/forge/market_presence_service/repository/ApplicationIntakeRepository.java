package com.griddynamics.forge.market_presence_service.repository;

import com.griddynamics.forge.market_presence_service.entity.ApplicationIntake;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationIntakeRepository extends JpaRepository<ApplicationIntake, Long> {

    boolean existsByJobPostingIdAndCandidateEmail(Long jobPostingId, String candidateEmail);

    List<ApplicationIntake> findByJobPostingId(Long jobPostingId);
}
