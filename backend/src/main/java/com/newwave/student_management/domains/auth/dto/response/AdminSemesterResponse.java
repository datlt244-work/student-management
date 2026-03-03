package com.newwave.student_management.domains.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class AdminSemesterResponse {
    private Integer semesterId;
    private String name;
    private Integer year;
    private String displayName;
    private LocalDate startDate;
    private LocalDate endDate;
    @JsonProperty("isCurrent")
    private boolean isCurrent;
    private String enrollmentStatus;
    private LocalDateTime publishedAt;
    /** Phút mà enrollment sẽ tự động đóng = publishedAt + 72h */
    private LocalDateTime enrollmentDeadline;
    private LocalDateTime createdAt;
}
