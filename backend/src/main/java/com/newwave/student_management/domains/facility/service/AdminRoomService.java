package com.newwave.student_management.domains.facility.service;

import com.newwave.student_management.domains.facility.dto.request.AdminCreateRoomRequest;
import com.newwave.student_management.domains.facility.dto.request.AdminUpdateRoomRequest;
import com.newwave.student_management.domains.facility.dto.response.AdminRoomResponse;
import com.newwave.student_management.domains.facility.dto.response.AdminRoomListResponse;
import org.springframework.data.domain.Pageable;

public interface AdminRoomService {

    AdminRoomListResponse getRooms(String search, String roomType, Pageable pageable);

    AdminRoomResponse getRoomDetail(Integer roomId);

    AdminRoomResponse createRoom(AdminCreateRoomRequest request);

    AdminRoomResponse updateRoom(Integer roomId, AdminUpdateRoomRequest request);

    void deleteRoom(Integer roomId);
}
