package com.newwave.student_management.domains.facility.entity;

import com.newwave.student_management.common.entity.JpaBaseEntity;
import com.newwave.student_management.domains.profile.entity.Teacher;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rooms")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Room extends JpaBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Integer roomId;

    @Column(name = "name", length = 50, unique = true, nullable = false)
    private String name;

    @Column(name = "capacity", nullable = false)
    private Integer capacity = 40;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", length = 20, nullable = false)
    private RoomType roomType;

    @Column(name = "has_projector")
    private Boolean hasProjector = true;

    /**
     * The teacher assigned to this room as their office.
     * Null means the room is available.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_teacher_id")
    private Teacher assignedTeacher;
}
