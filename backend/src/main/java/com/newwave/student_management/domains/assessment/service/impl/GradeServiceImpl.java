package com.newwave.student_management.domains.assessment.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.assessment.dto.response.StudentGradeResponse;
import com.newwave.student_management.domains.assessment.dto.response.StudentTranscriptResponse;
import com.newwave.student_management.domains.assessment.entity.Grade;
import com.newwave.student_management.domains.assessment.repository.GradeRepository;
import com.newwave.student_management.domains.assessment.service.IGradeService;
import com.newwave.student_management.domains.profile.entity.Student;
import com.newwave.student_management.domains.profile.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements IGradeService {

    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StudentGradeResponse> getGradesBySemester(UUID userId, Integer semesterId) {
        Student student = studentRepository.findByUser_UserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

        List<Grade> grades = gradeRepository
                .findByEnrollment_Student_StudentIdAndEnrollment_ScheduledClass_Semester_SemesterId(
                        student.getStudentId(), semesterId);

        return grades.stream()
                .map(this::mapToGradeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StudentTranscriptResponse getTranscript(UUID userId) {
        Student student = studentRepository.findByUser_UserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

        List<Grade> allGrades = gradeRepository.findByEnrollment_Student_StudentId(student.getStudentId());

        List<StudentGradeResponse> gradeResponses = allGrades.stream()
                .map(this::mapToGradeResponse)
                .collect(Collectors.toList());

        BigDecimal cumulativeGpa = calculateGpa(allGrades);
        int totalCredits = allGrades.stream()
                .mapToInt(g -> g.getEnrollment().getScheduledClass().getCourse().getCredits())
                .sum();

        return StudentTranscriptResponse.builder()
                .grades(gradeResponses)
                .cumulativeGpa(cumulativeGpa)
                .totalCredits(totalCredits)
                .build();
    }

    private StudentGradeResponse mapToGradeResponse(Grade grade) {
        return StudentGradeResponse.builder()
                .courseCode(grade.getEnrollment().getScheduledClass().getCourse().getCode())
                .courseName(grade.getEnrollment().getScheduledClass().getCourse().getName())
                .credits(grade.getEnrollment().getScheduledClass().getCourse().getCredits())
                .grade(grade.getGradeValue())
                .grade4(convertToScale4(grade.getGradeValue()))
                .feedback(grade.getFeedback())
                .build();
    }

    private BigDecimal calculateGpa(List<Grade> grades) {
        if (grades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalPoints = BigDecimal.ZERO;
        int totalCredits = 0;

        for (Grade grade : grades) {
            BigDecimal gValue10 = grade.getGradeValue();
            if (gValue10 == null)
                continue;

            int credits = grade.getEnrollment().getScheduledClass().getCourse().getCredits();
            totalCredits += credits;

            // Chuyển đổi điểm hệ 10 sang hệ 4 theo thang điểm chuẩn
            BigDecimal gValue4 = convertToScale4(gValue10);

            totalPoints = totalPoints.add(gValue4.multiply(BigDecimal.valueOf(credits)));
        }

        if (totalCredits == 0)
            return BigDecimal.ZERO;

        return totalPoints.divide(BigDecimal.valueOf(totalCredits), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal convertToScale4(BigDecimal grade10) {
        if (grade10 == null)
            return BigDecimal.ZERO;
        double val = grade10.doubleValue();
        if (val >= 9.0)
            return BigDecimal.valueOf(4.0);
        if (val >= 8.5)
            return BigDecimal.valueOf(3.7);
        if (val >= 8.0)
            return BigDecimal.valueOf(3.5);
        if (val >= 7.0)
            return BigDecimal.valueOf(3.0);
        if (val >= 6.5)
            return BigDecimal.valueOf(2.5);
        if (val >= 5.5)
            return BigDecimal.valueOf(2.0);
        if (val >= 5.0)
            return BigDecimal.valueOf(1.5);
        if (val >= 4.0)
            return BigDecimal.valueOf(1.0);
        return BigDecimal.valueOf(0.0);
    }
}
