package com.newwave.student_management.domains.assessment.repository;

import com.newwave.student_management.domains.assessment.entity.AssessmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentItemRepository extends JpaRepository<AssessmentItem, Integer> {
    List<AssessmentItem> findByCourse_CourseId(Integer courseId);
}
