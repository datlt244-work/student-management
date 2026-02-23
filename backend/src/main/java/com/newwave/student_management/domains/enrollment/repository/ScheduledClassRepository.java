package com.newwave.student_management.domains.enrollment.repository;

import com.newwave.student_management.domains.enrollment.entity.ScheduledClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.UUID;

@Repository
public interface ScheduledClassRepository
        extends JpaRepository<ScheduledClass, Integer>, JpaSpecificationExecutor<ScheduledClass> {
    long countBySemesterSemesterId(Integer semesterId);

    @Query("SELECT COUNT(sc) FROM ScheduledClass sc WHERE sc.teacher.teacherId = :teacherId AND " +
            "sc.semester.semesterId = :semesterId AND sc.dayOfWeek = :dayOfWeek AND " +
            "((sc.startTime < :endTime AND sc.endTime > :startTime))")
    long countOverlappingClasses(UUID teacherId, Integer semesterId, Integer dayOfWeek, LocalTime startTime,
            LocalTime endTime);
}
