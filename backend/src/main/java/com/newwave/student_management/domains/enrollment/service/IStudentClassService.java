package com.newwave.student_management.domains.enrollment.service;

import com.newwave.student_management.domains.enrollment.dto.response.StudentAvailableClassResponse;
import com.newwave.student_management.domains.enrollment.dto.response.StudentEnrolledClassResponse;
import java.util.List;
import java.util.UUID;

public interface IStudentClassService {
    List<StudentAvailableClassResponse> getAvailableClasses(UUID userId);

    List<StudentEnrolledClassResponse> getEnrolledClasses(UUID userId);

    void enroll(UUID userId, Integer classId);

    void unenroll(UUID userId, Integer classId);
}
