package com.newwave.student_management.domains.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSimpleResponse {
    private UUID teacherId;
    private String teacherCode;
    private String firstName;
    private String lastName;
    private String fullName;
    private Integer officeRoomId;
    private String officeRoomName;
}
