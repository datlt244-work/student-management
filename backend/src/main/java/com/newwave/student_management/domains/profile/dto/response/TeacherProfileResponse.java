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
    private String teacherCode;
    private String firstName;
    private String lastName;
    private String phone;
    private String specialization;
    private String academicRank;
    private String officeRoom;
    private DepartmentResponse department;

    public static TeacherProfileResponse fromEntity(Teacher teacher) {
        if (teacher == null) return null;
        return TeacherProfileResponse.builder()
                .teacherId(teacher.getTeacherId())
                .teacherCode(teacher.getTeacherCode())
                .firstName(teacher.getFirstName())
                .lastName(teacher.getLastName())
                .phone(teacher.getPhone())
                .specialization(teacher.getSpecialization())
                .academicRank(teacher.getAcademicRank())
                .officeRoom(teacher.getOfficeRoom())
                .department(DepartmentResponse.fromEntity(teacher.getDepartment()))
                .build();
    }
}
