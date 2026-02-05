package com.newwave.student_management.domains.enrollment.entity;

import com.newwave.student_management.common.entity.JpaBaseEntity;
import com.newwave.student_management.domains.curriculum.entity.Course;
import com.newwave.student_management.domains.profile.entity.Teacher;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scheduled_classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledClass extends JpaBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Integer classId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Column(name = "semester", length = 20, nullable = false)
    private String semester;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "room_number", length = 20)
    private String roomNumber;

    /**
     * Ví dụ: "Mon 8:00-10:00"
     */
    @Column(name = "schedule", length = 50)
    private String schedule;
}

