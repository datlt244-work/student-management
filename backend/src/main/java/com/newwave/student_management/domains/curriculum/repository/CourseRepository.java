package com.newwave.student_management.domains.curriculum.repository;

import com.newwave.student_management.domains.curriculum.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {

    boolean existsByCode(String code);

    long countByDepartment_DepartmentIdAndDeletedAtIsNull(Integer departmentId);
    
    long countByDepartment_DepartmentIdAndStatusAndDeletedAtIsNull(Integer departmentId, com.newwave.student_management.domains.curriculum.entity.CourseStatus status);

    long countByDeletedAtIsNull();

    @Query("""
            SELECT c.department.departmentId, COUNT(c)
            FROM Course c
            WHERE c.deletedAt IS NULL
              AND c.department IS NOT NULL
              AND c.department.departmentId IN :departmentIds
            GROUP BY c.department.departmentId
            """)
    List<Object[]> countActiveCoursesByDepartmentIds(@Param("departmentIds") List<Integer> departmentIds);

    @Query("""
            SELECT c
            FROM Course c
            LEFT JOIN FETCH c.department
            WHERE c.deletedAt IS NULL
              AND (:search IS NULL OR LOWER(c.name) LIKE :search OR LOWER(c.code) LIKE :search)
              AND (:status IS NULL OR c.status = :status)
              AND (:departmentId IS NULL OR c.department.departmentId = :departmentId)
            """)
    org.springframework.data.domain.Page<Course> searchAdminCourses(
            @Param("search") String search,
            @Param("status") com.newwave.student_management.domains.curriculum.entity.CourseStatus status,
            @Param("departmentId") Integer departmentId,
            org.springframework.data.domain.Pageable pageable
    );
}


