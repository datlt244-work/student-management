package com.newwave.student_management.domains.profile.entity;

import com.newwave.student_management.common.entity.JpaBaseEntity;
import com.newwave.student_management.domains.auth.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "teachers", indexes = {
        @Index(name = "idx_teachers_email", columnList = "email"),
        @Index(name = "idx_teachers_teacher_code", columnList = "teacher_code")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Teacher extends JpaBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "teacher_id")
    private UUID teacherId;

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
     * Mã giáo viên dạng HJxxxxxx (ví dụ: HJ170001)
     */
    @Column(name = "teacher_code", length = 10, unique = true)
    private String teacherCode;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "specialization", length = 100)
    private String specialization;

    @Column(name = "academic_rank", length = 50)
    private String academicRank;

    @Column(name = "office_room", length = 50)
    private String officeRoom;

    /**
     * Degrees / qualifications (free text, e.g. "Ph.D. Mathematics - Stanford;
     * M.Sc. Statistics - MIT").
     */
    @Column(name = "degrees_qualification", columnDefinition = "TEXT")
    private String degreesQualification;
}
