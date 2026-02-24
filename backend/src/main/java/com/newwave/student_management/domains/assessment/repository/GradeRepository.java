package com.newwave.student_management.domains.assessment.repository;

import com.newwave.student_management.domains.assessment.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Integer> {

    List<Grade> findByEnrollment_Student_StudentIdAndEnrollment_ScheduledClass_Semester_SemesterId(
            UUID studentId, Integer semesterId);

    List<Grade> findByEnrollment_Student_StudentId(UUID studentId);

    Optional<Grade> findByEnrollment_EnrollmentId(Integer enrollmentId);
}
