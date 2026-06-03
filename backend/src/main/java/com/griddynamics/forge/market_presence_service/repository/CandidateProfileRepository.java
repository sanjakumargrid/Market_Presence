package com.griddynamics.forge.market_presence_service.repository;

import com.griddynamics.forge.market_presence_service.entity.CandidateProfile;
import com.griddynamics.forge.market_presence_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {
    Optional<CandidateProfile> findByUser(User user);
}
