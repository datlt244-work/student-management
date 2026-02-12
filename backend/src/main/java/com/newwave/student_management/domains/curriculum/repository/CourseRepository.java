package com.newwave.student_management.domains.curriculum.repository;

import com.newwave.student_management.domains.curriculum.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {

    long countByDepartment_DepartmentIdAndDeletedAtIsNull(Integer departmentId);

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
}


