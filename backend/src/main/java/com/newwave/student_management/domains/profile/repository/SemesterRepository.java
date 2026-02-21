package com.newwave.student_management.domains.profile.repository;

import com.newwave.student_management.domains.profile.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Integer>, JpaSpecificationExecutor<Semester> {

    /**
     * Tìm semester hiện tại (is_current = true)
     */
    Optional<Semester> findByIsCurrentTrue();

    boolean existsByNameAndYear(String name, Integer year);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Semester s WHERE :date BETWEEN s.startDate AND s.endDate")
    java.util.List<Semester> findByDateInRange(java.time.LocalDate date);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Semester s WHERE s.semesterId <> :id AND " +
            "((s.startDate <= :end AND s.endDate >= :start))")
    java.util.List<Semester> findOverlappingSemesters(Integer id, java.time.LocalDate start, java.time.LocalDate end);
}
