
package com.newwave.student_management.domains.enrollment.repository;

import com.newwave.student_management.domains.enrollment.entity.ScheduledClass;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.UUID;

@Repository
public interface ScheduledClassRepository
                extends JpaRepository<ScheduledClass, Integer>, JpaSpecificationExecutor<ScheduledClass> {
        List<ScheduledClass> findByCourseCourseIdAndDeletedAtIsNull(Integer courseId);

        Optional<ScheduledClass> findByClassIdAndDeletedAtIsNull(Integer classId);

        /**
         * Lock PESSIMISTIC_WRITE — dùng cho fallback capacity check khi Redis down.
         * SELECT ... FOR UPDATE bảo đảm chỉ 1 thread đọc+ghi số chỗ tại một thời điểm.
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @QueryHints({ @jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000") })
        @Query("SELECT sc FROM ScheduledClass sc WHERE sc.classId = :classId AND sc.deletedAt IS NULL")
        Optional<ScheduledClass> findByClassIdWithLock(Integer classId);

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

        /**
         * Lấy tất cả lớp OPEN (chưa bị xóa) trong 1 semester.
         * Dùng cho ClassCacheService để sync lên Redis khi Admin publish.
         */
        List<ScheduledClass> findBySemesterSemesterIdAndStatusAndDeletedAtIsNull(
                        Integer semesterId,
                        com.newwave.student_management.domains.enrollment.entity.ScheduledClassStatus status);

        /**
         * Batch count enroll cho danh sách classId — tránh N+1 query trong fallback
         * path.
         * Trả về mảng Object[]{classId (Integer), count (Long)} cho mỗi lớp.
         */
        @Query("SELECT e.scheduledClass.classId, COUNT(e) FROM Enrollment e "
                        + "WHERE e.scheduledClass.classId IN :classIds "
                        + "GROUP BY e.scheduledClass.classId")
        List<Object[]> countEnrollmentsByClassIds(
                        @org.springframework.data.repository.query.Param("classIds") List<Integer> classIds);
}
