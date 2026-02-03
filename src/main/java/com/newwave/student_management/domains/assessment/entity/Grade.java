package com.newwave.student_management.domains.assessment.entity;

import com.newwave.student_management.common.entity.JpaBaseEntity;
import com.newwave.student_management.domains.enrollment.entity.Enrollment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "grades")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Grade extends JpaBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_id")
    private Integer gradeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Column(name = "grade_value", precision = 4, scale = 2)
    private BigDecimal gradeValue;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;
}

