package com.griddynamics.forge.market_presence_service.repository;

import com.griddynamics.forge.market_presence_service.entity.HandoffRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HandoffRecordRepository extends JpaRepository<HandoffRecord, Long> {

    Page<HandoffRecord> findByStatus(String status, Pageable pageable);

    Page<HandoffRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<HandoffRecord> findByApplicationIntakeId(Long applicationIntakeId);

    /** Non-paginated — used by bulk retry to load all PENDING records at once. */
    List<HandoffRecord> findByStatus(String status);
}
