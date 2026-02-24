package com.newwave.student_management.domains.assessment.service;

import com.newwave.student_management.domains.assessment.dto.response.StudentGradeResponse;
import com.newwave.student_management.domains.assessment.dto.response.StudentTranscriptResponse;

import java.util.List;
import java.util.UUID;

public interface IGradeService {
    List<StudentGradeResponse> getGradesBySemester(UUID userId, Integer semesterId);

    StudentTranscriptResponse getTranscript(UUID userId);
}
