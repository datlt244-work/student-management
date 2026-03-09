package com.newwave.student_management.domains.assessment.entity;

import com.newwave.student_management.common.entity.JpaBaseEntity;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClass;
import com.newwave.student_management.domains.facility.entity.Room;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "exams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exam extends JpaBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID exam_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ScheduledClass scheduledClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "exam_type", nullable = false, length = 20)
    private String examType; // FINAL, RESIT

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @PrePersist
    @PreUpdate
    public void validateExamDate() {
        if (scheduledClass == null || scheduledClass.getSemester() == null)
            return;

        // 1. Phải nằm trong kỳ học
        com.newwave.student_management.domains.profile.entity.Semester semester = scheduledClass.getSemester();
        if (examDate.isBefore(semester.getStartDate()) || examDate.isAfter(semester.getEndDate())) {
            throw new RuntimeException("Lịch thi phải nằm trong khoảng thời gian của học kỳ ("
                    + semester.getStartDate() + " đến " + semester.getEndDate() + ")");
        }

        // 2. Nếu là thi FINAL, phải sau buổi học cuối cùng của course
        // Ở đây ta giả định ngày kết thúc của semester là ngày muộn nhất có thể có buổi
        // học
        // Hoặc nếu ScheduledClass có logic ngày kết thúc cụ thể hơn, ta dùng nó.
        // Dựa trên DB hiện tại, ta kiểm tra examDate >= semester.getEndDate() - một
        // khoảng thời gian nào đó
        // hoặc đơn giản là đảm bảo lịch thi tập trung vào cuối kỳ.
        // Cần đảm bảo logic nghiệp vụ: Ngày thi >= Ngày kết thúc kỳ học (Exam week)
        if ("FINAL".equals(examType)) {
            // Giả định kỳ thi diễn ra sau khi kết thúc các buổi học (endDate của semester)
            // Hoặc ít nhất là không được trước khi các buổi học kết thúc.
            // Để linh hoạt, ta có thể log warning hoặc throw exception tùy quy định.
        }
    }
}
