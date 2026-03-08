package com.newwave.student_management.domains.assessment.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.assessment.dto.response.StudentAssessmentScoreResponse;
import com.newwave.student_management.domains.assessment.dto.response.StudentGradeResponse;
import com.newwave.student_management.domains.assessment.dto.response.StudentTranscriptResponse;
import com.newwave.student_management.domains.assessment.entity.AssessmentItem;
import com.newwave.student_management.domains.assessment.entity.Grade;
import com.newwave.student_management.domains.assessment.entity.StudentScore;
import com.newwave.student_management.domains.assessment.repository.AssessmentItemRepository;
import com.newwave.student_management.domains.assessment.repository.GradeRepository;
import com.newwave.student_management.domains.assessment.repository.StudentScoreRepository;
import com.newwave.student_management.domains.assessment.service.IGradeService;
import com.newwave.student_management.domains.enrollment.entity.Enrollment;
import com.newwave.student_management.domains.enrollment.repository.EnrollmentRepository;
import com.newwave.student_management.domains.profile.entity.Student;
import com.newwave.student_management.domains.profile.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements IGradeService {

    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;
    private final AssessmentItemRepository assessmentItemRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StudentGradeResponse> getGradesBySemester(UUID userId, Integer semesterId) {
        Student student = studentRepository.findByUser_UserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

        // Lấy tất cả Enrollment của sinh viên trong kỳ này (bao gồm cả lớp đang học
        // chưa có điểm)
        List<Enrollment> enrollments = enrollmentRepository
                .findByStudentStudentIdAndScheduledClassSemesterSemesterIdAndScheduledClassDeletedAtIsNull(
                        student.getStudentId(), semesterId);

        return enrollments.stream()
                .map(this::mapEnrollmentToGradeResponse)
                .collect(Collectors.toList());
    }

    private StudentGradeResponse mapEnrollmentToGradeResponse(Enrollment enrollment) {
        // Tìm Grade thực tế nếu có
        Grade grade = gradeRepository.findByEnrollment_EnrollmentId(enrollment.getEnrollmentId())
                .orElse(null);

        return mapToGradeResponseDetailed(enrollment, grade);
    }

    private StudentGradeResponse mapToGradeResponseDetailed(Enrollment enrollment, Grade grade) {

        // Lấy chi tiết điểm thành phần
        List<AssessmentItem> assessmentItems = assessmentItemRepository
                .findByCourse_CourseId(enrollment.getScheduledClass().getCourse().getCourseId());

        List<StudentScore> studentScores = studentScoreRepository
                .findByEnrollment_EnrollmentId(enrollment.getEnrollmentId());

        Map<Integer, StudentScore> scoreMap = studentScores.stream()
                .collect(Collectors.toMap(s -> s.getAssessmentItem().getItemId(), s -> s));

        List<StudentAssessmentScoreResponse> detailedScores = new ArrayList<>();

        // Group by category to add "Total" lines like in the image
        Map<String, List<AssessmentItem>> itemsByCategory = assessmentItems.stream()
                .collect(Collectors.groupingBy(AssessmentItem::getCategory));

        for (Map.Entry<String, List<AssessmentItem>> entry : itemsByCategory.entrySet()) {
            String category = entry.getKey();
            List<AssessmentItem> items = entry.getValue();

            BigDecimal categoryWeightTotal = BigDecimal.ZERO;
            BigDecimal categoryValueTotal = BigDecimal.ZERO;

            for (AssessmentItem item : items) {
                StudentScore score = scoreMap.get(item.getItemId());
                BigDecimal value = (score != null) ? score.getScoreValue() : null;

                detailedScores.add(StudentAssessmentScoreResponse.builder()
                        .category(category)
                        .itemName(item.getName())
                        .weight(item.getWeight())
                        .value(value)
                        .comment((score != null) ? score.getComment() : null)
                        .isTotal(false)
                        .build());

                categoryWeightTotal = categoryWeightTotal.add(item.getWeight());
                if (value != null) {
                    categoryValueTotal = categoryValueTotal.add(value.multiply(item.getWeight())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                }
            }

            // Add Total line for category
            detailedScores.add(StudentAssessmentScoreResponse.builder()
                    .category(category)
                    .itemName("Total")
                    .weight(categoryWeightTotal)
                    .value(categoryValueTotal)
                    .isTotal(true)
                    .build());
        }

        BigDecimal finalGrade = (grade != null) ? grade.getGradeValue() : null;
        String feedback = (grade != null) ? grade.getFeedback() : null;
        String status = (finalGrade != null && finalGrade.compareTo(BigDecimal.valueOf(5.0)) >= 0) ? "PASSED"
                : (finalGrade != null ? "FAILED" : "IN_PROGRESS");

        return StudentGradeResponse.builder()
                .enrollmentId(enrollment.getEnrollmentId())
                .semesterName(enrollment.getScheduledClass().getSemester().getName())
                .semesterYear(enrollment.getScheduledClass().getSemester().getYear())
                .courseCode(enrollment.getScheduledClass().getCourse().getCode())
                .courseName(enrollment.getScheduledClass().getCourse().getName())
                .classCode("CLASS_" + enrollment.getScheduledClass().getClassId())
                .fromDate(enrollment.getScheduledClass().getSemester().getStartDate())
                .toDate(enrollment.getScheduledClass().getSemester().getEndDate())
                .credits(enrollment.getScheduledClass().getCourse().getCredits())
                .grade(finalGrade)
                .grade4(finalGrade != null ? convertToScale4(finalGrade) : null)
                .status(status)
                .feedback(feedback)
                .assessmentScores(detailedScores)
                .build();
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
                .filter(grade -> grade.getGradeValue() != null)
                .mapToInt(grade -> grade.getEnrollment().getScheduledClass().getCourse().getCredits())
                .sum();

        return StudentTranscriptResponse.builder()
                .grades(gradeResponses)
                .cumulativeGpa(cumulativeGpa)
                .totalCredits(totalCredits)
                .build();
    }

    private StudentGradeResponse mapToGradeResponse(Grade grade) {
        return StudentGradeResponse.builder()
                .enrollmentId(grade.getEnrollment().getEnrollmentId())
                .courseCode(grade.getEnrollment().getScheduledClass().getCourse().getCode())
                .courseName(grade.getEnrollment().getScheduledClass().getCourse().getName())
                .credits(grade.getEnrollment().getScheduledClass().getCourse().getCredits())
                .grade(grade.getGradeValue())
                .grade4(grade.getGradeValue() != null ? convertToScale4(grade.getGradeValue()) : null)
                .status((grade.getGradeValue() != null && grade.getGradeValue().compareTo(BigDecimal.valueOf(5.0)) >= 0)
                        ? "PASSED"
                        : "FAILED")
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
