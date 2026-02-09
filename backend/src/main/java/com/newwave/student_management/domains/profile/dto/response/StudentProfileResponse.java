package com.newwave.student_management.domains.profile.dto.response;

import com.newwave.student_management.domains.profile.entity.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileResponse {

    private UUID studentId;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String phone;
    private String address;
    private DepartmentResponse department;

    public static StudentProfileResponse fromEntity(Student student) {
        if (student == null) return null;
        return StudentProfileResponse.builder()
                .studentId(student.getStudentId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .dob(student.getDob())
                .phone(student.getPhone())
                .address(student.getAddress())
                .department(DepartmentResponse.fromEntity(student.getDepartment()))
                .build();
    }
}
