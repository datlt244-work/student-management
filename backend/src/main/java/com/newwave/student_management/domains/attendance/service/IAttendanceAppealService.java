package com.newwave.student_management.domains.attendance.service;

import com.newwave.student_management.domains.attendance.dto.request.AttendanceAppealRequest;
import com.newwave.student_management.domains.attendance.dto.response.AttendanceAppealResponse;
import com.newwave.student_management.domains.attendance.dto.response.EvidenceUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface IAttendanceAppealService {
    AttendanceAppealResponse submitAppeal(UUID userId, AttendanceAppealRequest request);

    List<AttendanceAppealResponse> getStudentAppeals(UUID userId);

    EvidenceUploadResponse uploadEvidence(UUID userId, MultipartFile file);
}
