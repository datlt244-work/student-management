package com.newwave.student_management.domains.profile.dto.response;

import com.newwave.student_management.domains.profile.entity.Department;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {

    private Integer departmentId;
    private String name;
    private String officeLocation;

    public static DepartmentResponse fromEntity(Department department) {
        if (department == null) return null;
        return DepartmentResponse.builder()
                .departmentId(department.getDepartmentId())
                .name(department.getName())
                .officeLocation(department.getOfficeLocation())
                .build();
    }
}
