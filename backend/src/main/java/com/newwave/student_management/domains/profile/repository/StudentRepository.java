package com.newwave.student_management.domains.profile.repository;

import com.newwave.student_management.domains.profile.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    boolean existsByStudentCode(String studentCode);

    boolean existsByStudentCodeAndDeletedAtIsNull(String studentCode);

    long countByDepartment_DepartmentIdAndDeletedAtIsNull(Integer departmentId);
}
