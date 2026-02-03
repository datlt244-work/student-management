package com.newwave.student_management.domains.curriculum.entity;

import com.newwave.student_management.common.entity.JpaBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course extends JpaBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Integer courseId;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "credits", nullable = false)
    private Integer credits;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}

