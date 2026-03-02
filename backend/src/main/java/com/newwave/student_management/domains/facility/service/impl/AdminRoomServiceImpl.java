package com.newwave.student_management.domains.facility.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.facility.dto.request.AdminCreateRoomRequest;
import com.newwave.student_management.domains.facility.dto.request.AdminUpdateRoomRequest;
import com.newwave.student_management.domains.facility.dto.response.AdminRoomResponse;
import com.newwave.student_management.domains.facility.entity.Room;
import com.newwave.student_management.domains.facility.repository.RoomRepository;
import com.newwave.student_management.domains.facility.service.AdminRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.newwave.student_management.domains.facility.dto.response.AdminRoomListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRoomServiceImpl implements AdminRoomService {

    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminRoomListResponse getRooms(String search, String roomType, Pageable pageable) {
        log.info("Fetching rooms with search: {}, roomType: {}", search, roomType);
        com.newwave.student_management.domains.facility.entity.RoomType typeEnum = null;
        if (roomType != null && !roomType.isBlank()) {
            try {
                typeEnum = com.newwave.student_management.domains.facility.entity.RoomType.valueOf(roomType);
            } catch (IllegalArgumentException e) {
                // Ignore invalid room type, or throw exception. We pass null to skip filter or
                // we could throw error.
            }
        }
        Page<AdminRoomResponse> roomPage = roomRepository.searchRooms(search, typeEnum, pageable)
                .map(this::mapToResponse);

        return AdminRoomListResponse.builder()
                .content(roomPage.getContent())
                .page(roomPage.getNumber() + 1)
                .size(roomPage.getSize())
                .totalElements(roomPage.getTotalElements())
                .totalPages(roomPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminRoomResponse getRoomDetail(Integer roomId) {
        log.info("Fetching room detail for ID: {}", roomId);
        Room room = getRoomEntity(roomId);
        return mapToResponse(room);
    }

    @Override
    @Transactional
    public AdminRoomResponse createRoom(AdminCreateRoomRequest request) {
        log.info("Creating new room: {}", request.getRoomName());

        if (roomRepository.findByName(request.getRoomName()).isPresent()) {
            throw new AppException(ErrorCode.ROOM_EXISTS);
        }

        Room room = new Room();
        room.setName(request.getRoomName());
        room.setCapacity(request.getCapacity());
        room.setRoomType(
                com.newwave.student_management.domains.facility.entity.RoomType.valueOf(request.getRoomType()));
        // Note: 'notes' and 'status' domains don't exist in Room.java currently. Has
        // project feature instead

        Room savedRoom = roomRepository.save(room);
        return mapToResponse(savedRoom);
    }

    @Override
    @Transactional
    public AdminRoomResponse updateRoom(Integer roomId, AdminUpdateRoomRequest request) {
        log.info("Updating room with ID: {}", roomId);

        Room room = getRoomEntity(roomId);

        // Check name clash if name is changed
        if (!room.getName().equals(request.getRoomName()) &&
                roomRepository.findByName(request.getRoomName()).isPresent()) {
            throw new AppException(ErrorCode.ROOM_EXISTS);
        }

        room.setName(request.getRoomName());
        room.setCapacity(request.getCapacity());
        room.setRoomType(
                com.newwave.student_management.domains.facility.entity.RoomType.valueOf(request.getRoomType()));

        Room updatedRoom = roomRepository.save(room);
        return mapToResponse(updatedRoom);
    }

    @Override
    @Transactional
    public void deleteRoom(Integer roomId) {
        log.info("Deleting room with ID: {}", roomId);
        Room room = getRoomEntity(roomId);

        if (room.getAssignedTeacher() != null) {
            throw new AppException(ErrorCode.ROOM_HAS_ASSIGNED_TEACHER);
        }

        room.setDeletedAt(LocalDateTime.now());
        roomRepository.save(room);
    }

    private Room getRoomEntity(Integer roomId) {
        return roomRepository.findById(roomId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private AdminRoomResponse mapToResponse(Room room) {
        String teacherId = null;
        String teacherName = null;
        if (room.getAssignedTeacher() != null) {
            teacherId = room.getAssignedTeacher().getTeacherId().toString();
            teacherName = room.getAssignedTeacher().getFirstName() + " " + room.getAssignedTeacher().getLastName();
        }
        return AdminRoomResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getName())
                .capacity(room.getCapacity())
                .roomType(room.getRoomType() != null ? room.getRoomType().name() : null)
                .assignedTeacherId(teacherId)
                .assignedTeacherName(teacherName)
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}
