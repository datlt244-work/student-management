package com.newwave.student_management.domains.attendance.dto.response;

import com.newwave.student_management.domains.attendance.entity.AttendanceAppeal.AppealStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttendanceAppealResponse {
    UUID appealId;
    Integer classId;
    String courseName;
    LocalDate attendanceDate;
    String reason;
    String evidenceUrl;
    AppealStatus status;
    String teacherResponse;
    LocalDateTime createdAt;
}
