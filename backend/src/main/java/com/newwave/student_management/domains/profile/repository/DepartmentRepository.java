package com.newwave.student_management.domains.profile.repository;

import com.newwave.student_management.domains.profile.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {
}
