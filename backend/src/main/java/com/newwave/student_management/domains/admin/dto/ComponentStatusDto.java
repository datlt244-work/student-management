package com.newwave.student_management.domains.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentStatusDto {

    /**
     * Trạng thái thành phần: UP / DOWN / DEGRADED / UNKNOWN
     */
    private String status;

    /**
     * Mô tả chi tiết / thông điệp cho admin.
     */
    private String message;

    /**
     * Độ trễ (ms) nếu có thể đo, null nếu không áp dụng.
     */
    private Long latencyMs;
}


