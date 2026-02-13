package com.newwave.student_management.domains.profile.repository;

import com.newwave.student_management.domains.profile.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    @Query("""
            SELECT d
            FROM Department d
            WHERE d.deletedAt IS NULL
              AND (:search IS NULL OR LOWER(d.name) LIKE :search)
              AND (:status IS NULL OR d.status = :status)
            """)
    Page<Department> searchAdminDepartments(
            @Param("search") String search,
            @Param("status") com.newwave.student_management.domains.profile.entity.DepartmentStatus status,
            Pageable pageable
    );

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    Optional<Department> findByDepartmentIdAndDeletedAtIsNull(Integer departmentId);

    java.util.List<Department> findAllByStatusAndDeletedAtIsNull(com.newwave.student_management.domains.profile.entity.DepartmentStatus status);
}
