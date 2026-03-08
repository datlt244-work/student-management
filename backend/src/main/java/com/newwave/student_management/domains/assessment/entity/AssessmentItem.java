package com.newwave.student_management.domains.assessment.entity;

import com.newwave.student_management.common.entity.JpaBaseEntity;
import com.newwave.student_management.domains.curriculum.entity.Course;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "assessment_items")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentItem extends JpaBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer itemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "category", nullable = false)
    private String category; // VD: Participation, Progress test, Assignment, Final exam

    @Column(name = "name", nullable = false)
    private String name; // VD: Participation, Assignment 1, Assignment 2, Final exam

    @Column(name = "weight", precision = 5, scale = 2, nullable = false)
    private BigDecimal weight; // Trọng số (%)

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
