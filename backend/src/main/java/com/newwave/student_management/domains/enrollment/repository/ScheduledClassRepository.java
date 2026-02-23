package com.newwave.student_management.domains.enrollment.repository;

import com.newwave.student_management.domains.enrollment.entity.ScheduledClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduledClassRepository
        extends JpaRepository<ScheduledClass, Integer>, JpaSpecificationExecutor<ScheduledClass> {
    long countBySemesterSemesterId(Integer semesterId);
}
