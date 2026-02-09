package com.newwave.student_management.domains.profile.repository;

import com.newwave.student_management.domains.profile.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Integer> {

    /**
     * Tìm semester hiện tại (is_current = true)
     */
    Optional<Semester> findByIsCurrentTrue();
}
