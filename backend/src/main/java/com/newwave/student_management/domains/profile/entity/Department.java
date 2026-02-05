package com.newwave.student_management.domains.profile.entity;

import com.newwave.student_management.common.entity.JpaBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Department extends JpaBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Integer departmentId;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "office_location", length = 100)
    private String officeLocation;
}

