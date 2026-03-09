package com.newwave.student_management.domains.assessment.repository;

import com.newwave.student_management.domains.assessment.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExamRepository extends JpaRepository<Exam, UUID> {

    @Query("SELECT e FROM Exam e " +
            "JOIN e.scheduledClass sc " +
            "JOIN Enrollment en ON en.scheduledClass.classId = sc.classId " +
            "WHERE en.student.studentId = :studentId " +
            "AND sc.semester.semesterId = :semesterId " +
            "AND e.deletedAt IS NULL")
    List<Exam> findExamsByStudentAndSemester(@Param("studentId") UUID studentId,
            @Param("semesterId") Integer semesterId);
}
