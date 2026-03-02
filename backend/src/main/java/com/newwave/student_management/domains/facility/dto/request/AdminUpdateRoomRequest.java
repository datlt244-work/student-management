package com.newwave.student_management.domains.facility.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateRoomRequest {

    @NotBlank(message = "Room name is required")
    private String roomName;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private String roomType;

    private String notes;

    // Defaulting to true, or update as needed
    private Boolean status;
}
