package com.newwave.student_management.domains.profile.dto.response;

import com.newwave.student_management.domains.profile.entity.Teacher;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherProfileResponse {

    private UUID teacherId;
    private String firstName;
    private String lastName;
    private String phone;
    private String specialization;
    private DepartmentResponse department;

    public static TeacherProfileResponse fromEntity(Teacher teacher) {
        if (teacher == null) return null;
        return TeacherProfileResponse.builder()
                .teacherId(teacher.getTeacherId())
                .firstName(teacher.getFirstName())
                .lastName(teacher.getLastName())
                .phone(teacher.getPhone())
                .specialization(teacher.getSpecialization())
                .department(DepartmentResponse.fromEntity(teacher.getDepartment()))
                .build();
    }
}
