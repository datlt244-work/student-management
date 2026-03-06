package com.newwave.student_management.domains.attendance.repository;

import com.newwave.student_management.domains.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByStudentStudentIdAndScheduledClassClassId(UUID studentId, Integer classId);

    List<Attendance> findByScheduledClassClassId(Integer classId);

    Optional<Attendance> findByStudentStudentIdAndScheduledClassClassIdAndDate(UUID studentId, Integer classId,
            LocalDate date);

}
