package com.griddynamics.forge.market_presence_service.repository;

import com.griddynamics.forge.market_presence_service.entity.ExternalCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalCandidateRepository extends JpaRepository<ExternalCandidate, UUID> {
    Optional<ExternalCandidate> findByEmail(String email);
    boolean existsByEmail(String email);
}
