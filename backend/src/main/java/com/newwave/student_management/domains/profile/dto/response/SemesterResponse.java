package com.newwave.student_management.domains.profile.dto.response;

import com.newwave.student_management.domains.profile.entity.Semester;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemesterResponse {

    private Integer semesterId;
    private String name;
    private Integer year;
    private String displayName;
    private LocalDate startDate;
    private LocalDate endDate;

    public static SemesterResponse fromEntity(Semester semester) {
        if (semester == null) return null;
        return SemesterResponse.builder()
                .semesterId(semester.getSemesterId())
                .name(semester.getName())
                .year(semester.getYear())
                .displayName(semester.getDisplayName())
                .startDate(semester.getStartDate())
                .endDate(semester.getEndDate())
                .build();
    }
}
