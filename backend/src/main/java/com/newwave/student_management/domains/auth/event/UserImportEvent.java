package com.newwave.student_management.domains.auth.event;

import com.newwave.student_management.domains.auth.dto.request.AdminCreateUserRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserImportEvent {
    private UUID jobId;
    private int rowNumber;
    private int totalRows;
    private String rawDataJson; // For tracking failed payload easily without re-parsing
    private AdminCreateUserRequest requestPayload;
}
