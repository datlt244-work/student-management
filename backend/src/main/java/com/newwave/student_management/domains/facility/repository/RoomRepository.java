package com.newwave.student_management.domains.facility.repository;

import com.newwave.student_management.domains.facility.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {
        Optional<Room> findByName(String name);

        List<Room> findAllByDeletedAtIsNull();

        Optional<Room> findByNameAndDeletedAtIsNull(String name);

        @Query("SELECT r FROM Room r " +
                        "WHERE (:search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) "
                        +
                        "AND (:roomType IS NULL OR r.roomType = :roomType) " +
                        "AND r.deletedAt IS NULL")
        Page<Room> searchRooms(
                        @Param("search") String search,
                        @Param("roomType") com.newwave.student_management.domains.facility.entity.RoomType roomType,
                        Pageable pageable);
}
