package com.newwave.student_management.domains.profile.repository;

import com.newwave.student_management.domains.profile.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    @Query("SELECT s FROM Student s " +
            "LEFT JOIN FETCH s.department " +
            "WHERE s.user.userId = :userId AND s.deletedAt IS NULL")
    Optional<Student> findByUserIdWithDepartment(@Param("userId") UUID userId);

    Optional<Student> findByUser_UserId(UUID userId);

    Optional<Student> findByUser_UserIdAndDeletedAtIsNull(UUID userId);

    List<Student> findByDepartment_DepartmentIdAndDeletedAtIsNull(Integer departmentId);

    boolean existsByStudentCode(String studentCode);

    boolean existsByStudentCodeAndDeletedAtIsNull(String studentCode);

    long countByDepartment_DepartmentIdAndDeletedAtIsNull(Integer departmentId);

    Optional<Student> findByStudentCodeAndDeletedAtIsNull(String studentCode);

    @Query("SELECT s FROM Student s WHERE s.deletedAt IS NULL AND " +
            "(LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Student> searchRecipients(@Param("query") String query, org.springframework.data.domain.Pageable pageable);
}
