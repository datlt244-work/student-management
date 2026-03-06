package com.newwave.student_management.domains.attendance.dto.response;

import com.newwave.student_management.domains.attendance.entity.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordResponse {
    private LocalDate date;
    private AttendanceStatus status;
    private LocalDateTime recordTime;
}
