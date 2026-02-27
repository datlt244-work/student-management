package com.newwave.student_management.domains.auth.repository;

import com.newwave.student_management.domains.auth.entity.ImportJobError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ImportJobErrorRepository extends JpaRepository<ImportJobError, UUID> {
}
