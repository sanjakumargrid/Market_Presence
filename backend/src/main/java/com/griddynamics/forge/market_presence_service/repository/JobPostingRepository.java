package com.griddynamics.forge.market_presence_service.repository;

import com.griddynamics.forge.market_presence_service.entity.JobPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    Optional<JobPosting> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
            SELECT j FROM JobPosting j
            WHERE (:status IS NULL OR j.status = :status)
              AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', CAST(:location AS String), '%')))
              AND (:seniority IS NULL OR LOWER(j.seniority) = LOWER(CAST(:seniority AS String)))
              AND (:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', CAST(:title AS String), '%')))
            """)
    Page<JobPosting> findByFilters(
            @Param("status") String status,
            @Param("location") String location,
            @Param("seniority") String seniority,
            @Param("title") String title,
            Pageable pageable
    );
}
