package com.newwave.student_management.domains.enrollment.repository;

import com.newwave.student_management.domains.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {
    long countByScheduledClassClassId(Integer classId);

    List<Enrollment> findByScheduledClassClassId(Integer classId);

    boolean existsByScheduledClassClassIdAndStudentStudentId(Integer classId, UUID studentId);

    Optional<Enrollment> findByScheduledClassClassIdAndStudentStudentId(Integer classId, UUID studentId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.student.studentId = :studentId AND " +
            "e.scheduledClass.semester.semesterId = :semesterId AND e.scheduledClass.dayOfWeek = :dayOfWeek AND " +
            "((e.scheduledClass.startTime < :endTime AND e.scheduledClass.endTime > :startTime)) AND " +
            "e.scheduledClass.deletedAt IS NULL")
    long countStudentConflicts(UUID studentId, Integer semesterId, Integer dayOfWeek, LocalTime startTime,
            LocalTime endTime);
}
