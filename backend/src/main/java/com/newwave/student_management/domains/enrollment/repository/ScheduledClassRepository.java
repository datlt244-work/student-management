
package com.newwave.student_management.domains.enrollment.repository;

import com.newwave.student_management.domains.enrollment.entity.ScheduledClass;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.UUID;

@Repository
public interface ScheduledClassRepository
        extends JpaRepository<ScheduledClass, Integer>, JpaSpecificationExecutor<ScheduledClass> {
    List<ScheduledClass> findByCourseCourseIdAndDeletedAtIsNull(Integer courseId);

    Optional<ScheduledClass> findByClassIdAndDeletedAtIsNull(Integer classId);

    long countBySemesterSemesterId(Integer semesterId);

    @Query("SELECT COUNT(sc) FROM ScheduledClass sc WHERE sc.teacher.teacherId = :teacherId AND " +
            "sc.semester.semesterId = :semesterId AND sc.dayOfWeek = :dayOfWeek AND " +
            "(:excludeClassId IS NULL OR sc.classId <> :excludeClassId) AND " +
            "((sc.startTime < :endTime AND sc.endTime > :startTime)) AND sc.deletedAt IS NULL")
    long countOverlappingClasses(UUID teacherId, Integer semesterId, Integer dayOfWeek, LocalTime startTime,
            LocalTime endTime, Integer excludeClassId);
}
