package com.newwave.student_management.domains.profile.dto.response;

import com.newwave.student_management.domains.profile.entity.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileResponse {

    private UUID studentId;
    private String studentCode;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String gender;
    private String major;
    private String phone;
    private String address;
    private BigDecimal gpa;
    private Integer year;
    private String manageClass;
    private DepartmentResponse department;
    private SemesterResponse currentSemester;

    public static StudentProfileResponse fromEntity(Student student) {
        if (student == null) return null;
        return StudentProfileResponse.builder()
                .studentId(student.getStudentId())
                .studentCode(student.getStudentCode())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .dob(student.getDob())
                .gender(student.getGender())
                .major(student.getMajor())
                .phone(student.getPhone())
                .address(student.getAddress())
                .gpa(student.getGpa())
                .year(student.getYear())
                .manageClass(student.getManageClass())
                .department(DepartmentResponse.fromEntity(student.getDepartment()))
                .build();
    }

    public static StudentProfileResponse fromEntity(Student student, SemesterResponse currentSemester) {
        StudentProfileResponse response = fromEntity(student);
        if (response != null) {
            response.setCurrentSemester(currentSemester);
        }
        return response;
    }
}
