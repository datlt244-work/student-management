package com.newwave.student_management.domains.enrollment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSessionRequest {

    @NotNull(message = "Room is required")
    private Integer roomId;

    @NotNull(message = "Day of week is required")
    private Integer dayOfWeek; // 1-7 (Monday-Sunday)

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;
}
