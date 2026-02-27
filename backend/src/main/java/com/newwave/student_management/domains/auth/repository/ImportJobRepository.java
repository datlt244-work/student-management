package com.newwave.student_management.domains.auth.repository;

import com.newwave.student_management.domains.auth.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ImportJob i SET i.successCount = i.successCount + 1 WHERE i.id = :jobId")
    void incrementSuccessCount(@Param("jobId") UUID jobId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ImportJob i SET i.failureCount = i.failureCount + 1 WHERE i.id = :jobId")
    void incrementFailureCount(@Param("jobId") UUID jobId);
}
