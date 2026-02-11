package com.newwave.student_management.domains.profile.entity;

import com.newwave.student_management.common.entity.JpaBaseEntity;
import com.newwave.student_management.domains.auth.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "students", indexes = {
        @Index(name = "idx_students_email", columnList = "email"),
        @Index(name = "idx_students_student_code", columnList = "student_code")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student extends JpaBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "student_id")
    private UUID studentId;

    /**
     * Quan hệ 1-1 với User (UUID).
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /**
     * Mã sinh viên dạng HExxxxxx (ví dụ: HE170001)
     */
    @Column(name = "student_code", length = 10, unique = true)
    private String studentCode;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "major", length = 100)
    private String major;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;

    /**
     * GPA trên thang 4.0
     */
    @Column(name = "gpa", precision = 3, scale = 2)
    private BigDecimal gpa;

    /**
     * Year of study (e.g. 1, 2, 3, 4).
     */
    @Column(name = "year")
    private Integer year;

    /**
     * Manage class / class code (e.g. SE1701, IT-01).
     */
    @Column(name = "manage_class", length = 50)
    private String manageClass;
}

