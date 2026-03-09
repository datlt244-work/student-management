package com.newwave.student_management.domains.assessment.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.assessment.dto.response.ExamResponse;
import com.newwave.student_management.domains.assessment.entity.Exam;
import com.newwave.student_management.domains.assessment.repository.ExamRepository;
import com.newwave.student_management.domains.assessment.service.IExamService;
import com.newwave.student_management.domains.attendance.entity.Attendance;
import com.newwave.student_management.domains.attendance.entity.AttendanceStatus;
import com.newwave.student_management.domains.attendance.repository.AttendanceRepository;
import com.newwave.student_management.domains.enrollment.entity.ClassSession;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClass;
import com.newwave.student_management.domains.profile.entity.Semester;
import com.newwave.student_management.domains.profile.entity.Student;
import com.newwave.student_management.domains.profile.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements IExamService {

    private final ExamRepository examRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ExamResponse> getStudentExamSchedule(UUID userId, Integer semesterId) {
        Student student = studentRepository.findByUser_UserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

        List<Exam> exams = examRepository.findExamsByStudentAndSemester(student.getStudentId(), semesterId);

        return exams.stream().map(exam -> {
            ScheduledClass scheduledClass = exam.getScheduledClass();
            Semester semester = scheduledClass.getSemester();

            // Calculate eligibility
            List<Attendance> attendances = attendanceRepository.findByStudentStudentIdAndScheduledClassClassId(
                    student.getStudentId(), scheduledClass.getClassId());

            long totalAbsent = attendances.stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                    .count();

            // Calculate total expected sessions
            int totalSessions = calculateTotalSessions(scheduledClass);
            double absentPercentage = totalSessions > 0 ? (double) totalAbsent / totalSessions * 100 : 0;
            boolean isEligible = absentPercentage < 20;

            return ExamResponse.builder()
                    .examId(exam.getExam_id())
                    .classId(scheduledClass.getClassId())
                    .courseCode(scheduledClass.getCourse().getCode())
                    .courseName(scheduledClass.getCourse().getName())
                    .roomNumber(exam.getRoom() != null ? exam.getRoom().getName() : "TBD")
                    .examDate(exam.getExamDate())
                    .startTime(exam.getStartTime())
                    .endTime(exam.getEndTime())
                    .examType(exam.getExamType())
                    .note(exam.getNote())
                    .semesterStartDate(semester != null ? semester.getStartDate() : null)
                    .semesterEndDate(semester != null ? semester.getEndDate() : null)
                    .isEligible(isEligible)
                    .totalAbsent((int) totalAbsent)
                    .absentPercentage(absentPercentage)
                    .reason(!isEligible ? "Vắng mặt quá 20% (" + String.format("%.1f", absentPercentage) + "%)" : null)
                    .build();
        }).collect(Collectors.toList());
    }

    private int calculateTotalSessions(ScheduledClass scheduledClass) {
        Semester semester = scheduledClass.getSemester();
        if (semester == null || semester.getStartDate() == null || semester.getEndDate() == null)
            return 0;

        List<ClassSession> sessions = scheduledClass.getSessions();
        if (sessions == null || sessions.isEmpty())
            return 0;

        int count = 0;
        for (ClassSession session : sessions) {
            Integer targetDay = session.getDayOfWeek();
            LocalDate current = semester.getStartDate();
            while (!current.isAfter(semester.getEndDate())) {
                if (current.getDayOfWeek().getValue() == targetDay) {
                    count++;
                }
                current = current.plusDays(1);
            }
        }
        return count;
    }
}
