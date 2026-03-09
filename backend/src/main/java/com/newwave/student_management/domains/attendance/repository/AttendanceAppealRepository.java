package com.newwave.student_management.domains.attendance.repository;

import com.newwave.student_management.domains.attendance.entity.AttendanceAppeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceAppealRepository extends JpaRepository<AttendanceAppeal, UUID> {
    Optional<AttendanceAppeal> findByStudent_StudentIdAndScheduledClass_ClassIdAndAttendanceDate(
            UUID studentId, Integer classId, LocalDate attendanceDate);
}
