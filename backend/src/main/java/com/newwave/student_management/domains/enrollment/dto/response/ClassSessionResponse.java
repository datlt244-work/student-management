package com.newwave.student_management.domains.enrollment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSessionResponse {
    private Long sessionId;
    private Integer roomId;
    private String roomName;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}
