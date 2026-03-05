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
        @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.scheduledClass.classId = :classId AND e.status = 'ENROLLED'")
        long countByScheduledClassClassId(Integer classId);

        @Query("SELECT e FROM Enrollment e WHERE e.scheduledClass.classId = :classId AND e.status = 'ENROLLED'")
        List<Enrollment> findByScheduledClassClassId(Integer classId);

        List<Enrollment> findAllByScheduledClassClassId(Integer classId);

        @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Enrollment e WHERE e.scheduledClass.classId = :classId AND e.student.studentId = :studentId AND e.status = 'ENROLLED'")
        boolean existsByScheduledClassClassIdAndStudentStudentId(Integer classId, UUID studentId);

        @Query("SELECT e FROM Enrollment e WHERE e.scheduledClass.classId = :classId AND e.student.studentId = :studentId AND e.status = 'ENROLLED'")
        Optional<Enrollment> findByScheduledClassClassIdAndStudentStudentId(Integer classId, UUID studentId);

        @Query("SELECT e FROM Enrollment e WHERE e.scheduledClass.classId = :classId AND e.student.studentId = :studentId AND e.status = 'WAITLISTED'")
        Optional<Enrollment> findByScheduledClassClassIdAndStudentStudentIdWaitlisted(Integer classId, UUID studentId);

        @Query("SELECT COUNT(e) FROM Enrollment e JOIN e.scheduledClass.sessions sess WHERE e.student.studentId = :studentId AND "
                        +
                        "e.scheduledClass.semester.semesterId = :semesterId AND sess.dayOfWeek = :dayOfWeek AND " +
                        "((sess.startTime < :endTime AND sess.endTime > :startTime)) AND " +
                        "e.scheduledClass.deletedAt IS NULL AND e.status = 'ENROLLED'")
        long countStudentConflicts(UUID studentId, Integer semesterId, Integer dayOfWeek, LocalTime startTime,
                        LocalTime endTime);

        @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Enrollment e WHERE e.student.studentId = :studentId AND e.scheduledClass.course.courseId = :courseId AND e.scheduledClass.semester.semesterId = :semesterId AND e.scheduledClass.deletedAt IS NULL AND e.status = 'ENROLLED'")
        boolean existsByStudentStudentIdAndScheduledClassCourseCourseIdAndScheduledClassSemesterSemesterIdAndScheduledClassDeletedAtIsNull(
                        UUID studentId, Integer courseId, Integer semesterId);

        @Query("SELECT e FROM Enrollment e WHERE e.student.studentId = :studentId AND e.scheduledClass.semester.semesterId = :semesterId AND e.scheduledClass.deletedAt IS NULL AND e.status = 'ENROLLED'")
        List<Enrollment> findByStudentStudentIdAndScheduledClassSemesterSemesterIdAndScheduledClassDeletedAtIsNull(
                        UUID studentId, Integer semesterId);

        /**
         * Lấy danh sách classId mà SV đã đăng ký trong 1 semester.
         * Dùng cho cache read path: chỉ lấy ID, rất nhẹ.
         */
        @Query("SELECT e.scheduledClass.classId FROM Enrollment e " +
                        "WHERE e.student.studentId = :studentId " +
                        "AND e.scheduledClass.semester.semesterId = :semesterId " +
                        "AND e.scheduledClass.deletedAt IS NULL AND e.status = 'ENROLLED'")
        java.util.Set<Integer> findClassIdsByStudentIdAndSemesterId(UUID studentId, Integer semesterId);
}
