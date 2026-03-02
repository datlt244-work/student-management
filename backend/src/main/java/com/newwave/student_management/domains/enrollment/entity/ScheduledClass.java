package com.newwave.student_management.domains.enrollment.entity;

import com.newwave.student_management.common.entity.JpaBaseEntity;
import com.newwave.student_management.domains.curriculum.entity.Course;
import com.newwave.student_management.domains.profile.entity.Semester;
import com.newwave.student_management.domains.profile.entity.Teacher;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scheduled_classes", indexes = {
                @Index(name = "idx_scheduled_classes_course_id", columnList = "course_id"),
                @Index(name = "idx_scheduled_classes_teacher_id", columnList = "teacher_id"),
                @Index(name = "idx_scheduled_classes_semester_id", columnList = "semester_id")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledClass extends JpaBaseEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "class_id")
        private Integer classId;

        /**
         * Môn học của lớp này. Required.
         * FK: ON DELETE RESTRICT — không thể xóa course nếu còn lớp.
         */
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "course_id", nullable = false)
        private Course course;

        /**
         * Giảng viên phụ trách. Optional — lớp có thể chưa assign teacher.
         * FK: ON DELETE SET NULL.
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "teacher_id")
        private Teacher teacher;

        /**
         * Học kỳ. FK đến bảng semesters.
         * Lấy tên kỳ, năm, ngày bắt đầu/kết thúc qua JOIN.
         * FK: ON DELETE RESTRICT — không xóa semester nếu còn lớp.
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "semester_id")
        private Semester semester;

        @OneToMany(mappedBy = "scheduledClass", cascade = CascadeType.ALL, orphanRemoval = true)
        private java.util.List<ClassSession> sessions = new java.util.ArrayList<>();

        /**
         * Trạng thái lớp: OPEN | CLOSED | CANCELLED.
         */
        @Enumerated(EnumType.STRING)
        @Column(name = "status", length = 20, nullable = false)
        private ScheduledClassStatus status = ScheduledClassStatus.OPEN;

        /**
         * Sĩ số tối đa. Mặc định 40.
         */
        @Column(name = "max_students")
        private Integer maxStudents = 40;
}
