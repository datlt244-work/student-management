package com.newwave.student_management.domains.profile.repository;

import com.newwave.student_management.domains.profile.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    @Query("""
            SELECT d
            FROM Department d
            WHERE d.deletedAt IS NULL
              AND (:search IS NULL OR LOWER(d.name) LIKE :search)
            """)
    Page<Department> searchAdminDepartments(
            @Param("search") String search,
            Pageable pageable
    );
}
