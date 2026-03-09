package com.newwave.student_management.domains.assessment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAssessmentScoreResponse {
    private String category;
    private String itemName;
    private BigDecimal weight;
    private BigDecimal value;
    private String comment;
    @JsonProperty("isTotal")
    private boolean isTotal;
}
