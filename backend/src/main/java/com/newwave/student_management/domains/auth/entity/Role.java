package com.newwave.student_management.domains.auth.entity;

import com.newwave.student_management.common.entity.JpaBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roles")
public class Role extends JpaBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private int roleId;

    @Column(name = "role_name", length = 20, unique = true, nullable = false)
    private String roleName;

    @Column(columnDefinition = "TEXT")
    private String description;
}
