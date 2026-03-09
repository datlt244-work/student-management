package com.newwave.student_management.domains.attendance.entity;

import com.newwave.student_management.common.entity.JpaBaseEntity;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClass;
import com.newwave.student_management.domains.profile.entity.Student;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "attendance_appeals", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "student_id", "class_id", "attendance_date" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceAppeal extends JpaBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID appealId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ScheduledClass scheduledClass;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "evidence_url", nullable = false)
    private String evidenceUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AppealStatus status;

    @Column(name = "teacher_response", columnDefinition = "TEXT")
    private String teacherResponse;

    public enum AppealStatus {
        PENDING, APPROVED, REJECTED
    }
}
