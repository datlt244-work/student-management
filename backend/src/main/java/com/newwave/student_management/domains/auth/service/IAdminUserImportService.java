package com.newwave.student_management.domains.auth.service;

public interface IAdminUserImportService {
    byte[] generateTeacherTemplate();

    byte[] generateStudentTemplate();

    void triggerBatchImport(org.springframework.web.multipart.MultipartFile file, String role);
}
