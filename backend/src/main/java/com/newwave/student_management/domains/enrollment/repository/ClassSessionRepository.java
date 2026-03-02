package com.newwave.student_management.domains.enrollment.repository;

import com.newwave.student_management.domains.enrollment.entity.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

        @Query("SELECT COUNT(s) FROM ClassSession s " +
                        "WHERE s.room.roomId = :roomId " +
                        "AND s.dayOfWeek = :dayOfWeek " +
                        "AND (s.startTime < :endTime AND s.endTime > :startTime) " +
                        "AND s.scheduledClass.semester.semesterId = :semesterId " +
                        "AND (:excludeClassId IS NULL OR s.scheduledClass.classId <> :excludeClassId) " +
                        "AND s.scheduledClass.deletedAt IS NULL")
        long countRoomConflicts(
                        @Param("roomId") Integer roomId,
                        @Param("dayOfWeek") Integer dayOfWeek,
                        @Param("startTime") LocalTime startTime,
                        @Param("endTime") LocalTime endTime,
                        @Param("semesterId") Integer semesterId,
                        @Param("excludeClassId") Integer excludeClassId);
}
