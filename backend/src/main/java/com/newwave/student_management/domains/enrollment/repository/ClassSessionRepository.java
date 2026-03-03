package com.newwave.student_management.domains.enrollment.repository;

import com.newwave.student_management.domains.enrollment.entity.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    /**
     * Count room conflicts: checks if a room is already booked for overlapping time
     * in the same semester (optionally excluding a given class).
     */
    @Query("SELECT COUNT(cs) FROM ClassSession cs " +
            "WHERE cs.room.roomId = :roomId " +
            "AND cs.dayOfWeek = :dayOfWeek " +
            "AND cs.startTime < :endTime " +
            "AND cs.endTime > :startTime " +
            "AND cs.scheduledClass.semester.semesterId = :semesterId " +
            "AND cs.scheduledClass.deletedAt IS NULL " +
            "AND (:excludeClassId IS NULL OR cs.scheduledClass.classId <> :excludeClassId)")
    long countRoomConflicts(
            @Param("roomId") Integer roomId,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("semesterId") Integer semesterId,
            @Param("excludeClassId") Integer excludeClassId);
}
