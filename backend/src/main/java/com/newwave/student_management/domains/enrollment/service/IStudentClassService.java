package com.newwave.student_management.domains.enrollment.service;

import com.newwave.student_management.domains.enrollment.dto.response.StudentAvailableClassResponse;
import java.util.List;
import java.util.UUID;

public interface IStudentClassService {
    List<StudentAvailableClassResponse> getAvailableClasses(UUID userId);
}
