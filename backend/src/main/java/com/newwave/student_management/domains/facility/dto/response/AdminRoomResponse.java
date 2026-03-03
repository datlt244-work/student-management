package com.newwave.student_management.domains.facility.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRoomResponse {

    private Integer roomId;
    private String roomName;
    private Integer capacity;
    private String roomType;
    private String notes;
    private Boolean status;
    private String assignedTeacherId;
    private String assignedTeacherName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
