
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

        @Query("SELECT COUNT(sc) FROM ScheduledClass sc JOIN sc.sessions sess WHERE sc.teacher.teacherId = :teacherId AND "
                        +
                        "sc.semester.semesterId = :semesterId AND sess.dayOfWeek = :dayOfWeek AND " +
                        "(:excludeClassId IS NULL OR sc.classId <> :excludeClassId) AND " +
                        "((sess.startTime < :endTime AND sess.endTime > :startTime)) AND sc.deletedAt IS NULL")
        long countOverlappingClasses(UUID teacherId, Integer semesterId, Integer dayOfWeek, LocalTime startTime,
                        LocalTime endTime, Integer excludeClassId);

        @Query("SELECT sc FROM ScheduledClass sc " +
                        "WHERE sc.course.department.departmentId = :departmentId " +
                        "AND sc.semester.semesterId = :semesterId " +
                        "AND sc.status = 'OPEN' " +
                        "AND sc.deletedAt IS NULL " +
                        "AND sc.classId NOT IN (SELECT e.scheduledClass.classId FROM Enrollment e WHERE e.student.studentId = :studentId)")
        List<ScheduledClass> findAvailableClassesForStudent(Integer departmentId, Integer semesterId, UUID studentId);
}
