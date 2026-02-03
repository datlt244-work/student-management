package com.newwave.student_management.domains.enrollment.entity;

import com.newwave.student_management.common.entity.JpaBaseEntity;
import com.newwave.student_management.domains.profile.entity.Student;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_enrollments_student_class",
                        columnNames = {"student_id", "class_id"}
                )
        },
        indexes = {
                @Index(name = "idx_enrollments_student_id", columnList = "student_id"),
                @Index(name = "idx_enrollments_class_id", columnList = "class_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment extends JpaBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    private Integer enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private ScheduledClass scheduledClass;

    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;
}

