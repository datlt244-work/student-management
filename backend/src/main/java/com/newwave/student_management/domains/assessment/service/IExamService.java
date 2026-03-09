package com.newwave.student_management.domains.assessment.service;

import com.newwave.student_management.domains.assessment.dto.response.ExamResponse;

import java.util.List;
import java.util.UUID;

public interface IExamService {
    List<ExamResponse> getStudentExamSchedule(UUID userId, Integer semesterId);
}
