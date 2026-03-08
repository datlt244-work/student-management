package com.newwave.student_management.domains.assessment.repository;

import com.newwave.student_management.domains.assessment.entity.StudentScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentScoreRepository extends JpaRepository<StudentScore, Integer> {
    List<StudentScore> findByEnrollment_EnrollmentId(Integer enrollmentId);
}
