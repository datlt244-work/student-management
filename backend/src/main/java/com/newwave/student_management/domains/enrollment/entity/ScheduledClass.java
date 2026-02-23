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

import java.time.LocalTime;

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
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "teacher_id", nullable = false)
        private Teacher teacher;

        /**
         * Học kỳ. FK đến bảng semesters.
         * Lấy tên kỳ, năm, ngày bắt đầu/kết thúc qua JOIN.
         * FK: ON DELETE RESTRICT — không xóa semester nếu còn lớp.
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "semester_id")
        private Semester semester;

        /**
         * Phòng học, ví dụ: "A-101".
         */
        @Column(name = "room_number", length = 20)
        private String roomNumber;

        /**
         * Thứ trong tuần (1-7 = Thứ 2 - Chủ Nhật).
         */
        @Column(name = "day_of_week", nullable = false)
        private Integer dayOfWeek;

        /**
         * Giờ bắt đầu.
         */
        @Column(name = "start_time", nullable = false)
        private LocalTime startTime;

        /**
         * Giờ kết thúc.
         */
        @Column(name = "end_time", nullable = false)
        private LocalTime endTime;

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
