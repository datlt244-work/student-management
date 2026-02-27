package com.newwave.student_management.domains.auth.service;

public interface IAdminUserImportService {
    byte[] generateTeacherTemplate();

    byte[] generateStudentTemplate();
}
