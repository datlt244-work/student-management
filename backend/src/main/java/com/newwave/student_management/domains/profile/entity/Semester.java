package com.newwave.student_management.domains.profile.entity;

import com.newwave.student_management.common.entity.JpaBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "semesters", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "year" })
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Semester extends JpaBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "semester_id")
    private Integer semesterId;

    /**
     * Tên kỳ: SPRING, SUMMER, FALL
     */
    @Column(name = "name", length = 20, nullable = false)
    private String name;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_current")
    private boolean isCurrent;

    /**
     * Display format: "Spring 2026", "Summer 2025", etc.
     */
    public String getDisplayName() {
        String capitalized = name.charAt(0) + name.substring(1).toLowerCase();
        return capitalized + " " + year;
    }
}
