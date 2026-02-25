package com.newwave.student_management.domains.profile.repository;

import com.newwave.student_management.domains.profile.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, UUID> {

    @Query("SELECT t FROM Teacher t " +
            "LEFT JOIN FETCH t.department " +
            "WHERE t.user.userId = :userId AND t.deletedAt IS NULL")
    Optional<Teacher> findByUserIdWithDepartment(@Param("userId") UUID userId);

    Optional<Teacher> findByUser_UserId(UUID userId);

    Optional<Teacher> findByUser_UserIdAndDeletedAtIsNull(UUID userId);

    boolean existsByTeacherCode(String teacherCode);

    boolean existsByTeacherCodeAndDeletedAtIsNull(String teacherCode);

    long countByDepartment_DepartmentIdAndDeletedAtIsNull(Integer departmentId);

    Optional<Teacher> findByTeacherIdAndDeletedAtIsNull(UUID teacherId);

    java.util.List<Teacher> findByDepartment_DepartmentIdAndDeletedAtIsNull(Integer departmentId);

    Optional<Teacher> findByTeacherCodeAndDeletedAtIsNull(String teacherCode);

    @Query("SELECT t FROM Teacher t WHERE t.deletedAt IS NULL AND " +
            "(LOWER(t.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.teacherCode) LIKE LOWER(CONCAT('%', :query, '%')))")
    java.util.List<Teacher> searchRecipients(@Param("query") String query,
            org.springframework.data.domain.Pageable pageable);
}
