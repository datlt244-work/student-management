package com.newwave.student_management.domains.attendance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttendanceAppealRequest {

    @NotNull(message = "CLASS_ID_REQUIRED")
    Integer classId;

    @NotNull(message = "DATE_REQUIRED")
    LocalDate attendanceDate;

    @NotBlank(message = "REASON_REQUIRED")
    String reason;

    @NotBlank(message = "EVIDENCE_URL_REQUIRED")
    String evidenceUrl;
}
