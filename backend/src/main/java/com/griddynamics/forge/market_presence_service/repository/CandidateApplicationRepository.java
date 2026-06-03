package com.griddynamics.forge.market_presence_service.repository;

import com.griddynamics.forge.market_presence_service.entity.CandidateApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateApplicationRepository extends JpaRepository<CandidateApplication, UUID> {
    List<CandidateApplication> findByCandidateIdOrderByAppliedDateDesc(UUID candidateId);
    boolean existsByJobIdAndCandidateId(Long jobId, UUID candidateId);
}
