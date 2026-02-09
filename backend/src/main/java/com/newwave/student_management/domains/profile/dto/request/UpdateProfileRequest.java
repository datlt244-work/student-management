package com.newwave.student_management.domains.profile.dto.request;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    /**
     * Cập nhật student profile (chỉ hợp lệ khi role = STUDENT)
     */
    @Valid
    private UpdateStudentProfileRequest studentProfile;

    /**
     * Cập nhật teacher profile (chỉ hợp lệ khi role = TEACHER)
     */
    @Valid
    private UpdateTeacherProfileRequest teacherProfile;
}
